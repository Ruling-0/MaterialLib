package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaterialRefTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    @Test
    void resolveRejectsAnUnassignedField() {
        assertThrows(IllegalStateException.class, () -> MaterialRef.of(() -> null).resolve());
    }

    @Test
    void toStringNeverThrows() {
        Material material = registry.newMaterial("testmod", "TestIron", texture).build();
        registry.resolve();

        assertTrue(MaterialRef.of(() -> material).toString().contains("TestIron"));
        assertEquals("MaterialRef[unresolved]", MaterialRef.of(() -> null).toString());
        assertEquals("MaterialRef[unresolved]", MaterialRef.of(() -> { throw new IllegalStateException("boom"); })
            .toString());
    }
}
