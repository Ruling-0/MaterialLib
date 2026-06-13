package com.ruling_0.materiallib.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// The broker holding every registered [Material] and [Family].
///
/// The registry has two phases. During registration (all preInit handlers), mods register materials and families
/// through the builders and queue cross-mod changes through [MaterialEdit] and [FamilyEdit]; key lookups return
/// the registered objects, but their membership, shapes, and properties cannot be read yet, and neither can the
/// bulk collection views. During this mod's init handler, [#resolve] applies all queued edits in call order,
/// derives family membership and effective shape sets, and freezes the registry. From then on everything is
/// readable and nothing can be registered or edited, which guarantees dependent mods a complete registry in
/// their init and postInit handlers.
///
/// The game uses the single [#instance]; tests construct private registries directly.
public final class MaterialRegistry {

    private static final Logger LOG = LogManager.getLogger("materiallib");
    private static final MaterialRegistry INSTANCE = new MaterialRegistry();

    private static final Comparator<Material> MATERIAL_KEY_ORDER = Comparator.comparing(Material::getKey);

    private final Map<String, Material> materials = new Object2ObjectLinkedOpenHashMap<>();
    private final Map<String, Family> families = new Object2ObjectLinkedOpenHashMap<>();
    private final List<PendingOp> pendingOps = new ObjectArrayList<>();
    private boolean resolved;
    private Collection<Material> materialsView;
    private Collection<Family> familiesView;
    private Material[] materialsByIndex;

    MaterialRegistry() {}

    /// The registry instance the game runs on, also reachable through [MaterialLibAPI].
    public static MaterialRegistry instance() {
        return INSTANCE;
    }

    public MaterialBuilder newMaterial(String modid, String name, TextureSet textureSet) {
        return new MaterialBuilder(this, modid, name, textureSet);
    }

    public FamilyBuilder newFamily(String modid, String name) {
        return new FamilyBuilder(this, modid, name);
    }

    public MaterialEdit editMaterial(String modid, String name) {
        return new MaterialEdit(this, modid, name);
    }

    public FamilyEdit editFamily(String modid, String name) {
        return new FamilyEdit(this, modid, name);
    }

    /// The material with the given key, or null if none exists. Usable during registration, though the returned
    /// material is only readable after the registry resolves.
    public Material getMaterial(String modid, String name) {
        return materials.get(Names.key(modid, name));
    }

    /// The family with the given key, or null if none exists. Usable during registration, though the returned
    /// family is only readable after the registry resolves.
    public Family getFamily(String modid, String name) {
        return families.get(Names.key(modid, name));
    }

    /// The material assigned the given global index (see [Material#getIndex]), or null if no material has it.
    /// Resolves an item damage value back to its material for rendering, naming, and worldgen. Only available
    /// after the registry has resolved.
    public Material getMaterialByIndex(int index) {
        requireResolved("look up a material by index", "");
        return index >= 0 && index < materialsByIndex.length ? materialsByIndex[index] : null;
    }

    /// All registered materials. Only available after the registry has resolved; during registration the view
    /// would be incomplete.
    public Collection<Material> getMaterials() {
        requireResolved("list registered materials", "");
        return materialsView;
    }

    /// All registered families. Only available after the registry has resolved; during registration the view
    /// would be incomplete.
    public Collection<Family> getFamilies() {
        requireResolved("list registered families", "");
        return familiesView;
    }

    public boolean isResolved() { return resolved; }

    /// Applies all queued edits in call order, derives family membership and per-material shape sets, and
    /// freezes the registry.
    ///
    /// This is the lifecycle entry point invoked once by MaterialLib's own init handler. Calling it from any
    /// other mod freezes the registry early and breaks registration for every mod that has not finished its
    /// preInit, so other mods must never call it.
    public void resolve() {
        requireRegistration("resolve the registry");
        for (PendingOp op : pendingOps) {
            try {
                op.action.run();
            }
            catch (RuntimeException e) {
                throw new IllegalStateException("Failed to apply queued edit \"" + op.description + "\"", e);
            }
        }
        pendingOps.clear();

        assignMaterialIndices();

        Map<Family, Set<Material>> membership = new Reference2ObjectLinkedOpenHashMap<>();
        for (Family family : families.values()) {
            membership.put(family, new ReferenceLinkedOpenHashSet<>());
        }
        for (Material material : materials.values()) {
            material.resolveFamilies();
            for (Family family : material.getSortedFamiliesInternal()) {
                membership.get(family).add(material);
            }
        }
        for (Map.Entry<Family, Set<Material>> entry : membership.entrySet()) {
            entry.getKey().resolveMembers(entry.getValue());
        }
        for (Material material : materials.values()) {
            logPropertyCollisions(material);
            material.resolveShapes();
        }
        materialsView = Collections.unmodifiableCollection(materials.values());
        familiesView = Collections.unmodifiableCollection(families.values());

        resolved = true;
        LOG.info("Resolved {} materials and {} families", materials.size(), families.size());
    }

