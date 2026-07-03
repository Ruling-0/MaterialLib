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
            Map.of("Iron", 0, "Gold", 1),
            Map.of("Iron", 0, "Gold", 1));

        assertTrue(migration.isEmpty());
        assertEquals(MaterialMigration.UNCHANGED, migration.lookup(0));
        assertEquals(MaterialMigration.UNCHANGED, migration.lookup(1));
    }

    @Test
    void aMovedMaterialRemapsItsIndex() {
        MaterialMigration migration = new MaterialMigration(Map.of("Iron", 0), Map.of("Iron", 5));

        assertFalse(migration.isEmpty());
        assertEquals(5, migration.lookup(0));
    }

    @Test
    void aVanishedMaterialIsDeleted() {
        MaterialMigration migration = new MaterialMigration(Map.of("Iron", 0, "Gone", 3), Map.of("Iron", 0));

        assertEquals(MaterialMigration.DELETE, migration.lookup(3));
        assertEquals(MaterialMigration.UNCHANGED, migration.lookup(0));
    }

    @Test
    void unrelatedDamageValuesAreUnchanged() {
        MaterialMigration migration = new MaterialMigration(Map.of("Iron", 0), Map.of("Iron", 5));

        assertEquals(MaterialMigration.UNCHANGED, migration.lookup(99));
    }
}
