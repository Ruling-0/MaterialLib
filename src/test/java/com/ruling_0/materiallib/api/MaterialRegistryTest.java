package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaterialRegistryTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    @Test
    void buildRegistersAndResolves() {
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .build();
        registry.resolve();

        assertSame(material, registry.getMaterial("testmod", "TestIron"));
        assertEquals("testmod", material.getModId());
        assertEquals("TestIron", material.getName());
        assertEquals("testmod:TestIron", material.getKey());
        assertEquals("TestIron", StandardProperties.NAME.get(material));
        assertEquals(texture, StandardProperties.TEXTURE_SET.get(material));
        assertNull(material.getFamily());
        assertTrue(material.getShapes().isEmpty());
        assertTrue(registry.getMaterials().contains(material));
        assertTrue(registry.isResolved());
    }

    @Test
    void duplicateMaterialKeyThrows() {
        registry.newMaterial("testmod", "TestIron", texture)
            .build();
        MaterialBuilder duplicate = registry.newMaterial("testmod", "TestIron", texture);
        assertThrows(IllegalStateException.class, duplicate::build);
    }

    @Test
    void duplicateFamilyKeyThrows() {
        registry.newFamily("testmod", "Alloys")
            .build();
        FamilyBuilder duplicate = registry.newFamily("testmod", "Alloys");
        assertThrows(IllegalStateException.class, duplicate::build);
    }

    @Test
    void sameNameUnderDifferentModidsAllowed() {
        registry.newMaterial("modone", "Iron", texture)
            .build();
        registry.newMaterial("modtwo", "Iron", texture)
            .build();
        registry.resolve();
        assertEquals(2, registry.getMaterials().size());
    }

    @Test
    void invalidIdentifiersThrow() {
        assertThrows(IllegalArgumentException.class, () -> registry.newMaterial(null, "TestIron", texture));
        assertThrows(IllegalArgumentException.class, () -> registry.newMaterial("testmod", "", texture));
        assertThrows(IllegalArgumentException.class, () -> registry.newMaterial("testmod", "Test:Iron", texture));
        assertThrows(IllegalArgumentException.class, () -> registry.newMaterial("testmod", "Test Iron", texture));
        assertThrows(NullPointerException.class, () -> registry.newMaterial("testmod", "TestIron", null));
    }

    @Test
    void buildTwiceThrows() {
        MaterialBuilder builder = registry.newMaterial("testmod", "TestIron", texture);
        builder.build();
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void registrationAfterResolveThrows() {
        registry.resolve();
        MaterialBuilder material = registry.newMaterial("testmod", "TestIron", texture);
        assertThrows(IllegalStateException.class, material::build);
        FamilyBuilder family = registry.newFamily("testmod", "Alloys");
        assertThrows(IllegalStateException.class, family::build);
        MaterialEdit edit = registry.editMaterial("testmod", "TestIron");
        assertThrows(IllegalStateException.class, () -> edit.setTint(0));
    }

    @Test
    void readBeforeResolveThrows() {
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .build();
        assertThrows(IllegalStateException.class, material::getShapes);
        assertThrows(IllegalStateException.class, material::getFamily);
        assertThrows(IllegalStateException.class, () -> StandardProperties.TINT.get(material));
    }

    @Test
    void resolveTwiceThrows() {
        registry.resolve();
        assertThrows(IllegalStateException.class, registry::resolve);
    }

    @Test
    void editsOfMissingTargetsAreSkipped() {
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .build();
        registry.editMaterial("absentmod", "Missing")
            .setTint(0xFF0000FF);
        registry.editFamily("absentmod", "Missing")
            .setTint(0xFF0000FF);
        registry.resolve();
        assertEquals(0xFFFFFFFF, StandardProperties.TINT.get(material));
    }
}
