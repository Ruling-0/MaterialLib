package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/// Pins the level array [PaletteSprite] hands `loadSprite`. The array is sized by the client's mipmap setting and
/// its tail is deliberately null, which no headless or server-side run can observe: a ladder holding only the art
/// loads and stitches fine, then crashes the client in `TextureUtil.generateMipmapData`.
class PaletteSpriteTest {

    /// One slot per mipmap level sits above the art -- four of them at the default client setting -- and every one
    /// of them is left for the atlas to fill.
    @Test
    void theFrameLadderHoldsTheArtAndOneNullSlotPerMipmapLevel() {
        for (int mipmapLevels : new int[] { 0, 1, 4 }) {
            BufferedImage art = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            BufferedImage[] levels = PaletteSprite.frameLadder(art, mipmapLevels);

            assertEquals(1 + mipmapLevels, levels.length, "ladder length at " + mipmapLevels + " mipmap levels");
            assertSame(art, levels[0]);
            for (int level = 1; level < levels.length; level++) {
                assertNull(levels[level], "level " + level + " must be left for the atlas to generate");
            }
        }
    }
}
