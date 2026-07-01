package com.ruling_0.materiallib;

import net.minecraftforge.client.MinecraftForgeClient;

import com.ruling_0.materiallib.api.ShapeItem;
import com.ruling_0.materiallib.api.ShapeItemRenderer;
import com.ruling_0.materiallib.api.ShapeRegistry;

import cpw.mods.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ShapeItemRenderer renderer = new ShapeItemRenderer();
        for (ShapeItem item : ShapeRegistry.instance().getItemShapes()) {
            MinecraftForgeClient.registerItemRenderer(item, renderer);
        }
    }
}
