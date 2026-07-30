package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Map;

/// A form a material can take, such as an ingot, gear, block, or fluid.
///
/// A shape is identified by its owning mod and name, and contributes the prefixes of the oredict entries for every
/// material generating it. Materials declare which shapes they generate through [MaterialBuilder#generateShape]
/// (or at the group level through [FamilyBuilder#generateShape]), and the resulting per-material set is available
/// from [Material#getShapes] once the registry has resolved.
///
/// Like [Property] keys, shapes are compared by object identity: implementations are created once and shared as
/// constants, and two instances reporting the same modid and name are distinct shapes. The identifiers must
/// satisfy the same rules as material names (non-empty, no ':' or whitespace); the registry validates them
/// wherever a shape is passed in.
public interface Shape {

    String getModId();

    /// The shape's name, unique within its owning mod. Also names the texture file looked up inside a material's
    /// [TextureSet] folder.
    String getName();

    /// The oredict prefixes for this shape. The full oredict entry for a material is each prefix followed by the
    /// material name, e.g. "gear" + "TestIron" -> "gearTestIron". A shape may expose several prefixes, registering
    /// its item under each (e.g. "gear" and "cog" give both "gearTestIron" and "cogTestIron"). At least one for an
    /// item or block shape; a fluid shape has none..
    List<String> getOreDicts();

    /// The variant names a block shape declares through [BlockShapeBuilder#variants], in declaration order, or
    /// empty for a shape with no variants (the common case, and every non-block shape).
    default List<String> getVariants() { return List.of(); }

    /// Resolves a property for this shape: its own value, else the property's default. A shape that lost
    /// unification reads the owner's values, so the reference a mod kept from declaring it stays correct.
    ///
    /// Unlike [Material#getProperty] there is no inheritance tier: shapes have no grouping analogous to [Family].
    /// Values are declared through the shape builders and altered through [MaterialLibAPI#editShape].
    default <T> T getProperty(Property<T> property) {
        return property.getDefaultValue();
    }

    /// True if this shape sets the property explicitly; the property default does not count.
    default boolean hasProperty(Property<?> property) {
        return false;
    }

    /// The values set on this shape, excluding defaults.
    default Map<Property<?>, Object> getOwnProperties() { return Map.of(); }
}
