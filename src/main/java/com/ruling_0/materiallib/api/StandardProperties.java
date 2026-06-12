package com.ruling_0.materiallib.api;

import com.ruling_0.materiallib.MaterialLib;

/// The properties this mod defines and uses itself. Other mods define their own [Property] constants for values
/// consumed by their ShapeConsumers (melting point, tool durability, and the like).
public final class StandardProperties {

    private StandardProperties() {}

    /// The material's unlocalized name, used for item registration, translation keys, and oredict entries. Set
    /// automatically by [MaterialBuilder] from the name passed to [MaterialLibAPI#newMaterial]; setting it
    /// manually has no effect.
    public static final Property<String> NAME = Property.of(MaterialLib.MODID, "name");

    /// The texture set shapes draw their textures from. Set automatically by [MaterialBuilder] from the texture
    /// set passed to [MaterialLibAPI#newMaterial]; setting it manually has no effect.
    public static final Property<TextureSet> TEXTURE_SET = Property.of(MaterialLib.MODID, "textureSet");

    /// ARGB tint applied to the material's textures.
    public static final Property<Integer> TINT = Property.of(MaterialLib.MODID, "tint", 0xFFFFFFFF);
}
