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

    /// This shape's property values. The holder is composed into each implementation rather than inherited,
    /// since they extend unrelated Minecraft types; the [Shape] property accessors are answered from it here so
    /// each implementation only supplies the holder.
    ShapeProperties properties();

    @Override
    default <T> T getProperty(Property<T> property) {
        return properties().get(property);
    }

    @Override
    default boolean hasProperty(Property<?> property) {
        return properties().has(property);
    }

    @Override
    default Map<Property<?>, Object> getOwnProperties() { return properties().view(); }
}
