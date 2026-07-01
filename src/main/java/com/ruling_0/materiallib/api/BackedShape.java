package com.ruling_0.materiallib.api;

import net.minecraft.item.ItemStack;

/// A [Shape] backed by a registered Minecraft [net.minecraft.item.Item] ([ShapeItem]) or [net.minecraft.block.Block]
/// ([ShapeBlock]).
interface BackedShape extends Shape {

    /// Registers the backing item or block with the game. Called once at resolve from MaterialLib's init handler.
    void registerWithGame();

    /// Binds the materials that generate this shape, ascending by index. Called once when the registry resolves.
    void bindServedMaterials(Material[] materials);

    Material[] getServedMaterials();

    /// The itemstack of `material` in this shape, with the given stack size.
    ItemStack getStack(Material material, int amount);
}
