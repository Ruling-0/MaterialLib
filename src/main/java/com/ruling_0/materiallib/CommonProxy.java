package com.ruling_0.materiallib;

import java.io.File;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.ISaveHandler;

import net.minecraftforge.common.MinecraftForge;

import com.ruling_0.materiallib.api.MaterialOwnerStore;
import com.ruling_0.materiallib.api.MaterialRegistrationEvent;
import com.ruling_0.materiallib.api.MaterialRegistry;
import com.ruling_0.materiallib.api.OreDictUnificator;
import com.ruling_0.materiallib.api.ShapeOwnerStore;
import com.ruling_0.materiallib.api.ShapeRegistry;
import com.ruling_0.materiallib.api.WorldMaterialIds;
import com.ruling_0.materiallib.examples.ExampleContent;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    private LateHandlerCheck lateHandlerCheck;

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        OreDictUnificator.instance()
            .configure();

        MaterialLib.LOG.info("MaterialLib version " + Tags.VERSION);

        if (Config.registerExamples) {
            MinecraftForge.EVENT_BUS.register(new ExampleContent());
        }
        MaterialLib.LOG.info("Collecting registrations from MaterialRegistrationEvent handlers");
        MaterialRegistrationEvent registration = new MaterialRegistrationEvent();
        MinecraftForge.EVENT_BUS.post(registration);
        lateHandlerCheck = LateHandlerCheck.snapshot(registration);

        File dir = new File(event.getModConfigurationDirectory(), MaterialLib.MODID);
        MaterialOwnerStore.loadInto(MaterialRegistry.instance(), dir);
        MaterialRegistry.instance().resolve();
        MaterialOwnerStore.saveFrom(MaterialRegistry.instance(), dir);

        ShapeOwnerStore.loadInto(ShapeRegistry.instance(), dir);
        ShapeRegistry.instance().resolve();
        ShapeOwnerStore.saveFrom(ShapeRegistry.instance(), dir);
    }

    public void init(FMLInitializationEvent event) {
        ShapeRegistry.instance().registerFluidContainers();
        ShapeRegistry.instance().runInitConsumers();
    }

    public void postInit(FMLPostInitializationEvent event) {
        ShapeRegistry.instance().runPostInitConsumers();
        PosteaMigration.registerHandlers();
        if (lateHandlerCheck != null) {
            lateHandlerCheck.report();
        }
    }

    // Before the worlds load, so the world's id list is reconciled against the registry before any item loads.
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        ISaveHandler save = server.getActiveAnvilConverter().getSaveLoader(server.getFolderName(), false);
        File dir = new File(save.getWorldDirectory(), MaterialLib.MODID);
        PosteaMigration.setActiveMigration(WorldMaterialIds.check(MaterialRegistry.instance(), dir));
    }

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandDumpMats(event.getServer().getFile("materiallib-dump.csv")));
        event.registerServerCommand(new CommandMatInfo());
    }
}
