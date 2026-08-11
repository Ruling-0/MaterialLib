package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BlockShapeBuilderTest {

    @Test
    void aVariantBaseWithoutVariantsFailsLoudly() {
        BlockShapeBuilder builder = new BlockShapeBuilder("testmod", "ore").variantBase("stone", "minecraft:stone");

        assertThrows(IllegalStateException.class, builder::build);
    }
}
