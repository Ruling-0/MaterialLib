package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals("TestIron", material.getProperty(StandardProperties.NAME));
        assertEquals(texture, material.getProperty(StandardProperties.TEXTURE_SET));
        assertTrue(material.getFamilies().isEmpty());
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
        MaterialEdit materialEdit = registry.editMaterial("testmod", "TestIron");
        assertThrows(IllegalStateException.class, () -> materialEdit.setTint(0));
        FamilyEdit familyEdit = registry.editFamily("testmod", "Alloys");
        assertThrows(IllegalStateException.class, () -> familyEdit.setTint(0));
    }

    @Test
    void readBeforeResolveThrows() {
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .build();
        Family family = registry.newFamily("testmod", "Alloys")
            .build();
        assertThrows(IllegalStateException.class, material::getShapes);
        assertThrows(IllegalStateException.class, material::getFamilies);
        assertThrows(IllegalStateException.class, () -> material.getProperty(StandardProperties.TINT));
        assertThrows(IllegalStateException.class, family::getMaterials);
        assertThrows(IllegalStateException.class, family::getShapes);
        assertThrows(IllegalStateException.class, () -> family.getProperty(StandardProperties.TINT));
        assertThrows(IllegalStateException.class, registry::getMaterials);
        assertThrows(IllegalStateException.class, registry::getFamilies);
        assertThrows(IllegalStateException.class, material::getIndex);
        assertThrows(IllegalStateException.class, () -> registry.getMaterialByIndex(0));
    }

    @Test
    void skippedEditDoesNotStopLaterEdits() {
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .build();
        Material untouched = registry.newMaterial("testmod", "TestSilver", texture)
            .build();
        registry.editMaterial("absentmod", "Missing")
            .setTint(0xFF0000FF);
        registry.editFamily("absentmod", "Missing")
            .setTint(0xFF0000FF);
        registry.editMaterial("testmod", "TestIron")
            .setTint(0xFF00FF00);
        registry.resolve();

        assertEquals(0xFF00FF00, material.getProperty(StandardProperties.TINT));
        assertEquals(0xFFFFFFFF, untouched.getProperty(StandardProperties.TINT));
    }

    @Test
    void resolvedViewsAreUnmodifiable() {
        Shape gear = new TestShape("testmod", "gear");
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .generateShape(gear)
            .build();
        Family family = registry.newFamily("testmod", "Alloys")
            .addMaterial(material)
            .build();
        registry.resolve();

        assertThrows(UnsupportedOperationException.class, () -> material.getShapes().add(gear));
        assertThrows(UnsupportedOperationException.class, () -> material.getFamilies().remove(family));
        assertThrows(UnsupportedOperationException.class, () -> family.getMaterials().remove(material));
        assertThrows(UnsupportedOperationException.class, () -> family.getShapes().add(gear));
        assertThrows(UnsupportedOperationException.class, () -> registry.getMaterials().clear());
        assertThrows(
            UnsupportedOperationException.class,
            () -> material.getOwnProperties().put(StandardProperties.TINT, 0));
    }

    @Test
    void resolveTwiceThrows() {
        registry.resolve();
        assertThrows(IllegalStateException.class, registry::resolve);
    }
}
