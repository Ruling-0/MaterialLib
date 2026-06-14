package com.ruling_0.materiallib.api;

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
    /// `shape.examplemod.gear.examplemod.TestIron`. Present only where a lang file needs to replace the name the
    /// format string would otherwise build.
    static String overrideKey(Shape shape, Material material) {
        return "shape." + shape.getModId() + "." + shape.getName() + "." + material.getModId() + "." +
            material.getName();
    }

    /// Applies a shape's display format to a material name, e.g. `("%s Gear", "Iron")` to `Iron Gear`.
    static String format(String displayFormat, String materialName) {
        return String.format(displayFormat, materialName);
    }
}
