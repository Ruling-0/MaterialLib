package com.ruling_0.materiallib.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/// A callback an item [Shape]'s items run when a player uses one on a block, attached through [ShapeEdit#onItemUse].
@FunctionalInterface
public interface ItemUseCallback {

    /// Handles `player` using `stack`, an item of `material`, on the block at (`x`, `y`, `z`). Returns true when it
    /// acted, which ends the use and skips later callbacks.
    boolean onItemUse(Material material, ItemStack stack, EntityPlayer player, World world, int x, int y, int z,
                      int side, float hitX, float hitY, float hitZ);
}
