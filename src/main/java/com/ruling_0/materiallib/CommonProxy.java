package com.ruling_0.materiallib;

import com.ruling_0.materiallib.api.ItemShapeRegistry;
import com.ruling_0.materiallib.api.MaterialRegistry;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        MaterialLib.LOG.info("MaterialLib version " + Tags.VERSION);
    }

    // Mods depending on materiallib register materials in their preInit handlers, which all run before this.
    public void init(FMLInitializationEvent event) {
        MaterialRegistry.instance().resolve();
        ItemShapeRegistry.instance().resolve();
    }

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {}
}
