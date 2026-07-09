package com.ruling_0.materiallib.api;

/// Overrides the icon path a block [Shape] registers for one material, in place of its [TextureSet]-driven
/// lookup.
///
/// Set through [BlockShapeBuilder#iconPath], consulted once per served material when the block's icons register
/// (see [ShapeBlock#registerBlockIcons]). Returning null for a material falls back to the shape's usual
/// texture-set candidate chain, the same as when no pather is set at all; so does a non-null path naming a file
/// that does not exist on the block atlas. A shape registered through [MaterialLibAPI#registerBlockShape] as a
/// [ShapeBlock] subclass overrides [ShapeBlock#iconPathFor] directly instead of going through this interface.
@FunctionalInterface
public interface BlockIconPather {

    /// Returns the icon path to register for `material` in `shape`, or null to fall back to the shape's usual
    /// texture-set candidate chain for `material`.
    String iconPath(Shape shape, Material material);
}
