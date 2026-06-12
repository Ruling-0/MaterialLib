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
/// through the builders and queue cross-mod changes through [MaterialEdit] and [FamilyEdit]; nothing can be read
/// back yet. During this mod's init handler, [#resolve] applies all queued edits in call order, derives family
/// membership and effective shape sets, and freezes the registry. From then on everything is readable and nothing
/// can be registered or edited, which guarantees dependent mods a complete registry in their init and postInit
/// handlers.
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

    /// The material with the given key, or null if none exists.
    public Material getMaterial(String modid, String name) {
        return materials.get(Names.key(modid, name));
    }

    /// The family with the given key, or null if none exists.
    public Family getFamily(String modid, String name) {
        return families.get(Names.key(modid, name));
    }

    public Collection<Material> getMaterials() { return Collections.unmodifiableCollection(materials.values()); }

    public Collection<Family> getFamilies() { return Collections.unmodifiableCollection(families.values()); }

    public boolean isResolved() { return resolved; }

    /// Applies all queued edits in call order, derives family membership and per-material shape sets, and
    /// freezes the registry. Called once from this mod's init handler.
    public void resolve() {
        requireRegistration("resolve the registry");
        for (PendingOp op : pendingOps) {
            op.action.run();
        }
        pendingOps.clear();

        Map<Family, Set<Material>> membership = new LinkedHashMap<>();
        for (Family family : families.values()) {
            membership.put(family, new LinkedHashSet<>());
        }
        for (Material material : materials.values()) {
            Family family = material.getFamilyInternal();
            if (family != null) {
                membership.get(family).add(material);
            }
        }
        for (Map.Entry<Family, Set<Material>> entry : membership.entrySet()) {
            entry.getKey().setMembersInternal(entry.getValue());
        }

        resolved = true;
        for (Material material : materials.values()) {
            material.resolveShapes();
        }
        LOG.info("Resolved {} materials and {} families", materials.size(), families.size());
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

    void enqueueSetFamily(String materialModid, String materialName, String familyModid, String familyName) {
        String familyKey = Names.key(familyModid, familyName);
        enqueueMaterialOp(materialModid, materialName, "set family " + familyKey + " for material", material -> {
            Family family = families.get(familyKey);
            if (family == null) {
                LOG.warn(
                    "Skipping family assignment for material {}: no such family {} is registered",
                    material.getKey(),
                    familyKey);
                return;
            }
            Family previous = material.getFamilyInternal();
            if (previous != null && previous != family) {
                LOG.warn("Material {} moved from family {} to {}", material.getKey(), previous.getKey(), familyKey);
            }
            material.setFamilyInternal(family);
        });
    }

    void enqueueRemoveFromFamily(String materialModid, String materialName, String familyModid, String familyName) {
        String familyKey = Names.key(familyModid, familyName);
        enqueueMaterialOp(materialModid, materialName, "remove material from family " + familyKey + ",", material -> {
            Family current = material.getFamilyInternal();
            if (current == null || !current.getKey().equals(familyKey)) {
                LOG.warn(
                    "Skipping family removal for material {}: it belongs to {}, not {}",
                    material.getKey(),
                    current == null ? "no family" : current.getKey(),
                    familyKey);
                return;
            }
            material.setFamilyInternal(null);
        });
    }

    private void enqueue(String description, Runnable action) {
        requireRegistration(description);
        pendingOps.add(new PendingOp(description, action));
    }

    void requireResolved(String what) {
        if (!resolved) {
            throw new IllegalStateException(
                "Cannot " + what + ": the material registry has not resolved yet. " +
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
