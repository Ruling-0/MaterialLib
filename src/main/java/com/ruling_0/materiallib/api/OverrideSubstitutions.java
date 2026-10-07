package com.ruling_0.materiallib.api;

import static net.minecraftforge.common.util.Constants.NBT.TAG_STRING;

import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.postea.api.IDExtenderCompat;
import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/// Rewrites saved shape stacks and placed shape blocks of a pair served by a shape override into the overriding item.
///
/// Holds the numeric ids of one id mapping. [ShapeRegistry#overrideSubstitutions] rebuilds it after
/// [ShapeRegistry#invalidateOverrideSubstitutions].
public final class OverrideSubstitutions {

    private static final long NONE = -1L;

    private final Long2ObjectOpenHashMap<ItemStack> stacksById = new Long2ObjectOpenHashMap<>();
    private final Map<String, ItemStack> stacksByName = new Object2ObjectOpenHashMap<>();
    private final Long2LongOpenHashMap blocks = new Long2LongOpenHashMap();

    OverrideSubstitutions(Map<BackedShape, Map<Material, ShapeOverride>> overrides) {
        blocks.defaultReturnValue(NONE);
        for (Map.Entry<BackedShape, Map<Material, ShapeOverride>> shapeEntry : overrides.entrySet()) {
            for (Map.Entry<Material, ShapeOverride> entry : shapeEntry.getValue().entrySet()) {
                int index = entry.getKey().getIndex();
                ItemStack target = entry.getValue().resolve();
                Item shapeItem = shapeEntry.getKey() instanceof ShapeBlock block ? Item.getItemFromBlock(block) :
                    (Item) shapeEntry.getKey();
                stacksById.put(pack(Item.getIdFromItem(shapeItem), index), target);
                stacksByName.put(Item.itemRegistry.getNameForObject(shapeItem) + "@" + index, target);
                if (shapeEntry.getKey() instanceof ShapeBlock block) {
                    Block targetBlock = Block.getBlockFromItem(target.getItem());
                    if (targetBlock == null || targetBlock == Blocks.air) {
                        MaterialLib.LOG.warn(
                            "Placed {} blocks of {} cannot become {}, which has no block form; they are left in place",
                            shapeEntry.getKey(),
                            entry.getKey().getKey(),
                            entry.getValue().describe());
                        continue;
                    }
                    blocks.put(
                        pack(Block.getIdFromBlock(block), index),
                        pack(Block.getIdFromBlock(targetBlock), target.getItemDamage()));
                }
            }
        }
    }

    public boolean isEmpty() { return stacksById.isEmpty(); }

    /// Rewrites the id and damage of a saved stack of an overridden pair. Any other stack is left untouched.
    public void substituteStack(NBTTagCompound stack) {
        int damage = stack.getShort("Damage");
        if (stack.hasKey("id", TAG_STRING)) {
            ItemStack target = stacksByName.get(stack.getString("id") + "@" + damage);
            if (target == null) return;
            stack.setString("id", Item.itemRegistry.getNameForObject(target.getItem()));
            stack.setShort("Damage", (short) target.getItemDamage());
            return;
        }
        ItemStack target = stacksById.get(pack(IDExtenderCompat.getItemStackID(stack), damage));
        if (target == null) return;
        IDExtenderCompat.setItemStackID(stack, Item.getIdFromItem(target.getItem()));
        stack.setShort("Damage", (short) target.getItemDamage());
    }

    /// The `blockId << 32 | metadata` a placed block of an overridden pair becomes, or -1 when it stays. Unpack the
    /// result with [#blockId] and [#metadata].
    public long substituteBlock(int blockId, int metadata) {
        return blocks.get(pack(blockId, metadata));
    }

    public static int blockId(long packed) {
        return (int) (packed >>> 32);
    }

    public static int metadata(long packed) {
        return (int) packed;
    }

    private static long pack(int id, int meta) {
        return ((long) id << 32) | (meta & 0xFFFFFFFFL);
    }
}
