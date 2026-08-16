package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BlockShapeBuilderTest {

    @Test
    void aVariantBaseWithoutVariantsFailsLoudly() {
        BlockShapeBuilder builder = new BlockShapeBuilder("testmod", "ore").variantBase("stone", "minecraft:stone");

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void harvestToolDefaultsToPickaxeOnlyWithAHarvestLevelHook() {
        assertEquals(
            "wrench",
            blockWith(new BlockBehavior(null, null, null, (m, v) -> 1, "wrench")).getHarvestTool(0));
        assertEquals("wrench", blockWith(new BlockBehavior(null, null, null, null, "wrench")).getHarvestTool(0));
        assertEquals(
            "pickaxe",
            blockWith(new BlockBehavior(null, null, null, (m, v) -> 1, null)).getHarvestTool(0));
        assertNull(blockWith(BlockBehavior.NONE).getHarvestTool(0));
    }

    private static ShapeBlock blockWith(BlockBehavior behavior) {
        return new ShapeBlock(
            "testmod",
            "frame",
            "%s Frame",
            new String[] { "frame" },
            null,
            null,
            null,
            behavior,
            null);
    }
}
