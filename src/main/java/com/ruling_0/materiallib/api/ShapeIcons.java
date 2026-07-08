package com.ruling_0.materiallib.api;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

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
        for (Material material : materials) {
            TextureSet textureSet = material.getProperty(StandardProperties.TEXTURE_SET);
            iconsByIndex.put(material.getIndex(), register.registerIcon(textureSet.iconPath(shapeName)));
            String overlayPath = textureSet.overlayPath(shapeName);
            if (isItem) {
                if (ResourceUtil.resourceExists(ResourceUtil.getCompleteItemTextureResourceLocation(overlayPath))) {
                    overlaysByIndex.put(material.getIndex(), register.registerIcon(textureSet.overlayPath(shapeName)));
                }
                else overlaysByIndex.put(material.getIndex(), null);
            }
            else {
                if (ResourceUtil.resourceExists(ResourceUtil.getCompleteBlockTextureResourceLocation(overlayPath))) {
                    overlaysByIndex.put(material.getIndex(), register.registerIcon(textureSet.overlayPath(shapeName)));
                }
                else overlaysByIndex.put(material.getIndex(), null);
            }
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
}