    /// Numbers every registered material from 0 in ascending `modid:name` key order. The index becomes the item
    /// damage in every shape and the worldgen id, so the ordering must be deterministic; sorting by key gives the
    /// same assignment on every launch from the same material set. Feature 3 will make this append-only and
    /// persistent so the numbering survives materials being added or removed.
    private void assignMaterialIndices() {
        materialsByIndex = materials.values().toArray(new Material[0]);
        Arrays.sort(materialsByIndex, MATERIAL_KEY_ORDER);
        for (int i = 0; i < materialsByIndex.length; i++) {
            materialsByIndex[i].resolveIndex(i);
        }
    }

    /// Logs each property whose resolved value is ambiguous for a material: the material does not set it, and
    /// two or more of its families set conflicting values. The alphabetically-first family still wins.
    private void logPropertyCollisions(Material material) {
        Family[] sorted = material.getSortedFamiliesInternal();
        if (sorted.length < 2) return;
        Map<Property<?>, Family> firstSetters = new Reference2ObjectLinkedOpenHashMap<>();
        for (Family family : sorted) {
            for (Map.Entry<Property<?>, Object> entry : family.getOwnPropertiesInternal().entrySet()) {
                Property<?> property = entry.getKey();
                if (material.getOwnPropertiesInternal().containsKey(property)) continue;
                Family first = firstSetters.putIfAbsent(property, family);
                if (first != null && !entry.getValue().equals(first.getOwnPropertiesInternal().get(property))) {
                    LOG.warn(
                        "Material {} takes {} = {} from family {}; family {} sets conflicting value {}",
                        material.getKey(),
                        property,
                        first.getOwnPropertiesInternal().get(property),
                        first.getKey(),
                        family.getKey(),
                        entry.getValue());
                }
            }
        }
    }

    void register(Material material) {
        requireRegistration("register material " + material.getKey());
        Material existing = materials.putIfAbsent(material.getKey(), material);
        if (existing != null) {
            throw new IllegalStateException("Material " + material.getKey() + " is already registered");
        }
    }

    void register(Family family) {
        requireRegistration("register family " + family.getKey());
        Family existing = families.putIfAbsent(family.getKey(), family);
        if (existing != null) {
            throw new IllegalStateException("Family " + family.getKey() + " is already registered");
        }
    }

    void enqueueMaterialOp(String modid, String name, String description, Consumer<Material> op) {
        String key = Names.key(modid, name);
        enqueue(description + " " + key, () -> {
            Material material = materials.get(key);
            if (material == null) {
                LOG.warn("Skipping edit \"{} {}\": no such material is registered", description, key);
                return;
            }
            op.accept(material);
        });
    }

    void enqueueFamilyOp(String modid, String name, String description, Consumer<Family> op) {
        String key = Names.key(modid, name);
        enqueue(description + " " + key, () -> {
            Family family = families.get(key);
            if (family == null) {
                LOG.warn("Skipping edit \"{} {}\": no such family is registered", description, key);
                return;
            }
            op.accept(family);
        });
    }

    void enqueueAddToFamily(String materialModid, String materialName, String familyModid, String familyName) {
        String familyKey = Names.key(familyModid, familyName);
        enqueueMaterialOp(materialModid, materialName, "add to family " + familyKey + " material", material -> {
            Family family = families.get(familyKey);
            if (family == null) {
                LOG.warn(
                    "Skipping family addition for material {}: no such family {} is registered",
                    material.getKey(),
                    familyKey);
                return;
            }
            material.addFamilyInternal(family);
        });
    }

    void enqueueRemoveFromFamily(String materialModid, String materialName, String familyModid, String familyName) {
        String familyKey = Names.key(familyModid, familyName);
        enqueueMaterialOp(materialModid, materialName, "remove from family " + familyKey + " material", material -> {
            Family family = families.get(familyKey);
            if (family == null) {
                LOG.warn(
                    "Skipping family removal for material {}: no such family {} is registered",
                    material.getKey(),
                    familyKey);
                return;
            }
            if (!material.isMemberOfInternal(family)) {
                LOG.warn(
                    "Skipping family removal for material {}: it is not a member of {} at this point in the edit order",
                    material.getKey(),
                    familyKey);
                return;
            }
            material.removeFamilyInternal(family);
        });
    }

    private void enqueue(String description, Runnable action) {
        requireRegistration(description);
        pendingOps.add(new PendingOp(description, action));
    }

    void requireResolved(String action, String target) {
        if (!resolved) {
            throw new IllegalStateException(
                "Cannot " + action + target + ": the material registry has not resolved yet. " +
                    "Registry contents are readable from init onwards in mods depending on materiallib");
        }
    }

    private void requireRegistration(String what) {
        if (resolved) {
            throw new IllegalStateException(
                "Cannot " + what + ": the material registry has already resolved. " +
                    "Materials, families, and edits must be registered during preInit");
        }
    }

    private record PendingOp(String description, Runnable action) {}
}
