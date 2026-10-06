package com.ruling_0.materiallib.api;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/// A callback computing the drops of a block [Shape], set through [BlockShapeBuilder#drops]. Replaces the default
/// of dropping the placed block itself (e.g. a small ore dropping an item instead). The callback also decides what a
/// silk-touch break drops.
@FunctionalInterface
public interface BlockDropFunction {

    /// The itemstacks to drop when a block of `material` in `variant` breaks. `isSilkTouch` is whether the
    /// harvester's tool has silk touch. `harvester` is null when no player broke the block, and `variant` is null for
    /// a variant-less block shape.
    List<ItemStack> drops(Material material, String variant, int fortune, boolean isSilkTouch,
                          EntityPlayer harvester);
}
