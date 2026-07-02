package com.ruling_0.materiallib.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ruling_0.materiallib.MaterialLib;

/// Reconciles a world's saved material -> index assignment with this instance's.
///
/// The instance assignment is append-only, so a world loaded on the instance that created it always agrees; its
/// saved copy is just brought up to date as new materials are added. A world moved from another instance can
/// disagree -- a material now sits at a different index, or this instance has no index for it -- which would make
/// stored item stacks read as the wrong material. Such a mismatch produces a [MaterialMigration] that the caller
/// hands to Postea to remap stored stacks as they are read from disk, and is logged so it is never silent.
public final class WorldMaterialIds {

    private WorldMaterialIds() {}

    /// Compares the assignment saved in `worldFile` against the registry's resolved assignment, and returns the
    /// migration to apply to the world's stored stacks, or null when none is needed. A world with no saved
    /// assignment yet is stamped with the current one; a world that only lacks newly added materials has its saved
    /// copy refreshed; a genuine mismatch is logged, the saved copy advanced to the instance assignment, and the
    /// migration returned so stored stacks are remapped as they load.
    public static MaterialMigration check(MaterialRegistry registry, File worldFile) {
        Map<String, Integer> instance = registry.getAssignedIndices();
        Map<String, Integer> world = MaterialIdStore.read(worldFile);
        if (world.isEmpty()) {
            MaterialIdStore.write(worldFile, instance);
            return null;
        }
        Diff diff = diff(world, instance);
        if (!diff.isMismatch()) {
            if (!world.equals(instance)) MaterialIdStore.write(worldFile, instance);
            return null;
        }
        MaterialLib.LOG.warn(
            "This world was saved under a different material id assignment; migrating stored items to this " +
                "instance as they are read from disk. Moved: {}. Deleted: {}. Items in chunks or containers not " +
                "loaded this session keep the outdated ids.",
            diff.moved(),
            diff.removed());
        MaterialMigration migration = new MaterialMigration(world, instance);
        MaterialIdStore.write(worldFile, instance);
        return migration;
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
