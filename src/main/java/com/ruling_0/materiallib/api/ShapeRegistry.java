package com.ruling_0.materiallib.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import net.minecraftforge.oredict.OreDictionary;

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// Holds the item or block backing every [Shape] and finishes their setup once the material registry has resolved.
///
/// During preInit, [#register] records each shape -- a [ShapeItem] or a [ShapeBlock] -- as a candidate to own its
/// name (see [ShapeUnification]); the backing object itself is built by the caller or a builder. Item and block
/// shapes share one registry, and one name space, because every backing object is registered under the single
/// `materiallib:<name>` identity: a name backs an item or a block, never both. During MaterialLib's init, after
/// [MaterialRegistry#resolve], [#resolve] picks one owner per name, registers that owner's backing object with FML
/// under MaterialLib's own domain so a shape keeps a stable identity across instances, tells each backing object
/// which materials generate it, and registers the oredict entries. The game uses the single [#instance]; like
/// [MaterialRegistry#resolve], the lifecycle methods are public only so MaterialLib's own handler can call them
/// and must not be called by other mods.
public final class ShapeRegistry {

    private static final Logger LOG = LogManager.getLogger("materiallib");
    private static final ShapeRegistry INSTANCE = new ShapeRegistry();

    private final ShapeUnification unification = new ShapeUnification();
    private final List<BackedShape> backedShapes = new ObjectArrayList<>();
    private final List<ShapeItem> itemShapes = new ObjectArrayList<>();
    private final List<ShapeItem> itemShapesView = Collections.unmodifiableList(itemShapes);
    private final List<ShapeBlock> blockShapes = new ObjectArrayList<>();
    private final List<ShapeBlock> blockShapesView = Collections.unmodifiableList(blockShapes);
    private final Object2BooleanOpenHashMap<String> blockBackedByName = new Object2BooleanOpenHashMap<>();
    private Map<String, String> persistedOwners = new LinkedHashMap<>();
    private Map<String, String> assignedOwners = new LinkedHashMap<>();
    private boolean resolved;

    ShapeRegistry() {}

    public static ShapeRegistry instance() {
        return INSTANCE;
    }

    /// The backing item of every item shape, in the order their names were first registered. Used to attach client
    /// renderers and migration handlers; only populated once shapes resolve.
    public List<ShapeItem> getItemShapes() { return itemShapesView; }

    /// The backing block of every block shape, in the order their names were first registered. Used to attach
    /// migration handlers for the block's item form; only populated once shapes resolve.
    public List<ShapeBlock> getBlockShapes() { return blockShapesView; }

    /// Records a backed shape as a candidate to own its name and returns the shape to generate. The owner is chosen
    /// at [#resolve], so the returned shape is unified onto the owner's backing object then. Pass it to
    /// [MaterialBuilder#generateShape] or [FamilyBuilder#generateShape] regardless. A name backs an item or a
    /// block, never both, since the two would share one saved identity. Call from the owning mod's preInit.
    Shape register(BackedShape shape) {
        requireRegistration("register shape " + Names.key(shape.getModId(), shape.getName()));
        boolean block = shape instanceof ShapeBlock;
        String name = shape.getName();
        if (blockBackedByName.containsKey(name) && blockBackedByName.getBoolean(name) != block) {
            throw new IllegalStateException(
                "Shape name " + name + " is declared as both an item and a block shape; a name backs one kind only");
        }
        blockBackedByName.put(name, block);
        return unification.register(shape);
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

    /// The itemstack of `material` in `shape`, with the given stack size, routed to the shape's canonical backing
    /// object. The shape must be a backed shape that `material` generates.
    ItemStack getStack(Material material, Shape shape, int amount) {
        requireResolved("build an itemstack");
        Shape canonical = unification.canonical(shape);
        if (!(canonical instanceof BackedShape backed)) {
            throw new IllegalArgumentException(canonical + " is not a backed item or block shape");
        }
        if (!serves(backed, material)) {
            throw new IllegalArgumentException(
                "Material " + material.getKey() + " does not generate shape " + canonical);
        }
        return backed.getStack(material, amount);
    }

    /// True if `material` generates `shape`, tested by membership in the shape's bound served materials so it
    /// holds even when the material declared a non-owning alias of a unified name.
    private static boolean serves(BackedShape shape, Material material) {
        for (Material served : shape.getServedMaterials()) {
            if (served == material) return true;
        }
        return false;
    }

    /// Picks each name's owner, registers the owner's backing object, binds it to the materials that generate it,
    /// and registers the oredict entries. Invoked by MaterialLib's init handler after [MaterialRegistry#resolve];
    /// other mods must not call this.
    public void resolve() {
        requireRegistration("resolve shapes");
        MaterialRegistry.instance()
            .requireResolved("resolve shapes", "");
        assignedOwners = unification.resolve(persistedOwners);
        registerBackingObjects();
        bindServedMaterials();
        registerOreDictionary();
        resolved = true;
        LOG.info("Resolved {} item shapes and {} block shapes", itemShapes.size(), blockShapes.size());
    }

    /// Registers each name's owning item or block with FML under MaterialLib's domain (`materiallib:<name>`). The
    /// domain is MaterialLib's because this runs in MaterialLib's init handler (FML restriction).
    private void registerBackingObjects() {
        for (Shape shape : unification.canonicalShapes()) {
            if (!(shape instanceof BackedShape backed)) {
                throw new IllegalStateException(shape + " is not a backed shape and cannot be registered");
            }
            backed.registerWithGame();
            backedShapes.add(backed);
            if (backed instanceof ShapeItem item) {
                itemShapes.add(item);
            }
            else if (backed instanceof ShapeBlock block) {
                blockShapes.add(block);
            }
            else {
                throw new IllegalStateException(
                    backed + " is a backed shape but neither an item nor a block shape, so it would receive no " +
                        "migration handler or renderer");
            }
        }
    }

    private void requireRegistration(String what) {
        if (resolved) {
            throw new IllegalStateException(
                "Cannot " + what + ": shapes have already resolved. Shapes register during preInit");
        }
    }

    private void requireResolved(String what) {
        if (!resolved) {
            throw new IllegalStateException(
                "Cannot " + what + ": shapes have not resolved yet. They are available from init onwards");
        }
    }

    private void bindServedMaterials() {
        Map<BackedShape, List<Material>> served = new Reference2ObjectLinkedOpenHashMap<>();
        for (BackedShape shape : backedShapes) {
            served.put(shape, new ObjectArrayList<>());
        }
        for (Material material : MaterialRegistry.instance().getMaterials()) {
            for (Shape shape : material.getShapes()) {
                List<Material> materials = served.get(unification.canonical(shape));
                if (materials == null) {
                    throw new IllegalStateException(
                        "Material " + material.getKey() + " generates shape " + shape +
                            " which was never registered through MaterialLibAPI");
                }
                materials.add(material);
            }
        }
        Comparator<Material> byIndex = Comparator.comparingInt(Material::getIndex);
        for (Map.Entry<BackedShape, List<Material>> entry : served.entrySet()) {
            Material[] materials = entry.getValue()
                .toArray(new Material[0]);
            Arrays.sort(materials, byIndex);
            entry.getKey()
                .bindServedMaterials(materials);
        }
    }

    private void registerOreDictionary() {
        for (BackedShape shape : backedShapes) {
            for (Material material : shape.getServedMaterials()) {
                ItemStack stack = shape.getStack(material, 1);
                for (String prefix : shape.getOreDicts()) {
                    OreDictionary.registerOre(prefix + material.getName(), stack);
                }
            }
        }
    }
}
