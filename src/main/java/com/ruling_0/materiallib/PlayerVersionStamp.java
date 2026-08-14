package com.ruling_0.materiallib;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.event.entity.EntityEvent;

import com.ruling_0.materiallib.api.WorldMaterialIds;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/// Stamps player data with the material id list version it was written under, mirroring [ChunkVersionStamp]
/// for the playerdata side. Registered as extended entity properties; the stamp is saved in the player's own
/// NBT under [ChunkVersionStamp#KEY].
public final class PlayerVersionStamp implements IExtendedEntityProperties {

    private int loadedVersion = 1;

    /// The list version the player's data was last saved under; 1 when the data predates stamping.
    public int loadedVersion() {
        return loadedVersion;
    }

    /// The stamp attached to `player`.
    public static PlayerVersionStamp of(EntityPlayer player) {
        return (PlayerVersionStamp) player.getExtendedProperties(ChunkVersionStamp.KEY);
    }

    @Override
    public void saveNBTData(NBTTagCompound compound) {
        compound.setInteger(ChunkVersionStamp.KEY, WorldMaterialIds.currentListVersion());
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        loadedVersion = ChunkVersionStamp.read(compound);
    }

    @Override
    public void init(Entity entity, World world) {}

    /// Attaches a stamp to every player as it is constructed.
    public static final class Handler {

        @SubscribeEvent
        public void onEntityConstructing(EntityEvent.EntityConstructing event) {
            if (event.entity instanceof EntityPlayer player) {
                player.registerExtendedProperties(ChunkVersionStamp.KEY, new PlayerVersionStamp());
            }
        }
    }
}
