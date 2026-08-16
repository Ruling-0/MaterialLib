package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Locale;

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
            .toLowerCase(Locale.ENGLISH);
        ShapeFluid molten = new ShapeFluid("examplemod", "molten", "Molten %s", namer, null);

        assertEquals("legacy.testiron", molten.fluidName(iron));
    }

    @Test
    void iconPathFallsBackToTheTextureSetWithoutAPatherOrWhenItReturnsNull() {
        Material iron = registry.newMaterial("examplemod", "TestIron", texture)
            .build();
        registry.resolve();
        ShapeFluid noPather = new ShapeFluid("examplemod", "molten", "Molten %s");
        ShapeFluid nullPather = new ShapeFluid("examplemod", "molten", "Molten %s", null, null,
            (shape, material) -> null);

        assertEquals(texture.iconPath("molten"), noPather.iconPath(iron));
        assertEquals(texture.iconPath("molten"), nullPather.iconPath(iron));
    }

    @Test
    void iconPathUsesThePatherWhenItReturnsAPath() {
        Material iron = registry.newMaterial("examplemod", "TestIron", texture)
            .build();
        registry.resolve();
        FluidIconPather pather = (shape, material) -> "gregtech:fluids/fluid.molten";
        ShapeFluid molten = new ShapeFluid("examplemod", "molten", "Molten %s", null, null, pather);

        assertEquals("gregtech:fluids/fluid.molten", molten.iconPath(iron));
    }

    @Test
    void namerDivergenceIsDetectedPerServedMaterial() {
        Material iron = registry.newMaterial("examplemod", "TestIron", texture)
            .build();
        ShapeFluid canonical = new ShapeFluid("examplemod", "molten", "Molten %s");
        canonical.bindServedMaterials(new Material[] { iron });
        ShapeFluid candidate = new ShapeFluid("othermod", "molten", "Molten %s",
            (shape, material) -> "legacy." + material.getName()
                .toLowerCase(Locale.ENGLISH),
            null);

        assertSame(iron, canonical.firstNameDivergence(candidate));
    }

    @Test
    void agreeingNamersReportNoDivergence() {
        Material iron = registry.newMaterial("examplemod", "TestIron", texture)
            .build();
        ShapeFluid canonical = new ShapeFluid("examplemod", "molten", "Molten %s");
        canonical.bindServedMaterials(new Material[] { iron });
        ShapeFluid candidate = new ShapeFluid("othermod", "molten", "Molten %s");

        assertNull(canonical.firstNameDivergence(candidate));
    }
}
