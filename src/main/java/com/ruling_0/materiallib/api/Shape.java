package com.ruling_0.materiallib.api;

import java.util.List;

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
    /// empty for a shape with no variants (the common case, and every non-block shape). Shapes sharing a name must
    /// declare identical variant lists, or unification fails; see [ShapeUnification].
    default List<String> getVariants() { return List.of(); }
}
