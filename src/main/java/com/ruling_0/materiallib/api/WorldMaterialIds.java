package com.ruling_0.materiallib.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// Reconciles a world's saved material -> index assignment with this instance's.
///
/// The instance assignment is append-only, so a world loaded on the instance that created it always agrees; its
/// saved copy is just brought up to date as new materials are added. A world moved from another instance can
/// disagree -- a material now sits at a different index, or no longer exists -- which would make stored item
/// stacks read as the wrong material. Such a mismatch is logged prominently so it is never silent.
public final class WorldMaterialIds {

    private static final Logger LOG = LogManager.getLogger("materiallib");

    private WorldMaterialIds() {}

    /// Compares the assignment saved in `worldFile` against the registry's resolved assignment. A world with no
    /// saved assignment yet is stamped with the current one; a world that only lacks newly added materials has its
    /// saved copy refreshed; a genuine mismatch is logged, naming the moved and removed materials.
    public static void check(MaterialRegistry registry, File worldFile) {
        Map<String, Integer> instance = registry.getAssignedIndices();
        Map<String, Integer> world = MaterialIdStore.read(worldFile);
        if (world.isEmpty()) {
            MaterialIdStore.write(worldFile, instance);
            return;
        }
        Diff diff = diff(world, instance);
        if (!diff.isMismatch()) {
            if (!world.equals(instance)) MaterialIdStore.write(worldFile, instance);
            return;
        }
        LOG.warn(
            "This world's material ids differ from the instance, so stored items may show the wrong material. " +
                "Moved: {}. Removed: {}.",
            diff.moved(),
            diff.removed());
    }

    /// Compares a world assignment against the instance assignment by material key: a key the instance maps to a
    /// different index is `moved`, a key the instance no longer has is `removed`. New materials the world never saw
    /// are not reported -- no stored stack references them.
    static Diff diff(Map<String, Integer> world, Map<String, Integer> instance) {
        List<String> moved = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : world.entrySet()) {
            Integer now = instance.get(entry.getKey());
            if (now == null) {
                removed.add(entry.getKey() + " (index " + entry.getValue() + ")");
            }
            else if (!now.equals(entry.getValue())) {
                moved.add(entry.getKey() + " (" + entry.getValue() + " -> " + now + ")");
            }
        }
        return new Diff(moved, removed);
    }

    record Diff(List<String> moved, List<String> removed) {

        boolean isMismatch() { return !moved.isEmpty() || !removed.isEmpty(); }
    }
}
