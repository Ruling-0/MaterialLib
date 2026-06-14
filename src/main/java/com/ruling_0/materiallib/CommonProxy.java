package com.ruling_0.materiallib;

import java.io.File;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.ISaveHandler;

import com.ruling_0.materiallib.api.ItemShapeRegistry;
import com.ruling_0.materiallib.api.MaterialIdStore;
import com.ruling_0.materiallib.api.MaterialRegistry;
import com.ruling_0.materiallib.api.WorldMaterialIds;
import com.ruling_0.materiallib.examples.TempItemShapeExample;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        MaterialLib.LOG.info("MaterialLib version " + Tags.VERSION);

        // Temporary verification scaffolding; removed when the full examples land.
        TempItemShapeExample.register();
    }

    // Mods depending on materiallib register materials in their preInit handlers, which all run before this.
    public void init(FMLInitializationEvent event) {
        File dir = new File(Loader.instance().getConfigDir(), MaterialLib.MODID);
        MaterialIdStore.loadInto(MaterialRegistry.instance(), dir);
        MaterialRegistry.instance().resolve();
        MaterialIdStore.saveFrom(MaterialRegistry.instance(), dir);
        ItemShapeRegistry.instance().resolve();
    }

    public void postInit(FMLPostInitializationEvent event) {
        PosteaMigration.registerHandlers();
    }

    // Before the worlds load, so the per-world id copy is reconciled against the instance before any item loads.
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        ISaveHandler save = server.getActiveAnvilConverter()
            .getSaveLoader(server.getFolderName(), false);
        File worldFile = new File(new File(save.getWorldDirectory(), MaterialLib.MODID), "material-ids.json");
        PosteaMigration.setActiveMigration(WorldMaterialIds.check(MaterialRegistry.instance(), worldFile));
    }

    public void serverStarting(FMLServerStartingEvent event) {}
}
