package com.ruling_0.materiallib.api;

import java.util.Set;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/// The materials that generate a shape, bound once by the registry at resolve.
///
/// Each shape type ([ShapeItem], [ShapeBlock], [ShapeFluid]) holds one of these by composition, since they extend
/// unrelated Minecraft types and cannot share a supertype.
final class ServedMaterials {

    private Material[] materials = new Material[0];
    private Set<Material> lookup = new ReferenceOpenHashSet<>();
    private boolean bound;

    /// Binds the materials. `owner` names the shape in the error if it is bound twice.
    void bind(Object owner, Material[] materials) {
        if (bound) {
            throw new IllegalStateException(owner + " already has its served materials bound");
        }
        bound = true;
        this.materials = materials;
        this.lookup = new ReferenceOpenHashSet<>(materials);
    }

    /// Whether `material` is one of the bound materials, compared by identity.
    boolean contains(Material material) {
        return lookup.contains(material);
    }

    Material[] get() {
        return materials;
    }
}
