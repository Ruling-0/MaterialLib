package com.ruling_0.materiallib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.gtnewhorizons.postea.utility.BlockConversionInfo;
import com.ruling_0.materiallib.api.MaterialMigration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PosteaMigrationTest {

    private static final int BLOCK_ID = 42;

    @AfterEach
    void clearActiveMigration() {
        PosteaMigration.setActiveMigration(null);
    }

    @Test
    void transformBlockAppliesEveryLookupOutcome() {
        PosteaMigration.setActiveMigration(new MaterialMigration(Map.of(1, 5), List.of(2)));

        BlockConversionInfo unchanged = placedBlock(0);
        assertFalse(PosteaMigration.transformBlock(unchanged));
        assertEquals(BLOCK_ID, unchanged.blockID);
        assertEquals(0, unchanged.metadata);

        BlockConversionInfo moved = placedBlock(1);
        assertTrue(PosteaMigration.transformBlock(moved));
        assertEquals(BLOCK_ID, moved.blockID);
        assertEquals(5, moved.metadata);

        BlockConversionInfo deleted = placedBlock(2);
        assertTrue(PosteaMigration.transformBlock(deleted));
        assertEquals(0, deleted.blockID);
        assertEquals(0, deleted.metadata);
    }

    @Test
    void transformBlockWithoutAnActiveMigrationChangesNothing() {
        BlockConversionInfo info = placedBlock(2);

        assertFalse(PosteaMigration.transformBlock(info));
        assertEquals(BLOCK_ID, info.blockID);
        assertEquals(2, info.metadata);
    }

    private static BlockConversionInfo placedBlock(int metadata) {
        return new BlockConversionInfo("materiallib:block", BLOCK_ID, metadata, 0, 0, 0, null);
    }
}
