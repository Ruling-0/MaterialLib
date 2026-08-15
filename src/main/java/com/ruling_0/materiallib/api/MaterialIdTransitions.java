package com.ruling_0.materiallib.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/// The per-world chain of material id transitions, `v<K>-to-v<K+1>.json` files under
/// `<worldDir>/materiallib/transitions`.
///
/// Each step records how the assignment changed from one list version to the next: `moved` maps an old index
/// to its new one and `removed` lists indices whose material no longer exists. [#compose] folds any span of
/// steps into a single remap.
public final class MaterialIdTransitions {

    static final String DIRECTORY = "transitions";
    private static final Pattern FILE_NAME = Pattern.compile("v(\\d+)-to-v(\\d+)\\.json");

    private final Int2ObjectMap<Step> stepsByFromVersion;

    private MaterialIdTransitions(Int2ObjectMap<Step> stepsByFromVersion) {
        this.stepsByFromVersion = stepsByFromVersion;
    }

    /// Loads every transition file in `dir`; a missing directory yields an empty chain. Throws
    /// [IllegalStateException] on a corrupt or malformed file.
    public static MaterialIdTransitions load(File dir) {
        Int2ObjectMap<Step> steps = new Int2ObjectOpenHashMap<>();
        File[] files = dir.listFiles((parent, name) -> FILE_NAME.matcher(name).matches());
        if (files == null) return new MaterialIdTransitions(steps);
        for (File file : files) {
            Step step = readStep(file);
            if (steps.putIfAbsent(step.from(), step) != null) {
                throw new IllegalStateException(
                    corrupt(file) + " (a second transition from list version " + step.from() + ")");
            }
        }
        return new MaterialIdTransitions(steps);
    }

    /// The single remap taking indices stamped at `fromVersion` to `toVersion`: an entry per index that
    /// changes, mapped to its final index, or to [MaterialMigration#DELETE] when its material was removed
    /// along the way. Indices absent from the map are unchanged. Throws [IllegalStateException] when a step
    /// in the span is missing.
    public Int2IntMap compose(int fromVersion, int toVersion) {
        if (fromVersion > toVersion) {
            throw new IllegalArgumentException(
                "Cannot compose transitions from list version " + fromVersion + " back to " + toVersion);
        }
        Int2IntMap current = new Int2IntOpenHashMap();
        for (int version = fromVersion; version < toVersion; version++) {
            Step step = stepsByFromVersion.get(version);
            if (step == null) {
                throw new IllegalStateException(
                    "No transition from material id list version " + version + " to " + (version + 1) +
                        " is saved; the world's transition chain is incomplete");
            }
            // Folds the step onto the remap built so far. An index the earlier steps never touched maps
            // identity up to this step, so the step's own entries apply to it directly.
            Int2IntMap next = new Int2IntOpenHashMap();
            for (Int2IntMap.Entry entry : current.int2IntEntrySet()) {
                int at = entry.getIntValue();
                next.put(entry.getIntKey(), at == MaterialMigration.DELETE ? MaterialMigration.DELETE : step.apply(at));
            }
            for (int movedFrom : step.moved().keySet()) {
                if (!current.containsKey(movedFrom)) next.put(movedFrom, step.moved().get(movedFrom));
            }
            for (int removedIndex : step.removed()) {
                if (!current.containsKey(removedIndex)) next.put(removedIndex, MaterialMigration.DELETE);
            }
            current = next;
        }
        current.int2IntEntrySet().removeIf(entry -> entry.getIntKey() == entry.getIntValue());
        return current;
    }

    /// Writes the transition file for the step `from` -> `to` into `dir`.
    static void write(File dir, int from, int to, Map<Integer, Integer> moved, List<Integer> removed) {
        Data data = new Data();
        data.from = from;
        data.to = to;
        data.moved = JsonStore.sorted(moved, Map.Entry.comparingByKey());
        List<Integer> sortedRemoved = new ArrayList<>(removed);
        Collections.sort(sortedRemoved);
        data.removed = sortedRemoved;
        File file = new File(dir, "v" + from + "-to-v" + to + ".json");
        JsonStore.write(file, data,
            "Could not write the material id transition to " + file + "; refusing to continue.");
    }

    private static Step readStep(File file) {
        Data data = JsonStore.read(file, Data.class, corrupt(file));
        if (data == null || data.moved == null || data.removed == null || data.to != data.from + 1 ||
            data.from < 1) {
            throw new IllegalStateException(corrupt(file));
        }
        Matcher name = FILE_NAME.matcher(file.getName());
        if (!name.matches() || Integer.parseInt(name.group(1)) != data.from ||
            Integer.parseInt(name.group(2)) != data.to) {
            throw new IllegalStateException(corrupt(file) + " (the file name does not match its versions)");
        }
        Int2IntMap moved = new Int2IntOpenHashMap();
        for (Map.Entry<Integer, Integer> entry : data.moved.entrySet()) {
            if (entry.getKey() == null || entry.getKey() < 0 || entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalStateException(corrupt(file) + " (invalid moved entry " + entry + ")");
            }
            moved.put((int) entry.getKey(), (int) entry.getValue());
        }
        IntSet removed = new IntOpenHashSet();
        for (Integer index : data.removed) {
            if (index == null || index < 0 || moved.containsKey((int) index)) {
                throw new IllegalStateException(corrupt(file) + " (invalid removed index " + index + ")");
            }
            removed.add((int) index);
        }
        return new Step(data.from, moved, removed);
    }

    private static String corrupt(File file) {
        return "The material id transition at " + file + " is unreadable or malformed. Fix or restore the file.";
    }

    private record Step(int from, Int2IntMap moved, IntSet removed) {

        /// The index an index at this step's `from` version holds at `from + 1`, or [MaterialMigration#DELETE].
        int apply(int index) {
            if (moved.containsKey(index)) return moved.get(index);
            return removed.contains(index) ? MaterialMigration.DELETE : index;
        }
    }

    private static final class Data {

        int from;
        int to;
        LinkedHashMap<Integer, Integer> moved;
        List<Integer> removed;
    }
}
