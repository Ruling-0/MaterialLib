package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.util.ResourceLocation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// Pins how [PaletteAtlasSprite] flattens a layer stack: layers are strips of the base's exact size, baked and
/// drawn frame for frame, and any other size is left out. Also pins the level array it hands `loadSprite`: sized
/// by the client's mipmap setting, with a null tail that no headless run can observe.
class PaletteAtlasSpriteTest {

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFF808080;
    private static final int DARK = 0xFF404040;
    private static final int RED = 0xFFAA0000;
    private static final int GREEN = 0xFF00BB00;
    private static final int BLUE = 0xFF0000CC;

    private static final String BASE = "testmod:materials/anim/ingot";
    private static final String LAYER = BASE + ShapeIcons.LAYER_SUFFIX + 1;
    private static final String OVERLAY = BASE + ShapeIcons.OVERLAY_SUFFIX;
    private static final PaletteRef PALETTE = new PaletteRef("testmod", "anim", 0);

    /// Two 1x1 frames stacked into a 1x2 strip.
    private static final AnimationMetadataSection TWO_FRAMES = new AnimationMetadataSection(List.of(), 1, 1, 20);
    private static final AnimationMetadataSection LAYER_ONLY = new AnimationMetadataSection(List.of(), 1, 1, 5);

    private final PngResources resources = new PngResources();

    @BeforeEach
    void serveBaseAndPalette() throws IOException {
        PaletteSprites.clearCache();
        resources.put(itemPng(BASE), 1, 2, new int[] { WHITE, GRAY }, TWO_FRAMES);
        resources.put(PaletteSprites.palettePng(PALETTE), 1, 2, new int[] { RED, GREEN }, null);
    }

    /// The base's two shades take the palette's two entries. A numbered layer marking only the second frame is
    /// baked through its own single rank and drawn on that frame alone; an overlay marking only the first frame is
    /// drawn as authored on that frame alone. The layer's own animation metadata is ignored.
    @Test
    void layersBakeFrameForFrameWithTheBaseStrip() throws IOException {
        resources.put(itemPng(LAYER), 1, 2, new int[] { 0, DARK }, LAYER_ONLY);
        resources.put(itemPng(OVERLAY), 1, 2, new int[] { BLUE, 0 }, null);
        PaletteAtlasSprite sprite = new PaletteAtlasSprite("materiallib:mlbaked/anim", true, BASE, List.of(LAYER),
            OVERLAY, PALETTE);

        PaletteAtlasSprite.Art art = sprite.bake(resources);

        assertSame(TWO_FRAMES, art.animation());
        assertArrayEquals(new int[] { BLUE, RED }, art.image().getRGB(0, 0, 1, 2, null, 0, 1));
    }

    /// A layer authored at frame size against a two-frame base is left out.
    @Test
    void aLayerThatIsNotTheBaseStripIsLeftOut() throws IOException {
        resources.put(itemPng(LAYER), 1, 1, new int[] { DARK }, null);
        PaletteAtlasSprite sprite = new PaletteAtlasSprite("materiallib:mlbaked/anim", true, BASE, List.of(LAYER), null,
            PALETTE);

        PaletteAtlasSprite.Art art = sprite.bake(resources);

        assertArrayEquals(new int[] { RED, GREEN }, art.image().getRGB(0, 0, 1, 2, null, 0, 1));
    }

    /// One slot per mipmap level sits above the art -- four of them at the default client setting -- and every one
    /// of them is left for the atlas to fill.
    @Test
    void theFrameLadderHoldsTheArtAndOneNullSlotPerMipmapLevel() {
        for (int mipmapLevels : new int[] { 0, 1, 4 }) {
            BufferedImage art = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            BufferedImage[] levels = PaletteAtlasSprite.frameLadder(art, mipmapLevels);

            assertEquals(1 + mipmapLevels, levels.length, "ladder length at " + mipmapLevels + " mipmap levels");
            assertSame(art, levels[0]);
            for (int level = 1; level < levels.length; level++) {
                assertNull(levels[level], "level " + level + " must be left for the atlas to generate");
            }
        }
    }

    /// The complete item texture location GTNHLib derives from a `domain:path` texture key.
    private static ResourceLocation itemPng(String key) {
        int colon = key.indexOf(':');
        return new ResourceLocation(key.substring(0, colon), "textures/items/" + key.substring(colon + 1) + ".png");
    }

    /// Serves pngs and their animation metadata from memory in place of the client's resource packs.
    private static final class PngResources implements IResourceManager {

        private final Map<ResourceLocation, byte[]> pngs = new HashMap<>();
        private final Map<ResourceLocation, AnimationMetadataSection> animations = new HashMap<>();

        void put(ResourceLocation location, int width, int height, int[] argb,
                 AnimationMetadataSection animation) throws IOException {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, width, height, argb, 0, width);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bytes);
            pngs.put(location, bytes.toByteArray());
            if (animation != null) animations.put(location, animation);
        }

        @Override
        public Set<String> getResourceDomains() { return Set.of("testmod"); }

        @Override
        public IResource getResource(ResourceLocation location) throws IOException {
            byte[] png = pngs.get(location);
            if (png == null) throw new FileNotFoundException(location.toString());
            AnimationMetadataSection animation = animations.get(location);
            return new IResource() {

                @Override
                public InputStream getInputStream() { return new ByteArrayInputStream(png); }

                @Override
                public boolean hasMetadata() {
                    return animation != null;
                }

                @Override
                public IMetadataSection getMetadata(String name) {
                    return animation;
                }
            };
        }

        @Override
        public List<IResource> getAllResources(ResourceLocation location) throws IOException {
            return List.of(getResource(location));
        }
    }
}
