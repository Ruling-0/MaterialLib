package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Objects;

import com.ruling_0.materiallib.MaterialLib;

/// The properties this mod defines and uses itself. Other mods define their own [Property] constants for values
/// their shape and recipe generation consumes (melting point, tool durability, etc.).
public final class StandardProperties {

    private StandardProperties() {}

    /// The material's unlocalized name, used for item registration, translation keys, and oredict entries.
    /// Derived from the name passed to [MaterialLibAPI#newMaterial]; builders and edits reject attempts to set or
    /// remove it. Conventionally, this should start with a capital letter.
    public static final Property<String> NAME = Property.of(MaterialLib.MODID, "name");

    /// The texture set shapes draw their textures from. Derived from the texture set passed to
    /// [MaterialLibAPI#newMaterial]; builders and edits reject attempts to set or remove it.
    public static final Property<TextureSet> TEXTURE_SET = Property.of(MaterialLib.MODID, "textureSet");

    /// The ordered fallback texture sets, tried in order after [#TEXTURE_SET] when a texture is missing from it.
    /// Null when unset; an empty list is equivalent. A material-level list replaces a family-level one entirely
    /// (standard property shadowing), so a material narrowing its art must restate the tail, e.g. `[DULL, NONE]`
    /// on a material whose family declares `[NONE]`. Store an immutable list; the value is shared, never
    /// defensively copied.
    public static final Property<List<TextureSet>> FALLBACK_TEXTURE_SETS = Property.of(MaterialLib.MODID,
        "fallbackTextureSets");

    /// ARGB tint applied to the material's textures.
    public static final Property<Integer> TINT = Property.of(MaterialLib.MODID, "tint", 0xFFFFFFFF);

    /// ARGB tint applied to a fluid shape's fill icon in place of [#TINT], for fluid art that already encodes its
    /// color. Null when unset, falling back to [#TINT]. Applies to a fluid's fill layer and to the fill layer of a
    /// [ShapeFluidInContainer] holding it; every other shape uses [#TINT].
    public static final Property<Integer> FLUID_TINT = Property.of(MaterialLib.MODID, "fluidTint");

    /// ARGB tint applied to a [ShapeBlock#hasBaseTexture] composite's overlay icon layer in place of [#TINT], for
    /// overlay art that already encodes its color. Null when unset, falling back to [#TINT]. A block shape with no
    /// base texture consults [#BLOCK_TINT] instead.
    public static final Property<Integer> BLOCK_OVERLAY_TINT = Property.of(MaterialLib.MODID, "blockOverlayTint");

    /// ARGB tint applied to a plain (no-base-texture) block shape's icon in place of [#TINT], for whole-block art
    /// that already encodes its color. Null when unset, falling back to [#TINT]. A [ShapeBlock#hasBaseTexture]
    /// composite consults [#BLOCK_OVERLAY_TINT] instead.
    public static final Property<Integer> BLOCK_TINT = Property.of(MaterialLib.MODID, "blockTint");

    /// ARGB tint applied to a [ShapeFluidInContainer]'s fill layer in place of the fluid fill tint, for a legacy
    /// cell fill tinted differently than the fluid itself. Null when unset, falling back to [#FLUID_TINT], then
    /// [#TINT]. The fluid's own rendering (see [ShapeFluid]) never consults this property.
    public static final Property<Integer> CELL_TINT = Property.of(MaterialLib.MODID, "cellTint");

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
