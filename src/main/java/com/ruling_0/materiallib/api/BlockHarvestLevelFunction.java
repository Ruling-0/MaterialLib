package com.ruling_0.materiallib.api;

/// A callback computing the harvest level of a block [Shape], set through [BlockShapeBuilder#harvestLevel].
@FunctionalInterface
public interface BlockHarvestLevelFunction {

    /// The harvest level required for a block of `material` in `variant`. `variant` is null for a variant-less
    /// block shape.
    int apply(Material material, String variant);
}
