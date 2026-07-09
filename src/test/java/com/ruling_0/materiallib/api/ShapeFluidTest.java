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
        ShapeFluid molten = new ShapeFluid("examplemod", "molten", "Molten %s", namer, null);

        assertEquals("legacy.testiron", molten.fluidName(iron));
    }

    @Test
    void iconPathFallsBackToTheTextureSetWhenNoPatherIsSet() {
        Material iron = registry.newMaterial("examplemod", "TestIron", texture)
            .build();
        registry.resolve();
        ShapeFluid molten = new ShapeFluid("examplemod", "molten", "Molten %s");

        assertEquals(texture.iconPath("molten"), molten.iconPath(iron));
    }

    @Test
    void iconPathUsesThePatherWhenItReturnsAPath() {
        Material iron = registry.newMaterial("examplemod", "TestIron", texture)
            .build();
        FluidIconPather pather = (shape, material) -> "gregtech:fluids/fluid.molten";
        ShapeFluid molten = new ShapeFluid("examplemod", "molten", "Molten %s", null, null, pather);

        assertEquals("gregtech:fluids/fluid.molten", molten.iconPath(iron));
    }

    @Test
    void iconPathFallsBackToTheTextureSetWhenThePatherReturnsNull() {
        Material iron = registry.newMaterial("examplemod", "TestIron", texture)
            .build();
        registry.resolve();
        FluidIconPather pather = (shape, material) -> null;
        ShapeFluid molten = new ShapeFluid("examplemod", "molten", "Molten %s", null, null, pather);

        assertEquals(texture.iconPath("molten"), molten.iconPath(iron));
    }
}
