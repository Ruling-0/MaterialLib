package com.ruling_0.materiallib;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.postea.api.ChunkTransformContext;
import com.gtnewhorizons.postea.api.IDExtenderCompat;
import com.gtnewhorizons.postea.api.IVersionedTransformer;
import com.gtnewhorizons.postea.api.PlayerDataTransformContext;
import com.gtnewhorizons.postea.api.VersionedReplacementManager;
import com.ruling_0.materiallib.api.MaterialIdTransitions;
import com.ruling_0.materiallib.api.ShapeBlock;
import com.ruling_0.materiallib.api.ShapeItem;
import com.ruling_0.materiallib.api.ShapeRegistry;
import com.ruling_0.materiallib.api.WorldMaterialIds;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/// MaterialLib's Postea transformer: rewrites every shape block's metadata and every shape stack's damage in a
/// chunk or a player's data from the material id list version the data was saved under to the current one, through
/// the world's [MaterialIdTransitions] chain. Data stamped by neither Postea nor an earlier MaterialLib reads as list
/// version 1, the adopted baseline.
///
/// Shape ids are derived from the registries on first use after each id remap, since FML rewrites numeric ids to the
/// world's map after the server starts.
public final class PosteaMigration implements IVersionedTransformer {

    static final String KEY = MaterialLib.MODID + ":idList";

    private static volatile IntSet blockIds;
    private static volatile IntSet itemIds;

    private PosteaMigration() {}

    /// Registers the transformer with Postea. Call in postInit, once shapes resolve.
    public static void register() {
        VersionedReplacementManager.register(new PosteaMigration());
    }

    /// Drops the cached shape ids.
    public static void invalidateIds() {
        blockIds = null;
        itemIds = null;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public int currentVersion() {
        return WorldMaterialIds.currentListVersion();
    }

    @Override
    public void transformChunk(ChunkTransformContext ctx) {
        Int2IntMap remap = remap(storedOrLegacy(ctx.storedVersion(), ctx.levelTag()), ctx.currentVersion());
        if (remap.isEmpty()) return;
        IntSet blocks = blockIds();
        ctx.forEachBlock((x, y, z, id, meta) -> {
            if (!blocks.contains(id)) return;
            int result = remap.getOrDefault(meta, meta);
            if (result == meta) return;
            if (result == MaterialIdTransitions.DELETE) {
                ctx.setBlock(x, y, z, 0, 0);
            }
            else {
                ctx.setBlock(x, y, z, id, result);
            }
        });
        IntSet items = itemIds();
        ctx.forEachItemStackTag(stack -> remapStack(stack, remap, items));
    }

    @Override
    public void transformPlayer(PlayerDataTransformContext ctx) {
        Int2IntMap remap = remap(storedOrLegacy(ctx.storedVersion(), ctx.playerTag()), ctx.currentVersion());
        if (remap.isEmpty()) return;
        IntSet items = itemIds();
        ctx.forEachItemStackTag(stack -> remapStack(stack, remap, items));
    }

    /// The list version to migrate from: Postea's stamp, else the stamp earlier MaterialLib builds wrote on the
    /// same tag, else 1.
    static int storedOrLegacy(int stored, NBTTagCompound tag) {
        return stored == ChunkTransformContext.UNSTAMPED ? ChunkVersionStamp.read(tag) : stored;
    }

    private static Int2IntMap remap(int from, int to) {
        return from == to ? Int2IntMaps.EMPTY_MAP : WorldMaterialIds.transitions().remap(from, to);
    }

    /// Rewrites `stack`'s damage through `remap` when its item is one of `shapeItems`; a deleted index strips the
    /// item id so the stack loads as empty.
    static void remapStack(NBTTagCompound stack, Int2IntMap remap, IntSet shapeItems) {
        if (!shapeItems.contains(IDExtenderCompat.getItemStackID(stack))) return;
        int damage = stack.getShort("Damage");
        int result = remap.getOrDefault(damage, damage);
        if (result == damage) return;
        if (result == MaterialIdTransitions.DELETE) {
            stack.removeTag("id");
            stack.removeTag("idExt");
        }
        else {
            stack.setShort("Damage", (short) result);
        }
    }

    private static IntSet blockIds() {
        IntSet ids = blockIds;
        if (ids == null) {
            ids = new IntOpenHashSet();
            for (ShapeBlock block : ShapeRegistry.instance().getBlockShapes()) {
                ids.add(Block.getIdFromBlock(block));
            }
            blockIds = ids;
        }
        return ids;
    }

    private static IntSet itemIds() {
        IntSet ids = itemIds;
        if (ids == null) {
            ids = new IntOpenHashSet();
            ShapeRegistry registry = ShapeRegistry.instance();
            for (ShapeItem item : registry.getItemShapes()) {
                ids.add(Item.getIdFromItem(item));
            }
            for (ShapeBlock block : registry.getBlockShapes()) {
                ids.add(Item.getIdFromItem(Item.getItemFromBlock(block)));
            }
            itemIds = ids;
        }
        return ids;
    }
}
