package com.ruling_0.materiallib.api;

import java.util.Objects;

import com.ruling_0.materiallib.MaterialLib;

/// The properties this mod defines and uses itself. Other mods define their own [Property] constants for values
/// their shape and recipe generation consumes (melting point, tool durability, and the like).
public final class StandardProperties {

    private StandardProperties() {}

    /// The material's unlocalized name, used for item registration, translation keys, and oredict entries.
    /// Derived from the name passed to [MaterialLibAPI#newMaterial]; builders and edits reject attempts to set or
    /// remove it. Conventionally, this should start with a capital letter.
    public static final Property<String> NAME = Property.of(MaterialLib.MODID, "name");

    /// The texture set shapes draw their textures from. Derived from the texture set passed to
    /// [MaterialLibAPI#newMaterial]; builders and edits reject attempts to set or remove it.
    public static final Property<TextureSet> TEXTURE_SET = Property.of(MaterialLib.MODID, "textureSet");

    /// ARGB tint applied to the material's textures.
    public static final Property<Integer> TINT = Property.of(MaterialLib.MODID, "tint", 0xFFFFFFFF);

    /// Rejects the properties derived from builder arguments, which can never be set or removed directly.
    static void requireSettable(Property<?> property) {
        if (property == NAME || property == TEXTURE_SET) {
            throw new IllegalArgumentException(
                property + " is derived from the arguments of newMaterial and cannot be set or removed");
        }
    }

    /// Rejects a null property, a null value, or a property derived from builder arguments.
    static void requireSettable(Property<?> property, Object value) {
        Objects.requireNonNull(property, "property must not be null");
        Objects.requireNonNull(value, "value must not be null");
        requireSettable(property);
    }
}
