package com.ruling_0.materiallib.api;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/// Renders a [ShapeBlock#hasBaseTexture] composite -- an untinted base texture under a tinted material icon -- as a
/// single draw, in world and in every item form (GUI slot, hotbar, held, and dropped). A block with no base
/// texture keeps the vanilla full-cube render type and never reaches this handler; see [ShapeBlock#setRenderType].
///
/// Both [#renderWorldBlock] and [#renderInventoryBlock] draw the two layers back-to-back into the same
/// [Tessellator] batch, toggling [ShapeBlock#setLayerOverride] so [ShapeBlock#getIcon],
/// [ShapeBlock#getRenderColor], and [ShapeBlock#colorMultiplier] resolve the base for the first draw and the
/// overlay for the second. Sharing one batch submits the coplanar quads with identical vertex data, so the depth
/// test resolves the tie in submission order instead of z-fighting.
///
/// The whole composite draws in the solid chunk pass (the vanilla render-pass defaults), where the alpha test cuts
/// out the overlay's transparent pixels; the overlay icons are cutout textures, not translucent ones, matching
/// legacy GT ore blocks.
@SideOnly(Side.CLIENT)
public final class ShapeBlockRenderingHandler implements ISimpleBlockRenderingHandler {

    private static final int RENDER_ID = RenderingRegistry.getNextAvailableRenderId();

    @Override
    public int getRenderId() { return RENDER_ID; }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        if (!(block instanceof ShapeBlock shape)) return;
        block.setBlockBoundsForItemRender();
        renderer.setRenderBoundsFromBlock(block);
        renderer.useInventoryTint = true;

        GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        drawInventoryLayer(renderer, tessellator, shape, metadata, 0);
        drawInventoryLayer(renderer, tessellator, shape, metadata, 1);
        tessellator.draw();

        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
    }

    private static void drawInventoryLayer(RenderBlocks renderer, Tessellator tessellator, ShapeBlock shape,
                                           int metadata, int layer) {
        shape.setLayerOverride(layer);
        try {
            int color = shape.getRenderColor(metadata);
            tessellator.setColorOpaque_F(
                (color >> 16 & 255) / 255.0F,
                (color >> 8 & 255) / 255.0F,
                (color & 255) / 255.0F);
            drawInventoryFace(renderer, tessellator, shape, 0, metadata, 0.0F, -1.0F, 0.0F);
            drawInventoryFace(renderer, tessellator, shape, 1, metadata, 0.0F, 1.0F, 0.0F);
            drawInventoryFace(renderer, tessellator, shape, 2, metadata, 0.0F, 0.0F, -1.0F);
            drawInventoryFace(renderer, tessellator, shape, 3, metadata, 0.0F, 0.0F, 1.0F);
            drawInventoryFace(renderer, tessellator, shape, 4, metadata, -1.0F, 0.0F, 0.0F);
            drawInventoryFace(renderer, tessellator, shape, 5, metadata, 1.0F, 0.0F, 0.0F);
        }
        finally {
            shape.setLayerOverride(-1);
        }
    }

    private static void drawInventoryFace(RenderBlocks renderer, Tessellator tessellator, ShapeBlock shape, int side,
                                          int metadata, float nx, float ny, float nz) {
        IIcon icon = shape.getIcon(side, metadata);
        tessellator.setNormal(nx, ny, nz);
        switch (side) {
            case 0 -> renderer.renderFaceYNeg(shape, 0.0D, 0.0D, 0.0D, icon);
            case 1 -> renderer.renderFaceYPos(shape, 0.0D, 0.0D, 0.0D, icon);
            case 2 -> renderer.renderFaceZNeg(shape, 0.0D, 0.0D, 0.0D, icon);
            case 3 -> renderer.renderFaceZPos(shape, 0.0D, 0.0D, 0.0D, icon);
            case 4 -> renderer.renderFaceXNeg(shape, 0.0D, 0.0D, 0.0D, icon);
            case 5 -> renderer.renderFaceXPos(shape, 0.0D, 0.0D, 0.0D, icon);
            default -> throw new IllegalArgumentException("side must be 0..5, got " + side);
        }
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
                                    RenderBlocks renderer) {
        if (!(block instanceof ShapeBlock shape)) return false;
        boolean renderedBase = drawWorldLayer(renderer, shape, x, y, z, 0);
        boolean renderedOverlay = drawWorldLayer(renderer, shape, x, y, z, 1);
        return renderedBase || renderedOverlay;
    }

    private static boolean drawWorldLayer(RenderBlocks renderer, ShapeBlock shape, int x, int y, int z, int layer) {
        shape.setLayerOverride(layer);
        try {
            return renderer.renderStandardBlock(shape, x, y, z);
        }
        finally {
            shape.setLayerOverride(-1);
        }
    }
}
