package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Set;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/// The per-material icons of an item or block shape, keyed by material index.
///
/// [StandardProperties#TEXTURE_SET] is mandatory: [MaterialBuilder] requires one at construction and rejects any
/// attempt to unset it, so a material built through the public API is never missing one. [#tryBind] still treats a
/// null value defensively, the same as a texture set whose file does not exist, rather than assume the guarantee
/// always holds; [MaterialRegistry] separately warns at resolve if it ever finds one broken. A null
/// [StandardProperties#FALLBACK_TEXTURE_SET] is the routine case (most materials never set it, and unlike
/// [StandardProperties#TINT] it has no default), so it is treated as "no fallback available", never a warning.
final class ShapeIcons {

    /// The transparent placeholder icon path, present on both the item and block atlases; also used by
    /// [ShapeFluid#registerIcons] for a fluid whose material has no texture set.
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

    /// Registers one icon per served material from its texture set, trying each name in `shapeNameCandidates` in
    /// order and keeping the first that resolves. A variant block shape passes `<shapeName>_<variant>` before
    /// `<shapeName>`, so a variant needs no texture of its own unless it looks different from the plain shape.
    void bind(IIconRegister register, Material[] materials, List<String> shapeNameCandidates) {
        iconsByIndex.clear();
        overlaysByIndex.clear();
        for (Material material : materials) {
            for (String shapeName : shapeNameCandidates) {
                if (tryBind(register, material, shapeName)) break;
            }
        }
        emptyIcon = register.registerIcon(EMPTY_ICON);
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

    /// Resolves and registers `material`'s icon (and overlay, if any) under `shapeName`, trying its texture set,
    /// then its fallback texture set, then the same two on each of its unification alternatives. A null texture
    /// set -- the routine case for the optional fallback, or a broken material for the mandatory primary one -- is
    /// treated like a texture set whose file does not exist, so the search moves on instead of crashing. Returns
    /// whether an icon was bound; false leaves the material without one, for the caller to retry under another
    /// shape name or fall back to the empty placeholder icon (see [#get]).
    private boolean tryBind(IIconRegister register, Material material, String shapeName) {
        TextureSet textureSet = material.getProperty(StandardProperties.TEXTURE_SET);
        if (textureSet == null) {
            warnMissingTextureSet(material, shapeName);
        }
        else if (bindIfExists(register, material, textureSet, shapeName)) {
            return true;
        }
        TextureSet fallback = material.getProperty(StandardProperties.FALLBACK_TEXTURE_SET);
        if (fallback != null && bindIfExists(register, material, fallback, shapeName)) {
            return true;
        }
        for (Material alternative : material.getAlternatives()) {
            TextureSet alternativeTextureSet = alternative
                .getPropertyIgnoreCanonical(StandardProperties.TEXTURE_SET);
            if (alternativeTextureSet == null) {
                warnMissingTextureSet(alternative, shapeName);
            }
            else if (bindIfExists(register, alternative, alternativeTextureSet, shapeName)) {
                return true;
            }
            TextureSet alternativeFallback = alternative
                .getPropertyIgnoreCanonical(StandardProperties.FALLBACK_TEXTURE_SET);
            if (alternativeFallback != null && bindIfExists(register, alternative, alternativeFallback, shapeName)) {
                return true;
            }
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

    /// Logs once per material that it has no [StandardProperties#TEXTURE_SET], so a mod author notices instead of
    /// the icon silently falling back to the empty placeholder. This should be unreachable for a material built
    /// through [MaterialBuilder], which requires a texture set and rejects removing it; it only fires for a
    /// [Material] a mod somehow constructed outside that path.
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
