package com.ruling_0.materiallib.api;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import com.ruling_0.materiallib.Config;

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
///
/// The material layers of an emissive material (see [StandardProperties#EMISSIVE_LAYERS]) draw at full brightness
/// without ambient occlusion, face by face, so a chunk mesher keeps the vertex lightmap instead of recomputing smooth
/// lighting. In item form they go in a second [Tessellator] batch, since a batch applies the brightness it was given
/// last to every vertex. With [Config#renderEmissiveLayers] off they are not drawn at all.
@SideOnly(Side.CLIENT)
@ThreadSafeISBRH(perThread = false)
public final class ShapeBlockRenderingHandler implements ISimpleBlockRenderingHandler {

    static final int RENDER_ID = RenderingRegistry.getNextAvailableRenderId();

    /// Sky and block light both at 15, as packed lightmap coordinates.
    private static final int FULL_BRIGHT = 0xF000F0;

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
        boolean emissive = shape.emissive(metadata);
        if (!emissive) drawInventoryMaterialLayers(renderer, tessellator, shape, metadata);
        tessellator.draw();
        if (emissive && Config.renderEmissiveLayers) {
            tessellator.startDrawingQuads();
            tessellator.setBrightness(FULL_BRIGHT);
            drawInventoryMaterialLayers(renderer, tessellator, shape, metadata);
            tessellator.draw();
        }

        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
    }

    private static void drawInventoryMaterialLayers(RenderBlocks renderer, Tessellator tessellator, ShapeBlock shape,
                                                    int metadata) {
        for (int layer = 0, layers = shape.materialLayerCount(metadata); layer < layers; layer++) {
            drawInventoryLayer(renderer, tessellator, shape, shape.materialLayer(metadata, layer),
                layerColor(shape, metadata, layer));
        }
    }

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
        boolean emissive = shape.emissive(meta);
        if (emissive && !Config.renderEmissiveLayers) return rendered;
        for (int layer = 0, layers = shape.materialLayerCount(meta); layer < layers; layer++) {
            IIcon icon = shape.materialLayer(meta, layer);
            int color = layerColor(shape, meta, layer);
            boolean drawn = emissive ? drawEmissiveLayer(renderer, world, shape, x, y, z, icon, color) :
                drawWorldLayer(renderer, shape, x, y, z, icon, color);
            if (base == null && layer == 0) rendered = drawn;
        }
        return rendered;
    }

    /// Draws one layer's visible faces at full brightness, without ambient occlusion or per-face shading.
    private static boolean drawEmissiveLayer(RenderBlocks renderer, IBlockAccess world, ShapeBlock shape, int x,
                                             int y, int z, IIcon icon, int color) {
        boolean ambientOcclusion = renderer.enableAO;
        renderer.enableAO = false;
        try {
            float[] rgb = rgb(color);
            Tessellator tessellator = Tessellator.instance;
            tessellator.setBrightness(FULL_BRIGHT);
            tessellator.setColorOpaque_F(rgb[0], rgb[1], rgb[2]);
            boolean drawn = false;
            if (renderer.renderAllFaces || shape.shouldSideBeRendered(world, x, y - 1, z, 0)) {
                renderer.renderFaceYNeg(shape, x, y, z, icon);
                drawn = true;
            }
            if (renderer.renderAllFaces || shape.shouldSideBeRendered(world, x, y + 1, z, 1)) {
                renderer.renderFaceYPos(shape, x, y, z, icon);
                drawn = true;
            }
            if (renderer.renderAllFaces || shape.shouldSideBeRendered(world, x, y, z - 1, 2)) {
                renderer.renderFaceZNeg(shape, x, y, z, icon);
                drawn = true;
            }
            if (renderer.renderAllFaces || shape.shouldSideBeRendered(world, x, y, z + 1, 3)) {
                renderer.renderFaceZPos(shape, x, y, z, icon);
                drawn = true;
            }
            if (renderer.renderAllFaces || shape.shouldSideBeRendered(world, x - 1, y, z, 4)) {
                renderer.renderFaceXNeg(shape, x, y, z, icon);
                drawn = true;
            }
            if (renderer.renderAllFaces || shape.shouldSideBeRendered(world, x + 1, y, z, 5)) {
                renderer.renderFaceXPos(shape, x, y, z, icon);
                drawn = true;
            }
            return drawn;
        }
        finally {
            renderer.enableAO = ambientOcclusion;
        }
    }

    /// `color` as red, green and blue fractions, converted for the anaglyph 3D view when it is on.
    private static float[] rgb(int color) {
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        if (!EntityRenderer.anaglyphEnable) return new float[] { red, green, blue };
        return new float[] { (red * 30.0F + green * 59.0F + blue * 11.0F) / 100.0F,
            (red * 30.0F + green * 70.0F) / 100.0F, (red * 30.0F + blue * 70.0F) / 100.0F };
    }

    /// Draws one layer as a standard block of `icon` tinted `color`, mirroring [RenderBlocks#renderStandardBlock]'s
    /// dispatch. That method is not called directly: it takes its color from [ShapeBlock#colorMultiplier], which is
    /// white for every composite, in place of the per-layer color.
    private static boolean drawWorldLayer(RenderBlocks renderer, ShapeBlock shape, int x, int y, int z, IIcon icon,
                                          int color) {
        renderer.setOverrideBlockTexture(icon);
        try {
            float[] rgb = rgb(color);
            float red = rgb[0];
            float green = rgb[1];
            float blue = rgb[2];
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
