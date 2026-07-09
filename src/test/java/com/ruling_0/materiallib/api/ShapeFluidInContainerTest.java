package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ShapeFluidInContainerTest {

    @Test
    void aNonFluidShapeIsRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new TestContainer(new TestShape("testmod", "plate")));
    }

    private static final class TestContainer extends ShapeFluidInContainer {

        TestContainer(Shape fluidShape) {
            super("testmod", "cell", "%s Cell", fluidShape, (EmptyContainer) null, 1000, null, "cell");
        }
    }

    @Test
    void resolveEmptyIconPathDefaultsToTheDerivedPathWhenNoOverrideIsSet() {
        assertEquals(
            "testmod:materials/cell_empty",
            ShapeFluidInContainer.resolveEmptyIconPath("testmod", "cell", null));
    }

    @Test
    void resolveEmptyIconPathUsesTheOverrideWhenSet() {
        assertEquals(
            "gregtech:items/cell_base",
            ShapeFluidInContainer.resolveEmptyIconPath("testmod", "cell", "gregtech:items/cell_base"));
    }
}
