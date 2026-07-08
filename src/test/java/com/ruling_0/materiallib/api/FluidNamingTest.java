package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class FluidNamingTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("examplemod", "shiny");
    private final Shape molten = new TestShape("examplemod", "molten");
    private final Material iron = registry.newMaterial("examplemod", "TestIron", texture)
        .build();

    @Test
    void aValidLowercaseNameIsAcceptedAndRecorded() {
        Set<String> usedNames = new HashSet<>();

        assertEquals("molten.testiron", FluidNaming.validate("molten.testiron", molten, iron, usedNames));
        assertTrue(usedNames.contains("molten.testiron"));
    }

    @Test
    void aNullNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FluidNaming.validate(null, molten, iron, new HashSet<>()));
    }

    @Test
    void anEmptyNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FluidNaming.validate("", molten, iron, new HashSet<>()));
    }

    @Test
    void aNonLowercaseNameIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> FluidNaming.validate("Molten.TestIron", molten, iron, new HashSet<>()));
    }

    @Test
    void aNameAlreadyUsedThisResolveIsRejected() {
        Set<String> usedNames = new HashSet<>();
        FluidNaming.validate("molten.testiron", molten, iron, usedNames);

        assertThrows(
            IllegalStateException.class,
            () -> FluidNaming.validate("molten.testiron", molten, iron, usedNames));
    }
}
