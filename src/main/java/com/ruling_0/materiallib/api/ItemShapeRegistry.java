package com.ruling_0.materiallib.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameRegistry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// Holds the item backing every item [Shape] and finishes their setup once the material registry has resolved.
///
/// During preInit, [#register] records each shape as a candidate to own its name (see [ShapeUnification]); the
/// [ShapeItem] itself is built by the caller or the builder. During MaterialLib's init, after
/// [MaterialRegistry#resolve], [#resolve] picks one owner per name, registers that owner's item with FML under
/// MaterialLib's own domain so a shape keeps a stable item identity across instances, tells each item which
/// materials generate it, and registers the oredict entries. The game uses the single [#instance]; like
/// [MaterialRegistry#resolve], the lifecycle methods are public only so MaterialLib's own handler can call them
/// and must not be called by other mods.
public final class ItemShapeRegistry {

    private static final Logger LOG = LogManager.getLogger("materiallib");
    private static final ItemShapeRegistry INSTANCE = new ItemShapeRegistry();

    private final ShapeUnification unification = new ShapeUnification();
    private final List<ShapeItem> canonicalItems = new ObjectArrayList<>();
    private final List<ShapeItem> canonicalItemsView = Collections.unmodifiableList(canonicalItems);
    private Map<String, String> persistedOwners = new LinkedHashMap<>();
    private Map<String, String> assignedOwners = new LinkedHashMap<>();
    private boolean resolved;

    ItemShapeRegistry() {}

    public static ItemShapeRegistry instance() {
        return INSTANCE;
    }

    /// The backing item of every item shape, in the order their names were first registered. Used to attach client
    /// renderers and migration handlers; only populated once item shapes resolve.
    public List<ShapeItem> getItemShapes() { return canonicalItemsView; }

    /// Records an item shape as a candidate to own its name and returns the shape to generate. The owner is chosen
    /// at [#resolve], so the returned shape is unified onto the owner's item then rather than at this call; pass it
    /// to [MaterialBuilder#generateShape] or [FamilyBuilder#generateShape] regardless. Call from the owning mod's
    /// preInit.
    Shape register(ShapeItem item) {
        requireRegistration("register item shape " + Names.key(item.getModId(), item.getName()));
        return unification.register(item);
    }

    /// Sets the persisted shape owners to honor at resolve, loaded from the instance-global store. A name already
    /// in the store keeps its owner when that mod registers a candidate this session. Must be set before resolve.
    void setPersistedOwners(Map<String, String> owners) {
        requireRegistration("set persisted shape owners");
        this.persistedOwners = new LinkedHashMap<>(owners);
    }

    /// The full owner assignment after resolve: every persisted entry (including names whose owning mod is absent
    /// this session) plus the owners chosen this session. Written back to the store.
    Map<String, String> getAssignedOwners() {
        requireResolved("read assigned shape owners");
        return Collections.unmodifiableMap(assignedOwners);
    }

    /// The itemstack of `material` in `shape`, with the given stack size, routed to the shape's canonical item.
    /// The shape must be an item shape that `material` generates.
    ItemStack getStack(Material material, Shape shape, int amount) {
        requireResolved("build an itemstack");
        Shape canonical = unification.canonical(shape);
        if (!(canonical instanceof ShapeItem item)) {
            throw new IllegalArgumentException(canonical + " is not an item shape");
        }
        if (!material.hasShape(canonical)) {
            throw new IllegalArgumentException(
                "Material " + material.getKey() + " does not generate shape " + canonical);
        }
        return item.getStack(material, amount);
    }

    /// Picks each name's owner, registers the owner's item, binds it to the materials that generate it, and
    /// registers the oredict entries. Invoked by MaterialLib's init handler after [MaterialRegistry#resolve];
    /// other mods must not call this.
    public void resolve() {
        requireRegistration("resolve item shapes");
        MaterialRegistry.instance()
            .requireResolved("resolve item shapes", "");
        assignedOwners = unification.resolve(persistedOwners);
        registerCanonicalItems();
        bindServedMaterials();
        registerOreDictionary();
        resolved = true;
        LOG.info("Resolved {} item shapes", canonicalItems.size());
    }

    /// Registers each name's owning item with FML under MaterialLib's domain (`materiallib:<name>`). The domain is
    /// MaterialLib's because this runs in MaterialLib's init handler, which fixes the shape's saved identity
    /// regardless of which mod owns it -- so a world keeps its shape stacks when the owning mod changes.
    private void registerCanonicalItems() {
        for (Shape shape : unification.canonicalShapes()) {
            if (!(shape instanceof ShapeItem item)) {
                throw new IllegalStateException(shape + " is not an item shape and cannot back an item");
            }
            GameRegistry.registerItem(item, item.getName());
            canonicalItems.add(item);
        }
    }

    private void requireRegistration(String what) {
        if (resolved) {
            throw new IllegalStateException(
                "Cannot " + what + ": item shapes have already resolved. Item shapes register during preInit");
        }
    }

    private void requireResolved(String what) {
        if (!resolved) {
            throw new IllegalStateException(
                "Cannot " + what + ": item shapes have not resolved yet. They are available from init onwards");
        }
    }

    private void bindServedMaterials() {
        Map<ShapeItem, List<Material>> served = new Reference2ObjectLinkedOpenHashMap<>();
        for (ShapeItem item : canonicalItems) {
            served.put(item, new ObjectArrayList<>());
        }
        for (Material material : MaterialRegistry.instance().getMaterials()) {
            for (Shape shape : material.getShapes()) {
                if (!(shape instanceof ShapeItem)) continue;
                List<Material> materials = served.get(unification.canonical(shape));
                if (materials == null) {
                    throw new IllegalStateException(
                        "Material " + material.getKey() + " generates item shape " + shape +
                            " which was never registered through MaterialLibAPI");
                }
                materials.add(material);
            }
        }
        Comparator<Material> byIndex = Comparator.comparingInt(Material::getIndex);
        for (Map.Entry<ShapeItem, List<Material>> entry : served.entrySet()) {
            Material[] materials = entry.getValue()
                .toArray(new Material[0]);
            Arrays.sort(materials, byIndex);
            entry.getKey()
                .bindServedMaterials(materials);
        }
    }

    private void registerOreDictionary() {
        for (ShapeItem item : canonicalItems) {
            for (Material material : item.getServedMaterials()) {
                ItemStack stack = item.getStack(material, 1);
                for (String prefix : item.getOreDicts()) {
                    OreDictionary.registerOre(prefix + material.getName(), stack);
                }
            }
        }
    }
}
