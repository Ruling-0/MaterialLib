package com.ruling_0.materiallib.api;

import java.util.Objects;

/// Builds the translation keys and display strings for a [Shape] rendered for a particular [Material].
///
/// Two keys are involved. The material name key names the material on its own, so one translation serves every
/// shape. The override key names a specific shape-and-material pair, letting a lang file replace an irregular
/// name that the shape's format string cannot produce. Callers look the override key up first, fall back to the
/// material name key, and finally to [Material#getName]; [#format] then applies the shape's display format to
/// whichever material name was found. Keeping the key construction here, free of any game lookup, lets it be
/// covered by tests; the [Shape] and [Material] argument order keeps the keys stable across shapes that unify.
final class ShapeNaming {

    private ShapeNaming() {}

    /// The translation key for a material's own display name, e.g. `material.examplemod.TestIron`. One entry
    /// localizes the material across every shape it generates.
    static String materialNameKey(Material material) {
        return "material." + material.getModId() + "." + material.getName();
    }

    /// The translation key overriding the display name of one shape-and-material pair, e.g.
    /// `shape.examplemod.gear.examplemod.TestIron`. Present only where a lang file overrides that pair.
    static String overrideKey(Shape shape, Material material) {
        return "shape." + shape.getModId() + "." + shape.getName() + "." + material.getModId() + "." +
            material.getName();
    }

    /// Applies a shape's display format to a material name, e.g. `("%s Gear", "Iron")` to `Iron Gear`.
    static String format(String displayFormat, String materialName) {
        return String.format(displayFormat, materialName);
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
