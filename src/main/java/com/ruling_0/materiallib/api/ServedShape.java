package com.ruling_0.materiallib.api;

import java.util.Map;

/// A [Shape] the registry binds to the materials that generate it, whether the shape has a backing item or
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

    /// Whether `material` generates this shape. The materials are canonical instances, so this compares by
    /// identity.
    default boolean serves(Material material) {
        for (Material served : getServedMaterials()) {
            if (served == material) return true;
        }
        return false;
    }

    /// This shape's property holder; the [Shape] property accessors are answered from it. See
    /// [ShapeProperties] for why the holder is composed into each implementation.
    ShapeProperties properties();

    @Override
    default <T> T getProperty(Property<T> property) {
        return properties().get(property);
    }

    @Override
    default <T> Shape setProperty(Property<T> property, T value) {
        ShapeProperties.requireSettable(property, value);
        properties().set(this, property, value);
        return this;
    }

    @Override
    default boolean hasProperty(Property<?> property) {
        return properties().has(property);
    }

    @Override
    default Map<Property<?>, Object> getOwnProperties() { return properties().view(); }
}
