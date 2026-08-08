package com.ruling_0.materiallib.api;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/// The item a [ShapeFluidInContainer] returns when its fluid is drained: an [ItemStack] already known when the
/// container shape is built, an empty container item MaterialLib itself registers, or a `modid:name` identifier
/// resolved once at MaterialLib's init -- after every mod's items exist -- through
/// [FluidInContainerShapeBuilder#emptyContainer(String, int)].
sealed interface EmptyContainer {

    /// Resolves to the item stack, a fresh copy each call.
    ItemStack resolve();

    /// An item stack already known.
    record Eager(ItemStack stack) implements EmptyContainer {

        public Eager {
            stack = stack.copy();
        }

        @Override
        public ItemStack resolve() {
            return stack.copy();
        }
    }

    /// An empty container item registered through [MaterialLibAPI#registerEmptyContainer(String, String)], usable
    /// once its handle binds at resolve.
    record Registered(EmptyContainerHandle handle) implements EmptyContainer {

        @Override
        public ItemStack resolve() {
            return handle.getStack(1);
        }
    }

    /// An item identified by `modid:name` at `meta`, looked up through `lookup` when [#resolve] is called. Fails
    /// loudly if no item is registered under that identifier.
    record Deferred(String modid, String name, int meta, ItemLookup lookup) implements EmptyContainer {

        @Override
        public ItemStack resolve() {
            Item item = lookup.find(modid, name);
            if (item == null) {
                throw new IllegalStateException(
                    "No item is registered for " + modid + ":" + name + "; cannot resolve the empty container. " +
                        "Deferred empty containers resolve at MaterialLib's init, once every mod has registered " +
                        "its items");
            }
            return new ItemStack(item, 1, meta);
        }
    }

    /// Looks an item up by identifier; implemented by
    /// [cpw.mods.fml.common.registry.GameRegistry#findItem(String, String)] in production and faked in tests.
    @FunctionalInterface
    interface ItemLookup {

        Item find(String modid, String name);
    }
}
