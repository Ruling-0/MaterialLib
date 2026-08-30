package com.ruling_0.materiallib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.postea.api.ChunkTransformContext;
import com.gtnewhorizons.postea.api.IDExtenderCompat;
import com.ruling_0.materiallib.api.MaterialIdTransitions;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.junit.jupiter.api.Test;

class PosteaMigrationTest {

    private static final int SHAPE_ITEM_ID = 4200;
    private static final IntSet SHAPE_ITEMS = new IntOpenHashSet(new int[] { SHAPE_ITEM_ID });

    private static Int2IntMap remap() {
        Int2IntMap remap = new Int2IntOpenHashMap();
        remap.put(1, 5);
        remap.put(2, MaterialIdTransitions.DELETE);
        return remap;
    }

    private static NBTTagCompound stack(int id, int damage) {
        NBTTagCompound stack = new NBTTagCompound();
        IDExtenderCompat.setItemStackID(stack, id);
        stack.setByte("Count", (byte) 1);
        stack.setShort("Damage", (short) damage);
        return stack;
    }

    @Test
    void remapStackAppliesEveryOutcomeToShapeStacksOnly() {
        NBTTagCompound unchanged = stack(SHAPE_ITEM_ID, 0);
        NBTTagCompound moved = stack(SHAPE_ITEM_ID, 1);
        NBTTagCompound deleted = stack(SHAPE_ITEM_ID, 2);
        NBTTagCompound foreign = stack(7, 1);

        for (NBTTagCompound stack : new NBTTagCompound[] { unchanged, moved, deleted, foreign }) {
            PosteaMigration.remapStack(stack, remap(), SHAPE_ITEMS);
        }

        assertEquals(0, unchanged.getShort("Damage"));
        assertEquals(5, moved.getShort("Damage"));
        assertFalse(deleted.hasKey("id"));
        assertFalse(deleted.hasKey("idExt"));
        assertEquals(1, foreign.getShort("Damage"));
        assertTrue(foreign.hasKey("id"));
    }

    // Pins the adopted baseline: data Postea never stamped is list version 1.
    @Test
    void unstampedDataIsListVersionOne() {
        assertEquals(1, PosteaMigration.storedOrBaseline(ChunkTransformContext.UNSTAMPED));
        assertEquals(4, PosteaMigration.storedOrBaseline(4));
    }

    // A stamp ahead of the world's store (a restored store, copied region files) must not crash the chunk
    // load; leaving the data untouched keeps it recoverable once the right store is back.
    @Test
    void remapSkipsAStampNewerThanTheStore() {
        assertTrue(
            PosteaMigration.remap(MaterialIdTransitions.empty(), 3, 2)
                .isEmpty());
    }

    @Test
    void remapSkipsASpanWhoseTransitionIsMissing() {
        assertTrue(
            PosteaMigration.remap(MaterialIdTransitions.empty(), 1, 3)
                .isEmpty());
    }
}
