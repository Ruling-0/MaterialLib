package com.ruling_0.materiallib.api;

/// Overrides the icon path a block or fluid [Shape] registers for one material, ahead of that material's
/// [TextureSet]-driven lookup and behind any resource-pack override.
///
/// Set through [BlockShapeBuilder#iconPath] or [FluidShapeBuilder#iconPath]. Fluid textures live on the block
/// atlas.
@FunctionalInterface
public interface IconPather {

    /// Returns the icon path to register for `material` in `shape`, or null to fall back to that material's
    /// texture-set candidate chain. A path naming no existing file on the shape's atlas falls back the same way.
    String iconPath(Shape shape, Material material);
}
