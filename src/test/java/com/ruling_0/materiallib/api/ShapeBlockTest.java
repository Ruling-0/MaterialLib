package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/// Headless coverage for [ShapeBlock]'s base-texture bookkeeping. [ShapeBlock#renderPass] checks
/// [ShapeBlock#itemRenderPass] before falling back to
/// [net.minecraftforge.client.ForgeHooksClient#getWorldRenderPass], which needs a live client and is unreachable
/// from this suite (calling it throws `NoClassDefFoundError: org/lwjgl/LWJGLException`, since LWJGL is not on the
/// test runtime classpath); every case here sets [ShapeBlock#itemRenderPass] explicitly so that fallback is never
/// reached. [ShapeBlock#getRenderColor]'s tinted branch resolves a material through the process-wide
/// [MaterialRegistry#instance], which is unresolved (and throws on lookup) until MaterialLib's preInit runs, so
/// no test here can reach it at all; [ShapeBlock#getIcon] never consults a material and stands in for it where
/// only the pass selection, not the tint value, is under test. The -1 (unset) default and world-tessellation
/// behavior both need a live client --
/// the user must verify in-client that a variant ore block shows its base texture under the tinted material icon
/// placed in world, in inventory/hotbar (base only -- see [ShapeBlock#renderPass]'s javadoc), and held/dropped
/// (full composite, via [ShapeBlockItemRenderer]).
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

    /// Pass 1 is the overlay pass; [ShapeBlock#renderPass] must resolve it from [ShapeBlock#itemRenderPass] alone,
    /// the same as pass 0, never falling through to
    /// [net.minecraftforge.client.ForgeHooksClient#getWorldRenderPass] (which would throw here; see this class's
    /// javadoc) -- a future change that reordered those two checks would still pass
    /// [#itemRenderPassZeroIsUntintedWhenABaseTextureExists] (pass 0 short-circuits either order) but fail here.
    /// [ShapeBlock#getIcon] never consults a material, unlike [ShapeBlock#getRenderColor]'s tinted branch, so it
    /// is the one exercised here (see this class's javadoc on why the tinted branch cannot be).
    @Test
    void itemRenderPassOneNeverTouchesTheWorldRenderPass() {
        withBaseTexture.setItemRenderPass(1);
        try {
            assertDoesNotThrow(() -> withBaseTexture.getIcon(0, 0));
        }
        finally {
            withBaseTexture.setItemRenderPass(-1);
        }
    }
}
