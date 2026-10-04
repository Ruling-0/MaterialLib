package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ShapeNamingTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("examplemod", "shiny");

    private Material material(String modid, String name) {
        return registry.newMaterial(modid, name, texture).build();
    }

    @Test
    void materialNameKeyIsModidAndName() {
        Material iron = material("examplemod", "TestIron");
        registry.resolve();

        assertEquals("material.examplemod.TestIron", ShapeNaming.materialNameKey(iron));
    }

    @Test
    void aDeclaredDisplayNameKeyReplacesTheMaterialNameKey() {
        Material iron = registry.newMaterial("examplemod", "TestIron", texture)
            .setProperty(StandardProperties.DISPLAY_NAME_KEY, "Material.testiron")
            .build();
        registry.resolve();

        assertEquals("Material.testiron", ShapeNaming.materialNameKey(iron));
    }

    @Test
    void aMaterialFormatKeyPrecedesTheShapeFormatKeyForItsShapeOnly() {
        Material glass = registry.newMaterial("examplemod", "TestGlass", texture)
            .setProperty(StandardProperties.DISPLAY_NAME_FORMAT_KEYS, Map.of("plate", "format.pane"))
            .build();
        registry.resolve();
        TestShape plate = new TestShape("examplemod", "plate");
        plate.setProperty(StandardProperties.DISPLAY_NAME_FORMAT_KEY, "format.plate");
        TestShape foil = new TestShape("examplemod", "foil");
        foil.setProperty(StandardProperties.DISPLAY_NAME_FORMAT_KEY, "format.foil");

        assertEquals(List.of("format.pane", "format.plate"), ShapeNaming.formatKeys(plate, "plate", glass));
        assertEquals(List.of("format.foil"), ShapeNaming.formatKeys(foil, "foil", glass));
    }

    @Test
    void aTranslatedFormatThatIsInvalidFallsBackToTheDeclaredFormat() {
        assertEquals("Iron Gear", ShapeNaming.format("%s Zahnrad %d", "%s Gear", "Iron"));
        assertEquals("Iron Zahnrad", ShapeNaming.format("%s Zahnrad", "%s Gear", "Iron"));
    }

    @Test
    void overrideKeyCombinesShapeAndMaterial() {
        Material iron = material("examplemod", "TestIron");
        TestShape gear = new TestShape("othermod", "gear");

        assertEquals("shape.othermod.gear.examplemod.TestIron", ShapeNaming.overrideKey(gear, iron));
    }

    @Test
    void overrideKeyByNameMatchesTheShapeForm() {
        Material iron = material("examplemod", "TestIron");

        assertEquals(
            "shape.examplemod.testOre.examplemod.TestIron",
            ShapeNaming.overrideKey("examplemod", "testOre", iron));
    }

    @Test
    void requireValidFormatRejectsAFormatThatCannotTakeAStringArgument() {
        assertThrows(IllegalArgumentException.class, () -> ShapeNaming.requireValidFormat("%d"));
    }

    @Test
    void requireValidFormatAcceptsALiteralFormatWithoutAPlaceholder() {
        assertEquals("Bucket of Lava", ShapeNaming.requireValidFormat("Bucket of Lava"));
    }

    @Test
    void variantBlockNameCombinesShapeAndVariant() {
        assertEquals("ore_stone", ShapeNaming.variantBlockName("ore", "stone"));
    }
}
