package com.ruling_0.materiallib;

import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.postea.api.BlockReplacementManager;
import com.gtnewhorizons.postea.api.ItemStackReplacementManager;
import com.gtnewhorizons.postea.utility.BlockConversionInfo;
import com.ruling_0.materiallib.api.MaterialMigration;
import com.ruling_0.materiallib.api.ShapeBlock;
import com.ruling_0.materiallib.api.ShapeItem;
import com.ruling_0.materiallib.api.ShapeRegistry;

/// Handles migration of items and placed blocks from a world's previous material id list version to the current
/// one, as determined by [MaterialMigration]. Ultimately, either translates metadata on itemstacks/blocks or
/// deletes.
public final class PosteaMigration {

    // Matches the id Postea's ChunkFixerUtility.AIR_ID skips over when it walks a sub-chunk.
    private static final int AIR_BLOCK_ID = 0;

    private static volatile MaterialMigration active;

    private PosteaMigration() {}

    /// Registers a Postea item-stack transformer for every item and block shape, and a placed-block transformer for
    /// every block shape. Call in postInit, once shapes resolve.
    public static void registerHandlers() {
        ShapeRegistry registry = ShapeRegistry.instance();
        for (ShapeItem item : registry.getItemShapes()) {
            addStackHandler(item.getName());
        }
        for (ShapeBlock block : registry.getBlockShapes()) {
            addStackHandler(block.getName());
            addBlockHandler(block.getName());
        }
    }

    private static void addStackHandler(String name) {
        ItemStackReplacementManager
            .addTransformationHandler(MaterialLib.MODID + ":" + name, PosteaMigration::transformStack);
    }

    private static void addBlockHandler(String name) {
        BlockReplacementManager
            .addTransformationHandler(MaterialLib.MODID + ":" + name, PosteaMigration::transformBlock);
    }

    /// Sets the migration applied to stored stacks and placed blocks for the world now loading, or clears it when
    /// there is none.
    public static void setActiveMigration(MaterialMigration migration) { active = migration; }

    private static boolean transformStack(String originalId, NBTTagCompound tag) {
        MaterialMigration migration = active;
        if (migration == null || !tag.hasKey("Damage")) return false;
        int result = migration.lookup(tag.getInteger("Damage"));
        if (result == MaterialMigration.UNCHANGED) return false;
        if (result == MaterialMigration.DELETE) {
            tag.removeTag("id");
            tag.removeTag("idExt");
            return true;
        }
        // Damage is serialized as a short by both vanilla and EndlessIDs, so the result is written back as a short.
        tag.setShort("Damage", (short) result);
        return true;
    }

    static boolean transformBlock(BlockConversionInfo info) {
        MaterialMigration migration = active;
        if (migration == null) return false;
        int result = migration.lookup(info.metadata);
        if (result == MaterialMigration.UNCHANGED) return false;
        if (result == MaterialMigration.DELETE) {
            info.blockID = AIR_BLOCK_ID;
            info.metadata = 0;
        }
        else {
            info.metadata = result;
        }
        return true;
    }
}
