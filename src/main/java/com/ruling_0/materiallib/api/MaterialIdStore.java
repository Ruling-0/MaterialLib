package com.ruling_0.materiallib.api;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/// The per-world store of the material id list: the name -> index assignment the world last ran with, its
/// content hash, and the list version counting how many assignments the world has seen.
///
/// Lives at `<worldDir>/materiallib/material-ids.json`. [WorldMaterialIds] compares the stored hash against
/// the registry's at server start and advances the list version through a saved transition when they differ.
public final class MaterialIdStore {

    static final String FILE_NAME = "material-ids.json";
    private static final int FORMAT_VERSION = 1;

    private MaterialIdStore() {}

    /// The stored id list: the list version the world is on, the content hash of its assignment, and the
    /// name -> index map itself.
    public record WorldIds(int listVersion, String hash, Map<String, Integer> materials) {}

    /// Reads the store, or returns null when no file exists. Throws [IllegalStateException] on a corrupt or
    /// malformed file.
    public static WorldIds read(File file) {
        if (!file.isFile()) return null;
        Data data = JsonStore.read(file, Data.class, corrupt(file));
        if (data == null || data.materials == null || data.version != FORMAT_VERSION) {
            throw new IllegalStateException(corrupt(file));
        }
        validateIndices(file, data.materials);
        if (data.listVersion == null || data.listVersion < 1 || data.hash == null ||
            !data.hash.equals(hashOf(data.materials))) {
            throw new IllegalStateException(corrupt(file));
        }
        return new WorldIds(data.listVersion, data.hash, data.materials);
    }

    /// Writes the store atomically, or throws [IllegalStateException] on IO failure.
    public static void write(File file, int listVersion, String hash, Map<String, Integer> materials) {
        Data data = new Data();
        data.version = FORMAT_VERSION;
        data.listVersion = listVersion;
        data.hash = hash;
        data.materials = JsonStore.sorted(materials, Map.Entry.comparingByValue());
        JsonStore.write(
            file,
            data,
            "Could not write the material id list to " + file + "; refusing to continue.");
    }

    /// The content hash of a stored assignment: the material names ordered by index, hashed as
    /// [MaterialRegistry#contentHash].
    private static String hashOf(Map<String, Integer> materials) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(materials.entrySet());
        entries.sort(Map.Entry.comparingByValue());
        List<String> lines = new ArrayList<>(entries.size());
        for (Map.Entry<String, Integer> entry : entries) {
            lines.add(entry.getKey());
        }
        return MaterialRegistry.contentHash(lines);
    }

    /// Rejects an assignment whose keys are not bare material names or whose indices are not exactly
    /// 0..n-1.
    private static void validateIndices(File file, Map<String, Integer> materials) {
        IntSet used = new IntOpenHashSet();
        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            if (entry.getKey().indexOf(':') >= 0) {
                throw new IllegalStateException(
                    corrupt(file) + " (" + entry.getKey() + " is not a bare material name)");
            }
            Integer index = entry.getValue();
            if (index == null || index < 0 || index >= materials.size()) {
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
        return "The material id list at " + file +
            " is unreadable or malformed. Fix or restore the file; deleting it may change stored items and " +
            "placed blocks.";
    }

    private static final class Data {

        int version;
        Integer listVersion;
        String hash;
        LinkedHashMap<String, Integer> materials;
    }
}
