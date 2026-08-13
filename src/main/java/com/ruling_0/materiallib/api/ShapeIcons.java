package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/// The per-material icons of an item or block shape, keyed by material index. Icon lookups never return null: an
/// index that bound no icon resolves to the transparent [#EMPTY_ICON] placeholder. A material with a null
/// [StandardProperties#TEXTURE_SET] is warned about once and treated like one whose texture files do not exist; a
/// null [StandardProperties#FALLBACK_TEXTURE_SET] is skipped silently.
final class ShapeIcons {

    /// The transparent placeholder icon path, present on both the item and block atlases.
    static final String EMPTY_ICON = MaterialLib.MODID + ":empty";

    private final Int2ObjectMap<IIcon> iconsByIndex = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<IIcon> overlaysByIndex = new Int2ObjectOpenHashMap<>();
    private final Set<Material> warnedMissingTextureSet = new ReferenceOpenHashSet<>();
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
        List<String> unbound = null;
        for (Material material : materials) {
            if (bindPerMaterialOverride(register, material, perMaterialIconPath)) continue;
            if (!bindMaterial(register, material, shapeNameCandidates)) {
                if (unbound == null) unbound = new ObjectArrayList<>();
                unbound.add(material.getKey());
            }
        }
        warnUnbound(unbound, materials.length, shapeNameCandidates);
        emptyIcon = register.registerIcon(EMPTY_ICON);
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

    /// Registers `material`'s icon from `perMaterialIconPath`, if set and it names a file that exists on this
    /// atlas, returning whether it did.
    private boolean bindPerMaterialOverride(IIconRegister register, Material material,
                                            Function<Material, String> perMaterialIconPath) {
        if (perMaterialIconPath == null) return false;
        String path = perMaterialIconPath.apply(material);
        if (path == null || !checkResLoc(path)) return false;
        iconsByIndex.put(material.getIndex(), register.registerIcon(path));
        return true;
    }

    /// The icon for a material index, or the empty placeholder if none resolved.
    IIcon get(int index) {
        IIcon icon = iconsByIndex.get(index);
        return icon != null ? icon : emptyIcon;
    }

    /// The overlay icon for a material index
    IIcon getOverlay(int index) {
        IIcon icon = overlaysByIndex.get(index);
        return icon != null ? overlaysByIndex.get(index) : emptyIcon;
    }

    /// Binds `material`'s icon from the highest-priority source that resolves any candidate name: the material's
    /// own texture set, then its fallback texture set, then each unification alternative's texture set and
    /// fallback set. Source-major order keeps a material's own plain-shape texture ahead of a lower source's
    /// variant texture. Returns whether an icon was bound.
    private boolean bindMaterial(IIconRegister register, Material material, List<String> shapeNameCandidates) {
        TextureSet textureSet = material.getProperty(StandardProperties.TEXTURE_SET);
        if (textureSet == null) {
            warnMissingTextureSet(material, shapeNameCandidates.get(0));
        }
        else if (tryBindSet(register, material, textureSet, shapeNameCandidates)) {
            return true;
        }
        TextureSet fallback = material.getProperty(StandardProperties.FALLBACK_TEXTURE_SET);
        if (fallback != null && tryBindSet(register, material, fallback, shapeNameCandidates)) {
            return true;
        }
        for (Material alternative : material.getAlternatives()) {
            TextureSet alternativeTextureSet = alternative.getPropertyIgnoreCanonical(StandardProperties.TEXTURE_SET);
            if (alternativeTextureSet == null) {
                warnMissingTextureSet(alternative, shapeNameCandidates.get(0));
            }
            else if (tryBindSet(register, alternative, alternativeTextureSet, shapeNameCandidates)) {
                return true;
            }
            TextureSet alternativeFallback = alternative
                .getPropertyIgnoreCanonical(StandardProperties.FALLBACK_TEXTURE_SET);
            if (alternativeFallback != null &&
                tryBindSet(register, alternative, alternativeFallback, shapeNameCandidates)) {
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
        String overlayPath = textureSet.overlayPath(shapeName);
        if (checkResLoc(overlayPath)) {
            overlaysByIndex.put(material.getIndex(), register.registerIcon(overlayPath));
        }
        else overlaysByIndex.put(material.getIndex(), null);
    }

    private void warnMissingTextureSet(Material material, String shapeName) {
        if (!warnedMissingTextureSet.add(material)) return;
        MaterialLib.LOG.warn(
            "Material {} has no texture set for shape {}; its icon will fall back to the empty placeholder",
            material.getKey(),
            shapeName);
    }

    private boolean checkResLoc(String path) {
        if (isItem) return ResourceUtil.resourceExists(ResourceUtil.getCompleteItemTextureResourceLocation(path));
        else return ResourceUtil.resourceExists(ResourceUtil.getCompleteBlockTextureResourceLocation(path));
    }
}
