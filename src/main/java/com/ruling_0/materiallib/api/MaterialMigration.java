package com.ruling_0.materiallib.api;

import java.util.Map;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/// The damage-value remap that brings a world's stored item shapes onto this instance's index assignment.
///
/// Built by comparing the world's saved assignment to the instance assignment keyed by material name: a material the
/// instance places at a different index moves there. A material the world references that this instance's store
/// has no index for -- which happens only for a world brought from another instance carrying materials this
/// instance lacks -- is a deletion: its stacks are dropped, since the material cannot be shown. A material removed
/// on this instance is never a deletion; the append-only store keeps its reserved index, so its stacks move there
/// and render as a missing-material placeholder. The remap is by damage value alone because one index means the
/// same material across every shape.
///
///
public final class MaterialMigration {

    /// [#lookup] result: the stored stack should be removed, its material being unknown to this instance.
    public static final int DELETE = -1;

    /// [#lookup] result: the stored damage value needs no change.
    public static final int UNCHANGED = -2;

    private final Int2IntMap remap;

    public MaterialMigration(Map<String, Integer> worldAssignment, Map<String, Integer> instanceAssignment) {
        remap = new Int2IntOpenHashMap();
        for (Map.Entry<String, Integer> entry : worldAssignment.entrySet()) {
            int oldIndex = entry.getValue();
            Integer newIndex = instanceAssignment.get(entry.getKey());
            if (newIndex == null) {
                remap.put(oldIndex, DELETE);
            }
            else if (newIndex != oldIndex) {
                remap.put(oldIndex, (int) newIndex);
            }
        }
    }

    /// The new damage value for a stored damage value, or [#DELETE], or [#UNCHANGED].
    public int lookup(int oldIndex) {
        return remap.getOrDefault(oldIndex, UNCHANGED);
    }

    /// True if no stored stack needs migrating.
    public boolean isEmpty() { return remap.isEmpty(); }
}
