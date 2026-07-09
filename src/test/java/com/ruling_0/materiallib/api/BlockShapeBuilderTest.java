package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BlockShapeBuilderTest {

    private final BlockShapeBuilder builder = new BlockShapeBuilder("testmod", "block");

    @Test
    void iconPathRejectsANullPather() {
        assertThrows(NullPointerException.class, () -> builder.iconPath(null));
    }
}
