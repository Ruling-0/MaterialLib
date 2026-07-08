package com.ruling_0.materiallib.api;

/// Defines a set of textures corresponding to shapes, one per shape. Each must be named `<shape>.png`.
///
/// The `name` of this corresponds to the folder name holding the textures, either `textures/items/materials/<name>/`
/// for items or `textures/blocks/materials/<name>/` for blocks and fluids.
public final class TextureSet {

    private final String modid;
    private final String name;

    private TextureSet(String modid, String name) {
        this.modid = Names.validate("texture set modid", modid);
        this.name = Names.validate("texture set name", name);
    }

    public static TextureSet of(String modid, String name) {
        return new TextureSet(modid, name);
    }

    public String getModId() { return modid; }

    public String getName() { return name; }

    /// The icon identifier for a shape's texture in this set, e.g. `examplemod:materials/shiny/gear`.
    public String iconPath(String shapeName) {
        return modid + ":materials/" + name + "/" + shapeName;
    }

    public String overlayPath(String shapeName) {
        return modid + ":materials/" + name + "/" + shapeName + "_OVERLAY";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TextureSet other && modid.equals(other.modid) && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return 31 * modid.hashCode() + name.hashCode();
    }

    @Override
    public String toString() {
        return "TextureSet[" + Names.key(modid, name) + "]";
    }
}
