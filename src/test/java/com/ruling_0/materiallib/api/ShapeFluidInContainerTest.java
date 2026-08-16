package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ShapeFluidInContainerTest {

    @Test
    void aNonFluidShapeIsRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new TestContainer(new TestShape("testmod", "plate"), null));
    }

    @Test
    void emptyIconPathDerivesFromTheShapeUnlessOverridden() {
        Shape fluid = new ShapeFluid("testmod", "molten", "Molten %s");

        assertEquals("testmod:materials/cell_empty", new TestContainer(fluid, null).emptyIconPath());
        assertEquals("gregtech:items/cell_base", new TestContainer(fluid, "gregtech:items/cell_base").emptyIconPath());
    }

    private static final class TestContainer extends ShapeFluidInContainer {

        TestContainer(Shape fluidShape, String emptyIconOverride) {
            super("testmod", "cell", "%s Cell", fluidShape, (EmptyContainer) null, 1000, emptyIconOverride, "cell");
        }
    }
}
