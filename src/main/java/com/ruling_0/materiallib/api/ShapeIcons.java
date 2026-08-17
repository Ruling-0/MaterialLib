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
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/// The per-material icon layer stacks of an item or block shape, keyed by material index. Once [#bind] has run,
/// [#get] and [#layer] never return null: an index that bound no icon resolves to the transparent [#EMPTY_ICON]
/// placeholder. See [TextureSet] for the files a stack is built from and the tints its layers take. A material with a
/// null [StandardProperties#TEXTURE_SET] or [StandardProperties#FALLBACK_TEXTURE_SETS] -- or a null entry inside the
/// list -- is treated like one whose texture files do not exist. A resource-pack file at [#overridePath] reskins a
/// single material, outranks every other source, and draws untinted ([#isOverride]); see [#resolvePath].
final class ShapeIcons {

    /// The transparent placeholder icon path, present on both the item and block atlases.
    static final String EMPTY_ICON = MaterialLib.MODID + ":empty";

    /// The suffix marking a shape texture's companion overlay layer, appended to the base icon path.
    static final String OVERLAY_SUFFIX = "_OVERLAY";

    /// The suffix marking one numbered layer of a shape texture, appended to the base icon path together with the
    /// layer's 1-based number.
    static final String LAYER_SUFFIX = "_LAYER";

    /// The resource-pack override root; see [#resolvePath].
    static final String OVERRIDE_ROOT = MaterialLib.MODID + ":mloverrides/";

    private final Int2ObjectMap<IIcon[]> layersByIndex = new Int2ObjectOpenHashMap<>();
    private final IntOpenHashSet overlayIndices = new IntOpenHashSet();
    private final IntOpenHashSet overrideIndices = new IntOpenHashSet();
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
        layersByIndex.clear();
        overlayIndices.clear();
        overrideIndices.clear();
        emptyIcon = register.registerIcon(EMPTY_ICON);
        List<String> unbound = null;
        for (Material material : materials) {
            String path = resolvePath(material, shapeNameCandidates, perMaterialIconPath, this::checkResLoc);
            if (path == null) {
                if (unbound == null) unbound = new ObjectArrayList<>();
                unbound.add(material.getKey());
                continue;
            }
            if (path.startsWith(OVERRIDE_ROOT)) overrideIndices.add(material.getIndex());
            layersByIndex.put(material.getIndex(), registerStack(register, material.getIndex(), path));
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

    /// The first layer's icon for a material index, or the empty placeholder if none resolved.
    IIcon get(int index) {
        return layer(index, 0);
    }

    /// The icon at `layer` of a material index's stack, or the empty placeholder outside the stack's bounds.
    IIcon layer(int index, int layer) {
        IIcon[] layers = layersByIndex.get(index);
        return layers != null && layer >= 0 && layer < layers.length ? layers[layer] : emptyIcon;
    }

    /// The number of layers bound for a material index, at least one: an index that bound no icon reports the
    /// single placeholder layer [#get] resolves.
    int layerCount(int index) {
        IIcon[] layers = layersByIndex.get(index);
        return layers != null ? layers.length : 1;
    }

    /// Whether `layer` is the trailing `_OVERLAY` layer of a material index's stack.
    boolean isOverlayLayer(int index, int layer) {
        return overlayIndices.contains(index) && layer == layerCount(index) - 1;
    }

    /// Whether the icon bound for a material index came from the resource-pack override location.
    boolean isOverride(int index) {
        return overrideIndices.contains(index);
    }

    /// The ARGB tint `material`'s stack takes at `layer`; see [ShapeItem#getMaterialLayerColor].
    int layerColor(Material material, int layer) {
        int index = material.getIndex();
        if (isOverride(index)) return 0xFFFFFFFF;
        if (layer == 0) return MaterialTints.color(material, StandardProperties.TINT);
        if (isOverlayLayer(index, layer)) return 0xFFFFFFFF;
        return MaterialTints.layerColor(material, layer);
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

    /// Registers the layer stack rooted at `path` for a material index and returns it in draw order; see
    /// [TextureSet] for the files it is built from.
    private IIcon[] registerStack(IIconRegister register, int index, String path) {
        List<IIcon> layers = new ObjectArrayList<>();
        layers.add(register.registerIcon(path));
        for (int number = 1;; number++) {
            String layerPath = path + LAYER_SUFFIX + number;
            if (!checkResLoc(layerPath)) break;
            layers.add(register.registerIcon(layerPath));
        }
        String overlay = path + OVERLAY_SUFFIX;
        if (checkResLoc(overlay)) {
            layers.add(register.registerIcon(overlay));
            overlayIndices.add(index);
        }
        return layers.toArray(new IIcon[0]);
    }

    private boolean checkResLoc(String path) {
        return exists.test(path);
    }

    private static boolean textureExists(String path, boolean isItem) {
        if (isItem) return ResourceUtil.resourceExists(ResourceUtil.getCompleteItemTextureResourceLocation(path));
        else return ResourceUtil.resourceExists(ResourceUtil.getCompleteBlockTextureResourceLocation(path));
    }
}
