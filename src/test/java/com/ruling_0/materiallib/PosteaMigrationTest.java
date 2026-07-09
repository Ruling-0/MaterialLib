package com.ruling_0.materiallib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ruling_0.materiallib.api.MaterialMigration;

import org.junit.jupiter.api.Test;

class PosteaMigrationTest {

    @Test
    void unchangedReportsNoUpdate() {
        assertNull(PosteaMigration.blockUpdateFor(MaterialMigration.UNCHANGED, 42));
    }

    @Test
    void deleteReplacesWithAir() {
        PosteaMigration.BlockUpdate update = PosteaMigration.blockUpdateFor(MaterialMigration.DELETE, 42);

        assertEquals(0, update.metadata());
        assertEquals(PosteaMigration.blockUpdateFor(MaterialMigration.DELETE, 1)
            .blockId(), update.blockId());
    }

    @Test
    void remapKeepsTheBlockAndWritesTheNewMetadata() {
        PosteaMigration.BlockUpdate update = PosteaMigration.blockUpdateFor(5, 42);

        assertEquals(42, update.blockId());
        assertEquals(5, update.metadata());
    }
}
