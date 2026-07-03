package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldMaterialIdsTest {

    @TempDir
    File dir;

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    private File worldFile() {
        return new File(dir, "material-ids.json");
    }

    private MaterialRegistry resolvedWith(Map<String, Integer> assignment) {
        registry.setPersistedIndices(assignment);
        for (String name : assignment.keySet()) {
            registry.newMaterial("testmod", name, texture)
                .build();
        }
        registry.resolve();
        return registry;
    }

    @Test
    void diffReportsNothingWhenWorldAgreesWithInstance() {
        Map<String, Integer> world = Map.of("Iron", 0, "Gold", 1);
        Map<String, Integer> instance = Map.of("Iron", 0, "Gold", 1, "Tin", 2);

        assertFalse(WorldMaterialIds.diff(world, instance)
            .isMismatch());
    }

    @Test
    void diffReportsAMovedMaterial() {
        WorldMaterialIds.Diff diff = WorldMaterialIds.diff(Map.of("Iron", 0), Map.of("Iron", 5));

        assertTrue(diff.isMismatch());
        assertEquals(1, diff.moved()
            .size());
        assertTrue(diff.removed()
            .isEmpty());
    }

    @Test
    void diffReportsARemovedMaterial() {
        WorldMaterialIds.Diff diff = WorldMaterialIds.diff(Map.of("Iron", 0, "Gone", 1), Map.of("Iron", 0));

        assertTrue(diff.isMismatch());
        assertEquals(1, diff.removed()
            .size());
        assertTrue(diff.moved()
            .isEmpty());
    }

    @Test
    void checkStampsAFreshWorldWithTheInstanceAssignment() {
        Map<String, Integer> assignment = new LinkedHashMap<>();
        assignment.put("Iron", 0);
        assignment.put("Gold", 1);
        MaterialRegistry resolved = resolvedWith(assignment);

        assertNull(WorldMaterialIds.check(resolved, worldFile()));
        assertEquals(assignment, MaterialIdStore.read(worldFile()));
    }

    @Test
    void checkRefreshesAWorldMissingNewMaterials() {
        Map<String, Integer> assignment = new LinkedHashMap<>();
        assignment.put("Iron", 0);
        assignment.put("Gold", 1);
        MaterialRegistry resolved = resolvedWith(assignment);
        MaterialIdStore.write(worldFile(), Map.of("Iron", 0));

        assertNull(WorldMaterialIds.check(resolved, worldFile()));
        assertEquals(assignment, MaterialIdStore.read(worldFile()));
    }

    @Test
    void checkMigratesAndAdvancesTheCopyOnMismatch() {
        Map<String, Integer> assignment = new LinkedHashMap<>();
        assignment.put("Iron", 0);
        MaterialRegistry resolved = resolvedWith(assignment);
        MaterialIdStore.write(worldFile(), Map.of("Iron", 7));

        MaterialMigration migration = WorldMaterialIds.check(resolved, worldFile());

        assertNotNull(migration);
        assertEquals(0, migration.lookup(7));
        assertEquals(assignment, MaterialIdStore.read(worldFile()));
    }
}
