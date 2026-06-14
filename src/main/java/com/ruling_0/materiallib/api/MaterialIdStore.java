package com.ruling_0.materiallib.api;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// The instance-global store of the material -> index assignment, a JSON file under `config/materiallib`.
///
/// The assignment is append-only and shared by every world on the instance: [#loadInto] feeds the saved indices
/// to the registry before it resolves so existing materials keep their index, and [#saveFrom] writes the resolved
/// assignment (including indices reserved for removed materials) back. Keeping the mapping fixed across launches
/// is what stops stored item stacks from changing material when the material set changes.
public final class MaterialIdStore {

    private static final Logger LOG = LogManager.getLogger("materiallib");
    private static final String FILE_NAME = "material-ids.json";
    private static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();

    private MaterialIdStore() {}

    /// Loads the saved assignment from `<dir>/material-ids.json` and hands it to the registry to honor at resolve.
    public static void loadInto(MaterialRegistry registry, File dir) {
        registry.setPersistedIndices(read(new File(dir, FILE_NAME)));
    }

    /// Writes the registry's resolved assignment back to `<dir>/material-ids.json`.
    public static void saveFrom(MaterialRegistry registry, File dir) {
        write(new File(dir, FILE_NAME), registry.getAssignedIndices());
    }

    static Map<String, Integer> read(File file) {
        if (!file.isFile()) return new LinkedHashMap<>();
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            Data data = GSON.fromJson(reader, Data.class);
            if (data == null || data.materials == null) return new LinkedHashMap<>();
            return data.materials;
        }
        catch (IOException | JsonParseException e) {
            throw new IllegalStateException(
                "Could not read the material id assignment at " + file +
                    ". Fix or remove the file; deleting it reassigns ids and may change stored items.",
                e);
        }
    }

    static void write(File file, Map<String, Integer> indices) {
        Data data = new Data();
        data.version = FORMAT_VERSION;
        data.materials = sortByIndex(indices);
        try {
            File parent = file.getParentFile();
            if (parent != null) Files.createDirectories(parent.toPath());
            File temp = new File(parent, FILE_NAME + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e) {
            LOG.error("Could not write the material id assignment to {}", file, e);
        }
    }

    private static LinkedHashMap<String, Integer> sortByIndex(Map<String, Integer> indices) {
        List<Map.Entry<String, Integer>> entries = new ObjectArrayList<>(indices.entrySet());
        entries.sort(Map.Entry.comparingByValue());
        LinkedHashMap<String, Integer> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entries) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        return sorted;
    }

    private static final class Data {

        int version;
        LinkedHashMap<String, Integer> materials;
    }
}
