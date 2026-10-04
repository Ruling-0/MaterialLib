package com.ruling_0.materiallib.api;

/// Builds the display name of one material's stack of a [Shape] from its translation keys, for a mod whose lang
/// files compose names in ways a format string cannot, such as inflecting the material name to fit the phrase.
///
/// Set as a shape's [StandardProperties#NAME_FORMATTER]. MaterialLib calls it with the first translated format key:
/// the material's [StandardProperties#DISPLAY_NAME_FORMAT_KEYS] entry for the shape, else the shape's
/// [StandardProperties#DISPLAY_NAME_FORMAT_KEY]. A lang override for the exact shape-and-material pair still wins,
/// and a shape with neither key translated is named from its declared format without calling it.
@FunctionalInterface
public interface ShapeNameFormatter {

    /// The display name for `material` under the format `formatKey` translates, or null to have MaterialLib apply
    /// that format to [Material#getLocalizedName] instead.
    String displayName(String formatKey, Material material);
}
