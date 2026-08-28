package com.ruling_0.materiallib.api;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ruling_0.materiallib.MaterialLib;

/// Maintains the per-world material id list: the [MaterialIdStore] file and the [MaterialIdTransitions] chain
/// beside it, advanced whenever the registry's deterministic assignment differs from the one the world last
/// ran with.
public final class WorldMaterialIds {

    private static volatile int currentListVersion = 1;
    private static volatile MaterialIdTransitions transitions = MaterialIdTransitions.empty();

    private WorldMaterialIds() {}

    /// The list version the running world is on, established by [#check] at server start. Chunk and player
    /// data saved during the session is stamped with it.
    public static int currentListVersion() {
        return currentListVersion;
    }

    /// The transition chain of the running world, loaded by [#check].
    public static MaterialIdTransitions transitions() {
        return transitions;
    }

    /// Reconciles the world's stored id list under `dir` (the world's `materiallib` directory) with the
    /// registry's assignment, and loads the world's transition chain into [#transitions]. A world with no store
    /// adopts the current assignment at list version 1. A matching hash leaves everything untouched. A mismatch
    /// saves the transition from the stored version and advances the store.
    public static void check(MaterialRegistry registry, File dir) {
        Map<String, Integer> current = registry.getAssignedIndices();
        String hash = registry.getContentHash();
        File storeFile = new File(dir, MaterialIdStore.FILE_NAME);
        MaterialIdStore.WorldIds stored = MaterialIdStore.read(storeFile);
        if (stored == null) {
            MaterialIdStore.write(storeFile, 1, hash, current);
            currentListVersion = 1;
        }
        else if (hash.equals(stored.hash())) {
            currentListVersion = stored.listVersion();
        }
        else {
            Diff diff = diff(stored.materials(), current);
            int to = stored.listVersion() + 1;
            MaterialIdTransitions
                .write(new File(dir, MaterialIdTransitions.DIRECTORY), stored.listVersion(), to, diff.movedIndices(),
                    diff.removedIndices());
            MaterialIdStore.write(storeFile, to, hash, current);
            currentListVersion = to;
            if (!diff.isMismatch()) {
                MaterialLib.LOG.info(
                    "Materials were added since this world last ran; advancing its material id list to version {} " +
                        "with no index changes.",
                    to);
            }
            else {
                MaterialLib.LOG.warn(
                    "This world was saved under a different material id assignment; advancing it to list version {}. " +
                        "Chunks and player data are remapped as they are read, from whichever version they were saved " +
                        "under. Moved: {}. Deleted: {} -- placed blocks of these become air. Stacks kept outside chunks " +
                        "and player data are not remapped.",
                    to,
                    diff.describeMoved(),
                    diff.describeRemoved());
            }
        }
        transitions = MaterialIdTransitions.load(new File(dir, MaterialIdTransitions.DIRECTORY));
    }

    /// Compares the stored assignment against the current one by material name: a name now at a different
    /// index is moved, a name that no longer exists is removed. New names need no entry -- no stored data
    /// references them.
    static Diff diff(Map<String, Integer> stored, Map<String, Integer> current) {
        List<Move> moved = new ArrayList<>();
        List<Removal> removed = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : stored.entrySet()) {
            Integer now = current.get(entry.getKey());
            if (now == null) {
                removed.add(new Removal(entry.getKey(), entry.getValue()));
            }
            else if (!now.equals(entry.getValue())) {
                moved.add(new Move(entry.getKey(), entry.getValue(), now));
            }
        }
        return new Diff(moved, removed);
    }

    record Move(String name, int oldIndex, int newIndex) {}

    record Removal(String name, int index) {}

    record Diff(List<Move> moved, List<Removal> removed) {

        boolean isMismatch() { return !moved.isEmpty() || !removed.isEmpty(); }

        Map<Integer, Integer> movedIndices() {
            Map<Integer, Integer> indices = new LinkedHashMap<>();
            for (Move move : moved) {
                indices.put(move.oldIndex(), move.newIndex());
            }
            return indices;
        }

        List<Integer> removedIndices() {
            List<Integer> indices = new ArrayList<>();
            for (Removal removal : removed) {
                indices.add(removal.index());
            }
            return indices;
        }

        List<String> describeMoved() {
            List<String> notes = new ArrayList<>();
            for (Move move : moved) {
                notes.add(move.name() + " (" + move.oldIndex() + " -> " + move.newIndex() + ")");
            }
            return notes;
        }

        List<String> describeRemoved() {
            List<String> notes = new ArrayList<>();
            for (Removal removal : removed) {
                notes.add(removal.name() + " (index " + removal.index() + ")");
            }
            return notes;
        }
    }
}
