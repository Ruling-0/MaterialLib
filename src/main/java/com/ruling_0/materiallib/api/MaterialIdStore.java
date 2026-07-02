package com.ruling_0.materiallib.api;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/// The instance-global store of the material -> index assignment, a JSON file under `config/materiallib`.
///
/// The assignment is append-only and shared by every world on the instance: [#loadInto] feeds the saved indices
/// to the registry before it resolves so existing materials keep their index, and [#saveFrom] writes the resolved
/// assignment (including indices reserved for removed materials) back. Keeping the mapping fixed across launches
/// is what stops stored item stacks from changing material when the material set changes.
public final class MaterialIdStore {

    private static final String FILE_NAME = "material-ids.json";
    private static final int FORMAT_VERSION = 1;

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
        Data data = JsonStore.read(file, Data.class, corrupt(file));
        if (data == null || data.materials == null) {
            throw new IllegalStateException(corrupt(file));
        }
        validateIndices(file, data.materials);
        return data.materials;
    }

    static void write(File file, Map<String, Integer> indices) {
        Data data = new Data();
        data.version = FORMAT_VERSION;
        data.materials = JsonStore.sorted(indices, Map.Entry.comparingByValue());
        JsonStore.write(
            file,
            data,
            "Could not write the material id assignment to " + file +
                ". Stored item stacks would change material on the next launch; refusing to continue.");
    }

    /// Rejects an assignment whose indices are negative, null, or shared by two materials.
    private static void validateIndices(File file, Map<String, Integer> materials) {
        IntSet used = new IntOpenHashSet();
        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            Integer index = entry.getValue();
            if (index == null || index < 0) {
                throw new IllegalStateException(
                    corrupt(file) + " (" + entry.getKey() + " has an invalid index " + index + ")");
            }
            if (!used.add((int) index)) {
                throw new IllegalStateException(
                    corrupt(file) + " (index " + index + " is assigned to more than one material)");
            }
        }
    }

    private static String corrupt(File file) {
        return "The material id assignment at " + file +
            " is unreadable or malformed. Fix or remove the file; deleting it reassigns ids and may change " +
            "stored items.";
    }

    private static final class Data {

        int version;
        LinkedHashMap<String, Integer> materials;
    }
}
