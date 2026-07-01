package com.ruling_0.materiallib;

import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.postea.api.ItemStackReplacementManager;
import com.ruling_0.materiallib.api.MaterialMigration;
import com.ruling_0.materiallib.api.ShapeBlock;
import com.ruling_0.materiallib.api.ShapeItem;
import com.ruling_0.materiallib.api.ShapeRegistry;

/// Migrates stored shape stacks onto this instance's material index assignment as they load, through Postea.
///
/// A Postea item-stack transformer is registered for every shape during postInit -- both item shapes and the item
/// form of block shapes, whose stacks in inventories and containers carry the material index as their damage. At
/// world load the active migration is set from the per-world reconciliation; each stored stack then has its damage
/// rewritten to the instance index or is dropped when the migration marks its material for deletion (see
/// [MaterialMigration]).
///
/// FIXME: Postea truncated block metadata to a byte, so in-world blocks with metadata >127 cannot be transformed.
public final class PosteaMigration {

    private static volatile MaterialMigration active;

    private PosteaMigration() {}

    /// Registers a Postea item-stack transformer for every registered shape. Call in postInit, once shapes resolve.
    public static void registerHandlers() {
        ShapeRegistry registry = ShapeRegistry.instance();
        for (ShapeItem item : registry.getItemShapes()) {
            addStackHandler(item.getName());
        }
        for (ShapeBlock block : registry.getBlockShapes()) {
            addStackHandler(block.getName());
        }
    }

    private static void addStackHandler(String name) {
        ItemStackReplacementManager
            .addTransformationHandler(MaterialLib.MODID + ":" + name, PosteaMigration::transform);
    }

    /// Sets the migration applied to stored stacks for the world now loading, or clears it when there is none.
    public static void setActiveMigration(MaterialMigration migration) { active = migration; }

    private static boolean transform(String originalId, NBTTagCompound tag) {
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
}
