package com.ruling_0.materiallib.api;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

/// The pixel arithmetic behind [PaletteRef]: recoloring shape art through a palette column and flattening the
/// recolored layers into one sprite.
///
/// A base texture is first indexed -- its unique colors ranked from lightest to darkest, every pixel replaced by
/// its rank. Ranking by luminance rather than by authored color is what lets one artwork accept any palette: rank
/// 0 is the art's highlight, the last rank its deepest shadow, and a palette column supplies a color for each in
/// that same order. A rank map depends only on the base texture, so one map serves every material baking that
/// texture.
///
/// Everything here is plain ARGB `int[]`. Reading pngs, locating palettes, and handing sprites to the texture
/// atlas belong to the callers.
final class PaletteBaker {

    private PaletteBaker() {}

    /// A base texture reduced to palette ranks. `indices` holds each pixel's rank, or -1 where the pixel is fully
    /// transparent and takes no color from the palette; `alphas` holds each pixel's own alpha, which survives
    /// baking untouched. `uniqueCount` is the number of ranks, so a palette shorter than it will be clamped.
    record IndexedImage(int width, int height, int[] indices, int[] alphas, int uniqueCount) {}

    /// Ranks the unique colors of `argb` by descending luminance, breaking ties by descending RGB so the ranking
    /// is deterministic, and maps every pixel to its rank. Colors carried only by fully transparent pixels are
    /// ignored.
    static IndexedImage index(int[] argb, int width, int height) {
        IntOpenHashSet distinct = new IntOpenHashSet();
        for (int pixel : argb) {
            if ((pixel >>> 24) != 0) distinct.add(pixel & 0xFFFFFF);
        }
        int[] unique = distinct.toIntArray();
        IntArrays.quickSort(unique, (a, b) -> {
            int byLuminance = Integer.compare(luminance(b), luminance(a));
            return byLuminance != 0 ? byLuminance : Integer.compare(b, a);
        });
        Int2IntOpenHashMap rankOf = new Int2IntOpenHashMap(unique.length);
        for (int rank = 0; rank < unique.length; rank++) {
            rankOf.put(unique[rank], rank);
        }
        int[] indices = new int[argb.length];
        int[] alphas = new int[argb.length];
        for (int i = 0; i < argb.length; i++) {
            int alpha = argb[i] >>> 24;
            alphas[i] = alpha;
            indices[i] = alpha == 0 ? -1 : rankOf.get(argb[i] & 0xFFFFFF);
        }
        return new IndexedImage(width, height, indices, alphas, unique.length);
    }

    /// Recolors `base` through `palette`, one entry per rank. Ranks past the palette's last entry take that last
    /// entry, so art carrying more colors than the palette has entries flattens its darkest shades together
    /// instead of failing. A palette entry's own alpha is ignored: each pixel keeps the base's alpha, and a fully
    /// transparent pixel stays fully transparent.
    static int[] bake(IndexedImage base, int[] palette) {
        int[] indices = base.indices();
        int[] alphas = base.alphas();
        int[] baked = new int[indices.length];
        int last = palette.length - 1;
        for (int i = 0; i < baked.length; i++) {
            int rank = indices[i];
            if (rank < 0) continue;
            baked[i] = (alphas[i] << 24) | (palette[Math.min(rank, last)] & 0xFFFFFF);
        }
        return baked;
    }

    /// Column `column` of a palette png, its entries running top to bottom, or null if the column lies outside the
    /// png or holds no entries at all. Trailing fully transparent pixels are dropped, which is how columns of
    /// different lengths share one png; an interior transparent pixel is a real entry.
    static int[] paletteColumn(int[] argb, int width, int height, int column) {
        if (column < 0 || column >= width) return null;
        int entries = height;
        while (entries > 0 && (argb[(entries - 1) * width + column] >>> 24) == 0) {
            entries--;
        }
        if (entries == 0) return null;
        int[] palette = new int[entries];
        for (int entry = 0; entry < entries; entry++) {
            palette[entry] = argb[entry * width + column];
        }
        return palette;
    }

    /// Draws `src` over `dst` in place, leaving `dst` holding the flattened result. Both arrays cover the same
    /// pixels in the same order.
    static void compositeOver(int[] dst, int[] src) {
        /*
         * Straight-alpha source-over. The destination survives in proportion to what the source lets through,
         * carried = da * (255 - sa) / 255, and each channel is the alpha-weighted sum of source and destination
         * divided by the resulting alpha. Both divisions round half up by adding half the divisor first, so
         * successive layers do not drift darker. A fully opaque or fully clear source reaches the same result
         * through the general formula; the shortcuts only skip the arithmetic.
         */
        for (int i = 0; i < dst.length; i++) {
            int srcAlpha = src[i] >>> 24;
            if (srcAlpha == 0) continue;
            int dstAlpha = dst[i] >>> 24;
            if (srcAlpha == 255 || dstAlpha == 0) {
                dst[i] = src[i];
                continue;
            }
            int carried = (dstAlpha * (255 - srcAlpha) + 127) / 255;
            int alpha = srcAlpha + carried;
            int half = alpha / 2;
            int red = (((src[i] >> 16) & 0xFF) * srcAlpha + ((dst[i] >> 16) & 0xFF) * carried + half) / alpha;
            int green = (((src[i] >> 8) & 0xFF) * srcAlpha + ((dst[i] >> 8) & 0xFF) * carried + half) / alpha;
            int blue = ((src[i] & 0xFF) * srcAlpha + (dst[i] & 0xFF) * carried + half) / alpha;
            dst[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
    }

    private static int luminance(int rgb) {
        return 299 * ((rgb >> 16) & 0xFF) + 587 * ((rgb >> 8) & 0xFF) + 114 * (rgb & 0xFF);
    }
}
