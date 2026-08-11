package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ShapeFluidInContainerTest {

    @Test
    void aNonFluidShapeIsRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new TestContainer(new TestShape("testmod", "plate")));
    }

    private static final class TestContainer extends ShapeFluidInContainer {

        TestContainer(Shape fluidShape) {
            super("testmod", "cell", "%s Cell", fluidShape, (EmptyContainer) null, 1000, "cell");
        }
    }
}
