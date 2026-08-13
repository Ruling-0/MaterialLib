package com.ruling_0.materiallib.api;

/// Overrides the icon path a block [Shape] registers for one material, in place of its [TextureSet]-driven
/// lookup.
///
/// Set through [BlockShapeBuilder#iconPath], consulted once per served material when the block's icons register
/// (see [ShapeBlock#registerBlockIcons]). Returning null for a material falls back to the shape's texture-set
/// candidate chain; so does a non-null path naming no existing file on the block atlas. A [ShapeBlock] subclass
/// overrides [ShapeBlock#iconPathFor] instead.
@FunctionalInterface
public interface BlockIconPather {

    /// Returns the icon path to register for `material` in `shape`, or null to fall back to the shape's
    /// texture-set candidate chain.
    String iconPath(Shape shape, Material material);
}
