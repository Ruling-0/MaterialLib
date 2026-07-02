package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ShapeNamingTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("examplemod", "shiny");

    private Material material(String modid, String name) {
        return registry.newMaterial(modid, name, texture)
            .build();
    }

    @Test
    void materialNameKeyIsModidAndName() {
        Material iron = material("examplemod", "TestIron");

        assertEquals("material.examplemod.TestIron", ShapeNaming.materialNameKey(iron));
    }

    @Test
    void overrideKeyCombinesShapeAndMaterial() {
        Material iron = material("examplemod", "TestIron");
        TestShape gear = new TestShape("othermod", "gear");

        assertEquals("shape.othermod.gear.examplemod.TestIron", ShapeNaming.overrideKey(gear, iron));
    }

    @Test
    void requireValidFormatRejectsAFormatThatCannotTakeAStringArgument() {
        assertThrows(IllegalArgumentException.class, () -> ShapeNaming.requireValidFormat("%d"));
    }

    @Test
    void requireValidFormatAcceptsALiteralFormatWithoutAPlaceholder() {
        assertEquals("Bucket of Lava", ShapeNaming.requireValidFormat("Bucket of Lava"));
    }
}
