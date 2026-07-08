package com.ruling_0.materiallib.api;

/// The shape, variant, and material a placed MaterialLib block's (block, metadata) pair encodes, as resolved by
/// [MaterialLibAPI#lookupBlock]. `variant` is null for a variant-less block shape. `material` is null when the
/// metadata maps to no live material.
public record BlockMaterialInfo(Shape shape, String variant, Material material) {}
