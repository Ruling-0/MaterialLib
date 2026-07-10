package com.ruling_0.materiallib.api;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.ItemStack;

import net.minecraftforge.client.IItemRenderer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/// Renders a variant block's item form -- a normal full cube -- with the same base-and-overlay composite as
/// [ShapeBlock]'s world rendering, for every render type Forge dispatches to a custom [IItemRenderer]: a dropped
/// item entity, and the item equipped in a player's hand (third and first person). Vanilla's own inventory and
/// hotbar slot icon never reaches this class at all -- see [ShapeBlock#renderPass]'s javadoc for that path and
/// how [ShapeBlock] degrades gracefully there instead.
///
/// Vanilla's block-as-item renderer ([RenderBlocks#renderBlockAsItem]) draws exactly one icon per face, driven by
/// a single call to [net.minecraft.block.Block#getRenderColor], so it cannot reproduce [ShapeBlock#getIcon]'s
/// pass-conditioned base/overlay split by itself; unlike world chunk tessellation, it never sets
/// [net.minecraftforge.client.ForgeHooksClient#getWorldRenderPass], so [ShapeBlock#getIcon] always resolves the
/// tinted material icon and the untinted base never shows. This renderer calls `renderBlockAsItem` twice instead,
/// toggling [ShapeBlock#setItemRenderPass] between calls so [ShapeBlock#getIcon] and [ShapeBlock#getRenderColor]
/// pick the same base/overlay layer each pass that world tessellation would.
///
/// The two passes draw the same full cube geometry at the same depth, which -- unlike world chunk tessellation's
/// single tessellated pass -- z-fights under the default depth test: two independent draw calls' fragments land
/// at the same depth up to floating-point rounding, so the depth test can reject either one unpredictably. The
/// overlay pass is drawn with [org.lwjgl.opengl.GL11#GL_LEQUAL] depth testing (so an equal-depth fragment always
/// wins over the base pass already in the depth buffer) and scaled up by [#OVERLAY_SCALE] around the cube's
/// center (so it wins even under stricter depth tests some other mod's GL state might have left active), both
/// restored once the composite is done.
///
/// [com.ruling_0.materiallib.ClientProxy] registers one instance per variant block whose shape has a base texture
/// ([ShapeBlock#hasBaseTexture]); a plain block shape has nothing to composite and keeps the vanilla single-pass
/// item renderer.
@SideOnly(Side.CLIENT)
public final class ShapeBlockItemRenderer implements IItemRenderer {

    private static final float OVERLAY_SCALE = 1.002F;

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
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glPushMatrix();
            GL11.glTranslatef(0.5F, 0.5F, 0.5F);
            GL11.glScalef(OVERLAY_SCALE, OVERLAY_SCALE, OVERLAY_SCALE);
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
            renderBlocks.renderBlockAsItem(shape, meta, 1.0F);
            GL11.glPopMatrix();
        }
        finally {
            shape.setItemRenderPass(-1);
            renderBlocks.useInventoryTint = wasInventoryTint;
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.5F);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        }
    }
}
