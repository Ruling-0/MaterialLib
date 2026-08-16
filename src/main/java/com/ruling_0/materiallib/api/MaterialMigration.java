package com.ruling_0.materiallib.api;

import java.util.Collection;
import java.util.Map;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/// The damage-value remap that brings a world's stored item shapes and placed blocks from one material id list
/// version to the next.
///
/// Built from a transition's moved and removed index sets: a moved index is rewritten to its new value, and a
/// removed index -- one whose material no longer exists -- deletes its stacks and turns its placed blocks to air.
/// Every other index is unchanged. The remap is by damage value alone because one index means the same material
/// across every shape.
public final class MaterialMigration {

    /// [#lookup] result: the stored stack or placed block should be removed.
    public static final int DELETE = -1;

    /// [#lookup] result: the stored damage value needs no change.
    public static final int UNCHANGED = -2;

    private final Int2IntMap remap;

    public MaterialMigration(Map<Integer, Integer> moved, Collection<Integer> removed) {
        remap = new Int2IntOpenHashMap();
        for (Map.Entry<Integer, Integer> entry : moved.entrySet()) {
            remap.put((int) entry.getKey(), (int) entry.getValue());
        }
        for (int index : removed) {
            remap.put(index, DELETE);
        }
    }

    /// The new damage value for a stored damage value, or [#DELETE], or [#UNCHANGED].
    public int lookup(int oldIndex) {
        return remap.getOrDefault(oldIndex, UNCHANGED);
    }
}
