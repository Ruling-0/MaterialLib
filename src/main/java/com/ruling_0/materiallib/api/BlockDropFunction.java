package com.ruling_0.materiallib.api;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/// A callback computing the drops of a block [Shape], set through [BlockShapeBuilder#drops]. Replaces the default
/// of dropping the placed block itself (e.g. a small ore dropping an item instead), silk-touch breaks included: the
/// callback decides what a silk-touched block drops.
@FunctionalInterface
public interface BlockDropFunction {

    /// The itemstacks to drop when a block of `material` in `variant` breaks, given the harvesting `fortune`
    /// level, whether the harvester's tool carries silk touch, and the harvesting player, which is null when no
    /// player broke the block. `variant` is null for a variant-less block shape.
    List<ItemStack> drops(Material material, String variant, int fortune, boolean isSilkTouch,
                          EntityPlayer harvester);
}
