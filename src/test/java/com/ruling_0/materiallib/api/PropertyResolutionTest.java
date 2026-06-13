package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PropertyResolutionTest {

    private static final Property<Integer> MELTING_POINT = Property.of("testmod", "meltingPoint");
    private static final Property<Integer> DURABILITY = Property.of("testmod", "durability", 100);

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    @Test
    void materialValueOverridesFamilyValueOverridesDefault() {
        registry.newFamily("testmod", "Alloys")
            .setProperty(MELTING_POINT, 1000)
            .build();
        Material own = registry.newMaterial("testmod", "WithOwn", texture)
            .setProperty(MELTING_POINT, 2000)
            .addToFamily("testmod", "Alloys")
            .build();
        Material inherited = registry.newMaterial("testmod", "FromFamily", texture)
            .addToFamily("testmod", "Alloys")
            .build();
        Material standalone = registry.newMaterial("testmod", "Standalone", texture)
            .build();
        registry.resolve();

        assertEquals(2000, own.getProperty(MELTING_POINT));
        assertEquals(1000, inherited.getProperty(MELTING_POINT));
        assertNull(standalone.getProperty(MELTING_POINT));
        assertEquals(100, standalone.getProperty(DURABILITY));
    }

    @Test
    void tintDefaultsToWhite() {
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .build();
        Material tinted = registry.newMaterial("testmod", "TestGold", texture)
            .setTint(0xFFFFCC00)
            .build();
        registry.resolve();

        assertEquals(0xFFFFFFFF, material.getProperty(StandardProperties.TINT));
        assertEquals(0xFFFFCC00, tinted.getProperty(StandardProperties.TINT));
    }

    @Test
    void familyTintAppliesToMembers() {
        registry.newFamily("testmod", "Golds")
            .setTint(0xFFFFCC00)
            .build();
        Material member = registry.newMaterial("testmod", "TestGold", texture)
            .addToFamily("testmod", "Golds")
            .build();
        registry.resolve();

        assertEquals(0xFFFFCC00, member.getProperty(StandardProperties.TINT));
    }

    @Test
    void isSetIgnoresDefaults() {
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .setProperty(MELTING_POINT, 1500)
            .build();
        registry.resolve();

        assertTrue(material.hasProperty(MELTING_POINT));
        assertFalse(material.hasProperty(DURABILITY));
        assertEquals(100, material.getProperty(DURABILITY));
    }

    @Test
    void editsApplyRegardlessOfRegistrationOrder() {
        registry.editMaterial("testmod", "TestIron")
            .setProperty(MELTING_POINT, 1800);
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .build();
        registry.resolve();

        assertEquals(1800, material.getProperty(MELTING_POINT));
    }

    @Test
    void lastEditWins() {
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .build();
        registry.editMaterial("testmod", "TestIron")
            .setProperty(MELTING_POINT, 1000);
        registry.editMaterial("testmod", "TestIron")
            .setProperty(MELTING_POINT, 2000);
        registry.resolve();

        assertEquals(2000, material.getProperty(MELTING_POINT));
    }

    @Test
    void removePropertyRestoresFamilyValue() {
        registry.newFamily("testmod", "Alloys")
            .setProperty(MELTING_POINT, 1000)
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .setProperty(MELTING_POINT, 2000)
            .addToFamily("testmod", "Alloys")
            .build();
        registry.editMaterial("testmod", "TestIron")
            .removeProperty(MELTING_POINT);
        registry.resolve();

        assertEquals(1000, material.getProperty(MELTING_POINT));
    }

    @Test
    void familyEditSetsAndRemovesProperties() {
        registry.newFamily("testmod", "Alloys")
            .setProperty(MELTING_POINT, 1000)
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily("testmod", "Alloys")
            .build();
        registry.editFamily("testmod", "Alloys")
            .setProperty(DURABILITY, 500);
        registry.editFamily("testmod", "Alloys")
            .removeProperty(MELTING_POINT);
        registry.resolve();

        assertEquals(500, material.getProperty(DURABILITY));
        assertNull(material.getProperty(MELTING_POINT));
    }

    @Test
    void identityPropertiesCannotBeSetOrRemoved() {
        MaterialBuilder builder = registry.newMaterial("testmod", "TestIron", texture);
        assertThrows(IllegalArgumentException.class, () -> builder.setProperty(StandardProperties.NAME, "Imposter"));
        assertThrows(
            IllegalArgumentException.class,
            () -> builder.setProperty(StandardProperties.TEXTURE_SET, texture));

        FamilyBuilder familyBuilder = registry.newFamily("testmod", "Alloys");
        assertThrows(
            IllegalArgumentException.class,
            () -> familyBuilder.setProperty(StandardProperties.NAME, "Imposter"));

        MaterialEdit materialEdit = registry.editMaterial("testmod", "TestIron");
        assertThrows(
            IllegalArgumentException.class,
            () -> materialEdit.setProperty(StandardProperties.NAME, "Imposter"));
        assertThrows(
            IllegalArgumentException.class,
            () -> materialEdit.removeProperty(StandardProperties.TEXTURE_SET));

        FamilyEdit familyEdit = registry.editFamily("testmod", "Alloys");
        assertThrows(
            IllegalArgumentException.class,
            () -> familyEdit.setProperty(StandardProperties.TEXTURE_SET, texture));
        assertThrows(IllegalArgumentException.class, () -> familyEdit.removeProperty(StandardProperties.NAME));
    }

    @Test
    void nameIsIdentityDerived() {
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .build();
        registry.resolve();

        assertEquals("TestIron", material.getProperty(StandardProperties.NAME));
        assertEquals(texture, material.getProperty(StandardProperties.TEXTURE_SET));
    }

    @Test
    void collidingFamilyValuesResolveAlphabetically() {
        registry.newFamily("testmod", "Beta")
            .setProperty(MELTING_POINT, 2000)
            .build();
        registry.newFamily("testmod", "Alpha")
            .setProperty(MELTING_POINT, 1000)
            .build();
        Material inherited = registry.newMaterial("testmod", "Inherited", texture)
            .addToFamily("testmod", "Beta")
            .addToFamily("testmod", "Alpha")
            .build();
        Material overriding = registry.newMaterial("testmod", "Overriding", texture)
            .setProperty(MELTING_POINT, 3000)
            .addToFamily("testmod", "Beta")
            .addToFamily("testmod", "Alpha")
            .build();
        registry.resolve();

        assertEquals(1000, inherited.getProperty(MELTING_POINT));
        assertEquals(3000, overriding.getProperty(MELTING_POINT));
    }

    @Test
    void alphabeticalOrderComparesFullKey() {
        registry.newFamily("bmod", "Aaa")
            .setProperty(MELTING_POINT, 2000)
            .build();
        registry.newFamily("amod", "Zzz")
            .setProperty(MELTING_POINT, 1000)
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily("bmod", "Aaa")
            .addToFamily("amod", "Zzz")
            .build();
        registry.resolve();

        assertEquals(1000, material.getProperty(MELTING_POINT));
    }

    @Test
    void isSetFalseWhenNoFamilySetsProperty() {
        registry.newFamily("testmod", "Alpha")
            .build();
        registry.newFamily("testmod", "Beta")
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily("testmod", "Alpha")
            .addToFamily("testmod", "Beta")
            .build();
        registry.resolve();

        assertFalse(material.hasProperty(DURABILITY));
        assertEquals(100, material.getProperty(DURABILITY));
    }

    @Test
    void uncontestedFamilyValueAppliesRegardlessOfOrder() {
        registry.newFamily("testmod", "Alpha")
            .build();
        registry.newFamily("testmod", "Beta")
            .setProperty(MELTING_POINT, 2000)
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily("testmod", "Alpha")
            .addToFamily("testmod", "Beta")
            .build();
        registry.resolve();

        assertEquals(2000, material.getProperty(MELTING_POINT));
        assertTrue(material.hasProperty(MELTING_POINT));
    }

    @Test
    void propertyKeysCompareByIdentity() {
        Property<Integer> first = Property.of("testmod", "sameName");
        Property<Integer> second = Property.of("testmod", "sameName");
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .setProperty(first, 42)
            .build();
        registry.resolve();

        assertEquals(42, material.getProperty(first));
        assertNull(material.getProperty(second));
        assertFalse(material.hasProperty(second));
    }
}
