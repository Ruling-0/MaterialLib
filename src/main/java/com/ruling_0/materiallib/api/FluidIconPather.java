package com.ruling_0.materiallib.api;

/// Overrides the icon path a fluid [Shape] registers for one material's fluid, ahead of that material's
/// [TextureSet]-driven lookup.
///
/// Set through [FluidShapeBuilder#iconPath]. Fluid textures live on the block atlas.
@FunctionalInterface
public interface FluidIconPather {

    /// Returns the icon path to register for `material`'s fluid in `shape`, or null to fall back to that
    /// material's texture-set candidate chain. A path naming no existing file on the block atlas falls back the
    /// same way.
    String iconPath(Shape shape, Material material);
}
