package com.ruling_0.materiallib.api;

import java.util.Objects;

import com.ruling_0.materiallib.MaterialLib;

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

    /// Attaches behavior the shape's items run when a player uses one on a block; see [ItemUseCallback]. Callbacks
    /// run in the order attached. Items of a pair overridden by another mod's item (see
    /// [MaterialBuilder#addShapeOverride]) are that mod's items and do not run them. Skipped with a warning when
    /// the shape is not an item shape.
    public ShapeEdit onItemUse(ItemUseCallback callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        registry.enqueueShapeOp(modid, name, "attach item use callback to shape", shape -> {
            ShapeItem item = asItem(shape);
            if (item != null) item.addUseCallback(callback);
        });
        return this;
    }

    /// Attaches behavior the shape's items run every tick while dropped in the world; see [EntityItemCallback].
    /// Ordering, overridden pairs and non-item shapes are as for [#onItemUse].
    public ShapeEdit onEntityItemUpdate(EntityItemCallback callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        registry.enqueueShapeOp(modid, name, "attach dropped item callback to shape", shape -> {
            ShapeItem item = asItem(shape);
            if (item != null) item.addEntityItemCallback(callback);
        });
        return this;
    }

    private ShapeItem asItem(ServedShape shape) {
        if (shape instanceof ShapeItem item) return item;
        MaterialLib.LOG.warn("Skipping item callback from {}: shape {} is not an item shape", modid, name);
        return null;
    }
}
