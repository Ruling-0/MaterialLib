package com.ruling_0.materiallib.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;

/// Holds the item, block, or fluid backing every [Shape] and finishes their setup once the material registry has
/// resolved.
public final class ShapeRegistry {

    private static final ShapeRegistry INSTANCE = new ShapeRegistry();

    private final ShapeUnification unification = new ShapeUnification();
    private final List<ServedShape> servedShapes = new ObjectArrayList<>();
    private final List<BackedShape> backedShapes = new ObjectArrayList<>();
    private final List<ShapeItem> itemShapes = new ObjectArrayList<>();
    private final List<ShapeItem> itemShapesView = Collections.unmodifiableList(itemShapes);
    private final List<ShapeBlock> blockShapes = new ObjectArrayList<>();
    private final List<ShapeBlock> blockShapesView = Collections.unmodifiableList(blockShapes);
    private final List<ShapeFluid> fluidShapes = new ObjectArrayList<>();
    private final List<ShapeFluid> fluidShapesView = Collections.unmodifiableList(fluidShapes);
    private final List<ShapeFluidInContainer> containerShapes = new ObjectArrayList<>();
    private final Object2ObjectOpenHashMap<String, ShapeKind> kindByName = new Object2ObjectOpenHashMap<>();
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

    /// Every fluid shape, in the order their names were first registered. Used to bind fluid icons on the client;
    /// only populated once shapes resolve.
    public List<ShapeFluid> getFluidShapes() { return fluidShapesView; }

    /// Records a backed (item or block) shape as a candidate to own its name and returns the shape to generate. The
    /// owner is chosen at [#resolve], so the returned shape is unified onto the owner's backing object then. Pass it
    /// to [MaterialBuilder#generateShape] or [FamilyBuilder#generateShape] regardless. A name backs one kind only,
    /// since shapes that share a name unify onto one owner. Call from the owning mod's preInit.
    Shape register(BackedShape shape) {
        return registerShape(shape);
    }

    /// Records a fluid shape as a candidate to own its name and returns the shape to generate; see
    /// [#register(BackedShape)]. Call from the owning mod's preInit.
    Shape register(ShapeFluid shape) {
        return registerShape(shape);
    }

    private Shape registerShape(ServedShape shape) {
        requireRegistration("register shape " + Names.key(shape.getModId(), shape.getName()));
        recordKind(shape, kindOf(shape));
        return unification.register(shape);
    }

    /// Records the kind a name backs and rejects a name already declared as a different kind.
    private void recordKind(Shape shape, ShapeKind kind) {
        String name = shape.getName();
        ShapeKind existing = kindByName.get(name);
        if (existing != null && existing != kind) {
            throw new IllegalStateException(
                "Shape name " + name + " is declared as both a " + existing.label + " and a " + kind.label +
                    " shape; a name backs one kind only");
        }
        kindByName.put(name, kind);
    }

    private static ShapeKind kindOf(Shape shape) {
        if (shape instanceof ShapeFluid) return ShapeKind.FLUID;
        if (shape instanceof ShapeBlock) return ShapeKind.BLOCK;
        if (shape instanceof ShapeFluidInContainer) return ShapeKind.CONTAINER;
        if (shape instanceof ShapeItem) return ShapeKind.ITEM;
        throw new IllegalArgumentException(shape + " is not a registerable shape kind");
    }

    /// A fluid container is a distinct kind from a plain item even though it is item-backed, so a container never
    /// unifies with a plain item of the same name -- that merge would silently drop the loser's container mapping.
    private enum ShapeKind {

        ITEM("item"),
        BLOCK("block"),
        FLUID("fluid"),
        CONTAINER("fluid container");

        private final String label;

        ShapeKind(String label) {
            this.label = label;
        }
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

    /// The fluid stack of `material` in `shape`, with the given volume in millibuckets, routed to the shape's
    /// canonical fluid. The shape must be a fluid shape that `material` generates.
    FluidStack getFluidStack(Material material, Shape shape, int amount) {
        requireResolved("build a fluid stack");
        Shape canonical = unification.canonical(shape);
        if (!(canonical instanceof ShapeFluid fluid)) {
            throw new IllegalArgumentException(canonical + " is not a fluid shape");
        }
        if (!serves(fluid, material)) {
            throw new IllegalArgumentException(
                "Material " + material.getKey() + " does not generate shape " + canonical);
        }
        return fluid.fluidStack(material, amount);
    }

