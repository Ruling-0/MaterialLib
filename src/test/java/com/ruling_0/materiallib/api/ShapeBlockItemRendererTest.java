package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import org.junit.jupiter.api.Test;

/// Pins [ShapeBlockItemRenderer]'s render-type gating, the mechanism that composites a variant ore block's item
/// form in a GUI slot and the hotbar, not just when held or dropped. [ShapeBlock] is registered per block through
/// [com.ruling_0.materiallib.ClientProxy] against a single [net.minecraftforge.client.IItemRenderer] instance --
/// there is no separate opt-in for [ItemRenderType#INVENTORY] -- so
/// [net.minecraftforge.client.MinecraftForgeClient#getItemRenderer] only returns this renderer for a slot or
/// hotbar icon if [ShapeBlockItemRenderer#handleRenderType] answers `true` there the same as it does for
/// [ItemRenderType#EQUIPPED] and [ItemRenderType#ENTITY]; see [ShapeBlockItemRenderer]'s javadoc for the full
/// dispatch chain this was verified against. A future change that narrowed `handleRenderType` back down to only
/// the held/dropped cases -- the mistake an earlier round made from reading the wrong decompiled source -- would
/// silently drop GUI/hotbar compositing again without failing to compile or breaking anything else observable
/// outside a running client, which is exactly what this test guards against headlessly.
class ShapeBlockItemRendererTest {

    private final ShapeBlockItemRenderer renderer = new ShapeBlockItemRenderer(
        new ShapeBlock(
            "testmod",
            "ore_stone",
            "%s Ore",
            new String[] { "ore" },
            "ore",
            "stone",
            "minecraft:stone",
            BlockBehavior.NONE,
            null));

    @Test
    void handlesEveryItemRenderTypeIncludingInventory() {
        for (ItemRenderType type : ItemRenderType.values()) {
            assertTrue(renderer.handleRenderType(null, type), "expected handleRenderType(" + type + ") to be true");
        }
    }

    @Test
    void usesTheBlockRenderHelperForInventoryAndEquippedSlots() {
        assertTrue(renderer.shouldUseRenderHelper(ItemRenderType.INVENTORY, null, ItemRendererHelper.INVENTORY_BLOCK));
        assertTrue(renderer.shouldUseRenderHelper(ItemRenderType.EQUIPPED, null, ItemRendererHelper.EQUIPPED_BLOCK));
    }
}
