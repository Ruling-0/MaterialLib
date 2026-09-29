package com.ruling_0.materiallib.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/// Behavior an item [Shape]'s items run when a player uses one on a block, attached through
/// [ShapeEdit#onItemUse]. Every mod may attach callbacks to any item shape, whichever mod owns it.
@FunctionalInterface
public interface ItemUseCallback {

    /// Handles `player` using `stack`, an item of `material`, on the block at (`x`, `y`, `z`). Returns true when it
    /// acted, which ends the use: later callbacks and vanilla handling do not run.
    boolean onItemUse(Material material, ItemStack stack, EntityPlayer player, World world, int x, int y, int z,
                      int side, float hitX, float hitY, float hitZ);
}
