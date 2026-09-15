package com.ruling_0.materiallib.api;

import com.ruling_0.materiallib.MaterialLib;

/// A material's palette: column `column` of `assets/<modid>/textures/palettes/<name>.png`, counted from zero and
/// set through [MaterialBuilder#usePalette] or [MaterialEdit#usePalette].
///
/// A material carrying one has its shape art baked rather than tinted. The base texture, its `_LAYER<n>`s
/// and its `_OVERLAY` are recolored through the palette when the texture atlas stitches and flattened into a
/// single sprite. [StandardProperties#TINT] and its siblings stay settable and readable -- fluids and external
/// consumers still apply them -- but they no longer color the material's baked shape art.
///
/// A palette png holds one palette per column, its entries running top to bottom. Trailing fully-transparent pixels are
/// ignored; interior transparent entries are not. No maximum height is assumed. Baking sorts the base texture's
/// distinct shades (its grayscale values, lightest first) and maps them onto the column's entries in that order,
/// clamping to the last entry where the art carries more shades than the palette. Alpha is kept from the base art and
/// layers.
///
/// Only texture set art bakes. Overrides under `mloverrides/`, [IconPather] paths, and fluid still/flowing textures
/// are used without coloring. A missing palette png causes materials to fall back to tinting. A palette that cannot
/// be applied (invalid column index, fully-transparent column, unreadable layer) leaves the composed texture
/// uncolored.
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

    /// The atlas name of the sprite baked from the art at `resolvedBasePath` through this palette; materials
    /// sharing both share the sprite. `mlbaked/` should not match a file on disk, so the name always reaches the
    /// baking loader.
    public String bakedIconName(String resolvedBasePath) {
        return MaterialLib.MODID + ":mlbaked/" + resolvedBasePath.replace(':', '/') + "/" + modid + "/" + name + "/" +
            column;
    }
}
