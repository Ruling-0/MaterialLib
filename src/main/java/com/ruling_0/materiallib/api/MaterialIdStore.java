package com.ruling_0.materiallib.api;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/// The per-world store of the material name -> index assignment, compared by [WorldMaterialIds] against the
/// registry's deterministic assignment at server start.
public final class MaterialIdStore {

    private static final int FORMAT_VERSION = 2;

    private MaterialIdStore() {}

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

    /// Rejects an assignment whose keys are not bare material names or whose indices are negative, null, or
    /// shared by two materials.
    private static void validateIndices(File file, Map<String, Integer> materials) {
        IntSet used = new IntOpenHashSet();
        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            if (entry.getKey().indexOf(':') >= 0) {
                throw new IllegalStateException(
                    corrupt(file) + " (" + entry.getKey() + " is not a bare material name)");
            }
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
