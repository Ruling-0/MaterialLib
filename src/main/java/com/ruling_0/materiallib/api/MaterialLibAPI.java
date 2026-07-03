package com.ruling_0.materiallib.api;

import java.util.Collection;

import net.minecraft.item.ItemStack;

import net.minecraftforge.fluids.FluidStack;

/// The public entry point of MaterialLib, wrapping the game's [MaterialRegistry] instance.
///
/// All registration happens inside a handler for [MaterialRegistrationEvent], which MaterialLib fires during
/// its preInit: declaring shapes through the shape builders and register methods, creating materials and
/// families through [#newMaterial] and [#newFamily], altering ones belonging to other mods through
/// [#editMaterial] and [#editFamily], and registering shape consumers through [#registerShapeConsumer] and
/// [#registerPostInitShapeConsumer]. The registries then resolve, still within MaterialLib's preInit, so mods
/// depending on materiallib read the contents from their own preInit onwards. Consumers run during
/// MaterialLib's init and postInit; see [ShapeConsumer].
public final class MaterialLibAPI {

    private MaterialLibAPI() {}

    /// Starts building a material owned by `modid`, drawing its textures from `textureSet`. Register by calling
    /// [MaterialBuilder#build]. Materials declaring the same name from different mods unify into one material
    /// when the registry resolves; see [Material] for the ownership and merge rules.
    public static MaterialBuilder newMaterial(String modid, String name, TextureSet textureSet) {
        return MaterialRegistry.instance().newMaterial(modid, name, textureSet);
    }

    /// Starts building a family owned by `modid`. Register by calling [FamilyBuilder#build].
    public static FamilyBuilder newFamily(String modid, String name) {
        return MaterialRegistry.instance().newFamily(modid, name);
    }

    /// Starts building a simple item shape owned by `modid`. Finish with [ItemShapeBuilder#build]. For custom
    /// item behavior, subclass [ShapeItem] and use [#registerItemShape].
    public static ItemShapeBuilder newItemShape(String modid, String name) {
        return new ItemShapeBuilder(modid, name);
    }

    /// Registers a [ShapeItem] subclass and returns the shape to generate (see [ShapeRegistry#register]).
    public static Shape registerItemShape(ShapeItem item) {
        return ShapeRegistry.instance().register(item);
    }

    /// Starts building a simple block shape owned by `modid`. Finish with [BlockShapeBuilder#build]. For custom
    /// block behavior, subclass [ShapeBlock] and use [#registerBlockShape]. Block shapes rely on the EndlessIDs
    /// dependency, which widens block metadata once more than sixteen materials are registered.
    public static BlockShapeBuilder newBlockShape(String modid, String name) {
        return new BlockShapeBuilder(modid, name);
    }

    /// Registers a [ShapeBlock] subclass and returns the shape to generate (see [ShapeRegistry#register]).
    public static Shape registerBlockShape(ShapeBlock block) {
        return ShapeRegistry.instance().register(block);
    }

    /// Starts building a simple fluid shape owned by `modid`. Finish with [FluidShapeBuilder#build]. For custom
    /// fluid behavior, subclass [ShapeFluid] and use [#registerFluidShape].
    public static FluidShapeBuilder newFluidShape(String modid, String name) {
        return new FluidShapeBuilder(modid, name);
    }

    /// Registers a [ShapeFluid] subclass and returns the shape to generate (see [ShapeRegistry#register]).
    public static Shape registerFluidShape(ShapeFluid fluid) {
        return ShapeRegistry.instance().register(fluid);
    }

    /// Starts building a simple fluid-in-container shape owned by `modid`, e.g. a filled bucket or cell. Finish
    /// with [FluidInContainerShapeBuilder#build]. For custom behavior, subclass [ShapeFluidInContainer] and use
    /// [#registerFluidInContainerShape].
    public static FluidInContainerShapeBuilder newFluidInContainerShape(String modid, String name) {
        return new FluidInContainerShapeBuilder(modid, name);
    }

