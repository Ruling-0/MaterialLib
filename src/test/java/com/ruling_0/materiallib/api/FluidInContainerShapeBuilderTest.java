package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FluidInContainerShapeBuilderTest {

    private final FluidInContainerShapeBuilder builder = new FluidInContainerShapeBuilder("testmod", "cell");

    @Test
    void fluidRejectsAnEmptyShapeList() {
        assertThrows(IllegalArgumentException.class, builder::fluid);
    }

    @Test
    void emptyContainerRejectsAnIdentifierWithoutAModid() {
        assertThrows(IllegalArgumentException.class, () -> builder.emptyContainer("gt.metaitem.01", 32000));
    }
}
