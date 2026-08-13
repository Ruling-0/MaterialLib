package com.ruling_0.materiallib.api;

/// Overrides the icon path a fluid [Shape] registers for one material's fluid, in place of the material's
/// [TextureSet].
///
/// Set through [FluidShapeBuilder#iconPath], consulted once per served material when the shape's icons register
/// (on the client, from a blocks texture-stitch; see [ShapeFluid#registerIcons]). Returning null for a material
/// falls back to that material's texture-set lookup.
@FunctionalInterface
public interface FluidIconPather {

    /// Returns the icon path to register for `material`'s fluid in `shape`, or null to fall back to that
    /// material's texture-set lookup.
    String iconPath(Shape shape, Material material);
}
