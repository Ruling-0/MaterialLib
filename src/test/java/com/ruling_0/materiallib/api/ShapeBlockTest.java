package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/// Headless coverage for [ShapeBlock]'s block-level color callbacks on a composite. The tinted branch a plain
/// block takes resolves a material through the process-wide [MaterialRegistry#instance], which throws on lookup
/// before MaterialLib's preInit; the compositing itself needs a live client.
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

    @Test
    void aCompositeReportsWhiteToBlockLevelColorCallbacks() {
        assertEquals(0xFFFFFF, withBaseTexture.getRenderColor(0));
        assertEquals(0xFFFFFF, withBaseTexture.getRenderColor(7));
        assertEquals(0xFFFFFF, withBaseTexture.getRenderColor(999));
    }
}
