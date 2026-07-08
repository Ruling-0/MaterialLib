package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ShapeFluidTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("examplemod", "shiny");

    @Test
    void fluidNameIsShapeAndMaterialLowercasedByDefault() {
        Material iron = registry.newMaterial("examplemod", "TestIron", texture)
            .build();
        ShapeFluid molten = new ShapeFluid("examplemod", "molten", "Molten %s");

        assertEquals("molten.testiron", molten.fluidName(iron));
    }

    @Test
    void aCustomNamerOverridesTheDefaultName() {
        Material iron = registry.newMaterial("examplemod", "TestIron", texture)
            .build();
        FluidNamer namer = (shape, material) -> "legacy." + material.getName()
            .toLowerCase();
        ShapeFluid molten = new ShapeFluid("examplemod", "molten", "Molten %s", namer);

        assertEquals("legacy.testiron", molten.fluidName(iron));
    }
}
