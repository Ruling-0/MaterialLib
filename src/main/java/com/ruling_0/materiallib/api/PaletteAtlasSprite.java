package com.ruling_0.materiallib.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.ruling_0.materiallib.MaterialLib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// One atlas sprite baked from a shape's art through a [PaletteRef]: the base texture and its numbered layers
/// recolored by the palette's column, the overlay kept as authored, all flattened in draw order into the single
/// sprite the atlas stitches.
///
/// Baking happens inside Forge's custom-loader hook, which replaces the atlas's own read of a file named after
/// the sprite. The name therefore only has to be unique on the atlas, and the art comes from the paths passed to
/// the constructor. Animation follows the base texture: its `animation` metadata and strip height decide the
/// frames, and a layer authored for one frame is repeated over all of them.
///
/// A base texture that cannot be read leaves the sprite unstitched, so the atlas fills it with the missing
/// texture. Every other failure -- an unresolvable palette column, a layer of the wrong size -- still flattens
/// the art it has, uncolored, and logs.
@SideOnly(Side.CLIENT)
final class PaletteAtlasSprite extends TextureAtlasSprite {

    private record BaseArt(BufferedImage image, AnimationMetadataSection animation) {}

    private final boolean isItem;
    private final String basePath;
    private final List<String> layerPaths;
    private final String overlayPath;
    private final PaletteRef palette;
    private boolean warnedPalette;
    private boolean warnedLayer;

    /// A sprite named `name` baking the art at `basePath`, its numbered layers at `layerPaths` and, when
    /// `overlayPath` is non-null, its overlay. Every path names an item texture when `isItem`, a block texture
    /// otherwise.
    PaletteAtlasSprite(String name, boolean isItem, String basePath, List<String> layerPaths, String overlayPath,
                       PaletteRef palette) {
        super(name);
        this.isItem = isItem;
        this.basePath = basePath;
        this.layerPaths = layerPaths;
        this.overlayPath = overlayPath;
        this.palette = palette;
    }

    @Override
    public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
        return true;
    }

    @Override
    public boolean load(IResourceManager manager, ResourceLocation location) {
        try {
            BaseArt base = readBase(manager);
            if (base == null) return true;
            int width = base.image().getWidth();
            int height = base.image().getHeight();
            int frames = base.animation() != null ? height / width : 1;
            int[] pixels = base.image().getRGB(0, 0, width, height, null, 0, width);
            int[] entries = PaletteSprites.paletteEntries(manager, palette);
            if (entries != null) pixels = PaletteBaker.bake(PaletteBaker.index(pixels, width, height), entries);
            else warnPalette();
            for (String layerPath : layerPaths) {
                int[] layer = layerPixels(manager, layerPath, width);
                if (layer == null) continue;
                if (entries != null) layer = PaletteBaker.bake(PaletteBaker.index(layer, width, width), entries);
                compositeFrames(pixels, layer, width, frames);
            }
            if (overlayPath != null) {
                int[] overlay = layerPixels(manager, overlayPath, width);
                if (overlay != null) compositeFrames(pixels, overlay, width, frames);
            }
            BufferedImage composited = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            composited.setRGB(0, 0, width, height, pixels, 0, width);
            loadSprite(frameLadder(composited, Minecraft.getMinecraft().gameSettings.mipmapLevels),
                base.animation(),
                !isItem && Minecraft.getMinecraft().gameSettings.anisotropicFiltering > 1);
            return false;
        }
        catch (RuntimeException e) {
            MaterialLib.LOG.error("Could not bake {}; the sprite renders the missing texture", getIconName(), e);
            return true;
        }
    }

    /// The level array `loadSprite` takes for `composited`: the art in slot 0 and one null slot per mipmap level
    /// above it, exactly as vanilla's own read leaves them. `TextureUtil.generateMipmapData` indexes a slot per
    /// level and fills them itself, so an array sized to the art alone puts that indexing out of bounds.
    static BufferedImage[] frameLadder(BufferedImage composited, int mipmapLevels) {
        BufferedImage[] levels = new BufferedImage[1 + mipmapLevels];
        levels[0] = composited;
        return levels;
    }

    private BaseArt readBase(IResourceManager manager) {
        try {
            IResource resource = manager.getResource(texturePath(basePath));
            return new BaseArt(readImage(resource), (AnimationMetadataSection) resource.getMetadata("animation"));
        }
        catch (IOException e) {
            MaterialLib.LOG.error(
                "Could not read base texture {} of {}; the sprite renders the missing texture",
                basePath,
                getIconName(),
                e);
            return null;
        }
    }

    /// The top `frameWidth` x `frameWidth` frame of the art at `path`, or null when that art is unreadable or
    /// does not cover such a frame.
    private int[] layerPixels(IResourceManager manager, String path, int frameWidth) {
        BufferedImage image;
        try {
            image = readImage(manager.getResource(texturePath(path)));
        }
        catch (IOException e) {
            warnLayer("Could not read {} for {}; that layer is dropped", path, getIconName(), e);
            return null;
        }
        if (image.getWidth() != frameWidth || image.getHeight() < frameWidth) {
            warnLayer(
                "{} is {}x{} but {} draws {}px frames; that layer is dropped",
                path,
                image.getWidth(),
                image.getHeight(),
                getIconName(),
                frameWidth);
            return null;
        }
        if (image.getHeight() > frameWidth) {
            warnLayer("{} is animated, which a baked layer cannot be; only its top frame reaches {}", path,
                getIconName());
        }
        return image.getRGB(0, 0, frameWidth, frameWidth, null, 0, frameWidth);
    }

    /// Draws `layer` over every frame region of `composited`, which holds `frames` frames of `frameWidth` square.
    private static void compositeFrames(int[] composited, int[] layer, int frameWidth, int frames) {
        int frameSize = frameWidth * frameWidth;
        int[] frame = new int[frameSize];
        for (int offset = 0; offset < frames * frameSize; offset += frameSize) {
            System.arraycopy(composited, offset, frame, 0, frameSize);
            PaletteBaker.compositeOver(frame, layer);
            System.arraycopy(frame, 0, composited, offset, frameSize);
        }
    }

    private static BufferedImage readImage(IResource resource) throws IOException {
        try (InputStream stream = resource.getInputStream()) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) throw new IOException("no image reader accepted the file");
            return image;
        }
    }

    private ResourceLocation texturePath(String path) {
        if (isItem) return ResourceUtil.getCompleteItemTextureResourceLocation(path);
        else return ResourceUtil.getCompleteBlockTextureResourceLocation(path);
    }

    private void warnPalette() {
        if (warnedPalette) return;
        warnedPalette = true;
        MaterialLib.LOG.warn(
            "Could not resolve column {} of {}; {} keeps the colors its art was drawn in",
            palette.column(),
            PaletteSprites.palettePng(palette),
            getIconName());
    }

    private void warnLayer(String message, Object... args) {
        if (warnedLayer) return;
        warnedLayer = true;
        MaterialLib.LOG.warn(message, args);
    }
}
