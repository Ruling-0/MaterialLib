package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void readingAnAbsentFileGivesAnEmptyMap() {
        assertTrue(MaterialIdStore.read(file())
            .isEmpty());
    }

    @Test
    void writeThenReadRoundTrips() {
        Map<String, Integer> indices = new LinkedHashMap<>();
        indices.put("amod:Iron", 0);
        indices.put("bmod:Gold", 1);

        MaterialIdStore.write(file(), indices);

        assertEquals(indices, MaterialIdStore.read(file()));
    }

    @Test
    void writeOrdersEntriesByIndexForReadability() {
        Map<String, Integer> indices = new LinkedHashMap<>();
        indices.put("zmod:Last", 2);
        indices.put("amod:First", 0);
        indices.put("mmod:Middle", 1);

        MaterialIdStore.write(file(), indices);

        assertEquals(
            List.of("amod:First", "mmod:Middle", "zmod:Last"),
            new ArrayList<>(MaterialIdStore.read(file())
                .keySet()));
    }

    @Test
    void writeCreatesMissingParentDirectories() {
        Map<String, Integer> indices = new LinkedHashMap<>();
        indices.put("amod:Iron", 0);
        File nested = new File(new File(dir, "nested"), "material-ids.json");

        MaterialIdStore.write(nested, indices);

        assertEquals(indices, MaterialIdStore.read(nested));
    }

    @Test
    void readingACorruptFileFailsLoudly() throws Exception {
        Files.write(file().toPath(), "{ not valid json".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingAFilePresentButMissingMaterialsFailsLoudly() throws Exception {
        Files.write(file().toPath(), "{\"version\":1}".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingAFileWithEmptyMaterialsGivesAnEmptyMap() throws Exception {
        Files.write(file().toPath(), "{\"version\":1,\"materials\":{}}".getBytes(StandardCharsets.UTF_8));

        assertTrue(MaterialIdStore.read(file())
            .isEmpty());
    }

    @Test
    void readingANegativeIndexFailsLoudly() throws Exception {
        Files.write(file().toPath(), "{\"version\":1,\"materials\":{\"amod:A\":-1}}".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void readingADuplicateIndexFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":1,\"materials\":{\"amod:A\":0,\"amod:B\":0}}".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdStore.read(file()));
    }

    @Test
    void writeOverwritesAnExistingFile() {
        MaterialIdStore.write(file(), Map.of("amod:Iron", 0, "amod:Gold", 1));
        MaterialIdStore.write(file(), Map.of("amod:Iron", 0));

        assertEquals(Map.of("amod:Iron", 0), MaterialIdStore.read(file()));
    }
}
