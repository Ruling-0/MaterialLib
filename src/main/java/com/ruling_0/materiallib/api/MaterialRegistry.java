package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

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

    private final Map<String, Material> materials = new LinkedHashMap<>();
    private final Map<String, Family> families = new LinkedHashMap<>();
    private final List<PendingOp> pendingOps = new ArrayList<>();
    private boolean resolved;

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

    /// All registered materials. Only available after the registry has resolved; during registration the view
    /// would be incomplete.
    public Collection<Material> getMaterials() {
        requireResolved("list registered materials", "");
        return Collections.unmodifiableCollection(materials.values());
    }

    /// All registered families. Only available after the registry has resolved; during registration the view
    /// would be incomplete.
    public Collection<Family> getFamilies() {
        requireResolved("list registered families", "");
        return Collections.unmodifiableCollection(families.values());
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

        Map<Family, Set<Material>> membership = new LinkedHashMap<>();
        for (Family family : families.values()) {
            membership.put(family, new LinkedHashSet<>());
        }
        for (Material material : materials.values()) {
            material.resolveFamilies();
            for (Family family : material.getSortedFamiliesInternal()) {
                membership.get(family).add(material);
            }
        }
        for (Map.Entry<Family, Set<Material>> entry : membership.entrySet()) {
            entry.getKey().setMembersInternal(entry.getValue());
        }
        for (Material material : materials.values()) {
            logPropertyCollisions(material);
            material.resolveShapes();
        }

        resolved = true;
        LOG.info("Resolved {} materials and {} families", materials.size(), families.size());
    }

    /// Logs each property whose resolved value is ambiguous for a material: the material does not set it, and
    /// two or more of its families set conflicting values. The alphabetically-first family still wins.
    private void logPropertyCollisions(Material material) {
        List<Family> sorted = material.getSortedFamiliesInternal();
        if (sorted.size() < 2) return;
        Map<Property<?>, Family> firstSetters = new LinkedHashMap<>();
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
