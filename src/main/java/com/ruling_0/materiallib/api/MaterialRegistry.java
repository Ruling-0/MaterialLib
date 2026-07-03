package com.ruling_0.materiallib.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

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
/// bulk collection views. During this mod's init handler, [#resolve] merges same-name materials onto their
/// owners, applies all queued edits in call order, derives family membership and per-material shape sets, and
/// freezes the registry. From then on everything is readable and nothing can be registered or edited, which
/// guarantees dependent mods a complete registry in their init and postInit handlers.
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
    private final Map<String, Material> aliasKeys = new Object2ObjectLinkedOpenHashMap<>();
    private Map<String, String> persistedOwners = new LinkedHashMap<>();
    private Map<String, String> assignedOwners = new LinkedHashMap<>();

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
    /// material is only readable after the registry resolves. A key whose material unified onto another mod's
    /// returns the unified material.
    public Material getMaterial(String modid, String name) {
        return materialByKey(Names.key(modid, name));
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

    /// Ends registration and freezes the registry, as described in the class doc. Invoked once by MaterialLib's
    /// init handler; other mods must not call it.
    public void resolve() {
        requireRegistration("resolve the registry");
        unifyMaterials();
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

    /// Collapses materials that share a name down to one canonical material with one owning mod, folding each
    /// non-owning declaration into the owner and rerouting its key. The owner is chosen here rather than at
    /// registration so it is independent of mod load order, and the persisted owner keeps the choice stable when
    /// another mod declaring the same name is added.
    private void unifyMaterials() {
        Map<String, List<Material>> candidatesByName = new Object2ObjectLinkedOpenHashMap<>();
        for (Material material : materials.values()) {
            candidatesByName.computeIfAbsent(material.getName(), name -> new ObjectArrayList<>())
                .add(material);
        }
        assignedOwners = new LinkedHashMap<>(persistedOwners);
        for (Map.Entry<String, List<Material>> entry : candidatesByName.entrySet()) {
            String name = entry.getKey();
            List<Material> candidates = entry.getValue();
            String ownerModid = OwnerElection
                .choose("Material", name, candidates, Material::getModId, persistedOwners.get(name), "declared");
            assignedOwners.put(name, ownerModid);
            if (candidates.size() == 1) continue;
            candidates.sort(Comparator.comparing(Material::getModId));
            Material winner = null;
            for (Material candidate : candidates) {
                if (candidate.getModId()
                    .equals(ownerModid)) winner = candidate;
            }
            for (Material loser : candidates) {
                if (loser == winner) continue;
                materials.remove(loser.getKey());
                aliasKeys.put(loser.getKey(), winner);
                MaterialLib.LOG.info("Unified material {}:{} onto owner {}", loser.getModId(), name, ownerModid);
                winner.mergeFrom(loser);
            }
        }
    }

    /// Sets the persisted index assignment to honor at resolve, loaded from the instance-global store. Must be
    /// set before resolve.
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

    /// Sets the persisted material owners to honor at resolve, loaded from the instance-global store. Must be
    /// set before resolve.
    void setPersistedOwners(Map<String, String> owners) {
        requireRegistration("set persisted material owners");
        this.persistedOwners = new LinkedHashMap<>(owners);
    }

    /// The full owner assignment after resolve: every persisted entry (including names with no declaration this
    /// session) plus the owners chosen this session. Written back to the store.
    Map<String, String> getAssignedOwners() {
        requireResolved("read assigned material owners", "");
        return Collections.unmodifiableMap(assignedOwners);
    }

    /// The full index assignment rendered as a CSV table for debugging: one row per assigned index,
    /// including indices reserved for materials not loaded this session, with the assigned owner and,
    /// for loaded materials, shapes and families. Only available after the registry has resolved.
    public String dumpCsv() {
        return MaterialCsv.dump(this);
    }

    /// Assigns each material its global index, append-only against the persisted assignment: a material already in
    /// the store keeps its index, genuinely new materials take the next free indices in ascending name order, and
    /// indices of materials no longer present stay reserved (never reused) so existing item stacks do not change
    /// material. The index becomes the item damage in every shape and the worldgen id.
    private void assignMaterialIndices() {
        Map<String, Material> byName = new Object2ObjectLinkedOpenHashMap<>();
        for (Material material : materials.values()) {
            byName.put(material.getName(), material);
        }
        assignedIndices = new LinkedHashMap<>(persistedIndices);
        int next = 0;
        for (int index : assignedIndices.values()) {
            next = Math.max(next, index + 1);
        }
        List<String> newNames = new ObjectArrayList<>();
        for (String name : byName.keySet()) {
            if (!assignedIndices.containsKey(name)) newNames.add(name);
        }
        Collections.sort(newNames);
        for (String name : newNames) {
            assignedIndices.put(name, next++);
        }
        if (next - 1 > Short.MAX_VALUE) {
            throw new IllegalStateException(
                "Material index " + (next - 1) + " exceeds the item damage limit of " + Short.MAX_VALUE +
                    "; too many materials have been registered across this instance's history.");
        }

        materialsByIndex = new Material[next];
        for (Map.Entry<String, Integer> entry : assignedIndices.entrySet()) {
            Material material = byName.get(entry.getKey());
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
        enqueueOp(
            this::materialByKey,
            "Skipping edit \"{} {}\": no such material is registered",
            modid,
            name,
            description,
            op);
    }

    void enqueueFamilyOp(String modid, String name, String description, Consumer<Family> op) {
        enqueueOp(families::get, "Skipping edit \"{} {}\": no such family is registered", modid, name, description, op);
    }

    /// The material registered under a key, or the unified material a merged key was folded into.
    private Material materialByKey(String key) {
        Material material = materials.get(key);
        return material != null ? material : aliasKeys.get(key);
    }

    private <T> void enqueueOp(Function<String, T> lookup, String missingWarning, String modid, String name,
                               String description, Consumer<T> op) {
        String key = Names.key(modid, name);
        enqueue(description + " " + key, () -> {
            T target = lookup.apply(key);
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
                    "Registry contents are readable once MaterialLib's preInit has resolved the registry; " +
                    "mods depending on materiallib read from their preInit onwards");
        }
    }

    private void requireRegistration(String what) {
        if (resolved) {
            throw new IllegalStateException(
                "Cannot " + what + ": the material registry has already resolved. " +
                    "Materials, families, and edits register inside a MaterialRegistrationEvent handler " +
                    "subscribed during construction");
        }
    }

    private record PendingOp(String description, Runnable action) {}
}
