package com.ruling_0.materiallib;

import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

import com.ruling_0.materiallib.api.ShapeBlock;
import com.ruling_0.materiallib.api.ShapeBlockRenderingHandler;
import com.ruling_0.materiallib.api.ShapeFluidIcons;
import com.ruling_0.materiallib.api.ShapeItem;
import com.ruling_0.materiallib.api.ShapeItemRenderer;
import com.ruling_0.materiallib.api.ShapeRegistry;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ShapeItemRenderer renderer = new ShapeItemRenderer();
        for (ShapeItem item : ShapeRegistry.instance().getItemShapes()) {
            MinecraftForgeClient.registerItemRenderer(item, renderer);
        }
        ShapeBlockRenderingHandler blockRenderer = new ShapeBlockRenderingHandler();
        RenderingRegistry.registerBlockHandler(blockRenderer);
        for (ShapeBlock block : ShapeRegistry.instance().getBlockShapes()) {
            if (block.hasBaseTexture()) {
                block.setRenderType(blockRenderer.getRenderId());
            }
        }
        MinecraftForge.EVENT_BUS.register(new ShapeFluidIcons());
    }
}
