package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MaterialIndexTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    private Material material(String modid, String name) {
        return registry.newMaterial(modid, name, texture)
            .build();
    }

    private MaterialRegistry resolvedWith(List<String> namesInRegistrationOrder) {
        MaterialRegistry fresh = new MaterialRegistry();
        for (String name : namesInRegistrationOrder) {
            fresh.newMaterial("testmod", name, texture)
                .build();
        }
        fresh.resolve();
        return fresh;
    }

    @Test
    void indicesAreAssignedInAscendingNameOrder() {
        Material zebra = material("amod", "Zebra");
        Material apple = material("bmod", "Apple");
        registry.resolve();

        assertEquals(0, apple.getIndex());
        assertEquals(1, zebra.getIndex());
    }

    @Test
    void nameOrderIsCaseSensitive() {
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
    void registrationOrderDoesNotAffectTheAssignmentOrHash() {
        MaterialRegistry first = resolvedWith(List.of("Iron", "Gold", "Zinc", "Copper", "Aluminium"));
        MaterialRegistry second = resolvedWith(List.of("Aluminium", "Copper", "Zinc", "Gold", "Iron"));
        MaterialRegistry third = resolvedWith(List.of("Zinc", "Aluminium", "Iron", "Copper", "Gold"));

        assertEquals(first.getAssignedIndices(), second.getAssignedIndices());
        assertEquals(first.getAssignedIndices(), third.getAssignedIndices());
        assertEquals(first.getContentHash(), second.getContentHash());
        assertEquals(first.getContentHash(), third.getContentHash());
    }

    @Test
    void assignedIndicesAreDenseInAscendingNameOrder() {
        MaterialRegistry resolved = resolvedWith(List.of("Tin", "Bronze", "Steel"));

        assertEquals(List.of("Bronze", "Steel", "Tin"), List.copyOf(resolved.getAssignedIndices().keySet()));
        assertEquals(List.of(0, 1, 2), List.copyOf(resolved.getAssignedIndices().values()));
    }

    // Pins the on-disk hash contract: SHA-256 over the UTF-8 names joined with \n in index order. A failure
    // here means every stored world hash mismatches and triggers a spurious transition.
    @Test
    void contentHashIsStableAcrossReleases() {
        MaterialRegistry resolved = resolvedWith(List.of("Iron", "Gold"));

        assertEquals("eea95fcbb74c54d701d4d6fcc14df4f36b975c75e1c6d5acb02a37b475be3e35", resolved.getContentHash());
    }
}
