package com.ruling_0.materiallib.api;

import java.util.Objects;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// A named per-material icon binding on one texture atlas, with no backing item or block: the icon resolution of a
/// shape (resource-pack overrides, texture-set chain, fallbacks, unification alternatives, layer stack) for art a
/// mod composites in its own renderers, e.g. tool parts drawn by the material read from a stack's NBT, or an extra
/// face icon of a block the mod already owns. The set's name is the `<shape>` its art is filed under inside each
/// texture set, i.e. the `<shape>` of `materials/<set>/<shape>.png`.
///
/// Created through [MaterialLibClient#newIconSet] before the first texture stitch. Icons bind for every registered
/// material -- an icon set has no served set, since no material generates one -- when the chosen atlas stitches, and
/// re-bind on every resource reload; a caller must hold the set and read the icon per use, never caching the
/// returned [IIcon]. A material whose whole texture-set chain lacks the art binds the transparent placeholder.
@SideOnly(Side.CLIENT)
public final class IconSet {

    /// The texture atlas an icon set binds against, matching
    /// [net.minecraft.client.renderer.texture.TextureMap#getTextureType]'s two values.
    public enum Atlas {

        ITEMS(1),
        BLOCKS(0);

        private final int textureType;

        Atlas(int textureType) {
            this.textureType = textureType;
        }
    }

    private final String modid;
    private final String name;
    private final Atlas atlas;
    private final ShapeIcons icons;

    IconSet(String modid, String name, Atlas atlas) {
        this.modid = Names.validate("icon set modid", modid);
        this.name = Names.validate("icon set name", name);
        this.atlas = Objects.requireNonNull(atlas, "atlas must not be null");
        this.icons = new ShapeIcons(atlas == Atlas.ITEMS);
    }

    /// The icon bound for `material`, or the transparent placeholder when none resolved. Valid only once the
    /// atlas has stitched.
    public IIcon getIcon(Material material) {
        return icons.get(material.getIndex());
    }

    /// The `_OVERLAY` icon bound for `material`, or null when its resolved texture set has none.
    public IIcon getOverlayIcon(Material material) {
        return icons.getOverlayOrNull(material.getIndex());
    }

    /// The number of icon layers bound for `material`; see [TextureSet].
    public int getLayerCount(Material material) {
        return icons.layerCount(material.getIndex());
    }

    /// The icon at `layer` of `material`'s stack, or the transparent placeholder outside the stack's bounds.
    public IIcon getLayerIcon(Material material, int layer) {
        return icons.layer(material.getIndex(), layer);
    }

    /// The ARGB tint `material`'s layer `layer` takes; see [ShapeItem#getMaterialLayerColor].
    public int getLayerColor(Material material, int layer) {
        return icons.layerColor(material, layer);
    }

    /// Whether `material`'s icon bound from the resource-pack override location; see [ShapeItem#hasOverrideIcon].
    public boolean hasOverrideIcon(Material material) {
        return icons.isOverride(material.getIndex());
    }

    void bind(IIconRegister register, Material[] materials) {
        icons.bind(register, materials, name);
    }

    int atlasType() {
        return atlas.textureType;
    }

    @Override
    public String toString() {
        return "IconSet[" + Names.key(modid, name) + " on " + atlas + "]";
    }
}
