package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.ResourceLocation;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;

import org.junit.jupiter.api.Test;

/// Pins [ResourceUtil]'s block-texture path construction, the same construction
/// [ShapeBlock#registerBaseIcon] uses both to existence-check a variant's base texture and, on success, to
/// register it -- independent of any live Minecraft resource manager (see [ShapeBlockTest]'s javadoc for why the
/// existence check itself still needs a live client). A wrong construction here would make a real texture
/// (`minecraft:stone`) register as if it were missing, or send a mixed-case cross-mod domain
/// (`GalacticraftCore:moon`) looking under the wrong domain; 1.7.10 resource lookups are case-sensitive against
/// the packed jar entries a resource pack actually declares.
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
