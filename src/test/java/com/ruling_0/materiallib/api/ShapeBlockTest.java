package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/// Headless coverage for [ShapeBlock]'s base-texture bookkeeping. [ShapeBlock#getIcon] and [ShapeBlock#colorMultiplier]
/// additionally consult [net.minecraftforge.client.ForgeHooksClient#getWorldRenderPass], which only behaves as
/// documented around real world chunk tessellation, and [ShapeBlock#getRenderColor]'s non-white branch resolves a
/// material through the process-wide [MaterialRegistry#instance], which no test resolves; neither is exercised
/// here. Both need a live client (see the in-game example content) -- the user must verify in-client that a variant
/// ore block shows its base texture under the tinted material icon both placed in world and in inventory/held/
/// dropped item form.
class ShapeBlockTest {

    private final ShapeBlock withBaseTexture = new ShapeBlock(
        "testmod",
        "ore_stone",
        "%s Ore",
        new String[] { "ore" },
        "ore",
        "stone",
        "minecraft:stone",
        BlockBehavior.NONE,
        null);

    private final ShapeBlock withoutBaseTexture = new ShapeBlock("testmod", "ingot", "%s Ingot", "ingot");

    @Test
    void hasBaseTextureReflectsWhetherAVariantBaseWasDeclared() {
        assertTrue(withBaseTexture.hasBaseTexture());
        assertFalse(withoutBaseTexture.hasBaseTexture());
    }

    /// Pass 0 of the item-form composite ([ShapeBlockItemRenderer]) must draw the base texture untinted, the same
    /// as pass 0 of the world composite ([ShapeBlock#colorMultiplier]) does. This branch returns before consulting
    /// any material, so it holds regardless of the given metadata, including one bound to no live material.
    @Test
    void itemRenderPassZeroIsUntintedWhenABaseTextureExists() {
        withBaseTexture.setItemRenderPass(0);
        try {
            assertEquals(0xFFFFFF, withBaseTexture.getRenderColor(0));
            assertEquals(0xFFFFFF, withBaseTexture.getRenderColor(999));
        }
        finally {
            withBaseTexture.setItemRenderPass(-1);
        }
    }
}
