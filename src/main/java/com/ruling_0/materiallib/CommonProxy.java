package com.ruling_0.materiallib;

import com.ruling_0.materiallib.api.ItemShapeRegistry;
import com.ruling_0.materiallib.api.MaterialRegistry;
import com.ruling_0.materiallib.examples.TempItemShapeExample;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        MaterialLib.LOG.info("MaterialLib version " + Tags.VERSION);

        // Temporary feature-2 verification scaffolding; removed when feature 8's examples land.
        TempItemShapeExample.register();
    }

    // Mods depending on materiallib register materials in their preInit handlers, which all run before this.
    public void init(FMLInitializationEvent event) {
        MaterialRegistry.instance().resolve();
        ItemShapeRegistry.instance().resolve();
    }

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {}
}
