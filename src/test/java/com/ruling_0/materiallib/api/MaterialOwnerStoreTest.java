package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MaterialOwnerStoreTest {

    @TempDir
    File dir;

    private File file() {
        return new File(dir, "material-owners.json");
    }

    @Test
    void readingAnAbsentFileGivesAnEmptyMap() {
        assertTrue(MaterialOwnerStore.read(file())
            .isEmpty());
    }

    @Test
    void writeThenReadRoundTrips() {
        Map<String, String> owners = new LinkedHashMap<>();
        owners.put("testiron", "amod");
        owners.put("testgold", "bmod");

        MaterialOwnerStore.write(file(), owners);

        assertEquals(owners, MaterialOwnerStore.read(file()));
    }

    @Test
    void readingACorruptFileFailsLoudly() throws Exception {
        Files.write(file().toPath(), "{ not valid json".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialOwnerStore.read(file()));
    }

    @Test
    void readingAnOwnerThatIsNotAValidModidFailsLoudly() throws Exception {
        Files.write(
            file().toPath(),
            "{\"version\":1,\"owners\":{\"testiron\":\"a:b\"}}".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialOwnerStore.read(file()));
    }
}
