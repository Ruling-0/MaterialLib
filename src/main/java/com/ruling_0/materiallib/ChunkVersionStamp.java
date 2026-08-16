package com.ruling_0.materiallib;

import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.event.world.ChunkDataEvent;

import com.ruling_0.materiallib.api.WorldMaterialIds;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/// Stamps every saved chunk with the material id list version it was written under. The stamp records which
/// assignment the chunk's stored indices reference.
public final class ChunkVersionStamp {

    /// The int NBT key carrying the list version.
    public static final String KEY = "materiallib:idListVersion";

    /// The list version a stamped tag was written under. An absent key means the data predates stamping and
    /// is treated as list version 1, the adopted baseline.
    public static int read(NBTTagCompound tag) {
        return tag.hasKey(KEY) ? tag.getInteger(KEY) : 1;
    }

    @SubscribeEvent
    public void onChunkSave(ChunkDataEvent.Save event) {
        NBTTagCompound data = event.getData();
        NBTTagCompound level = data.getCompoundTag("Level");
        level.setInteger(KEY, WorldMaterialIds.currentListVersion());
    }
}
