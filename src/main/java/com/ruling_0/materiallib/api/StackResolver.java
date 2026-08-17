package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/// Resolves MaterialLib stacks from the material name and shape token a config entry names, so a config entry keeps
/// its target across sessions that renumber item metadata.
///
/// A material is named by its bare name, with no modid: declarations sharing a name unify into one material, so the
/// name identifies it on its own (see [Material]). A shape token is matched exactly and case-sensitively against the
/// registered names, which are an item shape's name, a variant-less block shape's name, or one variant block's
/// `<shapeName>_<variant>` (see [ShapeNaming#variantBlockName]). Fluid shapes are not among them. A lookup matching
/// nothing returns null or an empty list and logs one error naming what it was given.
///
/// The lookups read the resolved registries through caches built on first call, so they run from a consuming mod's
/// preInit onwards and never from inside a [MaterialRegistrationEvent] handler.
public final class StackResolver {

    private static Map<String, Material> materialsByName;
    private static Map<String, BackedShape> shapesByToken;

    private StackResolver() {}

    /// The material registered under the bare name `materialName`, or null when no material carries it.
    public static Material getMaterial(String materialName) {
        Material material = materialIndex().get(materialName);
        if (material == null) {
            MaterialLib.LOG.error("No material is registered under the name \"{}\"", materialName);
        }
        return material;
    }

    /// The item or block shape registered under `shapeToken`, or null when no shape carries it.
    public static Shape getShape(String shapeToken) {
        BackedShape shape = shapeIndex().get(shapeToken);
        if (shape == null) {
            MaterialLib.LOG.error("No item or block shape is registered under the name \"{}\"", shapeToken);
        }
        return shape;
    }

    /// The itemstack of the named material in the named shape, with the given stack size. Null when either name
    /// matches nothing, or when the material does not generate the shape.
    public static ItemStack getStack(String materialName, String shapeToken, int amount) {
        Material material = materialIndex().get(materialName);
        if (material == null) {
            MaterialLib.LOG.error("Cannot resolve {}:{}: no such material", materialName, shapeToken);
            return null;
        }
        BackedShape shape = shapeIndex().get(shapeToken);
        if (shape == null) {
            MaterialLib.LOG.error("Cannot resolve {}:{}: no such item or block shape", materialName, shapeToken);
            return null;
        }
        if (!shape.serves(material)) {
            MaterialLib.LOG
                .error("Cannot resolve {}:{}: that material does not generate that shape", materialName, shapeToken);
            return null;
        }
        return shape.getStack(material, amount);
    }

    /// The block shapes `shapeToken` names: the one registered under it, or every variant block registered as
    /// `<shapeToken>_<variant>`. Empty when neither matches.
    public static List<ShapeBlock> getBlockShapes(String shapeToken) {
        if (shapeIndex().get(shapeToken) instanceof ShapeBlock block) return List.of(block);
        List<ShapeBlock> variants = new ObjectArrayList<>();
        String prefix = shapeToken + "_";
        for (ShapeBlock block : ShapeRegistry.instance().getBlockShapes()) {
            if (block.getName().startsWith(prefix)) variants.add(block);
        }
        if (variants.isEmpty()) {
            MaterialLib.LOG.error("No block shape is registered as \"{}\" or as one of its variants", shapeToken);
        }
        return variants;
    }

    private static Map<String, Material> materialIndex() {
        if (materialsByName == null) {
            Map<String, Material> index = new Object2ObjectOpenHashMap<>();
            for (Material material : MaterialLibAPI.getMaterials()) {
                index.put(material.getName(), material);
            }
            materialsByName = index;
        }
        return materialsByName;
    }

    /// Indexes every backed shape under the name its backing object registered with. A [ShapeBlockVariants] group is
    /// not among them, only its per-variant blocks; see [#getBlockShapes].
    private static Map<String, BackedShape> shapeIndex() {
        if (shapesByToken == null) {
            ShapeRegistry registry = ShapeRegistry.instance();
            registry.requireResolved("resolve a shape by name");
            Map<String, BackedShape> index = new Object2ObjectOpenHashMap<>();
            for (ShapeItem item : registry.getItemShapes()) {
                index.put(item.getName(), item);
            }
            for (ShapeBlock block : registry.getBlockShapes()) {
                index.put(block.getName(), block);
            }
            shapesByToken = index;
        }
        return shapesByToken;
    }
}
