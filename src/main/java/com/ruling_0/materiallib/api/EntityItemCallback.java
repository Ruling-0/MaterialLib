package com.ruling_0.materiallib.api;

import net.minecraft.entity.item.EntityItem;

/// A callback an item [Shape]'s items run every tick while dropped in the world, attached through
/// [ShapeEdit#onEntityItemUpdate].
@FunctionalInterface
public interface EntityItemCallback {

    /// Handles one tick of `entity`, whose stack is an item of `material`. Returns true when it acted, which skips
    /// later callbacks and the entity's own update for this tick.
    boolean onEntityItemUpdate(Material material, EntityItem entity);
}