    /// Registers a [ShapeFluidInContainer] subclass and returns the shape to generate (see [ShapeRegistry#register]).
    public static Shape registerFluidInContainerShape(ShapeFluidInContainer container) {
        return ShapeRegistry.instance().register(container);
    }

    /// Registers a consumer invoked during MaterialLib's init once per material generating the shape named
    /// `shapeName`; see [ShapeConsumer] for the dispatch and error contract. Targeting is by name so the target
    /// may come from another, possibly absent mod; a name no mod registered is skipped with a warning.
    public static void registerShapeConsumer(String modid, String shapeName, ShapeConsumer consumer) {
        ShapeRegistry.instance().registerConsumer(ShapeConsumers.Phase.INIT, modid, shapeName, consumer);
    }

    /// Registers a consumer for `shape`, targeted by its name; see
    /// [#registerShapeConsumer(String, String, ShapeConsumer)].
    public static void registerShapeConsumer(String modid, Shape shape, ShapeConsumer consumer) {
        ShapeRegistry.instance().registerConsumer(ShapeConsumers.Phase.INIT, modid, shape.getName(), consumer);
    }

    /// Registers a consumer invoked during MaterialLib's postInit, after every mod's init; otherwise identical
    /// to [#registerShapeConsumer(String, String, ShapeConsumer)].
    public static void registerPostInitShapeConsumer(String modid, String shapeName, ShapeConsumer consumer) {
        ShapeRegistry.instance().registerConsumer(ShapeConsumers.Phase.POST_INIT, modid, shapeName, consumer);
    }

    /// Registers a postInit consumer for `shape`, targeted by its name; see
    /// [#registerPostInitShapeConsumer(String, String, ShapeConsumer)].
    public static void registerPostInitShapeConsumer(String modid, Shape shape, ShapeConsumer consumer) {
        ShapeRegistry.instance().registerConsumer(ShapeConsumers.Phase.POST_INIT, modid, shape.getName(), consumer);
    }

    /// The itemstack of `material` in `shape`, with the given stack size. The shape must be a backed (item or
    /// block) shape that the material generates. Only available after shapes have resolved, at the end of
    /// MaterialLib's preInit.
    public static ItemStack getStack(Material material, Shape shape, int amount) {
        return ShapeRegistry.instance().getStack(material, shape, amount);
    }

    /// The fluid stack of `material` in `shape`, with the given volume in millibuckets. The shape must be a fluid
    /// shape that the material generates. Only available after shapes have resolved, at the end of MaterialLib's
    /// preInit.
    public static FluidStack getFluidStack(Material material, Shape shape, int amount) {
        return ShapeRegistry.instance().getFluidStack(material, shape, amount);
    }

    /// Queues changes to a material registered by any mod; see [MaterialEdit].
    public static MaterialEdit editMaterial(String modid, String name) {
        return MaterialRegistry.instance().editMaterial(modid, name);
    }

    /// Queues changes to a family registered by any mod; see [FamilyEdit].
    public static FamilyEdit editFamily(String modid, String name) {
        return MaterialRegistry.instance().editFamily(modid, name);
    }

    /// The material with the given key, or null if none exists. A key whose material unified onto another mod's
    /// returns the unified material.
    public static Material getMaterial(String modid, String name) {
        return MaterialRegistry.instance().getMaterial(modid, name);
    }

    /// The family with the given key, or null if none exists.
    public static Family getFamily(String modid, String name) {
        return MaterialRegistry.instance().getFamily(modid, name);
    }

    /// The material assigned the given global metadata index (see [Material#getIndex]), or null if none has it.
    /// Lets worldgen and other consumers map an item damage value back to its material. Only available after the
    /// registry has resolved.
    public static Material getMaterialByIndex(int index) {
        return MaterialRegistry.instance().getMaterialByIndex(index);
    }

    /// All registered materials; only available after the registry has resolved.
    public static Collection<Material> getMaterials() { return MaterialRegistry.instance().getMaterials(); }

    /// All registered families; only available after the registry has resolved.
    public static Collection<Family> getFamilies() { return MaterialRegistry.instance().getFamilies(); }
}
