package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void aLegacyFileIsAdoptedAsListVersionOneAndRewritten() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":2,\"materials\":{\"Gold\":0,\"Iron\":1}}".getBytes(StandardCharsets.UTF_8));

        MaterialIdStore.WorldIds stored = MaterialIdStore.read(file());

        assertEquals(1, stored.listVersion());
        assertEquals(MaterialRegistry.contentHash(List.of("Gold", "Iron")), stored.hash());
        assertEquals(Map.of("Gold", 0, "Iron", 1), stored.materials());
        String rewritten = new String(Files.readAllBytes(file().toPath()), StandardCharsets.UTF_8);
        assertTrue(rewritten.contains("\"version\": 3"));
        assertEquals(stored, MaterialIdStore.read(file()));
    }

    @Test
    void aLegacyFileHashOrdersNamesByIndex() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":2,\"materials\":{\"Iron\":1,\"Gold\":0}}".getBytes(StandardCharsets.UTF_8));

        assertEquals(
            MaterialRegistry.contentHash(List.of("Gold", "Iron")),
            MaterialIdStore.read(file())
                .hash());
    }

    // A sparse legacy map covers the same names as a dense assignment without meaning the same indices; its
    // hash must differ or the launch flow would skip the migration the world needs.
    @Test
    void aSparseLegacyFileNeverHashesLikeADenseAssignment() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":2,\"materials\":{\"Iron\":7}}".getBytes(StandardCharsets.UTF_8));

        assertNotEquals(
            MaterialRegistry.contentHash(List.of("Iron")),
            MaterialIdStore.read(file())
                .hash());
    }

    @Test
    void readingACorruptFileFailsLoudly() throws Exception {
        Files.write(file().toPath(), "{ not valid json".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingAFilePresentButMissingMaterialsFailsLoudly() throws Exception {
        Files.write(file().toPath(),
            "{\"version\":3,\"listVersion\":1,\"hash\":\"x\"}".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingAFileWithoutAHashFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":3,\"listVersion\":1,\"materials\":{\"A\":0}}".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingAFileWhoseHashDisagreesWithItsMaterialsFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":3,\"listVersion\":1,\"hash\":\"stale\",\"materials\":{\"A\":0}}"
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingAFileWithoutAListVersionFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":3,\"hash\":\"x\",\"materials\":{\"A\":0}}".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingAFutureFormatFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":4,\"listVersion\":1,\"hash\":\"x\",\"materials\":{\"A\":0}}"
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingANegativeIndexFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":3,\"listVersion\":1,\"hash\":\"x\",\"materials\":{\"A\":-1}}"
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingADuplicateIndexFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":3,\"listVersion\":1,\"hash\":\"x\",\"materials\":{\"A\":0,\"B\":0}}"
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingAKeyWithAColonFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":3,\"listVersion\":1,\"hash\":\"x\",\"materials\":{\"amod:Iron\":0}}"
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }
}
