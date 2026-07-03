package com.ruling_0.materiallib.api;

import net.minecraftforge.client.event.TextureStitchEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// Binds every fluid shape's icons when the block texture atlas stitches.
///
/// Forge fluids draw from the block atlas, so their still and flowing icons must be registered during the blocks
/// [TextureStitchEvent] rather than through a block's or item's own icon callback. Registered on the Forge event
/// bus from the client proxy.
@SideOnly(Side.CLIENT)
public final class ShapeFluidIcons {

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 0) return;
        for (ShapeFluid fluid : ShapeRegistry.instance().getFluidShapes()) {
            fluid.registerIcons(event.map);
        }
    }
}
