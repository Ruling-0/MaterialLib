package com.ruling_0.materiallib.api;

/// The shape, variant, and material an item stack stands for, as resolved by [MaterialLibAPI#lookupStack].
/// `shape` is the canonical shape. `variant` is null except for a variant block shape's item. `material` is null
/// when a MaterialLib item's damage maps to no live material.
public record StackMaterialInfo(Shape shape, String variant, Material material) {}
