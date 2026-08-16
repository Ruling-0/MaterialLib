package com.ruling_0.materiallib.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
/// The registry has two phases. During registration (inside [MaterialRegistrationEvent] handlers), mods register
/// materials and families through the builders and queue cross-mod changes through [MaterialEdit] and
/// [FamilyEdit]; key lookups return the registered objects, but their membership, shapes, and properties cannot
/// be read yet, and neither can the bulk collection views. Once every handler has returned, [#resolve] merges
/// same-name materials onto their owners, applies all queued edits in call order, derives family membership and
/// per-material shape sets, and freezes the registry. From then on everything is readable and nothing can be
/// registered or edited.
///
/// Indices are assigned deterministically at resolve: the post-unification material names sorted ascending take
/// indices 0..n-1, so identical registered sets derive identical assignments.
///
/// The game uses the single [#instance].
public final class MaterialRegistry {

    private static final MaterialRegistry INSTANCE = new MaterialRegistry();

    private final Map<String, Material> materials = new Object2ObjectLinkedOpenHashMap<>();
    private final Map<String, Family> families = new Object2ObjectLinkedOpenHashMap<>();
    private final PendingOps pendingOps = new PendingOps();
    private boolean resolved;
    private Collection<Material> materialsView;
    private Collection<Family> familiesView;
    private Material[] materialsByIndex;
    private Map<String, Integer> assignedIndices = new LinkedHashMap<>();
    private String contentHash;
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

    /// Ends registration and freezes the registry, once the registration event has returned; other mods must not
    /// call it.
    public void resolve() {
        requireRegistration("resolve the registry");
        unifyMaterials();
        pendingOps.drain();

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

    /// Unifies materials that share the same name onto a single canonical one, through an [OwnerElection].
    private void unifyMaterials() {
        Map<String, List<Material>> candidatesByName = new Object2ObjectLinkedOpenHashMap<>();
        for (Material material : materials.values()) {
            candidatesByName.computeIfAbsent(material.getName(), name -> new ObjectArrayList<>()).add(material);
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
                if (candidate.getModId().equals(ownerModid)) winner = candidate;
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

    /// The index assignment after resolve: every registered material name mapped to its index, dense over
    /// 0..n-1 in ascending name order.
    Map<String, Integer> getAssignedIndices() {
        requireResolved("read assigned material indices", "");
        return Collections.unmodifiableMap(assignedIndices);
    }

    /// The SHA-256 hex fingerprint of the index assignment: the material names joined with `\n` in index
    /// order, hashed as UTF-8. Instances with equal hashes agree on every index.
    String getContentHash() {
        requireResolved("read the material list hash", "");
        return contentHash;
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

    /// The full index assignment rendered as a CSV table for debugging: one row per assigned index in
    /// ascending order, with the assigned owner, shapes, and families. Only available after the registry has
    /// resolved.
    public String dumpCsv() {
        return MaterialCsv.dump(this);
    }

    /// Assigns each material its global index and fingerprints the result as [#getContentHash].
    private void assignMaterialIndices() {
        Map<String, Material> byName = new Object2ObjectLinkedOpenHashMap<>();
        for (Material material : materials.values()) {
            byName.put(material.getName(), material);
        }
        List<String> names = new ObjectArrayList<>(byName.keySet());
        Collections.sort(names);
        if (names.size() - 1 > Short.MAX_VALUE) {
            throw new IllegalStateException(
                "Material index " + (names.size() - 1) + " exceeds the item damage limit of " + Short.MAX_VALUE +
                    "; too many materials are registered.");
        }

        assignedIndices = new LinkedHashMap<>();
        materialsByIndex = new Material[names.size()];
        for (int index = 0; index < names.size(); index++) {
            String name = names.get(index);
            assignedIndices.put(name, index);
            Material material = byName.get(name);
            material.resolveIndex(index);
            materialsByIndex[index] = material;
        }
        contentHash = contentHash(names);
    }

    /// The SHA-256 hex digest of `namesInIndexOrder` joined with `\n`, encoded as UTF-8.
    static String contentHash(List<String> namesInIndexOrder) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
        byte[] bytes = digest.digest(String.join("\n", namesInIndexOrder).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
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
        pendingOps.add(description, action);
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
}
