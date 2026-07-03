package com.ruling_0.materiallib.api;

import net.minecraft.item.ItemStack;

/// A [Shape] backed by a registered Minecraft [net.minecraft.item.Item] ([ShapeItem]) or [net.minecraft.block.Block]
/// ([ShapeBlock]), whose stacks carry the material as their damage or metadata.
interface BackedShape extends ServedShape {

    /// Registers the backing item or block with the game. Called once at resolve from MaterialLib's preInit handler.
    void registerWithGame();

    /// The itemstack of `material` in this shape, with the given stack size.
    ItemStack getStack(Material material, int amount);
}
