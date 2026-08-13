package com.ruling_0.materiallib.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.ResourceLocation;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;

import org.junit.jupiter.api.Test;

/// Pins testOre's declared variant base textures to [ResourceUtil#getCompleteBlockTextureResourceLocation]'s
/// convention: no `blocks/` segment in the identifier, naming files the 1.7.10 vanilla jar ships.
class ExampleContentTest {

    @Test
    void variantBaseTexturesResolveToTheVanillaBlockTextures() {
        ResourceLocation stone = ResourceUtil
            .getCompleteBlockTextureResourceLocation(ExampleContent.TEST_ORE_STONE_BASE_TEXTURE);
        assertEquals("minecraft", stone.getResourceDomain());
        assertEquals("textures/blocks/stone.png", stone.getResourcePath());

        ResourceLocation cobblestone = ResourceUtil
            .getCompleteBlockTextureResourceLocation(ExampleContent.TEST_ORE_COBBLESTONE_BASE_TEXTURE);
        assertEquals("minecraft", cobblestone.getResourceDomain());
        assertEquals("textures/blocks/cobblestone.png", cobblestone.getResourcePath());
    }
}
