package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MaterialCsvTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    @Test
    void dumpListsEveryAssignedIndexAscendingWithOwnerShapesAndFamilies() {
        // Registration order (Iron before Copper) and shape declaration order (plate before gear)
        // deliberately differ from the sorted output, so these assertions fail if either sort is dropped.
        Family family = registry.newFamily("testmod", "Metals").generateShape(new TestShape("amod", "gear")).build();
        registry.newMaterial("testmod", "Iron", texture)
            .generateShape(new TestShape("amod", "plate"))
            .addToFamily(family)
            .build();
        registry.newMaterial("testmod", "Copper", texture).build();
        registry.resolve();

        assertEquals(
            "index,name,owner,shapes,families\n" + "0,Copper,testmod,,\n" +
                "1,Iron,testmod,amod:gear;amod:plate,testmod:Metals\n",
            registry.dumpCsv());
    }

    @Test
    void fieldsContainingCommasOrQuotesAreQuotedWithDoubledQuotes() {
        registry.newMaterial("testmod", "Iron,\"Cast\"", texture).build();
        registry.resolve();

        assertEquals("index,name,owner,shapes,families\n" + "0,\"Iron,\"\"Cast\"\"\",testmod,,\n", registry.dumpCsv());
    }

    @Test
    void dumpingBeforeResolveFails() {
        assertThrows(IllegalStateException.class, registry::dumpCsv);
    }
}
