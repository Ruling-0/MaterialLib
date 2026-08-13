package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.ResourceLocation;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;

import org.junit.jupiter.api.Test;

/// Pins the block-texture path construction [ShapeBlock]'s base-texture existence check and registration both
/// resolve through. 1.7.10 resource lookups are case-sensitive, so a mixed-case domain must survive intact.
class ResourceUtilBaseTexturePathTest {

    @Test
    void vanillaDomainedPathResolvesUnderTheBlockAtlasConvention() {
        ResourceLocation location = ResourceUtil.getCompleteBlockTextureResourceLocation("minecraft:stone");
        assertEquals("minecraft", location.getResourceDomain());
        assertEquals("textures/blocks/stone.png", location.getResourcePath());
    }

    @Test
    void mixedCaseDomainIsPreservedRatherThanLowercased() {
        ResourceLocation location = ResourceUtil.getCompleteBlockTextureResourceLocation("GalacticraftCore:moon");
        assertEquals("GalacticraftCore", location.getResourceDomain());
        assertEquals("textures/blocks/moon.png", location.getResourcePath());
    }

    @Test
    void domainlessPathDefaultsToMinecraft() {
        ResourceLocation location = ResourceUtil.getCompleteBlockTextureResourceLocation("stone");
        assertEquals("minecraft", location.getResourceDomain());
        assertEquals("textures/blocks/stone.png", location.getResourcePath());
    }
}
