package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

class ShapeFluidInContainerTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");
    private final ShapeFluid liquid = new ShapeFluid("testmod", "liquid", "Liquid %s");
    private final ShapeFluid gas = new ShapeFluid("testmod", "gas", "Gaseous %s");

    @Test
    void selectFluidPicksTheOnlyShapeThatServesTheMaterial() {
        Material iron = registry.newMaterial("testmod", "TestIron", texture)
            .build();
        gas.bindServedMaterials(new Material[] { iron });

        assertSame(gas, ShapeFluidInContainer.selectFluid(iron, List.of(liquid, gas)));
    }

    @Test
    void selectFluidPrefersAnEarlierListedShapeOverALaterOne() {
        Material iron = registry.newMaterial("testmod", "TestIron", texture)
            .build();
        liquid.bindServedMaterials(new Material[] { iron });
        gas.bindServedMaterials(new Material[] { iron });

        assertSame(liquid, ShapeFluidInContainer.selectFluid(iron, List.of(liquid, gas)));
    }

    @Test
    void selectFluidReturnsNullWhenNoListedShapeServesTheMaterial() {
        Material iron = registry.newMaterial("testmod", "TestIron", texture)
            .build();

        assertNull(ShapeFluidInContainer.selectFluid(iron, List.of(liquid, gas)));
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
