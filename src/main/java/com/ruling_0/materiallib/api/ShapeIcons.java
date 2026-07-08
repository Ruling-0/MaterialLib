package com.ruling_0.materiallib.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/// The per-material icons of an item or block shape, keyed by material index, with a placeholder icon for
/// an index that maps to no live material (a reserved or unknown index).
final class ShapeIcons {

    private final Int2ObjectMap<IIcon> iconsByIndex = new Int2ObjectOpenHashMap<>();

    /// Registers one icon per served material from its texture set, plus the missing-material placeholder.
    void bind(IIconRegister register, Material[] materials, String shapeName) {
        iconsByIndex.clear();
        for (Material material : materials) {
            TextureSet textureSet = material.getProperty(StandardProperties.TEXTURE_SET);
            iconsByIndex.put(material.getIndex(), register.registerIcon(textureSet.iconPath(shapeName)));
        }
    }

    /// The icon for a material index, or the missing-material placeholder.
    IIcon get(int index) {
        IIcon icon = iconsByIndex.get(index);
        return icon != null ? icon : ((TextureMap) Minecraft.getMinecraft().getTextureManager()
            .getTexture(TextureMap.locationBlocksTexture)).getAtlasSprite("missingno");
    }
}
