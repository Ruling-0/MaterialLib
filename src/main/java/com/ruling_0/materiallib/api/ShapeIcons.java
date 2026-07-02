package com.ruling_0.materiallib.api;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/// The per-material icons of an item or block shape, keyed by material index, with a placeholder icon for
/// an index that maps to no live material (a reserved or unknown index).
final class ShapeIcons {

    private static final String MISSING_ICON = MaterialLib.MODID + ":missing_material";

    private final Int2ObjectMap<IIcon> iconsByIndex = new Int2ObjectOpenHashMap<>();
    private IIcon missingIcon;

    /// Registers one icon per served material from its texture set, plus the missing-material placeholder.
    void bind(IIconRegister register, Material[] materials, String shapeName) {
        iconsByIndex.clear();
        for (Material material : materials) {
            TextureSet textureSet = material.getProperty(StandardProperties.TEXTURE_SET);
            iconsByIndex.put(material.getIndex(), register.registerIcon(textureSet.iconPath(shapeName)));
        }
        missingIcon = register.registerIcon(MISSING_ICON);
    }

    /// The icon for a material index, or the missing-material placeholder.
    IIcon get(int index) {
        IIcon icon = iconsByIndex.get(index);
        return icon != null ? icon : missingIcon;
    }
}
