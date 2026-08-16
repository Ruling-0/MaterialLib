package com.ruling_0.materiallib.api;

/// Overrides the icon path a block [Shape] registers for one material, in place of its [TextureSet]-driven
/// lookup.
///
/// Set through [BlockShapeBuilder#iconPath].
@FunctionalInterface
public interface BlockIconPather {

    /// Returns the icon path to register for `material` in `shape`, or null to fall back to the shape's
    /// texture-set candidate chain. A path naming no existing file on the block atlas falls back the same way.
    String iconPath(Shape shape, Material material);
}
