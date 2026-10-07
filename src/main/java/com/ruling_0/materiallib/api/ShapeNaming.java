package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Builds the translation keys and display strings for a [Shape] rendered for a particular [Material].
///
/// Three kinds of key name a shape stack. The override key replaces the name of one shape-and-material pair outright.
/// Otherwise a format key ([#formatKeys]) translates the format, such as `%s Gear`, that combines the shape with the
/// material name, and the shape's declared format applies when none translates. The material name key translates
/// the material name, falling back to [Material#getName]. The [Shape] and [Material] argument order keeps the keys
/// stable across shapes that unify.
final class ShapeNaming {

    private ShapeNaming() {}

    /// The translation keys of a shape's display-name format for `material`, most specific first: the material's
    /// [StandardProperties#DISPLAY_NAME_FORMAT_KEYS] entry for `shapeName`, then the shape's
    /// [StandardProperties#DISPLAY_NAME_FORMAT_KEY]. Unset keys are left out.
    static List<String> formatKeys(Shape shape, String shapeName, Material material) {
        List<String> keys = new ArrayList<>(2);
        Map<String, String> materialKeys = material.getProperty(StandardProperties.DISPLAY_NAME_FORMAT_KEYS);
        if (materialKeys != null && materialKeys.containsKey(shapeName)) keys.add(materialKeys.get(shapeName));
        String shapeKey = shape.getProperty(StandardProperties.DISPLAY_NAME_FORMAT_KEY);
        if (shapeKey != null) keys.add(shapeKey);
        return keys;
    }

    /// The translation key for a material's own display name: its [StandardProperties#DISPLAY_NAME_KEY], else
    /// `material.<modid>.<name>`, e.g. `material.examplemod.TestIron`.
    static String materialNameKey(Material material) {
        String key = material.getProperty(StandardProperties.DISPLAY_NAME_KEY);
        return key != null ? key : "material." + material.getModId() + "." + material.getName();
    }

    /// The translation key overriding the display name of one shape-and-material pair, e.g.
    /// `shape.examplemod.gear.examplemod.TestIron`. Present only where a lang file overrides that pair.
    static String overrideKey(Shape shape, Material material) {
        return overrideKey(shape.getModId(), shape.getName(), material);
    }

    /// As [#overrideKey(Shape, Material)], for a shape name that differs from the backing object's own, e.g. a
    /// variant block looked up under its declared group name.
    static String overrideKey(String shapeModid, String shapeName, Material material) {
        return "shape." + shapeModid + "." + shapeName + "." + material.getModId() + "." + material.getName();
    }

    /// Applies a shape's display format to a material name, e.g. `("%s Gear", "Iron")` to `Iron Gear`.
    static String format(String displayFormat, String materialName) {
        return String.format(displayFormat, materialName);
    }

    /// Applies `translatedFormat` to a material name, or `declaredFormat` when the translation is null or not a
    /// valid format string.
    static String format(String translatedFormat, String declaredFormat, String materialName) {
        if (translatedFormat != null) {
            try {
                return format(translatedFormat, materialName);
            }
            catch (IllegalFormatException ignored) {}
        }
        return format(declaredFormat, materialName);
    }

    /// Validates a shape's display-name format by applying it to an empty material name, and returns it. Rejects a
    /// null format or one that is not a valid format string.
    static String requireValidFormat(String displayNameFormat) {
        Objects.requireNonNull(displayNameFormat, "displayNameFormat must not be null");
        try {
            format(displayNameFormat, "");
        }
        catch (RuntimeException e) {
            throw new IllegalArgumentException(
                "displayNameFormat \"" + displayNameFormat + "\" is not a valid format string", e);
        }
        return displayNameFormat;
    }

    /// The display format for a shape: the given format, or the material name followed by the capitalized
    /// shape name (e.g. `gear` to `"%s Gear"`) when none was set.
    static String formatOrDefault(String shapeName, String displayNameFormat) {
        return displayNameFormat != null ? displayNameFormat : "%s " + capitalize(shapeName);
    }

    /// The registry name of one variant's backing block: the shape name, an underscore, and the variant name,
    /// e.g. `("ore", "stone")` -> `"ore_stone"`. Also the block's default texture file name; see [ShapeIcons].
    static String variantBlockName(String shapeName, String variant) {
        return shapeName + "_" + variant;
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("shape name must not be null or empty");
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
