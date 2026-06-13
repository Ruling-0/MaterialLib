package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaterialIndexTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    private Material material(String modid, String name) {
        return registry.newMaterial(modid, name, texture)
            .build();
    }

    @Test
    void indicesAreAssignedInAscendingKeyOrder() {
        Material bIron = material("bmod", "Iron");
        Material aZinc = material("amod", "Zinc");
        Material aAaa = material("amod", "Aaa");
        registry.resolve();

        assertEquals(0, aAaa.getIndex());
        assertEquals(1, aZinc.getIndex());
        assertEquals(2, bIron.getIndex());
    }

    @Test
    void keyOrderComparesFullKeyNotJustName() {
        Material amodZzz = material("amod", "Zzz");
        Material bmodAaa = material("bmod", "Aaa");
        registry.resolve();

        assertEquals(0, amodZzz.getIndex());
        assertEquals(1, bmodAaa.getIndex());
    }

    @Test
    void keyOrderIsCaseSensitive() {
        Material upper = material("testmod", "Zinc");
        Material lower = material("testmod", "aluminium");
        registry.resolve();

        assertEquals(0, upper.getIndex());
        assertEquals(1, lower.getIndex());
    }

    @Test
    void getMaterialByIndexRoundTrips() {
        Material first = material("amod", "First");
        Material second = material("amod", "Second");
        registry.resolve();

        assertEquals(first, registry.getMaterialByIndex(first.getIndex()));
        assertEquals(second, registry.getMaterialByIndex(second.getIndex()));
    }

    @Test
    void singleMaterialGetsIndexZero() {
        Material only = material("amod", "Only");
        registry.resolve();

        assertEquals(0, only.getIndex());
        assertEquals(only, registry.getMaterialByIndex(0));
    }

    @Test
    void getMaterialByIndexReturnsNullOutsideRange() {
        material("amod", "Only");
        registry.resolve();

        assertNull(registry.getMaterialByIndex(-1));
        assertNull(registry.getMaterialByIndex(1));
    }

    @Test
    void emptyRegistryResolvesWithNoIndices() {
        registry.resolve();

        assertTrue(registry.getMaterials().isEmpty());
        assertNull(registry.getMaterialByIndex(0));
        assertNull(registry.getMaterialByIndex(-1));
    }

    @Test
    void getIndexThrowsBeforeResolve() {
        Material material = material("amod", "Early");

        assertThrows(IllegalStateException.class, material::getIndex);
    }

    @Test
    void getMaterialByIndexThrowsBeforeResolve() {
        material("amod", "Early");

        assertThrows(IllegalStateException.class, () -> registry.getMaterialByIndex(0));
    }
}
