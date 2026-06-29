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
/// rewritten to the instance index as it deserializes, or is dropped when the migration marks its material for
/// deletion (see [MaterialMigration]). A world that matches the instance sets no migration, so the transformers
/// stay inert.
///
/// Blocks placed in the world store the index as chunk metadata, which would need Postea's separate
/// `BlockReplacementManager`. That path hands the transformer only the low byte of the metadata (the meta is cast
/// to a signed byte before it is delivered), so it cannot carry material indices above 127; placed-block migration
/// is therefore unsupported while Postea truncates the metadata to a byte. This affects only a world moved to a
/// different instance whose assignment diverges, since the append-only index assignment means a world never needs
/// migrating on the instance that created it.
public final class PosteaMigration {

    private static volatile MaterialMigration active;

    private PosteaMigration() {}

    /// Registers a Postea item-stack transformer for every registered shape. Call in postInit, once shapes resolve.
    /// Every backing object is registered under MaterialLib's domain, so the handler is keyed by `materiallib:<name>`,
    /// the stack's saved id.
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

    /// Sets the migration applied to stored stacks for the world now loading, or clears it (null) when there is
    /// none, so a clean world loaded after a migrated one in the same session leaves the transformers inert.
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
