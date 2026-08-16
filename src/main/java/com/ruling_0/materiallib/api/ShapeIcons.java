package com.ruling_0.materiallib.api;

import java.util.List;

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

    /// Registers one icon per served material from its texture set, looked up under `shapeName`.
    void bind(IIconRegister register, Material[] materials, String shapeName) {
        bind(register, materials, List.of(shapeName));
    }

    /// Registers one icon per served material, from the highest-priority texture source that resolves any name in
    /// `shapeNameCandidates`; within one source, earlier names win. A variant block shape passes
    /// `<shapeName>_<variant>` before `<shapeName>`, so a variant needs no texture of its own unless it looks
    /// different from the plain shape.
    void bind(IIconRegister register, Material[] materials, List<String> shapeNameCandidates) {
        iconsByIndex.clear();
        overlaysByIndex.clear();
        for (Material material : materials) {
            bindMaterial(register, material, shapeNameCandidates);
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

    /// Binds `material`'s icon from the highest-priority source that resolves any candidate name: the material's
    /// own texture set, then its fallback texture set, then each unification alternative's texture set and
    /// fallback set. Source-major order keeps a material's own plain-shape texture ahead of a lower source's
    /// variant texture.
    private void bindMaterial(IIconRegister register, Material material, List<String> shapeNameCandidates) {
        if (tryBindSet(register, material, material.getProperty(StandardProperties.TEXTURE_SET), shapeNameCandidates,
            false)) {
            return;
        }
        if (tryBindSet(register, material, material.getProperty(StandardProperties.FALLBACK_TEXTURE_SET),
            shapeNameCandidates, true)) {
            return;
        }
        for (Material alternative : material.getAlternatives()) {
            if (tryBindSet(register, alternative,
                alternative.getPropertyIgnoreCanonical(StandardProperties.TEXTURE_SET), shapeNameCandidates, false)) {
                return;
            }
            if (tryBindSet(register, alternative,
                alternative.getPropertyIgnoreCanonical(StandardProperties.FALLBACK_TEXTURE_SET), shapeNameCandidates,
                true)) {
                return;
            }
        }
    }

    /// Registers `material`'s icon (and overlay, if any) from `textureSet` under the first candidate name whose
    /// texture file exists. Returns whether an icon was bound.
    private boolean tryBindSet(IIconRegister register, Material material, TextureSet textureSet,
                               List<String> shapeNameCandidates, boolean fallback) {
        for (String shapeName : shapeNameCandidates) {
            if (checkResLoc(textureSet.iconPath(shapeName))) {
                setIcons(register, material, shapeName, fallback);
                return true;
            }
        }
        return false;
    }

    private void setIcons(IIconRegister register, Material material, String shapeName, boolean fallback) {
        TextureSet textureSet = fallback ?
            material.getPropertyIgnoreCanonical(StandardProperties.FALLBACK_TEXTURE_SET) :
            material.getPropertyIgnoreCanonical(StandardProperties.TEXTURE_SET);
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
