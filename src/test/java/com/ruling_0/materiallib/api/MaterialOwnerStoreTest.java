package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MaterialOwnerStoreTest {

    @TempDir
    File dir;

    @Test
    void writeThenReadRoundTrips() {
        File file = new File(dir, "material-owners.json");
        Map<String, String> owners = new LinkedHashMap<>();
        owners.put("testiron", "amod");
        owners.put("testgold", "bmod");

        MaterialOwnerStore.write(file, owners);

        assertEquals(owners, MaterialOwnerStore.read(file));
    }
}
