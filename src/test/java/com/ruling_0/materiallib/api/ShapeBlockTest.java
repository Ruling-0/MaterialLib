package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/// Headless coverage for [ShapeBlock]'s base-texture and render-type bookkeeping. [ShapeBlock#getRenderColor]'s
/// tinted branch resolves a material through the process-wide [MaterialRegistry#instance], which is unresolved
/// (and throws on lookup) until MaterialLib's preInit runs, so no test here reaches it; only the untinted (layer 0)
/// branch, which returns before consulting any material, is exercised.
///
/// World compositing, item-form compositing, and [ShapeBlockRenderingHandler]'s registration all need a live
/// client -- the user must verify in-client that a variant ore block shows its base texture under the tinted
/// material icon, in world and in every item-form context: GUI slot, hotbar, held, and dropped.
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

    /// [ShapeBlockRenderingHandler] drives the composite entirely through [ShapeBlock#setLayerOverride]; layer 0
    /// (the base) must render untinted whether or not a base texture exists, the same as
    /// [ShapeBlock#colorMultiplier] does for world tessellation's solid layer.
    @Test
    void layerZeroIsUntintedWhenABaseTextureExists() {
        withBaseTexture.setLayerOverride(0);
        try {
            assertEquals(0xFFFFFF, withBaseTexture.getRenderColor(0));
            assertEquals(0xFFFFFF, withBaseTexture.getRenderColor(999));
        }
        finally {
            withBaseTexture.setLayerOverride(-1);
        }
    }

    /// Pins the render-type wiring [com.ruling_0.materiallib.ClientProxy] relies on: a block defaults to the
    /// vanilla full-cube render type (0) and reports whatever [ShapeBlockRenderingHandler]'s render ID
    /// [ShapeBlock#setRenderType] was last given, so a future change that stopped wiring composite blocks to their
    /// handler would leave them silently rendering as plain cubes instead of failing to compile.
    @Test
    void getRenderTypeReflectsWhateverWasSet() {
        assertEquals(0, withBaseTexture.getRenderType());
        withBaseTexture.setRenderType(64);
        try {
            assertEquals(64, withBaseTexture.getRenderType());
        }
        finally {
            withBaseTexture.setRenderType(0);
        }
    }
}
