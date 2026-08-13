package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/// Headless coverage for [ShapeBlock]'s base-texture and render-type bookkeeping. [ShapeBlock#getRenderColor]'s
/// tinted branch resolves a material through the process-wide [MaterialRegistry#instance], which throws on lookup
/// before MaterialLib's preInit, so only the untinted (layer 0) branch is exercised; the compositing itself needs
/// a live client.
class ShapeBlockTest {

    private final ShapeBlock withBaseTexture = new ShapeBlock(
        "testmod",
        "ore_stone",
        "%s Ore",
        new String[] { "ore" },
        "ore",
        "stone",
        "minecraft:stone",
        BlockBehavior.NONE,
        null);

    private final ShapeBlock withoutBaseTexture = new ShapeBlock("testmod", "ingot", "%s Ingot", "ingot");

    @Test
    void hasBaseTextureReflectsWhetherAVariantBaseWasDeclared() {
        assertTrue(withBaseTexture.hasBaseTexture());
        assertFalse(withoutBaseTexture.hasBaseTexture());
    }

    @Test
    void layerZeroIsUntintedWhenABaseTextureExists() {
        assertEquals(0xFFFFFF, withBaseTexture.getRenderColor(0));
        assertEquals(0xFFFFFF, withBaseTexture.getRenderColor(999));
    }

    @Test
    void getRenderTypeReflectsWhateverWasSet() {
        assertEquals(0, withBaseTexture.getRenderType());
        withBaseTexture.setRenderType(64);
        try {
            assertEquals(64, withBaseTexture.getRenderType());
        }
        finally {
            withBaseTexture.setRenderType(0);
        }
    }
}
