package com.ruling_0.materiallib.api;

import net.minecraft.item.ItemStack;

/// A [Shape] backed by a registered Minecraft item or block whose damage or metadata is a material's global index
/// ([Material#getIndex]), so one backing object carries every material that generates the shape.
///
/// [ShapeItem] and [ShapeBlock] cannot share a supertype (one extends [net.minecraft.item.Item], the other
/// [net.minecraft.block.Block]), so this interface is the common surface [ShapeRegistry] resolves them through:
/// it registers each backing object with the game, binds the materials that generate the shape, and builds
/// itemstacks, all without caring which kind it holds.
interface BackedShape extends Shape {

    /// Registers the backing item or block with the game. Called once at resolve from MaterialLib's init handler,
    /// so every shape lands under the `materiallib` domain.
    void registerWithGame();

    /// Binds the materials that generate this shape, ascending by index. Called once when the registry resolves.
    void bindServedMaterials(Material[] materials);

    Material[] getServedMaterials();

    /// The itemstack of `material` in this shape, with the given stack size. The damage or block metadata is the
    /// material's global index, so the stack is the same across launches for the same material set.
    ItemStack getStack(Material material, int amount);
}
