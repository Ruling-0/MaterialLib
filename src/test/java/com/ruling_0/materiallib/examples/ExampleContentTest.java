package com.ruling_0.materiallib.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.ResourceLocation;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;

import org.junit.jupiter.api.Test;

/// Pins testOre's declared variant base textures against
/// [ResourceUtil#getCompleteBlockTextureResourceLocation]'s convention, independent of any live Minecraft resource
/// manager (see `ShapeBlockTest`'s javadoc for why the existence check itself still needs a live client). A
/// `blocks/` segment folded into the identifier -- e.g. `"minecraft:blocks/stone"` instead of `"minecraft:stone"`
/// -- resolves to a path no vanilla jar has (`textures/blocks/blocks/stone.png`) and silently falls back to the
/// transparent placeholder icon; this test would have failed against that regression.
class ExampleContentTest {

    @Test
    void testOreStoneBaseTextureResolvesToTheVanillaStoneTexture() {
        ResourceLocation location = ResourceUtil
            .getCompleteBlockTextureResourceLocation(ExampleContent.TEST_ORE_STONE_BASE_TEXTURE);
        assertEquals("minecraft", location.getResourceDomain());
        assertEquals("textures/blocks/stone.png", location.getResourcePath());
    }

    @Test
    void testOreCobblestoneBaseTextureResolvesToTheVanillaCobblestoneTexture() {
        ResourceLocation location = ResourceUtil
            .getCompleteBlockTextureResourceLocation(ExampleContent.TEST_ORE_COBBLESTONE_BASE_TEXTURE);
        assertEquals("minecraft", location.getResourceDomain());
        assertEquals("textures/blocks/cobblestone.png", location.getResourcePath());
    }
}
