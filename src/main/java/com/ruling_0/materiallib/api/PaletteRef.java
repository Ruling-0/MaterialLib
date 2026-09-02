package com.ruling_0.materiallib.api;

import com.ruling_0.materiallib.MaterialLib;

/// A material's palette: column `column` of `assets/<modid>/textures/palettes/<name>.png`, counted from zero and
/// set through [MaterialBuilder#usePalette] or [MaterialEdit#usePalette].
///
/// A material carrying one has its shape art baked rather than tinted. The base texture, its `_LAYER<n>` siblings
/// and its `_OVERLAY` are recolored through the palette when the texture atlas stitches and flattened into a
/// single sprite, so the material draws in as many hues as its column carries instead of the one multiplied hue a
/// tint can give. [StandardProperties#TINT] and its siblings stay settable and readable -- fluids and external
/// consumers still apply them -- but they no longer color the material's baked shape art.
///
/// A palette png holds one palette per column, its entries running top to bottom. Trailing fully-transparent
/// pixels mark a column shorter than the png is tall, letting ragged palettes share one file; an interior
/// transparent entry is a real entry. No maximum height is assumed. Baking sorts the base texture's distinct shades
/// (its grayscale values, lightest first) and maps them onto the column's entries in that order, clamping to the
/// last entry where the art carries more shades than the palette; every pixel keeps its own alpha.
///
/// Only texture-set art bakes. A resource-pack override under `mloverrides/` and an [IconPather] path both
/// outrank a palette and draw exactly as authored, and a fluid's still and flow textures never bake. A palette
/// png missing when icons bind leaves the material on the tint path with a warning. A palette that cannot be
/// applied afterwards -- a column past the png's width, an entirely transparent column, an unreadable layer --
/// still flattens the art, unrecolored and untinted.
///
/// A resource pack recolors every material drawing from a palette png by replacing that png.
public record PaletteRef(String modid, String name, int column) {

    public PaletteRef {
        Names.validate("palette modid", modid);
        Names.validate("palette name", name);
        if (column < 0) {
            throw new IllegalArgumentException("palette column must not be negative, was " + column);
        }
    }

    /// The atlas name of the sprite baked from the art at `resolvedBasePath` through this palette. Naming it after
    /// the resolved path lets materials sharing both base art and palette column share one sprite, while art a
    /// texture set varies bakes separately. `mlbaked/` names no file on disk, so the name always reaches the
    /// baking loader.
    public String bakedIconName(String resolvedBasePath) {
        return MaterialLib.MODID + ":mlbaked/" + resolvedBasePath.replace(':', '/') + "/" + modid + "/" + name + "/" +
            column;
    }
}