    /// True if `material` generates `shape`, tested by membership in the shape's bound served materials so it
    /// holds even when the material declared a non-owning alias of a unified name.
    private static boolean serves(ServedShape shape, Material material) {
        for (Material served : shape.getServedMaterials()) {
            if (served == material) return true;
        }
        return false;
    }

    /// Picks each name's owner, registers the owner's backing object, binds each shape to the materials that
    /// generate it, registers fluids and fluid containers, and registers the oredict entries.
    /// Invoked by MaterialLib's init handler after [MaterialRegistry#resolve]; other mods must not call this.
    public void resolve() {
        requireRegistration("resolve shapes");
        MaterialRegistry.instance()
            .requireResolved("resolve shapes", "");
        assignedOwners = unification.resolve(persistedOwners);
        collectCanonicalShapes();
        bindServedMaterials();
        validateFluidContainers();
        registerFluids();
        registerFluidContainers();
        registerOreDictionary();
        resolved = true;
        MaterialLib.LOG.info(
            "Resolved {} item shapes, {} block shapes, and {} fluid shapes",
            itemShapes.size(),
            blockShapes.size(),
            fluidShapes.size());
    }

    /// Sorts every canonical shape into its kind and registers each backing item or block with FML under
    /// MaterialLib's domain (`materiallib:<name>`). The domain is MaterialLib's because this runs in MaterialLib's
    /// init handler (FML restriction). Fluid shapes register their Forge fluids, and fluid containers their
    /// container mappings, later -- once served materials are known.
    private void collectCanonicalShapes() {
        for (Shape shape : unification.canonicalShapes()) {
            if (!(shape instanceof ServedShape served)) {
                throw new IllegalStateException(shape + " is not a registerable shape kind");
            }
            servedShapes.add(served);
            if (served instanceof ShapeFluid fluid) {
                fluidShapes.add(fluid);
            }
            else if (served instanceof BackedShape backed) {
                backed.registerWithGame();
                backedShapes.add(backed);
                if (backed instanceof ShapeItem item) {
                    itemShapes.add(item);
                    if (item instanceof ShapeFluidInContainer container) {
                        containerShapes.add(container);
                    }
                }
                else if (backed instanceof ShapeBlock block) {
                    blockShapes.add(block);
                }
                else {
                    throw new IllegalStateException(
                        backed + " is a backed shape but neither an item nor a block shape, so it would receive " +
                            "no migration handler or renderer");
                }
            }
            else {
                throw new IllegalStateException(
                    served + " is a served shape but neither backed nor a fluid, so it would never be registered");
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
        Map<ServedShape, List<Material>> served = new Reference2ObjectLinkedOpenHashMap<>();
        for (ServedShape shape : servedShapes) {
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
        for (Map.Entry<ServedShape, List<Material>> entry : served.entrySet()) {
            Material[] materials = entry.getValue()
                .toArray(new Material[0]);
            Arrays.sort(materials, byIndex);
            entry.getKey()
                .bindServedMaterials(materials);
        }
    }

    private void registerFluids() {
        for (ShapeFluid fluid : fluidShapes) {
            fluid.registerFluids();
        }
    }

    /// Enforces that every material generating a fluid-in-container shape also generates the fluid that container
    /// holds.
    private void validateFluidContainers() {
        for (ShapeFluidInContainer container : containerShapes) {
            ShapeFluid fluid = canonicalFluidOf(container);
            for (Material material : container.getServedMaterials()) {
                if (!serves(fluid, material)) {
                    throw new IllegalStateException(
                        "Material " + material.getKey() + " generates fluid-in-container shape " + container +
                            " but not its fluid shape " + fluid +
                            "; a material with a fluid container must also generate the container's fluid");
                }
            }
        }
    }

    private void registerFluidContainers() {
        for (ShapeFluidInContainer container : containerShapes) {
            container.registerContainers(canonicalFluidOf(container));
        }
    }

    /// The canonical fluid a container holds, following unification from the fluid shape the container was built
    /// with, so the container maps to the same fluid the material generates.
    private ShapeFluid canonicalFluidOf(ShapeFluidInContainer container) {
        Shape canonical = unification.canonical(container.getFluidShape());
        if (!(canonical instanceof ShapeFluid fluid)) {
            throw new IllegalStateException(
                container + " names " + canonical + " as its fluid, which is not a fluid shape");
        }
        return fluid;
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
