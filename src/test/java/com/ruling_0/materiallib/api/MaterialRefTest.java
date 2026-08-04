package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaterialRefTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    private Material resolvedMaterial(String name) {
        Material material = registry.newMaterial("testmod", name, texture)
            .build();
        registry.resolve();
        return material;
    }

    @Test
    void resolveReadsTheSupplier() {
        Material material = resolvedMaterial("TestIron");

        assertSame(material, MaterialRef.of(() -> material).resolve());
    }

    @Test
    void resolveRejectsAnUnassignedField() {
        assertThrows(IllegalStateException.class, () -> MaterialRef.of(() -> null).resolve());
    }

    @Test
    void ofRejectsANullSupplier() {
        assertThrows(NullPointerException.class, () -> MaterialRef.of(null));
    }

    @Test
    void toStringNeverThrows() {
        Material material = resolvedMaterial("TestIron");

        assertTrue(MaterialRef.of(() -> material).toString().contains("TestIron"));
        assertEquals("MaterialRef[unresolved]", MaterialRef.of(() -> null).toString());
        assertEquals("MaterialRef[unresolved]", MaterialRef.of(() -> { throw new IllegalStateException("boom"); })
            .toString());
    }
}
