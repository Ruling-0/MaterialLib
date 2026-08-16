package com.ruling_0.materiallib.api;

import java.util.Objects;

/// Queued cross-mod changes to a [Shape] identified by name, obtained from [MaterialLibAPI#editShape].
/// Queuing, ordering, and skip behavior are as in [MaterialEdit]; an edit addressed to any declaration of a
/// unified name applies to the shape that owns the name.
///
/// A shape is addressed by name alone, unlike a material: shape names are unified across mods into one owner
/// (see [ShapeUnification]), so the modid identifies the editor rather than the target.
public final class ShapeEdit {

    private final ShapeRegistry registry;
    private final String modid;
    private final String name;

    ShapeEdit(ShapeRegistry registry, String modid, String name) {
        this.registry = registry;
        this.modid = Names.validate("shape modid", modid);
        this.name = Names.validate("shape name", name);
    }

    /// Sets a property value on the shape.
    public <T> ShapeEdit setProperty(Property<T> property, T value) {
        ShapeProperties.requireSettable(property, value);
        registry.enqueueShapeOp(
            modid,
            name,
            "set " + property + " on shape",
            shape -> shape.properties().set(shape, property, value));
        return this;
    }

    /// Clears the shape's value for a property, letting the property default show again.
    public ShapeEdit removeProperty(Property<?> property) {
        Objects.requireNonNull(property, "property must not be null");
        registry.enqueueShapeOp(
            modid,
            name,
            "remove " + property + " from shape",
            shape -> shape.properties().remove(shape, property));
        return this;
    }
}
