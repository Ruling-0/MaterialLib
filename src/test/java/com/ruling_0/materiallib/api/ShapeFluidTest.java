package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    /// Constructs a [Material] directly -- [MaterialBuilder] rejects a missing [StandardProperties#TEXTURE_SET] --
    /// to pin that a fluid whose texture-set chain holds nothing registers the placeholder. Only this branch runs
    /// headless: every other outcome checks whether a texture file exists, which needs a live client.
    @Test
    void aMaterialWithNoTextureSetResolvesThePlaceholderIconPath() {
        Map<Property<?>, Object> properties = Map.of(StandardProperties.NAME, "Broken");
        Material broken = new Material(registry, "examplemod", "Broken", properties, Set.of(), List.of());
        registry.register(broken);
        registry.resolve();
        ShapeFluid molten = new ShapeFluid("examplemod", "molten", "Molten %s");

        assertEquals(ShapeIcons.EMPTY_ICON, molten.resolveIconPath(broken));
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
