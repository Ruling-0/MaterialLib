package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class VariantSetTest {

    @Test
    void namesKeepDeclarationOrder() {
        VariantSet<String> set = VariantSet.of(List.of("stone", "granite", "andesite"), v -> v);

        assertEquals(List.of("stone", "granite", "andesite"), set.names());
    }

    @Test
    void firstReturnsTheFirstDeclaredVariantsValue() {
        VariantSet<String> set = VariantSet.of(List.of("stone", "granite"), v -> v + "Value");

        assertEquals("stoneValue", set.first());
    }

    @Test
    void getReturnsTheNamedVariantsValue() {
        VariantSet<String> set = VariantSet.of(List.of("stone", "granite"), v -> v + "Value");

        assertEquals("graniteValue", set.get("granite"));
    }

    @Test
    void getRejectsAnUndeclaredVariant() {
        VariantSet<String> set = VariantSet.of(List.of("stone"), v -> v);

        assertThrows(IllegalArgumentException.class, () -> set.get("granite"));
    }

    @Test
    void anEmptyVariantListIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> VariantSet.of(List.of(), v -> v));
    }

    @Test
    void aNullVariantListIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> VariantSet.of(null, v -> v));
    }

    @Test
    void aRepeatedVariantNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> VariantSet.of(List.of("stone", "stone"), v -> v));
    }

    @Test
    void anInvalidVariantNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> VariantSet.of(List.of("bad name"), v -> v));
    }

    @Test
    void valuesFollowDeclarationOrder() {
        VariantSet<String> set = VariantSet.of(List.of("b", "a"), v -> v);

        assertEquals(List.of("b", "a"), List.copyOf(set.values()));
    }
}
