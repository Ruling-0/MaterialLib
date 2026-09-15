package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ruling_0.materiallib.api.PaletteBaker.IndexedImage;

import org.junit.jupiter.api.Test;

/// Pins the recoloring [PaletteBaker] performs: the order it ranks a base texture's colors in, what a rank with no
/// palette entry behind it resolves to, which alpha a baked pixel keeps, the entries a palette column contributes,
/// and the blend that flattens layers.
class PaletteBakerTest {

    private static final int RED = 0xFFAA0000;
    private static final int GREEN = 0xFF00BB00;
    private static final int BLUE = 0xFF0000CC;

    /// Rank 0 is the lightest color of the art, so it takes the palette's first entry.
    @Test
    void ranksUniqueColorsFromLightestToDarkest() {
        int[] base = { 0xFFFFFFFF, 0xFF808080, 0xFF202020, 0xFFFFFFFF };

        IndexedImage indexed = PaletteBaker.index(base, 2, 2);

        assertEquals(3, indexed.uniqueCount());
        assertArrayEquals(new int[] { 0, 1, 2, 0 }, indexed.indices());
        assertArrayEquals(new int[] { RED, GREEN, BLUE, RED },
            PaletteBaker.bake(indexed, new int[] { RED, GREEN, BLUE }));
    }

    /// A pixel ranks by its brightest channel alone, so hue never separates shades: a saturated pixel shares its
    /// rank with the gray of the same value.
    @Test
    void pixelsOfTheSameValueShareARankWhateverTheirHue() {
        int[] base = { 0xFF0000FF, 0xFFFFFFFF, 0xFF404040 };

        IndexedImage indexed = PaletteBaker.index(base, 3, 1);

        assertEquals(2, indexed.uniqueCount());
        assertArrayEquals(new int[] { 0, 0, 1 }, indexed.indices());
    }

    /// Art with more colors than the palette has entries keeps drawing: its extra shades all take the last entry.
    @Test
    void ranksPastTheLastPaletteEntryClampToIt() {
        IndexedImage indexed = PaletteBaker.index(new int[] { 0xFFFFFFFF, 0xFF808080, 0xFF202020 }, 3, 1);

        assertArrayEquals(new int[] { RED, GREEN, GREEN }, PaletteBaker.bake(indexed, new int[] { RED, GREEN }));
    }

    /// A single-color base has one rank, not a range to spread the palette across.
    @Test
    void aSingleColorBaseTakesTheFirstPaletteEntry() {
        IndexedImage indexed = PaletteBaker.index(new int[] { 0xFF808080, 0xFF808080 }, 2, 1);

        assertEquals(1, indexed.uniqueCount());
        assertArrayEquals(new int[] { RED, RED }, PaletteBaker.bake(indexed, new int[] { RED, GREEN, BLUE }));
    }

    /// The palette decides color and the base decides alpha, so partially transparent art bakes just as
    /// transparent. A fully transparent pixel contributes no rank at all, whatever color it carries.
    @Test
    void bakedPixelsKeepTheBaseAlphaAndTransparentPixelsTakeNoRank() {
        int[] base = { 0xFFFFFFFF, 0x80FFFFFF, 0x00123456, 0xFF000000 };

        IndexedImage indexed = PaletteBaker.index(base, 2, 2);

        assertEquals(2, indexed.uniqueCount());
        assertArrayEquals(new int[] { 0, 0, -1, 1 }, indexed.indices());
        assertArrayEquals(new int[] { RED, 0x80AA0000, 0, GREEN },
            PaletteBaker.bake(indexed, new int[] { RED, GREEN }));
    }

    /// Columns of different lengths share one png, so the trailing transparent pixels padding a short column are
    /// not entries. An interior transparent pixel is one.
    @Test
    void aColumnDropsTrailingTransparentEntriesAndKeepsInteriorOnes() {
        // spotless:off
        int[] png = {
            RED,   0xFF111111,
            0,     0xFF222222,
            BLUE,  0,
            0,     0 };
        // spotless:on

        assertArrayEquals(new int[] { RED, 0, BLUE }, PaletteBaker.paletteColumn(png, 2, 4, 0));
        assertArrayEquals(new int[] { 0xFF111111, 0xFF222222 }, PaletteBaker.paletteColumn(png, 2, 4, 1));
    }

    /// A column outside the png or holding nothing at all has no palette to report.
    @Test
    void anOutOfRangeOrFullyTransparentColumnHasNoPalette() {
        int[] png = { RED, 0, BLUE, 0 };

        assertNull(PaletteBaker.paletteColumn(png, 2, 2, 1));
        assertNull(PaletteBaker.paletteColumn(png, 2, 2, 2));
        assertNull(PaletteBaker.paletteColumn(png, 2, 2, -1));
    }

    /// Layers flatten in the order they are drawn: a later opaque pixel replaces what is under it, a clear one
    /// leaves it alone.
    @Test
    void layersCompositeInDrawOrder() {
        int[] flattened = { BLUE, BLUE, BLUE };

        PaletteBaker.compositeOver(flattened, new int[] { GREEN, GREEN, 0 });
        PaletteBaker.compositeOver(flattened, new int[] { RED, 0, 0 });

        assertArrayEquals(new int[] { RED, GREEN, BLUE }, flattened);
    }

    /// A partially transparent layer mixes with what is under it, and over a partially transparent destination the
    /// flattened pixel ends up more opaque than either.
    @Test
    void aPartiallyTransparentLayerBlendsIntoWhatIsBeneathIt() {
        int[] flattened = { 0xFF0000FF, 0x80FF0000 };

        PaletteBaker.compositeOver(flattened, new int[] { 0x80FF0000, 0x800000FF });

        assertArrayEquals(new int[] { 0xFF80007F, 0xC05500AA }, flattened);
    }
}
