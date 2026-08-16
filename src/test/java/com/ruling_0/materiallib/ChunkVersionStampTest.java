package com.ruling_0.materiallib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

class ChunkVersionStampTest {

    // Pins the adopted baseline: data saved before stamping existed carries no key and must read as list
    // version 1.
    @Test
    void anUnstampedTagReadsAsListVersionOne() {
        assertEquals(1, ChunkVersionStamp.read(new NBTTagCompound()));
    }
}
