package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FluidShapeBuilderTest {

    private final FluidShapeBuilder builder = new FluidShapeBuilder("testmod", "molten");

    @Test
    void iconPathRejectsANullPather() {
        assertThrows(NullPointerException.class, () -> builder.iconPath((FluidIconPather) null));
    }

    @Test
    void iconPathRejectsANullConstantPath() {
        assertThrows(NullPointerException.class, () -> builder.iconPath((String) null));
    }
}
