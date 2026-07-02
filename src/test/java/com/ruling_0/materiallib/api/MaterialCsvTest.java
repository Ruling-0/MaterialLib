package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

class MaterialCsvTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    @Test
    void dumpListsEveryAssignedIndexAscendingWithOwnerLoadedStateShapesAndFamilies() {
        registry.setPersistedIndices(Map.of("Iron", 1, "Ghost", 0));
        registry.setPersistedOwners(Map.of("Ghost", "oldmod"));
        Family family = registry.newFamily("testmod", "Metals")
            .generateShape(new TestShape("amod", "plate"))
            .build();
        registry.newMaterial("testmod", "Iron", texture)
            .generateShape(new TestShape("amod", "gear"))
            .addToFamily(family)
            .build();
        registry.resolve();

        assertEquals(
            "index,name,owner,loaded,shapes,families\n" + "0,Ghost,oldmod,no,,\n" +
                "1,Iron,testmod,yes,amod:gear;amod:plate,testmod:Metals\n",
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
