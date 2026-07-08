package com.ruling_0.materiallib.api;

import java.util.List;

import net.minecraft.item.ItemStack;

/// A callback computing the drops of a block [Shape], set through [BlockShapeBuilder#drops]. Replaces the default
/// of dropping the placed block itself, letting a shape (e.g. a small ore) drop an item instead, or vary drops by
/// fortune or silk touch.
@FunctionalInterface
public interface BlockDropFunction {

    /// The itemstacks to drop when a block of `material` in `variant` breaks, given the harvesting `fortune`
    /// level and whether it was silk-touched. `variant` is null for a variant-less block shape.
    List<ItemStack> drops(Material material, String variant, int fortune, boolean isSilkTouch);
}
