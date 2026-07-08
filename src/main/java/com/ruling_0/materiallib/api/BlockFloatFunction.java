package com.ruling_0.materiallib.api;

/// A callback computing a per-material, per-variant float block property, set through
/// [BlockShapeBuilder#hardness] or [BlockShapeBuilder#resistance].
@FunctionalInterface
public interface BlockFloatFunction {

    /// The property's value for a block of `material` in `variant`. `variant` is null for a variant-less block
    /// shape.
    float apply(Material material, String variant);
}
