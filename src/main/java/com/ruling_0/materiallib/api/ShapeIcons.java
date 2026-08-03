package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.function.Function;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/// The per-material icons of an item or block shape, keyed by material index. [#get] and [#getOverlay] never
/// return null: an index that bound no icon resolves to the transparent [#EMPTY_ICON] placeholder. A material
/// with a null [StandardProperties#TEXTURE_SET] or [StandardProperties#FALLBACK_TEXTURE_SETS] -- or a null entry
/// inside the list -- is treated like one whose texture files do not exist.
final class ShapeIcons {

    /// The transparent placeholder icon path, present on both the item and block atlases.
    static final String EMPTY_ICON = MaterialLib.MODID + ":empty";

    /// The suffix marking a shape texture's companion overlay layer, appended to the base icon path; see
    /// [TextureSet#overlayPath].
    static final String OVERLAY_SUFFIX = "_OVERLAY";

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
        bind(register, materials, shapeNameCandidates, null);
    }

    /// As [#bind(IIconRegister, Material[], List)], additionally trying `perMaterialIconPath` for each material
    /// before the texture-set candidates. A non-null path naming a file that exists on this atlas binds that icon
    /// directly; a null return, or a path naming no existing file, falls through to the candidate chain. A null
    /// `perMaterialIconPath` skips the override entirely.
    void bind(IIconRegister register, Material[] materials, List<String> shapeNameCandidates,
              Function<Material, String> perMaterialIconPath) {
        iconsByIndex.clear();
        overlaysByIndex.clear();
        emptyIcon = register.registerIcon(EMPTY_ICON);
        List<String> unbound = null;
        for (Material material : materials) {
            if (bindPerMaterialOverride(register, material, perMaterialIconPath)) continue;
            if (!bindMaterial(register, material, shapeNameCandidates)) {
                if (unbound == null) unbound = new ObjectArrayList<>();
                unbound.add(material.getKey());
            }
        }
        warnUnbound(unbound, materials.length, shapeNameCandidates);
    }

    private void warnUnbound(List<String> unbound, int total, List<String> shapeNameCandidates) {
        if (unbound == null) return;
        int examples = Math.min(unbound.size(), 5);
        MaterialLib.LOG.warn(
            "No {} icon resolved under {} for {}/{} served materials (e.g. {}); they will render the " +
                "transparent placeholder",
            isItem ? "item" : "block",
            shapeNameCandidates,
            unbound.size(),
            total,
            String.join(", ", unbound.subList(0, examples)));
    }

    /// Registers `material`'s icon, and its `_OVERLAY` sibling if one exists, from `perMaterialIconPath`, if set
    /// and it names a file that exists on this atlas, returning whether it did.
    private boolean bindPerMaterialOverride(IIconRegister register, Material material,
                                            Function<Material, String> perMaterialIconPath) {
        if (perMaterialIconPath == null) return false;
        String path = perMaterialIconPath.apply(material);
        if (path == null || !checkResLoc(path)) return false;
        iconsByIndex.put(material.getIndex(), register.registerIcon(path));
        putOverlay(register, material, path + OVERLAY_SUFFIX);
        return true;
    }

    /// The icon for a material index, or the empty placeholder if none resolved.
    IIcon get(int index) {
        IIcon icon = iconsByIndex.get(index);
        return icon != null ? icon : emptyIcon;
    }

    /// The overlay icon for a material index, or the empty placeholder if none resolved.
    IIcon getOverlay(int index) {
        IIcon icon = overlaysByIndex.get(index);
        return icon != null ? icon : emptyIcon;
    }

    /// The overlay icon for a material index, or null if none resolved, for a caller that composites the overlay
    /// itself and needs to distinguish "no overlay" from the transparent placeholder. [#setIcons] stores an
    /// explicit null for a material whose winning texture set has no `_OVERLAY` file, so a plain map miss covers
    /// both that case and an index nothing ever bound.
    IIcon getOverlayOrNull(int index) {
        return overlaysByIndex.get(index);
    }

    /// Binds `material`'s icon from the highest-priority source that resolves any candidate name: the material's
    /// own texture set, then each of its fallback texture sets in order, then the same chain on each unification
    /// alternative. Source-major order keeps a material's own plain-shape texture ahead of a lower source's
    /// variant texture. Returns whether an icon was bound.
    private boolean bindMaterial(IIconRegister register, Material material, List<String> shapeNameCandidates) {
        if (tryBindFromSets(
            register,
            material,
            material.getProperty(StandardProperties.TEXTURE_SET),
            material.getProperty(StandardProperties.FALLBACK_TEXTURE_SETS),
            shapeNameCandidates)) {
            return true;
        }
        for (Material alternative : material.getAlternatives()) {
            if (tryBindFromSets(
                register,
                alternative,
                alternative.getPropertyIgnoreCanonical(StandardProperties.TEXTURE_SET),
                alternative.getPropertyIgnoreCanonical(StandardProperties.FALLBACK_TEXTURE_SETS),
                shapeNameCandidates)) {
                return true;
            }
        }
        return false;
    }

    /// Registers `material`'s icon from the first of `textureSet` then `fallbacks` that carries a candidate name,
    /// returning whether one did.
    private boolean tryBindFromSets(IIconRegister register, Material material, TextureSet textureSet,
                                    List<TextureSet> fallbacks, List<String> shapeNameCandidates) {
        if (textureSet != null && tryBindSet(register, material, textureSet, shapeNameCandidates)) {
            return true;
        }
        if (fallbacks == null) return false;
        for (TextureSet fallback : fallbacks) {
            if (fallback != null && tryBindSet(register, material, fallback, shapeNameCandidates)) {
                return true;
            }
        }
        return false;
    }

    /// Registers `material`'s icon (and overlay, if any) from `textureSet` under the first candidate name whose
    /// texture file exists. Returns whether an icon was bound.
    private boolean tryBindSet(IIconRegister register, Material material, TextureSet textureSet,
                               List<String> shapeNameCandidates) {
        for (String shapeName : shapeNameCandidates) {
            if (bindIfExists(register, material, textureSet, shapeName)) return true;
        }
        return false;
    }

    /// Registers `material`'s icon and overlay from `textureSet` under `shapeName` when that texture set names a
    /// file that exists, returning whether it did.
    private boolean bindIfExists(IIconRegister register, Material material, TextureSet textureSet,
                                 String shapeName) {
        if (!checkResLoc(textureSet.iconPath(shapeName))) return false;
        setIcons(register, material, textureSet, shapeName);
        return true;
    }

    private void setIcons(IIconRegister register, Material material, TextureSet textureSet, String shapeName) {
        iconsByIndex.put(material.getIndex(), register.registerIcon(textureSet.iconPath(shapeName)));
        putOverlay(register, material, textureSet.overlayPath(shapeName));
    }

    /// Registers `material`'s overlay from `overlayPath` when that file exists, or records an explicit null when
    /// it does not, so [#getOverlayOrNull] reports "this material has no overlay" rather than "never bound".
    private void putOverlay(IIconRegister register, Material material, String overlayPath) {
        overlaysByIndex
            .put(material.getIndex(), checkResLoc(overlayPath) ? register.registerIcon(overlayPath) : null);
    }

    private boolean checkResLoc(String path) {
        if (isItem) return ResourceUtil.resourceExists(ResourceUtil.getCompleteItemTextureResourceLocation(path));
        else return ResourceUtil.resourceExists(ResourceUtil.getCompleteBlockTextureResourceLocation(path));
    }
}
