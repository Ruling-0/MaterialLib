package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

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
    void persistedMaterialsKeepTheirIndex() {
        Map<String, Integer> persisted = new LinkedHashMap<>();
        persisted.put("amod:Zinc", 5);
        persisted.put("amod:Iron", 2);
        registry.setPersistedIndices(persisted);
        Material zinc = material("amod", "Zinc");
        Material iron = material("amod", "Iron");
        registry.resolve();

        assertEquals(5, zinc.getIndex());
        assertEquals(2, iron.getIndex());
    }

    @Test
    void newMaterialsAppendAfterTheHighestPersistedIndex() {
        Map<String, Integer> persisted = new LinkedHashMap<>();
        persisted.put("amod:Iron", 3);
        registry.setPersistedIndices(persisted);
        Material iron = material("amod", "Iron");
        Material copper = material("amod", "Copper");
        Material tin = material("amod", "Tin");
        registry.resolve();

        assertEquals(3, iron.getIndex());
        assertEquals(4, copper.getIndex());
        assertEquals(5, tin.getIndex());
    }

    @Test
    void removedMaterialsKeepTheirIndexReserved() {
        Map<String, Integer> persisted = new LinkedHashMap<>();
        persisted.put("amod:Ghost", 0);
        persisted.put("amod:Real", 1);
        registry.setPersistedIndices(persisted);
        Material real = material("amod", "Real");
        registry.resolve();

        assertEquals(1, real.getIndex());
        assertNull(registry.getMaterialByIndex(0));
        assertEquals(real, registry.getMaterialByIndex(1));
        assertEquals(0, registry.getAssignedIndices()
            .get("amod:Ghost")
            .intValue());
    }

    @Test
    void assignedIndicesCombinePersistedAndNew() {
        Map<String, Integer> persisted = new LinkedHashMap<>();
        persisted.put("amod:Old", 0);
        registry.setPersistedIndices(persisted);
        material("amod", "Old");
        material("amod", "New");
        registry.resolve();

        Map<String, Integer> assigned = registry.getAssignedIndices();
        assertEquals(0, assigned.get("amod:Old")
            .intValue());
        assertEquals(1, assigned.get("amod:New")
            .intValue());
    }

    @Test
    void newMaterialAppendsAboveAReservedHighestIndex() {
        Map<String, Integer> persisted = new LinkedHashMap<>();
        persisted.put("amod:A", 0);
        persisted.put("amod:Z", 10);
        registry.setPersistedIndices(persisted);
        material("amod", "A");
        Material added = material("amod", "M");
        registry.resolve();

        assertEquals(11, added.getIndex());
        assertNull(registry.getMaterialByIndex(10));
    }

    @Test
    void reResolvingWithTheAssignedMapReproducesTheSameIndices() {
        material("bmod", "Iron");
        material("amod", "Zinc");
        registry.resolve();
        Map<String, Integer> firstAssignment = new LinkedHashMap<>(registry.getAssignedIndices());

        MaterialRegistry relaunch = new MaterialRegistry();
        relaunch.setPersistedIndices(firstAssignment);
        relaunch.newMaterial("bmod", "Iron", texture)
            .build();
        relaunch.newMaterial("amod", "Zinc", texture)
            .build();
        relaunch.resolve();

        assertEquals(firstAssignment, relaunch.getAssignedIndices());
    }
}
