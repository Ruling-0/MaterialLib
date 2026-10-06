package com.ruling_0.materiallib;

import static net.minecraftforge.common.util.Constants.NBT.TAG_STRING;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.postea.api.ChunkTransformContext;
import com.gtnewhorizons.postea.api.CustomDataTransformContext;
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

/// Remaps shape block metadata and shape stack damage in saved data from the material id list version it was saved
/// under to the current one, through the world's [MaterialIdTransitions] chain. Covers chunks, player data and mods'
/// own world storage.
///
/// Data Postea never stamped is treated as list version 1. Loading data the chain cannot bring to the current version
/// fails.
///
/// Shape ids are cached until the next FML id remap ([#invalidateIds]), which rewrites them to the world's map.
public final class PosteaMigration implements IVersionedTransformer {

    static final String KEY = MaterialLib.MODID + ":idList";

    private static volatile IntSet blockIds;
    private static volatile IntSet itemIds;
    private static volatile Set<String> itemNames;

    private PosteaMigration() {}

    /// Registers the transformer with Postea. Call in postInit, once shapes resolve.
    public static void register() {
        VersionedReplacementManager.register(new PosteaMigration());
    }

    /// Drops the cached shape ids and names.
    public static void invalidateIds() {
        blockIds = null;
        itemIds = null;
        itemNames = null;
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
        Int2IntMap remap = remap(
            WorldMaterialIds.transitions(),
            storedOrBaseline(ctx.storedVersion()),
            ctx.currentVersion());
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
        Set<String> names = itemNames();
        ctx.forEachItemStackTag(stack -> remapStack(stack, remap, items, names));
    }

    @Override
    public void transformPlayer(PlayerDataTransformContext ctx) {
        Int2IntMap remap = remap(
            WorldMaterialIds.transitions(),
            storedOrBaseline(ctx.storedVersion()),
            ctx.currentVersion());
        if (remap.isEmpty()) return;
        IntSet items = itemIds();
        Set<String> names = itemNames();
        ctx.forEachItemStackTag(stack -> remapStack(stack, remap, items, names));
    }

    @Override
    public void transformCustomData(CustomDataTransformContext ctx) {
        Int2IntMap remap = remap(
            WorldMaterialIds.transitions(),
            storedOrBaseline(ctx.storedVersion()),
            ctx.currentVersion());
        if (remap.isEmpty()) return;
        IntSet items = itemIds();
        Set<String> names = itemNames();
        ctx.forEachItemStackTag(stack -> remapStack(stack, remap, items, names));
    }

    /// The list version to migrate from: Postea's stamp, or 1 for data that has none.
    static int storedOrBaseline(int stored) {
        return stored == ChunkTransformContext.UNSTAMPED ? 1 : stored;
    }

    /// The remap from `from` to `to`. Throws [IllegalStateException] when the world's transition chain cannot
    /// compose the span.
    static Int2IntMap remap(MaterialIdTransitions transitions, int from, int to) {
        if (from == to) return Int2IntMaps.EMPTY_MAP;
        try {
            return transitions.remap(from, to);
        }
        catch (IllegalArgumentException | IllegalStateException e) {
            throw new IllegalStateException(
                "Saved data stamped at material id list version " + from +
                    " cannot be brought to the world's version " +
                    to + ". Restore the world's materiallib directory that matches its chunks and player data.",
                e);
        }
    }

    /// Rewrites `stack`'s damage through `remap` when its item is in `shapeItems` (numeric id) or `shapeItemNames`
    /// (registry name). A deleted index strips the item id, so the stack loads as empty.
    static void remapStack(NBTTagCompound stack, Int2IntMap remap, IntSet shapeItems, Set<String> shapeItemNames) {
        boolean shape = stack.hasKey("id", TAG_STRING) ? shapeItemNames.contains(stack.getString("id")) :
            shapeItems.contains(IDExtenderCompat.getItemStackID(stack));
        if (!shape) return;
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

    private static Set<String> itemNames() {
        Set<String> names = itemNames;
        if (names == null) {
            names = new HashSet<>();
            ShapeRegistry registry = ShapeRegistry.instance();
            for (ShapeItem item : registry.getItemShapes()) {
                names.add(Item.itemRegistry.getNameForObject(item));
            }
            for (ShapeBlock block : registry.getBlockShapes()) {
                names.add(Item.itemRegistry.getNameForObject(Item.getItemFromBlock(block)));
            }
            itemNames = names;
        }
        return names;
    }
}
