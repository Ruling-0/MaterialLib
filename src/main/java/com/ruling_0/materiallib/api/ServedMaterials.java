package com.ruling_0.materiallib.api;

/// The materials that generate a shape, bound once by the registry at resolve.
///
/// Each shape type ([ShapeItem], [ShapeBlock], [ShapeFluid]) holds one of these by composition, since they extend
/// unrelated Minecraft types and cannot share a supertype. Binding is set-once so a resolved shape's materials stay
/// fixed.
final class ServedMaterials {

    private Material[] materials = new Material[0];
    private boolean bound;

    /// Binds the materials, ascending by index. `owner` names the shape in the error if it is bound twice.
    void bind(Object owner, Material[] materials) {
        if (bound) {
            throw new IllegalStateException(owner + " already has its served materials bound");
        }
        bound = true;
        this.materials = materials;
    }

    Material[] get() {
        return materials;
    }
}
