package com.ruling_0.materiallib.api;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/// Renders a [ShapeBlock] composite -- an untinted base texture, where the variant declares one, under the
/// material's icon layer stack -- in world and in every item form (GUI slot, hotbar, held, and dropped). A block
/// with neither a base texture nor layered material art keeps the vanilla full-cube render type and never reaches
/// this handler; see [ShapeBlock#setRenderType].
///
/// Each layer is drawn with its icon and color passed in explicitly, under a [RenderBlocks] override texture, so
/// the handler holds no state and one shared instance serves every thread of Angelica's off-thread chunk meshing.
/// [#renderInventoryBlock] draws the layers back-to-back into the same [Tessellator] batch; [#renderWorldBlock]
/// makes one standard-block draw per layer with explicit colors. Submitting the coplanar quads with identical
/// vertex data lets the depth test resolve the tie in submission order instead of z-fighting.
///
/// The whole composite draws in the solid chunk pass (the vanilla render-pass defaults), where the alpha test cuts
/// out the overlay's transparent pixels; the overlay icons are cutout textures, not translucent ones, matching
/// legacy GT ore blocks.
@SideOnly(Side.CLIENT)
@ThreadSafeISBRH(perThread = false)
public final class ShapeBlockRenderingHandler implements ISimpleBlockRenderingHandler {

    /// The render ID this handler is registered under; see [ShapeBlock#setRenderType].
    static final int RENDER_ID = RenderingRegistry.getNextAvailableRenderId();

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

        GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        IIcon base = shape.baseIcon();
        if (base != null) {
            drawInventoryLayer(renderer, tessellator, shape, base, 0xFFFFFF);
        }
        for (int layer = 0, layers = shape.materialLayerCount(metadata); layer < layers; layer++) {
            drawInventoryLayer(renderer, tessellator, shape, shape.materialLayer(metadata, layer),
                layerColor(shape, metadata, layer));
        }
        tessellator.draw();

        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
    }

    /// The color a material icon layer draws with; see [ShapeBlock#tintFor] and [ShapeBlock#layerTint].
    private static int layerColor(ShapeBlock shape, int meta, int layer) {
        return layer == 0 ? shape.tintFor(meta) : shape.layerTint(meta, layer);
    }

    private static void drawInventoryLayer(RenderBlocks renderer, Tessellator tessellator, ShapeBlock shape,
                                           IIcon icon, int color) {
        tessellator.setColorOpaque_F((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F);
        drawInventoryFace(renderer, tessellator, shape, 0, icon, 0.0F, -1.0F, 0.0F);
        drawInventoryFace(renderer, tessellator, shape, 1, icon, 0.0F, 1.0F, 0.0F);
        drawInventoryFace(renderer, tessellator, shape, 2, icon, 0.0F, 0.0F, -1.0F);
        drawInventoryFace(renderer, tessellator, shape, 3, icon, 0.0F, 0.0F, 1.0F);
        drawInventoryFace(renderer, tessellator, shape, 4, icon, -1.0F, 0.0F, 0.0F);
        drawInventoryFace(renderer, tessellator, shape, 5, icon, 1.0F, 0.0F, 0.0F);
    }

    private static void drawInventoryFace(RenderBlocks renderer, Tessellator tessellator, ShapeBlock shape, int side,
                                          IIcon icon, float nx, float ny, float nz) {
        tessellator.setNormal(nx, ny, nz);
        switch (side) {
            case 0 -> renderer.renderFaceYNeg(shape, 0.0D, 0.0D, 0.0D, icon);
            case 1 -> renderer.renderFaceYPos(shape, 0.0D, 0.0D, 0.0D, icon);
            case 2 -> renderer.renderFaceZNeg(shape, 0.0D, 0.0D, 0.0D, icon);
            case 3 -> renderer.renderFaceZPos(shape, 0.0D, 0.0D, 0.0D, icon);
            case 4 -> renderer.renderFaceXNeg(shape, 0.0D, 0.0D, 0.0D, icon);
            case 5 -> renderer.renderFaceXPos(shape, 0.0D, 0.0D, 0.0D, icon);
        }
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
                                    RenderBlocks renderer) {
        if (!(block instanceof ShapeBlock shape)) return false;
        // The destroy-progress crack arrives through RenderBlocks.renderBlockUsingTexture, which sets its own
        // override texture; a single draw stamps that texture once.
        if (renderer.hasOverrideBlockTexture()) return renderer.renderStandardBlock(shape, x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        IIcon base = shape.baseIcon();
        boolean rendered = base != null && drawWorldLayer(renderer, shape, x, y, z, base, 0xFFFFFF);
        for (int layer = 0, layers = shape.materialLayerCount(meta); layer < layers; layer++) {
            boolean drawn = drawWorldLayer(renderer, shape, x, y, z, shape.materialLayer(meta, layer),
                layerColor(shape, meta, layer));
            if (base == null && layer == 0) rendered = drawn;
        }
        return rendered;
    }

    /// Draws one layer as a standard block of `icon` tinted `color`, mirroring [RenderBlocks#renderStandardBlock]'s
    /// dispatch. That method is not called directly: it takes its color from [ShapeBlock#colorMultiplier], which is
    /// white for every composite, in place of the per-layer color.
    private static boolean drawWorldLayer(RenderBlocks renderer, ShapeBlock shape, int x, int y, int z, IIcon icon,
                                          int color) {
        renderer.setOverrideBlockTexture(icon);
        try {
            float red = (color >> 16 & 255) / 255.0F;
            float green = (color >> 8 & 255) / 255.0F;
            float blue = (color & 255) / 255.0F;
            if (EntityRenderer.anaglyphEnable) {
                float anaglyphRed = (red * 30.0F + green * 59.0F + blue * 11.0F) / 100.0F;
                float anaglyphGreen = (red * 30.0F + green * 70.0F) / 100.0F;
                float anaglyphBlue = (red * 30.0F + blue * 70.0F) / 100.0F;
                red = anaglyphRed;
                green = anaglyphGreen;
                blue = anaglyphBlue;
            }
            if (Minecraft.isAmbientOcclusionEnabled() && shape.getLightValue() == 0) {
                return renderer.partialRenderBounds ?
                    renderer.renderStandardBlockWithAmbientOcclusionPartial(shape, x, y, z, red, green, blue) :
                    renderer.renderStandardBlockWithAmbientOcclusion(shape, x, y, z, red, green, blue);
            }
            return renderer.renderStandardBlockWithColorMultiplier(shape, x, y, z, red, green, blue);
        }
        finally {
            renderer.clearOverrideBlockTexture();
        }
    }
}
