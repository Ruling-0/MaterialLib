package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MaterialIdStoreTest {

    @TempDir
    File dir;

    private File file() {
        return new File(dir, "material-ids.json");
    }

    @Test
    void readingAnAbsentFileGivesNull() {
        assertNull(MaterialIdStore.read(file()));
    }

    @Test
    void writeThenReadRoundTrips() {
        Map<String, Integer> indices = new LinkedHashMap<>();
        indices.put("Iron", 0);
        indices.put("Gold", 1);

        MaterialIdStore.write(file(), 4, MaterialRegistry.contentHash(List.of("Iron", "Gold")), indices);

        MaterialIdStore.WorldIds stored = MaterialIdStore.read(file());
        assertEquals(4, stored.listVersion());
        assertEquals(MaterialRegistry.contentHash(List.of("Iron", "Gold")), stored.hash());
        assertEquals(indices, stored.materials());
    }

    @Test
    void writeOrdersEntriesByIndexForReadability() {
        Map<String, Integer> indices = new LinkedHashMap<>();
        indices.put("Last", 2);
        indices.put("First", 0);
        indices.put("Middle", 1);

        MaterialIdStore.write(file(), 1, MaterialRegistry.contentHash(List.of("First", "Middle", "Last")), indices);

        assertEquals(
            List.of("First", "Middle", "Last"),
            new ArrayList<>(
                MaterialIdStore.read(file())
                    .materials()
                    .keySet()));
    }

    @Test
    void readingACorruptFileFailsLoudly() throws Exception {
        Files.write(file().toPath(), "{ not valid json".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingAFilePresentButMissingMaterialsFailsLoudly() throws Exception {
        Files.write(file().toPath(),
            "{\"version\":1,\"listVersion\":1,\"hash\":\"x\"}".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingAFileWithoutAHashFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":1,\"listVersion\":1,\"materials\":{\"A\":0}}".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingAFileWhoseHashDisagreesWithItsMaterialsFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":1,\"listVersion\":1,\"hash\":\"stale\",\"materials\":{\"A\":0}}"
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingAFileWithoutAListVersionFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":1,\"hash\":\"x\",\"materials\":{\"A\":0}}".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingAnotherFormatVersionFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":2,\"listVersion\":1,\"hash\":\"x\",\"materials\":{\"A\":0}}"
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingANegativeIndexFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":1,\"listVersion\":1,\"hash\":\"x\",\"materials\":{\"A\":-1}}"
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    // A sparse map would hash by its name order alone and so masquerade as the dense assignment of the same
    // names; validation rejects it before the hash check can be fooled.
    @Test
    void readingASparseAssignmentFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            ("{\"version\":1,\"listVersion\":1,\"hash\":\"" + MaterialRegistry.contentHash(List.of("Iron")) +
                "\",\"materials\":{\"Iron\":7}}").getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingADuplicateIndexFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":1,\"listVersion\":1,\"hash\":\"x\",\"materials\":{\"A\":0,\"B\":0}}"
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingAKeyWithAColonFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":1,\"listVersion\":1,\"hash\":\"x\",\"materials\":{\"amod:Iron\":0}}"
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }
}
