package com.ruling_0.materiallib.api;

/// Identifies a folder of material textures supplied by a mod, holding one texture per shape name.
///
/// The folder lives in the owning mod's assets under `textures/items/materials/<name>/` for item shapes and
/// `textures/blocks/materials/<name>/` for block and fluid shapes, with files named `<shape>.png`. [#iconPath] builds
/// the `modid:path` identifier accepted by icon registration; whether it resolves against the items or blocks texture
/// directory is decided by what registers it.
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
