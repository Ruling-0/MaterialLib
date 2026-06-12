package com.ruling_0.materiallib.api;

/// A typed key for a value attached to a [Material] or [Family].
///
/// Properties are compared by object identity. A property is created once, stored in a constant, and shared by
/// every mod that reads or writes it. Creating a second property with the same modid and name yields a distinct
/// key whose values are invisible to holders of the first, so properties meant for cross-mod use must be exposed
/// as constants (see [StandardProperties] for the ones this mod provides).
///
/// Value resolution for a material checks, in order: the material's own value, the value of the material's
/// Family (if any), then [#getDefaultValue()].
public final class Property<T> {

    private final String modid;
    private final String name;
    private final T defaultValue;

    private Property(String modid, String name, T defaultValue) {
        this.modid = Names.validate("property modid", modid);
        this.name = Names.validate("property name", name);
        this.defaultValue = defaultValue;
    }

    /// Creates a property with no default value; [#get] returns null where the property is unset.
    public static <T> Property<T> of(String modid, String name) {
        return new Property<>(modid, name, null);
    }

    /// Creates a property that falls back to `defaultValue` where unset.
    public static <T> Property<T> of(String modid, String name, T defaultValue) {
        return new Property<>(modid, name, defaultValue);
    }

    public String getModId() { return modid; }

    public String getName() { return name; }

    /// The value returned where neither a material nor its family sets this property, or null if none was given.
    public T getDefaultValue() { return defaultValue; }

    /// Resolves this property for a material: the material's own value, else its family's value, else the
    /// default. Only available after the registry has resolved.
    public T get(Material material) {
        return material.getProperty(this);
    }

    /// Resolves this property for a family: the family's own value, else the default. Only available after the
    /// registry has resolved.
    public T get(Family family) {
        return family.getProperty(this);
    }

    /// True if the material or its family sets this property explicitly (the default value does not count).
    public boolean isSet(Material material) {
        return material.hasProperty(this);
    }

    /// True if the family sets this property explicitly (the default value does not count).
    public boolean isSet(Family family) {
        return family.hasProperty(this);
    }

    @Override
    public String toString() {
        return "Property[" + Names.key(modid, name) + "]";
    }
}
