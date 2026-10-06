package com.ruling_0.materiallib.api;

/// The canonical fluid shape and the material a fluid belongs to, as resolved by [MaterialLibAPI#lookupFluid].
public record FluidMaterialInfo(Shape shape, Material material) {}
