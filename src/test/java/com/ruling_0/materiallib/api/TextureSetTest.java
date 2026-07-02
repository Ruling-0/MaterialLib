package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class TextureSetTest {

    @Test
    void iconPathPointsIntoMaterialsFolder() {
        TextureSet set = TextureSet.of("testmod", "shiny");
        assertEquals("testmod:materials/shiny/gear", set.iconPath("gear"));
    }

    @Test
    void equalityIsByModidAndName() {
        assertEquals(TextureSet.of("testmod", "shiny"), TextureSet.of("testmod", "shiny"));
        assertNotEquals(TextureSet.of("testmod", "shiny"), TextureSet.of("testmod", "dull"));
        assertNotEquals(TextureSet.of("testmod", "shiny"), TextureSet.of("othermod", "shiny"));
    }
}
