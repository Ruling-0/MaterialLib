package com.ruling_0.materiallib.api;

/// Builds a [Shape]'s display names for a mod whose lang files compose names in ways a format string cannot, such
/// as inflecting the material name to fit the phrase. Set it as the shape's [StandardProperties#NAME_FORMATTER].
///
/// MaterialLib calls it only when a format key translates, passing the first that does: the material's
/// [StandardProperties#DISPLAY_NAME_FORMAT_KEYS] entry for the shape, else the shape's
/// [StandardProperties#DISPLAY_NAME_FORMAT_KEY]. A lang override for the exact shape-and-material pair still wins.
@FunctionalInterface
public interface ShapeNameFormatter {

    /// The display name of `material`'s stack, or null to apply the translation of `formatKey` to
    /// [Material#getLocalizedName].
    String displayName(String formatKey, Material material);
}
