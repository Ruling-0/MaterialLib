package com.ruling_0.materiallib.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

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
    private final Object2ObjectOpenHashMap<String, ShapeType> typeByName = new Object2ObjectOpenHashMap<>();
    private final ShapeConsumers consumers = new ShapeConsumers();
    private final Object2ObjectOpenHashMap<String, ServedShape> servedByName = new Object2ObjectOpenHashMap<>();
    private final Map<Block, Shape> shapeByBlock = new Reference2ObjectOpenHashMap<>();
    private final Map<Block, String> variantByBlock = new Reference2ObjectOpenHashMap<>();
    private Map<String, String> persistedOwners = new LinkedHashMap<>();
    private Map<String, String> assignedOwners = new LinkedHashMap<>();
    private boolean resolved;

    ShapeRegistry() {}

    public static ShapeRegistry instance() {
        return INSTANCE;
    }

    /// The backing item of every item shape, in the order their names were first registered. Only populated once
    /// shapes resolve.
    public List<ShapeItem> getItemShapes() { return itemShapesView; }

    /// The backing block of every block shape, in the order their names were first registered. Only populated
    /// once shapes resolve.
    public List<ShapeBlock> getBlockShapes() { return blockShapesView; }

    /// Every fluid shape, in the order their names were first registered. Only populated once shapes resolve.
    public List<ShapeFluid> getFluidShapes() { return fluidShapesView; }

    /// Records a shape as a candidate to own its name and returns the shape to generate. The owner is chosen at
    /// [#resolve], so the returned shape is unified onto the owner's backing object or fluid then.
    Shape register(ServedShape shape) {
        requireRegistration("register shape " + Names.key(shape.getModId(), shape.getName()));
        recordType(shape, typeOf(shape));
        return unification.register(shape);
    }

    /// Records the type a name backs and rejects a name already declared as a different type.
    private void recordType(Shape shape, ShapeType type) {
        String name = shape.getName();
        ShapeType existing = typeByName.get(name);
        if (existing != null && existing != type) {
            throw new IllegalStateException(
                "Shape name " + name + " is declared as both a " + existing.label + " and a " + type.label +
                    " shape; a name backs one type only");
        }
        typeByName.put(name, type);
    }

    private static ShapeType typeOf(Shape shape) {
        if (shape instanceof ShapeFluid) return ShapeType.FLUID;
        if (shape instanceof ShapeBlock) return ShapeType.BLOCK;
        if (shape instanceof ShapeBlockVariants) return ShapeType.BLOCK;
        if (shape instanceof ShapeFluidInContainer) return ShapeType.CONTAINER;
        if (shape instanceof ShapeItem) return ShapeType.ITEM;
        throw new IllegalArgumentException(shape + " is not a registerable shape type");
    }

    private enum ShapeType {

        ITEM("item"),
        BLOCK("block"),
        FLUID("fluid"),
        CONTAINER("fluid container");

        private final String label;

        ShapeType(String label) {
            this.label = label;
        }
    }

    /// Records a consumer to invoke during `phase` for every material generating the shape named `shapeName`.
    void registerConsumer(ShapeConsumers.Phase phase, String modid, String shapeName, ShapeConsumer consumer) {
        requireRegistration("register a shape consumer targeting " + shapeName);
        consumers.register(phase, modid, shapeName, consumer);
    }

    /// Sets the persisted shape owners to honor at resolve, loaded from the instance-global store. Must be set
    /// before resolve.
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
        material = material.canonical();
        Shape canonical = unification.canonical(shape);
        if (!(canonical instanceof BackedShape backed)) {
            throw new IllegalArgumentException(canonical + " is not a backed item or block shape");
        }
        requireServes(backed, material);
        return backed.getStack(material, amount);
    }

    /// The itemstack of `material` in the given variant of `shape`, with the given stack size. The shape must be
    /// a block shape declared with [BlockShapeBuilder#variants] that `material` generates.
    ItemStack getStack(Material material, Shape shape, String variant, int amount) {
        requireResolved("build an itemstack");
        material = material.canonical();
        Shape canonical = unification.canonical(shape);
        if (!(canonical instanceof ShapeBlockVariants variants)) {
            throw new IllegalArgumentException(canonical + " is not a variant block shape");
        }
        requireServes(variants, material);
        return variants.getStack(material, variant, amount);
    }

    /// The backing block of a variant-less block shape, for a consumer (e.g. worldgen) to place directly. The
    /// shape must be a block shape declared with no variants.
    Block getBlock(Shape shape) {
        requireResolved("look up a block");
        Shape canonical = unification.canonical(shape);
        if (!(canonical instanceof ShapeBlock block)) {
            throw new IllegalArgumentException(canonical + " is not a variant-less block shape");
        }
        return block;
    }

    /// The backing block of one variant of a block shape declared with [BlockShapeBuilder#variants], for a
    /// consumer (e.g. worldgen) to place directly. Fails when `variant` was not declared.
    Block getBlock(Shape shape, String variant) {
        requireResolved("look up a block");
        Shape canonical = unification.canonical(shape);
        if (!(canonical instanceof ShapeBlockVariants variants)) {
            throw new IllegalArgumentException(canonical + " is not a variant block shape");
        }
        return variants.blockFor(variant);
    }

    /// The shape, variant, and material a MaterialLib block encodes at the given metadata, or null when `block`
    /// was not registered by MaterialLib. `variant` is null on the result for a variant-less block shape; `material`
    /// is null when the metadata maps to no live material.
    BlockMaterialInfo lookupBlock(Block block, int metadata) {
        requireResolved("look up a block");
        Shape shape = shapeByBlock.get(block);
        if (shape == null) return null;
        Material material = MaterialRegistry.instance().getMaterialByIndex(metadata);
        return new BlockMaterialInfo(shape, variantByBlock.get(block), material);
    }

    /// The fluid stack of `material` in `shape`, with the given volume in millibuckets, routed to the shape's
    /// canonical fluid. The shape must be a fluid shape that `material` generates.
    FluidStack getFluidStack(Material material, Shape shape, int amount) {
        requireResolved("build a fluid stack");
        material = material.canonical();
        Shape canonical = unification.canonical(shape);
        if (!(canonical instanceof ShapeFluid fluid)) {
            throw new IllegalArgumentException(canonical + " is not a fluid shape");
        }
        requireServes(fluid, material);
        return fluid.fluidStack(material, amount);
    }

    /// Rejects a material that does not generate a shape.
    private static void requireServes(ServedShape shape, Material material) {
        if (!serves(shape, material)) {
            throw new IllegalArgumentException(
                "Material " + material.getKey() + " does not generate shape " + shape);
        }
    }

    private static boolean serves(ServedShape shape, Material material) {
        for (Material served : shape.getServedMaterials()) {
            if (served == material) return true;
        }
        return false;
    }

    /// Picks each name's owner, registers the owner's backing object, binds each shape to the materials that
    /// generate it, registers fluids and fluid containers, and registers the oredict entries.
    /// Invoked by MaterialLib's preInit handler after [MaterialRegistry#resolve]; other mods must not call this.
    public void resolve() {
        requireRegistration("resolve shapes");
        MaterialRegistry.instance().requireResolved("resolve shapes", "");
        assignedOwners = unification.resolve(persistedOwners);
        collectCanonicalShapes();
        bindServedMaterials();
        validateFluidContainers();
        registerFluids();
        registerFluidContainers();
        registerOreDictionary();
        resolved = true;
        MaterialLib.LOG.info("Resolved {} item shapes, {} block shapes, and {} fluid shapes", itemShapes.size(),
            blockShapes.size(), fluidShapes.size());
    }

    /// Runs every init-phase shape consumer once per (shape, material) pair for the shape it targets.
    /// Invoked once by MaterialLib's init handler; other mods must not call this.
    public void runInitConsumers() {
        requireResolved("run shape consumers");
        consumers.run(ShapeConsumers.Phase.INIT, servedByName);
    }

    /// Runs every postInit-phase shape consumer once per (shape, material) pair for the shape it targets.
    /// Invoked once by MaterialLib's postInit handler; other mods must not call this.
    public void runPostInitConsumers() {
        requireResolved("run shape consumers");
        consumers.run(ShapeConsumers.Phase.POST_INIT, servedByName);
    }

    /// Sorts every canonical shape into its type and registers each backing item or block with FML under
    /// MaterialLib's domain (`materiallib:<name>`). The domain is MaterialLib's because this runs in MaterialLib's
    /// preInit handler (FML restriction). Fluid shapes register their Forge fluids, and fluid containers their
    /// container mappings, later -- once served materials are known.
    private void collectCanonicalShapes() {
        for (Shape shape : unification.canonicalShapes()) {
            if (!(shape instanceof ServedShape served)) {
                throw new IllegalStateException(shape + " is not a registerable shape type");
            }
            servedShapes.add(served);
            servedByName.put(served.getName(), served);
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
                    shapeByBlock.put(block, block);
                }
                else if (backed instanceof ShapeBlockVariants variants) {
                    for (String variant : variants.getVariants()) {
                        ShapeBlock block = variants.blockFor(variant);
                        blockShapes.add(block);
                        shapeByBlock.put(block, variants);
                        variantByBlock.put(block, variant);
                    }
                }
                else {
                    throw new IllegalStateException(
                        backed + " is a backed shape but neither an item nor a block shape.");
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
                "Cannot " + what + ": shapes have already resolved. Registration happens inside a " +
                    "MaterialRegistrationEvent handler subscribed during construction");
        }
    }

    private void requireResolved(String what) {
        if (!resolved) {
            throw new IllegalStateException(
                "Cannot " + what + ": shapes have not resolved yet. They are available once MaterialLib's " +
                    "preInit has resolved them");
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
            Material[] materials = entry.getValue().toArray(new Material[0]);
            Arrays.sort(materials, byIndex);
            entry.getKey().bindServedMaterials(materials);
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
