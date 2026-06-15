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

class ShapeOwnerStoreTest {

    @TempDir
    File dir;

    private File file() {
        return new File(dir, "shape-owners.json");
    }

    @Test
    void readingAnAbsentFileGivesAnEmptyMap() {
        assertTrue(ShapeOwnerStore.read(file())
            .isEmpty());
    }

    @Test
    void writeThenReadRoundTrips() {
        Map<String, String> owners = new LinkedHashMap<>();
        owners.put("gear", "amod");
        owners.put("plate", "bmod");

        ShapeOwnerStore.write(file(), owners);

        assertEquals(owners, ShapeOwnerStore.read(file()));
    }

    @Test
    void writeOrdersEntriesByNameForReadability() {
        Map<String, String> owners = new LinkedHashMap<>();
        owners.put("rod", "zmod");
        owners.put("gear", "amod");
        owners.put("plate", "mmod");

        ShapeOwnerStore.write(file(), owners);

        assertEquals(
            List.of("gear", "plate", "rod"),
            new ArrayList<>(ShapeOwnerStore.read(file())
                .keySet()));
    }

    @Test
    void writeCreatesMissingParentDirectories() {
        Map<String, String> owners = Map.of("gear", "amod");
        File nested = new File(new File(dir, "nested"), "shape-owners.json");

        ShapeOwnerStore.write(nested, owners);

        assertEquals(owners, ShapeOwnerStore.read(nested));
    }

    @Test
    void readingACorruptFileFailsLoudly() throws Exception {
        Files.write(file().toPath(), "{ not valid json".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> ShapeOwnerStore.read(file()));
    }

    @Test
    void readingAFilePresentButMissingOwnersFailsLoudly() throws Exception {
        Files.write(file().toPath(), "{\"version\":1}".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> ShapeOwnerStore.read(file()));
    }

    @Test
    void readingAFileWithEmptyOwnersGivesAnEmptyMap() throws Exception {
        Files.write(file().toPath(), "{\"version\":1,\"owners\":{}}".getBytes(StandardCharsets.UTF_8));

        assertTrue(ShapeOwnerStore.read(file())
            .isEmpty());
    }

    @Test
    void readingAnEmptyOwnerFailsLoudly() throws Exception {
        Files.write(file().toPath(), "{\"version\":1,\"owners\":{\"gear\":\"\"}}".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> ShapeOwnerStore.read(file()));
    }

    @Test
    void readingAnOwnerThatIsNotAValidModidFailsLoudly() throws Exception {
        Files.write(file().toPath(), "{\"version\":1,\"owners\":{\"gear\":\"a:b\"}}".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> ShapeOwnerStore.read(file()));
    }

    @Test
    void writeOverwritesAnExistingFile() {
        ShapeOwnerStore.write(file(), Map.of("gear", "amod", "plate", "bmod"));
        ShapeOwnerStore.write(file(), Map.of("gear", "amod"));

        assertEquals(Map.of("gear", "amod"), ShapeOwnerStore.read(file()));
    }
}
