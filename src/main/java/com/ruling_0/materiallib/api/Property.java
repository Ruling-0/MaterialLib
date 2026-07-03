package com.ruling_0.materiallib.api;

/// A typed key for a value attached to a [Material] or [Family].
///
/// Properties are compared by object identity. A property is created once, stored in a constant, and shared by
/// every mod that reads or writes it. Creating a second property with the same modid and name yields a distinct
/// key whose values are invisible to holders of the first, so properties meant for cross-mod use must be exposed
/// as constants (see [StandardProperties] for the ones this mod provides).
///
/// Values are read through [Material#getProperty] and [Family#getProperty]. Resolution for a material checks,
/// in order: the material's own value, the values of the material's [Family]s (taking the first family that
/// sets the property, in alphabetical, case-sensitive `modid:name` key order), then [#getDefaultValue()].
/// Conflicting family values are logged when the registry resolves, compared via `equals`.
public final class Property<T> {

    private final String modid;
    private final String name;
    private final T defaultValue;

    private Property(String modid, String name, T defaultValue) {
        this.modid = Names.validate("property modid", modid);
        this.name = Names.validate("property name", name);
        this.defaultValue = defaultValue;
    }

    /// Creates a property with no default value; lookups return null where the property is unset.
    public static <T> Property<T> of(String modid, String name) {
        return new Property<>(modid, name, null);
    }

    /// Creates a property that falls back to `defaultValue` where unset.
    public static <T> Property<T> of(String modid, String name, T defaultValue) {
        return new Property<>(modid, name, defaultValue);
    }

    public String getModId() { return modid; }

    public String getName() { return name; }

    /// The fallback value, or null if none was given.
    public T getDefaultValue() { return defaultValue; }

    @Override
    public String toString() {
        return "Property[" + Names.key(modid, name) + "]";
    }
}
