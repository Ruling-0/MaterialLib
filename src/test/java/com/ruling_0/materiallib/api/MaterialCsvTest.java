package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MaterialCsvTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    @Test
    void dumpListsEveryAssignedIndexAscendingWithOwnerLoadedStateShapesAndFamilies() {
        // Persisted insertion order (Iron before Ghost) and shape declaration order (plate before gear)
        // deliberately differ from the sorted output, so these assertions fail if either sort is dropped.
        Map<String, Integer> indices = new LinkedHashMap<>();
        indices.put("Iron", 3);
        indices.put("Ghost", 0);
        registry.setPersistedIndices(indices);
        registry.setPersistedOwners(Map.of("Ghost", "oldmod"));
        Family family = registry.newFamily("testmod", "Metals")
            .generateShape(new TestShape("amod", "gear"))
            .build();
        registry.newMaterial("testmod", "Iron", texture)
            .generateShape(new TestShape("amod", "plate"))
            .addToFamily(family)
            .build();
        registry.resolve();

        assertEquals(
            "index,name,owner,loaded,shapes,families\n" + "0,Ghost,oldmod,no,,\n" +
                "3,Iron,testmod,yes,amod:gear;amod:plate,testmod:Metals\n",
            registry.dumpCsv());
    }

    @Test
    void fieldsContainingCommasOrQuotesAreQuotedWithDoubledQuotes() {
        registry.newMaterial("testmod", "Iron,\"Cast\"", texture)
            .build();
        registry.resolve();

        assertEquals(
            "index,name,owner,loaded,shapes,families\n" + "0,\"Iron,\"\"Cast\"\"\",testmod,yes,,\n",
            registry.dumpCsv());
    }

    @Test
    void aReservedIndexWithNoPersistedOwnerDumpsAnEmptyOwnerField() {
        registry.setPersistedIndices(Map.of("Ghost", 0));
        registry.resolve();

        assertEquals("index,name,owner,loaded,shapes,families\n" + "0,Ghost,,no,,\n", registry.dumpCsv());
    }

    @Test
    void dumpingBeforeResolveFails() {
        assertThrows(IllegalStateException.class, registry::dumpCsv);
    }
}
