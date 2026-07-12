package com.ruling_0.materiallib.api;

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
    /// [MaterialLibAPI#newMaterial]; builders and edits reject attempts to set or remove it, so a material built
    /// through the public API always has one. [ShapeIcons] and [MaterialRegistry#resolve] still guard against a
    /// null value rather than assume the guarantee always holds.
    public static final Property<TextureSet> TEXTURE_SET = Property.of(MaterialLib.MODID, "textureSet");

    /// The fallback texture set, for if a texture does not exist within the normal texture set. Optional and null
    /// by default, unlike [#TINT] which falls back to a concrete default value: most materials never set this, and
    /// [ShapeIcons] treats a null value as "no fallback available" rather than as an error.
    public static final Property<TextureSet> FALLBACK_TEXTURE_SET = Property.of(MaterialLib.MODID,
        "fallbackTextureSet");

    /// ARGB tint applied to the material's textures.
    public static final Property<Integer> TINT = Property.of(MaterialLib.MODID, "tint", 0xFFFFFFFF);

    /// ARGB tint applied to a fluid shape's fill icon in place of [#TINT], for a fluid whose art already encodes
    /// its color (e.g. dedicated, hand-drawn fluid art) and so must not also receive the material's general tint.
    /// Optional and null by default, like [#FALLBACK_TEXTURE_SET]: most materials never set this, and [ShapeFluid]/
    /// [ShapeFluidInContainer] fall back to [#TINT] when it is unset. Applies only to the fluid fill layer (and the
    /// matching fill layer of any fluid-in-container shape holding it) -- every other shape a material generates
    /// (dust, ingot, block, etc.) always uses [#TINT].
    public static final Property<Integer> FLUID_TINT = Property.of(MaterialLib.MODID, "fluidTint");

    /// ARGB tint applied to a [ShapeBlock#hasBaseTexture] block shape's overlay icon layer in place of [#TINT],
    /// for a material whose overlay art already encodes its own color (e.g. hand-painted ore splotches carried
    /// over from a pre-tinted legacy icon) and so must not also receive the material's general tint. Optional and
    /// null by default, like [#FLUID_TINT]: most materials never set this, and [ShapeBlock] falls back to [#TINT]
    /// when it is unset. Applies only to a base-textured block shape's overlay layer (e.g. `ore`/`oreSmall`'s
    /// tinted icon drawn over their untinted per-variant stone background) -- a block shape with no base texture,
    /// such as a material's compressed storage block, always uses [#TINT] directly.
    public static final Property<Integer> BLOCK_OVERLAY_TINT = Property.of(MaterialLib.MODID, "blockOverlayTint");

    /// ARGB tint applied to a plain (no-base-texture) block shape's icon in place of [#TINT], for a material
    /// whose whole-block art already encodes its own color (e.g. a dedicated pre-colored storage-block texture)
    /// and so must not also receive the material's general tint. Optional and null by default, like
    /// [#FLUID_TINT]: most materials never set this, and [ShapeBlock] falls back to [#TINT] when it is unset.
    /// The plain-block counterpart of [#BLOCK_OVERLAY_TINT], which covers only the overlay layer of a
    /// base-textured composite; a composite block shape never consults this property.
    public static final Property<Integer> BLOCK_TINT = Property.of(MaterialLib.MODID, "blockTint");

    /// ARGB tint applied to a [ShapeFluidInContainer]'s fill layer in place of [#FLUID_TINT], for a material whose
    /// fluid renders one color in the world (e.g. untinted over dedicated art) but whose legacy cell fill was
    /// tinted differently. Optional and null by default, like [#FLUID_TINT]: most materials never set this, and
    /// [ShapeFluidInContainer] falls back to [#FLUID_TINT], then [#TINT], when it is unset. Applies only to a
    /// container's fill layer -- the fluid's own world/tank/GUI rendering (see [ShapeFluid]) always uses
    /// [#FLUID_TINT], never this property.
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
