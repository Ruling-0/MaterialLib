package com.ruling_0.materiallib.api;

import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;

/// The foreign item standing in for a (material, shape) pair MaterialLib would otherwise mint itself, declared
/// through [MaterialBuilder#addShapeOverride(Shape, Item)] and its overloads.
///
/// Vanilla items exist while materials register, so they are held as an [Eager] stack. Another mod's item does
/// not exist until that mod's own preInit, after MaterialLib has resolved, so it is held as a [Named] registry
/// name that binds at MaterialLib's init.
sealed interface ShapeOverride {

    /// Whether the item's mod is present this session. An absent override is treated as never declared.
    boolean isAvailable(Predicate<String> modLoaded);

    /// The item stack with size 1, a fresh copy each call.
    ItemStack resolve();

    /// The stable `modid:name:meta` identity of the item, fingerprinted into the world's override digest.
    String describe();

    /// An item stack already known.
    record Eager(ItemStack stack) implements ShapeOverride {

        public Eager {
            stack = stack.copy();
            stack.stackSize = 1;
        }

        @Override
        public boolean isAvailable(Predicate<String> modLoaded) {
            return true;
        }

        @Override
        public ItemStack resolve() {
            return stack.copy();
        }

        @Override
        public String describe() {
            return Item.itemRegistry.getNameForObject(stack.getItem()) + ":" + stack.getItemDamage();
        }
    }

    /// An item or block named by its registry name, looked up on first use and then kept.
    final class Named implements ShapeOverride {

        private final String modid;
        private final String itemName;
        private final int meta;
        private ItemStack stack;

        Named(String modid, String itemName, int meta) {
            this.modid = modid;
            this.itemName = itemName;
            this.meta = meta;
        }

        @Override
        public boolean isAvailable(Predicate<String> modLoaded) {
            return modLoaded.test(modid);
        }

        @Override
        public ItemStack resolve() {
            if (stack == null) {
                Item item = GameRegistry.findItem(modid, itemName);
                if (item == null) {
                    Block block = GameRegistry.findBlock(modid, itemName);
                    item = block == null ? null : Item.getItemFromBlock(block);
                }
                if (item == null) {
                    throw new IllegalStateException(
                        "Shape override " + describe() + " names no registered item or block. Named overrides " +
                            "bind at MaterialLib's init, so the item has to be registered during its mod's preInit");
                }
                stack = new ItemStack(item, 1, meta);
            }
            return stack.copy();
        }

        @Override
        public String describe() {
            return modid + ":" + itemName + ":" + meta;
        }
    }
}
