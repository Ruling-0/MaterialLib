package com.ruling_0.materiallib;

import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.postea.api.ItemStackReplacementManager;
import com.ruling_0.materiallib.api.ItemShapeRegistry;
import com.ruling_0.materiallib.api.MaterialMigration;
import com.ruling_0.materiallib.api.ShapeItem;

/// Migrates stored item shape stacks onto this instance's material index assignment as they load, through Postea.
///
/// A Postea transformer is registered for every item shape during postInit; at world load the active migration is
/// set from the per-world reconciliation. Each stored stack of an item shape then has its damage (the material
/// index) rewritten to the instance index as it deserializes, or is dropped when the material no longer exists. A
/// world that matches the instance sets no migration, so the transformers stay inert.
public final class PosteaMigration {

    private static MaterialMigration active;

    private PosteaMigration() {}

    /// Registers a Postea transformer for every registered item shape. Call in postInit, once item shapes resolve.
    public static void registerHandlers() {
        for (ShapeItem item : ItemShapeRegistry.instance()
            .getItemShapes()) {
            ItemStackReplacementManager
                .addTransformationHandler(item.getModId() + ":" + item.getName(), PosteaMigration::transform);
        }
    }

    /// Sets the migration applied to stored stacks for the world now loading, or clears it (null) when there is
    /// none, so a clean world loaded after a migrated one in the same session leaves the transformers inert.
    public static void setActiveMigration(MaterialMigration migration) { active = migration; }

    private static boolean transform(String originalId, NBTTagCompound tag) {
        MaterialMigration migration = active;
        if (migration == null) return false;
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
