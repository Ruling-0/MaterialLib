package com.ruling_0.materiallib.api;

import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;

/// The foreign item serving a (material, shape) pair in place of one MaterialLib mints, declared through
/// [MaterialBuilder#addShapeOverride(Shape, Item)] and its overloads.
///
/// A vanilla item or block exists while materials register and is held as an [Eager] stack. Another mod's item is
/// held as a [Named] registry name that binds at MaterialLib's init.
sealed interface ShapeOverride {

    /// Whether the item's mod is present this session.
    boolean isAvailable(Predicate<String> modLoaded);

    /// The item stack with size 1, a fresh copy each call. Throws [IllegalStateException] for a [Named] override whose
    /// name matches no registered item or block.
    ItemStack resolve();

    /// The stable `modid:name:meta` identity of the item.
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
