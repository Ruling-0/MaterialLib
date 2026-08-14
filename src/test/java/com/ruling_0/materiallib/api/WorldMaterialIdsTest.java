package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldMaterialIdsTest {

    @TempDir
    File dir;

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    private File storeFile() {
        return new File(dir, "material-ids.json");
    }

    private File transitionsDir() {
        return new File(dir, "transitions");
    }

    private MaterialRegistry resolvedWith(String... names) {
        for (String name : names) {
            registry.newMaterial("testmod", name, texture)
                .build();
        }
        registry.resolve();
        return registry;
    }

    @Test
    void diffReportsNothingWhenTheWorldAgreesWithTheCurrentAssignment() {
        Map<String, Integer> stored = Map.of("Iron", 0, "Gold", 1);
        Map<String, Integer> current = Map.of("Iron", 0, "Gold", 1, "Tin", 2);

        assertFalse(
            WorldMaterialIds.diff(stored, current)
                .isMismatch());
    }

    @Test
    void diffReportsAMovedMaterial() {
        WorldMaterialIds.Diff diff = WorldMaterialIds.diff(Map.of("Iron", 0), Map.of("Iron", 5));

        assertTrue(diff.isMismatch());
        assertEquals(Map.of(0, 5), diff.movedIndices());
        assertTrue(
            diff.removedIndices()
                .isEmpty());
    }

    @Test
    void diffReportsARemovedMaterial() {
        WorldMaterialIds.Diff diff = WorldMaterialIds.diff(Map.of("Iron", 0, "Gone", 1), Map.of("Iron", 0));

        assertTrue(diff.isMismatch());
        assertEquals(List.of(1), diff.removedIndices());
        assertTrue(
            diff.movedIndices()
                .isEmpty());
    }

    @Test
    void checkStampsAFreshWorldAtListVersionOne() {
        MaterialRegistry resolved = resolvedWith("Gold", "Iron");

        assertNull(WorldMaterialIds.check(resolved, dir));

        MaterialIdStore.WorldIds stored = MaterialIdStore.read(storeFile());
        assertEquals(1, stored.listVersion());
        assertEquals(resolved.getContentHash(), stored.hash());
        assertEquals(resolved.getAssignedIndices(), stored.materials());
        assertEquals(1, WorldMaterialIds.currentListVersion());
        assertFalse(transitionsDir().exists());
    }

    @Test
    void checkLeavesAMatchingWorldUntouched() {
        MaterialRegistry resolved = resolvedWith("Gold", "Iron");
        MaterialIdStore.write(storeFile(), 3, resolved.getContentHash(), resolved.getAssignedIndices());

        assertNull(WorldMaterialIds.check(resolved, dir));

        assertEquals(3, MaterialIdStore.read(storeFile()).listVersion());
        assertEquals(3, WorldMaterialIds.currentListVersion());
        assertFalse(transitionsDir().exists());
    }

    @Test
    void checkWritesATransitionAndAdvancesTheStoreOnMismatch() {
        MaterialRegistry resolved = resolvedWith("Iron");
        MaterialIdStore
            .write(storeFile(), 1, MaterialRegistry.contentHash(List.of("Gone", "Iron")), Map.of("Gone", 0, "Iron", 1));

        MaterialMigration migration = WorldMaterialIds.check(resolved, dir);

        assertNotNull(migration);
        assertEquals(0, migration.lookup(1));
        assertEquals(MaterialMigration.DELETE, migration.lookup(0));
        MaterialIdStore.WorldIds stored = MaterialIdStore.read(storeFile());
        assertEquals(2, stored.listVersion());
        assertEquals(resolved.getContentHash(), stored.hash());
        assertEquals(resolved.getAssignedIndices(), stored.materials());
        assertEquals(2, WorldMaterialIds.currentListVersion());
        assertEquals(
            0,
            MaterialIdTransitions.load(transitionsDir())
                .compose(1, 2)
                .get(1));
        assertEquals(
            MaterialMigration.DELETE,
            MaterialIdTransitions.load(transitionsDir())
                .compose(1, 2)
                .get(0));
    }

    @Test
    void checkKeepsTheTransitionChainContiguousAcrossRepeatedMismatches() {
        MaterialRegistry resolved = resolvedWith("Iron");
        MaterialIdStore
            .write(storeFile(), 4, MaterialRegistry.contentHash(List.of("Gone", "Iron")), Map.of("Gone", 0, "Iron", 1));

        WorldMaterialIds.check(resolved, dir);

        assertEquals(5, MaterialIdStore.read(storeFile()).listVersion());
        assertEquals(
            0,
            MaterialIdTransitions.load(transitionsDir())
                .compose(4, 5)
                .get(1));
    }

    @Test
    void aPureAdditionAdvancesTheVersionWithoutAMigration() {
        MaterialRegistry resolved = resolvedWith("Iron", "Zinc");
        MaterialIdStore.write(storeFile(), 1, MaterialRegistry.contentHash(List.of("Iron")), Map.of("Iron", 0));

        assertNull(WorldMaterialIds.check(resolved, dir));

        assertEquals(2, MaterialIdStore.read(storeFile()).listVersion());
        assertTrue(
            MaterialIdTransitions.load(transitionsDir())
                .compose(1, 2)
                .isEmpty());
    }

    @Test
    void aLegacyWorldFileIsAdoptedAtListVersionOneAndMigrated() throws Exception {
        MaterialRegistry resolved = resolvedWith("Iron");
        Files.createDirectories(dir.toPath());
        Files.write(
            storeFile().toPath(),
            "{\"version\":2,\"materials\":{\"Iron\":7}}".getBytes(StandardCharsets.UTF_8));

        MaterialMigration migration = WorldMaterialIds.check(resolved, dir);

        assertNotNull(migration);
        assertEquals(0, migration.lookup(7));
        assertEquals(2, MaterialIdStore.read(storeFile()).listVersion());
        assertEquals(
            0,
            MaterialIdTransitions.load(transitionsDir())
                .compose(1, 2)
                .get(7));
    }
}
