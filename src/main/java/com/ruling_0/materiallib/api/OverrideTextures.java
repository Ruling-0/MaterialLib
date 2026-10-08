package com.ruling_0.materiallib.api;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;

import com.ruling_0.materiallib.MaterialLib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// Whether any resource pack serving the MaterialLib domain carries resource-pack override textures (see
/// [ShapeIcons#overridePath]), so the per-material override lookups can be skipped when none does. A pack that is
/// neither a folder nor a zip file is assumed to carry some. The packs are scanned again whenever the resource manager
/// rebuilds them.
@SideOnly(Side.CLIENT)
final class OverrideTextures {

    private static final String OVERRIDE_DIR = ShapeIcons.OVERRIDE_ROOT.substring(MaterialLib.MODID.length() + 1);
    private static final String ITEMS = "assets/" + MaterialLib.MODID + "/textures/items/" + OVERRIDE_DIR;
    private static final String BLOCKS = "assets/" + MaterialLib.MODID + "/textures/blocks/" + OVERRIDE_DIR;

    private static Object scannedPacks;
    private static boolean items;
    private static boolean blocks;

    private OverrideTextures() {}

    /// Whether an override texture may exist on the item atlas, or the block atlas when `isItem` is false.
    static boolean mayExist(boolean isItem) {
        IResourceManager manager = Minecraft.getMinecraft().getResourceManager();
        if (!(manager instanceof SimpleReloadableResourceManager simple)) return true;
        Object domain = simple.domainResourceManagers.get(MaterialLib.MODID);
        if (!(domain instanceof FallbackResourceManager packs)) return true;
        if (packs != scannedPacks) {
            scan(packs);
            scannedPacks = packs;
        }
        return isItem ? items : blocks;
    }

    private static void scan(FallbackResourceManager packs) {
        items = false;
        blocks = false;
        for (Object pack : packs.resourcePacks) {
            File file = pack instanceof AbstractResourcePack abstractPack ? abstractPack.resourcePackFile : null;
            if (file == null) {
                items = true;
                blocks = true;
            }
            else if (file.isDirectory()) {
                items |= new File(file, ITEMS).isDirectory();
                blocks |= new File(file, BLOCKS).isDirectory();
            }
            else {
                scanZip(file);
            }
        }
    }

    private static void scanZip(File file) {
        try (ZipFile zip = new ZipFile(file)) {
            zip.stream()
                .forEach(entry -> {
                    items |= entry.getName().startsWith(ITEMS);
                    blocks |= entry.getName().startsWith(BLOCKS);
                });
        }
        catch (IOException e) {
            items = true;
            blocks = true;
        }
    }
}
