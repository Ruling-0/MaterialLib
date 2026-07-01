package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NamesTest {

    @Test
    void validateOreDictsRejectsAnEmptyPrefixList() {
        assertThrows(IllegalArgumentException.class, () -> Names.validateOreDicts());
    }
}
