package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ShapeBlockVariantsTest {

    @Test
    void aKeyNamingADeclaredVariantIsAccepted() {
        assertDoesNotThrow(
            () -> ShapeBlockVariants
                .requireDeclaredVariants(List.of("stone", "granite"), Set.of("stone"), "a variant base texture"));
    }

    @Test
    void aKeyNamingAnUndeclaredVariantFailsLoudly() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> ShapeBlockVariants
                .requireDeclaredVariants(List.of("stone"), Set.of("granite"), "a variant base texture"));

        assertTrue(e.getMessage().contains("granite"));
        assertTrue(e.getMessage().contains("a variant base texture"));
    }

    @Test
    void anEmptyKeySetIsAlwaysAccepted() {
        assertDoesNotThrow(() -> ShapeBlockVariants.requireDeclaredVariants(List.of("stone"), Set.of(), "anything"));
    }
}
