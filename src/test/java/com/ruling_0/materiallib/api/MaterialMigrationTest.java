package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MaterialMigrationTest {

    @Test
    void anEmptyTransitionNeedsNoMigration() {
        MaterialMigration migration = new MaterialMigration(Map.of(), List.of());

        assertEquals(MaterialMigration.UNCHANGED, migration.lookup(0));
        assertEquals(MaterialMigration.UNCHANGED, migration.lookup(1));
    }

    @Test
    void aMovedIndexIsRemapped() {
        MaterialMigration migration = new MaterialMigration(Map.of(0, 5), List.of());

        assertEquals(5, migration.lookup(0));
    }

    @Test
    void aRemovedIndexIsDeleted() {
        MaterialMigration migration = new MaterialMigration(Map.of(0, 5), List.of(3));

        assertEquals(MaterialMigration.DELETE, migration.lookup(3));
        assertEquals(MaterialMigration.UNCHANGED, migration.lookup(1));
    }
}
