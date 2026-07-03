package com.ruling_0.materiallib.api;

import net.minecraft.item.ItemStack;

import net.minecraftforge.client.IItemRenderer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The renderer registered for every [ShapeItem], dispatching each stack to its material's custom
/// [IItemRenderer] when one is set through [MaterialLibClient].
///
/// [#handleRenderType] returns false for materials with no custom renderer, which leaves the stack to default
/// item rendering (the material's texture and [StandardProperties#TINT]).
@SideOnly(Side.CLIENT)
public final class ShapeItemRenderer implements IItemRenderer {

    @Override
    public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
        IItemRenderer renderer = rendererFor(stack);
        return renderer != null && renderer.handleRenderType(stack, type);
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
        IItemRenderer renderer = rendererFor(stack);
        return renderer != null && renderer.shouldUseRenderHelper(type, stack, helper);
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        IItemRenderer renderer = rendererFor(stack);
        if (renderer != null) {
            renderer.renderItem(type, stack, data);
        }
    }

    private static IItemRenderer rendererFor(ItemStack stack) {
        Material material = MaterialRegistry.instance().getMaterialByIndex(stack.getItemDamage());
        return material != null ? MaterialLibClient.getItemRenderer(material) : null;
    }
}
