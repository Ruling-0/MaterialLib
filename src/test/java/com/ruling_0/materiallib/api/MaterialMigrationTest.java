package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class MaterialMigrationTest {

    @Test
    void anIdenticalAssignmentNeedsNoMigration() {
        MaterialMigration migration = new MaterialMigration(
            Map.of("amod:Iron", 0, "amod:Gold", 1),
            Map.of("amod:Iron", 0, "amod:Gold", 1));

        assertTrue(migration.isEmpty());
        assertEquals(MaterialMigration.UNCHANGED, migration.lookup(0));
        assertEquals(MaterialMigration.UNCHANGED, migration.lookup(1));
    }

    @Test
    void aMovedMaterialRemapsItsIndex() {
        MaterialMigration migration = new MaterialMigration(Map.of("amod:Iron", 0), Map.of("amod:Iron", 5));

        assertFalse(migration.isEmpty());
        assertEquals(5, migration.lookup(0));
    }

    @Test
    void aVanishedMaterialIsDeleted() {
        MaterialMigration migration = new MaterialMigration(
            Map.of("amod:Iron", 0, "amod:Gone", 3),
            Map.of("amod:Iron", 0));

        assertEquals(MaterialMigration.DELETE, migration.lookup(3));
        assertEquals(MaterialMigration.UNCHANGED, migration.lookup(0));
    }

    @Test
    void aMaterialStillReservedOnThisInstanceMovesRatherThanDeletes() {
        MaterialMigration migration = new MaterialMigration(Map.of("amod:Gone", 3), Map.of("amod:Gone", 9));

        assertEquals(9, migration.lookup(3));
    }

    @Test
    void unrelatedDamageValuesAreUnchanged() {
        MaterialMigration migration = new MaterialMigration(Map.of("amod:Iron", 0), Map.of("amod:Iron", 5));

        assertEquals(MaterialMigration.UNCHANGED, migration.lookup(99));
    }

    @Test
    void sizeCountsMovedAndDeleted() {
        MaterialMigration migration = new MaterialMigration(
            Map.of("amod:Iron", 0, "amod:Gold", 1, "amod:Gone", 2),
            Map.of("amod:Iron", 0, "amod:Gold", 7));

        assertEquals(2, migration.size());
    }
}
