package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gtnewhorizon.gtnhlib.color.ColorResource;
import com.ruling_0.materiallib.MaterialLib;

/// Reads the standard ARGB tint properties through [ColorResource], so a resource pack retints one material with a
/// lang entry; see [StandardProperties] for the pack-facing key and value format.
///
/// One [ColorResource] per unified material and property, and one per unified material and numbered icon layer, is
/// cached here for the life of the game. GTNHLib tracks its instances weakly and refreshes only the reachable ones
/// when resource packs reload. Angelica meshes chunks on several threads at once (see [ShapeBlockRenderingHandler]),
/// so the cache is concurrent. A dedicated server carries no lang files, so every read there returns the value the
/// material declares.
final class MaterialTints {

    private static final Map<Property<Integer>, Map<Material, ColorResource>> RESOURCES = new ConcurrentHashMap<>();
    private static final Map<Material, Map<Integer, ColorResource>> LAYER_RESOURCES = new ConcurrentHashMap<>();

    private MaterialTints() {}

    /// The ARGB tint `material` resolves for `property`, taken from a resource pack's lang entry where one exists.
    /// The property must resolve non-null for the material.
    static int color(Material material, Property<Integer> property) {
        return resourceFor(material, property).getColor();
    }

    /// The [ColorResource] backing `property` for `material`, created on first read and shared by every declaration
    /// unified onto the same material.
    static ColorResource resourceFor(Material material, Property<Integer> property) {
        Map<Material, ColorResource> byMaterial = RESOURCES.computeIfAbsent(property,
            ignored -> new ConcurrentHashMap<>());
        return byMaterial.computeIfAbsent(material.canonical(),
            canonical -> new ColorResource(MaterialLib.MODID, canonical.getName() + "." + property.getName(),
                String.format("0x%08X", canonical.getProperty(property)), true));
    }

    /// The ARGB tint `material` resolves for its `_LAYER<layer>` icon, or opaque white where
    /// [StandardProperties#LAYER_TINTS] codes no element for that layer.
    static int layerColor(Material material, int layer) {
        ColorResource resource = layerResourceFor(material, layer);
        return resource != null ? resource.getColor() : 0xFFFFFFFF;
    }

    /// The [ColorResource] backing `material`'s `_LAYER<layer>` tint, or null where
    /// [StandardProperties#LAYER_TINTS] codes no element for that layer. Created on first read and shared by every
    /// declaration unified onto the same material.
    static ColorResource layerResourceFor(Material material, int layer) {
        Material canonical = material.canonical();
        Integer coded = codedLayerTint(canonical, layer);
        if (coded == null) return null;
        Map<Integer, ColorResource> byLayer = LAYER_RESOURCES.computeIfAbsent(canonical,
            ignored -> new ConcurrentHashMap<>());
        return byLayer.computeIfAbsent(layer,
            number -> new ColorResource(MaterialLib.MODID, canonical.getName() + ".tint." + number,
                String.format("0x%08X", coded), true));
    }

    /// The tint `canonical` codes for its `_LAYER<layer>` icon, or null where the list stops short of it.
    private static Integer codedLayerTint(Material canonical, int layer) {
        List<Integer> tints = canonical.getProperty(StandardProperties.LAYER_TINTS);
        if (tints == null || layer < 1 || layer > tints.size()) return null;
        return tints.get(layer - 1);
    }
}
