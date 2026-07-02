package com.ruling_0.materiallib.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;

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

    private static final MaterialRegistry INSTANCE = new MaterialRegistry();

    private final Map<String, Material> materials = new Object2ObjectLinkedOpenHashMap<>();
    private final Map<String, Family> families = new Object2ObjectLinkedOpenHashMap<>();
    private final List<PendingOp> pendingOps = new ObjectArrayList<>();
    private boolean resolved;
    private Collection<Material> materialsView;
    private Collection<Family> familiesView;
    private Material[] materialsByIndex;
    private Map<String, Integer> persistedIndices = new LinkedHashMap<>();
    private Map<String, Integer> assignedIndices = new LinkedHashMap<>();

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

    /// The material assigned the given global index (see [Material#getIndex]), or null if none has it. Only
    /// available after the registry has resolved.
    public Material getMaterialByIndex(int index) {
        requireResolved("look up a material by index", "");
        return index >= 0 && index < materialsByIndex.length ? materialsByIndex[index] : null;
    }

    /// All registered materials. Only available after the registry has resolved.
    public Collection<Material> getMaterials() {
        requireResolved("list registered materials", "");
        return materialsView;
    }

    /// All registered families. Only available after the registry has resolved.
    public Collection<Family> getFamilies() {
        requireResolved("list registered families", "");
        return familiesView;
    }

    public boolean isResolved() { return resolved; }

    /// Applies all queued edits in call order, derives family membership and per-material shape sets, and
    /// freezes the registry.
    ///
    /// Invoked once by MaterialLib's init handler; other mods must not call it.
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
        MaterialLib.LOG.info("Resolved {} materials and {} families", materials.size(), families.size());
    }

    /// Sets the persisted index assignment to honor at resolve, loaded from the instance-global store. Existing
    /// materials keep their stored index; only genuinely new materials are numbered. Must be set before resolve.
    void setPersistedIndices(Map<String, Integer> indices) {
        requireRegistration("set persisted material indices");
        this.persistedIndices = new LinkedHashMap<>(indices);
    }

    /// The full index assignment after resolve: every persisted entry (including indices reserved for materials
    /// not registered this session) plus the indices newly assigned this session. Written back to the store.
    Map<String, Integer> getAssignedIndices() {
        requireResolved("read assigned material indices", "");
        return Collections.unmodifiableMap(assignedIndices);
    }

    /// Assigns each material its global index, append-only against the persisted assignment: a material already in
    /// the store keeps its index, genuinely new materials take the next free indices in ascending `modid:name` key
    /// order, and indices of materials no longer present stay reserved (never reused) so existing item stacks do
    /// not change material. The index becomes the item damage in every shape and the worldgen id.
    private void assignMaterialIndices() {
        assignedIndices = new LinkedHashMap<>(persistedIndices);
        int next = 0;
        for (int index : assignedIndices.values()) {
            next = Math.max(next, index + 1);
        }
        List<String> newKeys = new ObjectArrayList<>();
        for (String key : materials.keySet()) {
            if (!assignedIndices.containsKey(key)) newKeys.add(key);
        }
        Collections.sort(newKeys);
        for (String key : newKeys) {
            assignedIndices.put(key, next++);
        }
        if (next - 1 > Short.MAX_VALUE) {
            throw new IllegalStateException(
                "Material index " + (next - 1) + " exceeds the item damage limit of " + Short.MAX_VALUE +
                    "; too many materials have been registered across this instance's history.");
        }

        materialsByIndex = new Material[next];
        for (Map.Entry<String, Integer> entry : assignedIndices.entrySet()) {
            Material material = materials.get(entry.getKey());
            if (material != null) {
                material.resolveIndex(entry.getValue());
                materialsByIndex[entry.getValue()] = material;
            }
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
                    MaterialLib.LOG.warn(
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
        enqueueOp(materials, "Skipping edit \"{} {}\": no such material is registered", modid, name, description, op);
    }

    void enqueueFamilyOp(String modid, String name, String description, Consumer<Family> op) {
        enqueueOp(families, "Skipping edit \"{} {}\": no such family is registered", modid, name, description, op);
    }

    private <T> void enqueueOp(Map<String, T> table, String missingWarning, String modid, String name,
                               String description, Consumer<T> op) {
        String key = Names.key(modid, name);
        enqueue(description + " " + key, () -> {
            T target = table.get(key);
            if (target == null) {
                MaterialLib.LOG.warn(missingWarning, description, key);
                return;
            }
            op.accept(target);
        });
    }

    void enqueueAddToFamily(String materialModid, String materialName, String familyModid, String familyName) {
        String familyKey = Names.key(familyModid, familyName);
        enqueueMaterialOp(materialModid, materialName, "add to family " + familyKey + " material", material -> {
            Family family = familyForEdit(familyKey, "addition", material);
            if (family == null) return;
            material.addFamilyInternal(family);
        });
    }

    void enqueueRemoveFromFamily(String materialModid, String materialName, String familyModid, String familyName) {
        String familyKey = Names.key(familyModid, familyName);
        enqueueMaterialOp(materialModid, materialName, "remove from family " + familyKey + " material", material -> {
            Family family = familyForEdit(familyKey, "removal", material);
            if (family == null) return;
            if (!material.isMemberOfInternal(family)) {
                MaterialLib.LOG.warn(
                    "Skipping family removal for material {}: it is not a member of {} at this point in the edit order",
                    material.getKey(),
                    familyKey);
                return;
            }
            material.removeFamilyInternal(family);
        });
    }

    /// The family for a queued family membership edit, or null after logging a skip warning when `familyKey` is
    /// not registered.
    private Family familyForEdit(String familyKey, String action, Material material) {
        Family family = families.get(familyKey);
        if (family == null) {
            MaterialLib.LOG.warn(
                "Skipping family {} for material {}: no such family {} is registered",
                action,
                material.getKey(),
                familyKey);
        }
        return family;
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
