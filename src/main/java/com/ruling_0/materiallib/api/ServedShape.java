package com.ruling_0.materiallib.api;

/// A [Shape] the registry binds to the materials that generate it, whether or not the shape has a backing item or
/// block.
///
/// Both a [BackedShape] (an item or block) and a [ShapeFluid] (a name-keyed Forge fluid) need to know their
/// materials at resolve: the backing objects to build stacks and register oredict, the fluid to register one
/// Forge fluid per material. The registry sorts the materials ascending by index and binds them once, so
/// downstream registration sees a stable order.
interface ServedShape extends Shape {

    /// Binds the materials that generate this shape, ascending by index. Called once when the registry resolves.
    void bindServedMaterials(Material[] materials);

    Material[] getServedMaterials();
}
