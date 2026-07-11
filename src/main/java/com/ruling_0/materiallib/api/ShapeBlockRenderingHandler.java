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
/// single draw, in world and in every item form (GUI slot, hotbar, held, and dropped).
/// [com.ruling_0.materiallib.ClientProxy]
/// registers one instance of this handler for every such block, through [#RENDER_ID] and [ShapeBlock#setRenderType];
/// a block with no base texture keeps the vanilla full-cube render type (0) and never reaches this class.
///
/// Both [#renderWorldBlock] and [#renderInventoryBlock] draw the base layer and the tinted overlay back-to-back
/// into the same [Tessellator] batch, toggling [ShapeBlock#setLayerOverride] between the two so [ShapeBlock#getIcon],
/// [ShapeBlock#getRenderColor], and [ShapeBlock#colorMultiplier] resolve the base layer for the first draw and the
/// overlay for the second -- the composite that used to take two separate render passes (world chunk tessellation's
/// solid and alpha passes, or two `renderBlockAsItem` calls) now happens in one. Because the two layers share a
/// tessellator batch, their coplanar quads are submitted back-to-back with identical vertex data, so the depth test
/// resolves the tie deterministically in submission order instead of z-fighting the way two independent draw calls
/// would; this is the same technique GT5-Unofficial's `gregtech.common.render.GTRendererBlock` uses for its own
/// texture-array composites.
///
/// Composite blocks keep the vanilla render-pass defaults (`getRenderBlockPass` 0, `canRenderInPass` only pass 0),
/// so the whole composite -- opaque base included -- lives in the solid chunk pass, where the alpha test cuts out
/// the overlay's transparent pixels the same way it did for legacy GT ores (whose composite quads all draw in pass
/// 0 too: every GT `ITexture` gates itself by its `IIconContainer#canRenderInPass`, default pass 0). The overlay
/// icons are cutout textures, not translucent ones, so the blended pass buys nothing; an opaque cube there would
/// instead sort against genuine translucents (water, glass) and pay for translucency sorting in Angelica's mesher.
/// Item contexts likewise cut out via the alpha test alone: `RenderItem` and `ItemRenderer` only enable blending
/// for a block whose `getRenderBlockPass` is nonzero, and the default 0 gives the composite the same alpha-tested
/// draw legacy ore items always had.
///
/// This handler holds no mutable state, so one instance is safe to reuse from any thread a world mesher (e.g.
/// Angelica's Celeritas) calls it from.
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
