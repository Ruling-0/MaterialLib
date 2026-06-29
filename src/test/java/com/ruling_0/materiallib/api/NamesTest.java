package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class NamesTest {

    @Test
    void validateOreDictsRejectsAnEmptyPrefixList() {
        assertThrows(IllegalArgumentException.class, () -> Names.validateOreDicts());
    }

    @Test
    void validateOreDictsReturnsTheValidatedPrefixes() {
        assertEquals(List.of("gear", "cog"), Names.validateOreDicts("gear", "cog"));
    }
}
