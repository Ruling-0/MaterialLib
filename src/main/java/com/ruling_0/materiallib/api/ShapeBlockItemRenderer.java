package com.ruling_0.materiallib.api;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.ItemStack;

import net.minecraftforge.client.IItemRenderer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/// Renders a variant block's item form -- a normal full cube -- with the same base-and-overlay composite as
/// [ShapeBlock]'s world rendering.
///
/// Vanilla's block-as-item renderer ([RenderBlocks#renderBlockAsItem]) draws exactly one icon per face, driven by
/// a single call to [net.minecraft.block.Block#getRenderColor], so it cannot reproduce [ShapeBlock#getIcon]'s
/// pass-conditioned base/overlay split by itself; unlike world chunk tessellation, it never sets
/// [net.minecraftforge.client.ForgeHooksClient#getWorldRenderPass], so [ShapeBlock#getIcon] always resolves the
/// tinted material icon and the untinted base never shows. This renderer calls `renderBlockAsItem` twice instead,
/// toggling [ShapeBlock#setItemRenderPass] between calls so [ShapeBlock#getIcon] and [ShapeBlock#getRenderColor]
/// pick the same base/overlay layer each pass that world tessellation would.
///
/// [com.ruling_0.materiallib.ClientProxy] registers one instance per variant block whose shape has a base texture
/// ([ShapeBlock#hasBaseTexture]); a plain block shape has nothing to composite and keeps the vanilla single-pass
/// item renderer.
@SideOnly(Side.CLIENT)
public final class ShapeBlockItemRenderer implements IItemRenderer {

    private final ShapeBlock shape;

    public ShapeBlockItemRenderer(ShapeBlock shape) {
        this.shape = shape;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        RenderBlocks renderBlocks = data.length > 0 && data[0] instanceof RenderBlocks rb ? rb : new RenderBlocks();
        int meta = item.getItemDamage();
        boolean wasInventoryTint = renderBlocks.useInventoryTint;
        renderBlocks.useInventoryTint = true;

        try {
            shape.setItemRenderPass(0);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.5F);
            renderBlocks.renderBlockAsItem(shape, meta, 1.0F);

            shape.setItemRenderPass(1);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            renderBlocks.renderBlockAsItem(shape, meta, 1.0F);
        }
        finally {
            shape.setItemRenderPass(-1);
            renderBlocks.useInventoryTint = wasInventoryTint;
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.5F);
        }
    }
}
