package com.ruling_0.materiallib;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.postea.api.BlockReplacementManager;
import com.gtnewhorizons.postea.api.ItemStackReplacementManager;
import com.gtnewhorizons.postea.utility.BlockConversionInfo;
import com.ruling_0.materiallib.api.MaterialMigration;
import com.ruling_0.materiallib.api.ShapeBlock;
import com.ruling_0.materiallib.api.ShapeItem;
import com.ruling_0.materiallib.api.ShapeRegistry;

/// Handles migration of items and placed blocks from the assignment stored in the world to that stored on the
/// instance, as determined by [MaterialMigration]. Ultimately, either translates metadata on itemstacks/blocks or
/// deletes.
public final class PosteaMigration {

    private static final int AIR_BLOCK_ID = Block.getIdFromBlock(Blocks.air);

    private static volatile MaterialMigration active;

    private PosteaMigration() {}

    /// Registers a Postea item-stack and placed-block transformer for every item and block shape, including each
    /// variant backing block of a [com.ruling_0.materiallib.api.BlockShapeBuilder#variants] shape (
    /// [ShapeRegistry#getBlockShapes] already lists those individually). Call in postInit, once shapes resolve.
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
        int result = migration.lookup(tag.getShort("Damage"));
        if (result == MaterialMigration.UNCHANGED) return false;
        if (result == MaterialMigration.DELETE) {
            tag.removeTag("id");
            tag.removeTag("idExt");
            return true;
        }
        tag.setShort("Damage", (short) result);
        return true;
    }

    private static boolean transformBlock(BlockConversionInfo info) {
        MaterialMigration migration = active;
        if (migration == null) return false;
        BlockUpdate update = blockUpdateFor(migration.lookup(info.metadata), info.blockID);
        if (update == null) return false;
        info.blockID = update.blockId();
        info.metadata = update.metadata();
        return true;
    }

    /// The block id and metadata a placed block should carry given a [MaterialMigration#lookup] result and the
    /// block's current numeric id, or `null` when the result is [MaterialMigration#UNCHANGED] and the block needs
    /// no transformation. [MaterialMigration#DELETE] replaces the block with air; any other result keeps the same
    /// block with the remapped metadata.
    static BlockUpdate blockUpdateFor(int result, int currentBlockId) {
        if (result == MaterialMigration.UNCHANGED) return null;
        if (result == MaterialMigration.DELETE) return new BlockUpdate(AIR_BLOCK_ID, 0);
        return new BlockUpdate(currentBlockId, result);
    }

    record BlockUpdate(int blockId, int metadata) {}
}
