package com.ruling_0.materiallib.api;

/// A form a material can take, such as an ingot, gear, block, or fluid.
///
/// A shape is identified by its owning mod and name, and contributes the prefix of the oredict entries for every
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

    /// The oredict prefix for this shape. The full oredict entry for a material is this prefix followed by the
    /// material name, e.g. "gear" + "TestIron" -> "gearTestIron".
    String getOreDict();
}
