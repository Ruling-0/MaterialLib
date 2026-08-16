package com.ruling_0.materiallib.api;

import net.minecraft.item.ItemStack;

/// The item a [ShapeFluidInContainer] returns when its fluid is drained: either an [ItemStack] already known when
/// the container shape is built, or an empty container item MaterialLib registers itself.
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

    /// An empty container item registered through [MaterialLibAPI#registerEmptyContainer(String, String, String)],
    /// usable
    /// once its handle binds at resolve.
    record Registered(EmptyContainerHandle handle) implements EmptyContainer {

        @Override
        public ItemStack resolve() {
            return handle.getStack(1);
        }
    }
}
