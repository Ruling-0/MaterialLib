package com.ruling_0.materiallib.api;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/// The per-material icons of an item or block shape, keyed by material index.
final class ShapeIcons {

    private static final String EMPTY_ICON = MaterialLib.MODID + ":empty";

    private final Int2ObjectMap<IIcon> iconsByIndex = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<IIcon> overlaysByIndex = new Int2ObjectOpenHashMap<>();
    private final boolean isItem;
    private IIcon emptyIcon;

    ShapeIcons(boolean isItem) {
        this.isItem = isItem;
    }

    /// Registers one icon per served material from its texture set
    void bind(IIconRegister register, Material[] materials, String shapeName) {
        iconsByIndex.clear();
        overlaysByIndex.clear();
        for (Material material : materials) {
            String path = material.getProperty(StandardProperties.TEXTURE_SET).iconPath(shapeName);
            if (!checkResLoc(path)) {
                path = material.getProperty(StandardProperties.FALLBACK_TEXTURE_SET).iconPath(shapeName);
                if (checkResLoc(path)) {
                    setIcons(register, material, shapeName, true);
                    continue;
                }
                for (Material alternative : material.getAlternatives()) {
                    path = alternative.getPropertyIgnoreCanonical(StandardProperties.TEXTURE_SET).iconPath(shapeName);
                    if (checkResLoc(path)) {
                        setIcons(register, alternative, shapeName, false);
                        break;
                    }
                    path = alternative.getPropertyIgnoreCanonical(StandardProperties.FALLBACK_TEXTURE_SET).iconPath(shapeName);
                    if (checkResLoc(path)) {
                        setIcons(register, alternative, shapeName, true);
                        break;
                    }
                }
            }
            else setIcons(register, material, shapeName, false);
        }
        emptyIcon = register.registerIcon(EMPTY_ICON);
    }

    /// The icon for a material index
    IIcon get(int index) {
        return iconsByIndex.get(index);
    }

    /// The overlay icon for a material index
    IIcon getOverlay(int index) {
        IIcon icon = overlaysByIndex.get(index);
        return icon != null ? overlaysByIndex.get(index) : emptyIcon;
    }

    private void setIcons(IIconRegister register, Material material, String shapeName, boolean fallback) {
        TextureSet textureSet = fallback ? material.getPropertyIgnoreCanonical(StandardProperties.FALLBACK_TEXTURE_SET) : material.getPropertyIgnoreCanonical(StandardProperties.TEXTURE_SET);
        iconsByIndex.put(material.getIndex(), register.registerIcon(textureSet.iconPath(shapeName)));
        String overlayPath = textureSet.overlayPath(shapeName);
        if (checkResLoc(overlayPath)) {
            overlaysByIndex.put(material.getIndex(), register.registerIcon(overlayPath));
        }
        else overlaysByIndex.put(material.getIndex(), null);
    }

    private boolean checkResLoc(String path) {
        if (isItem) return ResourceUtil.resourceExists(ResourceUtil.getCompleteItemTextureResourceLocation(path));
        else return ResourceUtil.resourceExists(ResourceUtil.getCompleteBlockTextureResourceLocation(path));
    }
}
