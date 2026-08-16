package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/// The per-material icons of an item or block shape, keyed by material index. Once [#bind] has run, [#get] and
/// [#getOverlay] never return null: an index that bound no icon resolves to the transparent [#EMPTY_ICON]
/// placeholder. A material with a null [StandardProperties#TEXTURE_SET] or [StandardProperties#FALLBACK_TEXTURE_SETS]
/// -- or a null entry inside the list -- is treated like one whose texture files do not exist. A resource-pack file
/// at [#overridePath] reskins a single material and outranks every other source; see [#resolvePath].
final class ShapeIcons {

    /// The transparent placeholder icon path, present on both the item and block atlases.
    static final String EMPTY_ICON = MaterialLib.MODID + ":empty";

    /// The suffix marking a shape texture's companion overlay layer, appended to the base icon path.
    static final String OVERLAY_SUFFIX = "_OVERLAY";

    /// The resource-pack override root; see [#resolvePath].
    static final String OVERRIDE_ROOT = MaterialLib.MODID + ":mloverrides/";

    private final Int2ObjectMap<IIcon> iconsByIndex = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<IIcon> overlaysByIndex = new Int2ObjectOpenHashMap<>();
    private final boolean isItem;
    private final Predicate<String> exists;
    private IIcon emptyIcon;

    ShapeIcons(boolean isItem) {
        this(isItem, path -> textureExists(path, isItem));
    }

    /// As [#ShapeIcons(boolean)], with `exists` deciding whether an icon path names a file on this atlas.
    ShapeIcons(boolean isItem, Predicate<String> exists) {
        this.isItem = isItem;
        this.exists = exists;
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
    /// ahead of the texture-set candidates; see [#resolvePath]. A null `perMaterialIconPath` skips that source.
    void bind(IIconRegister register, Material[] materials, List<String> shapeNameCandidates,
              Function<Material, String> perMaterialIconPath) {
        iconsByIndex.clear();
        overlaysByIndex.clear();
        emptyIcon = register.registerIcon(EMPTY_ICON);
        List<String> unbound = null;
        for (Material material : materials) {
            String path = resolvePath(material, shapeNameCandidates, perMaterialIconPath, this::checkResLoc);
            if (path == null) {
                if (unbound == null) unbound = new ObjectArrayList<>();
                unbound.add(material.getKey());
                continue;
            }
            iconsByIndex.put(material.getIndex(), register.registerIcon(path));
            putOverlay(register, material, path + OVERLAY_SUFFIX);
        }
        warnUnbound(unbound, materials.length, shapeNameCandidates);
    }

    private void warnUnbound(List<String> unbound, int total, List<String> shapeNameCandidates) {
        if (unbound == null) return;
        int examples = Math.min(unbound.size(), 5);
        MaterialLib.LOG.warn(
            "No {} icon resolved under {} for {}/{} materials (e.g. {}); they will render the " +
                "transparent placeholder",
            isItem ? "item" : "block",
            shapeNameCandidates,
            unbound.size(),
            total,
            String.join(", ", unbound.subList(0, examples)));
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
    /// itself and needs to distinguish "no overlay" from the transparent placeholder.
    IIcon getOverlayOrNull(int index) {
        return overlaysByIndex.get(index);
    }

    /// The resource-pack override icon path for `material`'s art filed under `shapeName`.
    static String overridePath(Material material, String shapeName) {
        return OVERRIDE_ROOT + material.getName() + "/" + shapeName;
    }

    /// The icon path of the highest-priority source that carries a name in `shapeNameCandidates` whose texture
    /// file `exists` accepts, or null when every source misses. Sources run in order: the resource-pack override
    /// ([#overridePath]), `perMaterialIconPath` when non-null, [StandardProperties#TEXTURE_SET],
    /// [StandardProperties#FALLBACK_TEXTURE_SETS] in list order, then the texture set and fallbacks of each
    /// unification alternative. Within one source, earlier candidate names win. Source-major order keeps a
    /// material's own plain-shape texture ahead of a lower source's variant texture.
    static String resolvePath(Material material, List<String> shapeNameCandidates,
                              Function<Material, String> perMaterialIconPath, Predicate<String> exists) {
        for (String shapeName : shapeNameCandidates) {
            String override = overridePath(material, shapeName);
            if (exists.test(override)) return override;
        }
        if (perMaterialIconPath != null) {
            String perMaterial = perMaterialIconPath.apply(material);
            if (perMaterial != null && exists.test(perMaterial)) return perMaterial;
        }
        String path = resolveFromSets(
            material.getProperty(StandardProperties.TEXTURE_SET),
            material.getProperty(StandardProperties.FALLBACK_TEXTURE_SETS),
            shapeNameCandidates,
            exists);
        if (path != null) return path;
        for (Material alternative : material.getAlternatives()) {
            path = resolveFromSets(
                alternative.getPropertyIgnoreCanonical(StandardProperties.TEXTURE_SET),
                alternative.getPropertyIgnoreCanonical(StandardProperties.FALLBACK_TEXTURE_SETS),
                shapeNameCandidates,
                exists);
            if (path != null) return path;
        }
        return null;
    }

    /// The icon path from the first of `textureSet` then `fallbacks` carrying a candidate name whose texture file
    /// exists, or null when none does.
    private static String resolveFromSets(TextureSet textureSet, List<TextureSet> fallbacks,
                                          List<String> shapeNameCandidates, Predicate<String> exists) {
        String path = resolveSet(textureSet, shapeNameCandidates, exists);
        if (path != null) return path;
        if (fallbacks == null) return null;
        for (TextureSet fallback : fallbacks) {
            path = resolveSet(fallback, shapeNameCandidates, exists);
            if (path != null) return path;
        }
        return null;
    }

    /// The icon path of the first candidate name whose texture file exists in `textureSet`, or null when
    /// `textureSet` is null or carries none of them.
    private static String resolveSet(TextureSet textureSet, List<String> shapeNameCandidates,
                                     Predicate<String> exists) {
        if (textureSet == null) return null;
        for (String shapeName : shapeNameCandidates) {
            String path = textureSet.iconPath(shapeName);
            if (exists.test(path)) return path;
        }
        return null;
    }

    /// Registers `material`'s overlay from `path` when that file exists.
    private void putOverlay(IIconRegister register, Material material, String path) {
        if (!checkResLoc(path)) return;
        overlaysByIndex.put(material.getIndex(), register.registerIcon(path));
    }

    private boolean checkResLoc(String path) {
        return exists.test(path);
    }

    private static boolean textureExists(String path, boolean isItem) {
        if (isItem) return ResourceUtil.resourceExists(ResourceUtil.getCompleteItemTextureResourceLocation(path));
        else return ResourceUtil.resourceExists(ResourceUtil.getCompleteBlockTextureResourceLocation(path));
    }
}
