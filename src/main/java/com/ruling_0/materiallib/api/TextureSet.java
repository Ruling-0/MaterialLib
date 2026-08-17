package com.ruling_0.materiallib.api;

/// Defines a set of textures corresponding to shapes, one per shape. Each must be named `<shape>.png`.
///
/// The `name` of this corresponds to the folder name holding the textures, either `textures/items/materials/<name>/`
/// for items or `textures/blocks/materials/<name>/` for blocks and fluids.
///
/// A shape's art may be a stack of layers drawn back to front: `<shape>.png`, then `<shape>_LAYER1.png`,
/// `<shape>_LAYER2.png` and upward until a number is absent, then `<shape>_OVERLAY.png` last where it exists. The
/// first layer takes the material's tint for the shape being drawn; `<shape>_LAYER<n>.png` takes element `n - 1` of
/// [StandardProperties#LAYER_TINTS], or no tint where the list codes none; `_OVERLAY` art carries its own colors and
/// always draws untinted. The whole stack comes from the one texture source that wins the shape: a set carrying
/// only `<shape>_LAYER1.png` contributes nothing.
///
/// A resource pack reskins one material alone, outranking every texture set, with a file at
/// `assets/materiallib/textures/<items|blocks>/mloverrides/<materialName>/<shape>.png`. `<materialName>` is the
/// material's registry name in its exact case, and `<shape>` is the name the art is filed under inside a texture
/// set. Its `_LAYER<n>` and `_OVERLAY` siblings are read from the override location too, and the whole stack draws
/// untinted. A pack recolors art it does not replace through the tint lang keys instead; see [StandardProperties].
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
