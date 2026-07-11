package com.ruling_0.materiallib.api;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.ItemStack;

import net.minecraftforge.client.IItemRenderer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/// Renders a variant block's item form -- a normal full cube -- with the same base-and-overlay composite as
/// [ShapeBlock]'s world rendering, for every [ItemRenderType] Forge dispatches to a custom [IItemRenderer]:
/// [ItemRenderType#ENTITY] (a dropped item entity), [ItemRenderType#EQUIPPED] and
/// [ItemRenderType#EQUIPPED_FIRST_PERSON] (held in a player's hand, third and first person), and
/// [ItemRenderType#INVENTORY] -- a GUI inventory slot or hotbar icon. [#handleRenderType] answers every type the
/// same way and [#renderItem] does not branch on `type`, so registering one instance per block (see below) covers
/// all four uniformly.
///
/// Vanilla's inventory/hotbar icon path (`RenderItem#renderItemIntoGUI`) draws exactly one icon per face with no
/// second pass, but that method is not what a GUI slot or hotbar actually calls: they call
/// `RenderItem#renderItemAndEffectIntoGUI`, which tries
/// `net.minecraftforge.client.ForgeHooksClient#renderInventoryItem` first -- and that consults
/// `net.minecraftforge.client.MinecraftForgeClient#getItemRenderer(item, ItemRenderType.INVENTORY)`, i.e. this
/// class, before ever reaching the single-icon fallback. The same is true for `ItemRenderType.ENTITY`
/// (`ForgeHooksClient#renderEntityItem`) and `ItemRenderType.EQUIPPED`
/// (`net.minecraft.client.renderer.ItemRenderer#renderItem`, which checks `MinecraftForgeClient#getItemRenderer`
/// before falling back to a block's render type at all). All of this is verified against the RetroFuturaGradle
/// dev environment's Forge-patched sources (`build/rfg/mcp_patched_minecraft-sources.jar`) -- not the unpatched
/// vanilla decompile, which lacks every one of those hooks and previously led to the wrong conclusion that
/// inventory and hotbar slots could not reach a custom [IItemRenderer]. Angelica, which this pack runs, does not
/// mixin into `RenderItem`, `ItemRenderer`, or `GuiContainer`, so none of this dispatch chain is altered by it.
///
/// Vanilla's block-as-item renderer ([RenderBlocks#renderBlockAsItem]) draws exactly one icon per face, driven by
/// a single call to [net.minecraft.block.Block#getRenderColor], so it cannot reproduce [ShapeBlock#getIcon]'s
/// pass-conditioned base/overlay split by itself. This renderer calls `renderBlockAsItem` twice instead, toggling
/// [ShapeBlock#setItemRenderPass] between calls so [ShapeBlock#getIcon] and [ShapeBlock#getRenderColor] pick the
/// base layer for the first call and the tinted overlay for the second, the same split world chunk tessellation
/// gets from its own two render passes (see [ShapeBlock#renderPass]).
///
/// The two passes draw the same full cube geometry at the same depth, which -- unlike world chunk tessellation's
/// single tessellated pass -- z-fights under the default depth test: two independent draw calls' fragments land
/// at the same depth up to floating-point rounding, so the depth test can reject either one unpredictably. The
/// overlay pass is drawn with [org.lwjgl.opengl.GL11#GL_LEQUAL] depth testing and scaled up by [#OVERLAY_SCALE]
/// so every overlay face sits strictly outside the base cube's, both restored once the composite is done. The
/// scale is applied around the origin because that is where `renderBlockAsItem` centers the cube it draws: for a
/// standard block it rotates, then translates by (-0.5, -0.5, -0.5), then emits the unit-cube faces, so the cube
/// spans -0.5..+0.5 in this renderer's frame -- it does NOT span 0..1. Scaling around (0.5, 0.5, 0.5) would map
/// the face planes at +0.5 exactly onto themselves, and those still-coincident faces speckle per pixel wherever
/// the two passes' slightly different matrices round their equal depths in opposite directions -- most visibly
/// under the GUI slot/hotbar transform (`ForgeHooksClient#renderInventoryItem`), which magnifies the cube 10x
/// through a Z-mirrored ortho projection.
///
/// [com.ruling_0.materiallib.ClientProxy] registers one instance per variant block whose shape has a base texture
/// ([ShapeBlock#hasBaseTexture]); a plain block shape has nothing to composite and keeps the vanilla single-pass
/// item renderer.
///
/// Angelica (verified at its pack-shipped tag `2.1.49`) mixes into [RenderBlocks#renderBlockAsItem] itself
/// (`angelica.itemrenderer.MixinRenderBlocks`, on by default via `optimizeInWorldItemRendering`) and caches its
/// draw calls into a GL display list keyed only by `(Block, meta)`, replayed on any later call with
/// `brightness == 1.0F`, `useInventoryTint == true`, no AO, and no override texture -- exactly the state both
/// passes below use. Since the two passes share the same `(block, meta)` key and that key knows nothing about
/// [ShapeBlock#setItemRenderPass], the second call hits the first pass's cached list and replays the base layer
/// again instead of re-deriving the tinted overlay, so GUI/hotbar icons render as the untinted base only. Passing
/// [#BRIGHTNESS] instead of `1.0F` is imperceptible (an ULP below 1.0, i.e. a no-op in an environment without this
/// mixin) but permanently fails Angelica's cache-eligibility check, forcing both passes through its real,
/// per-call `RenderBlocks` draw every time.
@SideOnly(Side.CLIENT)
public final class ShapeBlockItemRenderer implements IItemRenderer {

    private static final float OVERLAY_SCALE = 1.002F;
    private static final float BRIGHTNESS = Math.nextDown(1.0F);

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
            renderBlocks.renderBlockAsItem(shape, meta, BRIGHTNESS);

            shape.setItemRenderPass(1);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glPushMatrix();
            GL11.glScalef(OVERLAY_SCALE, OVERLAY_SCALE, OVERLAY_SCALE);
            renderBlocks.renderBlockAsItem(shape, meta, BRIGHTNESS);
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
