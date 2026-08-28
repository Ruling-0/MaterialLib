package com.ruling_0.materiallib;

import net.minecraft.nbt.NBTTagCompound;

/// The list-version stamp MaterialLib wrote on chunk `Level` tags and player root tags before Postea stamped
/// versions itself. Read as the fallback for data those builds saved.
public final class ChunkVersionStamp {

    public static final String KEY = "materiallib:idListVersion";

    private ChunkVersionStamp() {}

    /// The list version a tag was written under; 1 when the key is absent.
    public static int read(NBTTagCompound tag) {
        return tag.hasKey(KEY) ? tag.getInteger(KEY) : 1;
    }
}
