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
/// the constructor. Animation follows the base texture: every layer is a strip of the base's exact size, drawn
/// frame for frame, and only the base's animation metadata is read.
///
/// A base texture that cannot be read is resolved as the missing texture. Other failures resolve as flattened
/// but uncolored, with a log entry. A layer of any other size is left out.
@SideOnly(Side.CLIENT)
final class PaletteAtlasSprite extends TextureAtlasSprite {

    /// The base art, or a flattened strip, with the animation it plays.
    record Art(BufferedImage image, AnimationMetadataSection animation) {}

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
            Art art = bake(manager);
            if (art == null) return true;
            loadSprite(frameLadder(art.image(), Minecraft.getMinecraft().gameSettings.mipmapLevels), art.animation(),
                !isItem && Minecraft.getMinecraft().gameSettings.anisotropicFiltering > 1);
            return false;
        }
        catch (RuntimeException e) {
            MaterialLib.LOG.error("Could not bake {}", getIconName(), e);
            return true;
        }
    }

    /// The flattened strip with the base texture's animation, or null when the base texture cannot be read.
    Art bake(IResourceManager manager) {
        Art base = readBase(manager);
        if (base == null) return null;
        int width = base.image().getWidth();
        int height = base.image().getHeight();
        int[] pixels = base.image().getRGB(0, 0, width, height, null, 0, width);
        int[] entries = PaletteSprites.paletteEntries(manager, palette);
        if (entries != null) pixels = PaletteBaker.bake(PaletteBaker.index(pixels, width, height), entries);
        else warnPalette();
        for (String layerPath : layerPaths) {
            int[] layer = layerPixels(manager, layerPath, width, height);
            if (layer == null) continue;
            if (entries != null) layer = PaletteBaker.bake(PaletteBaker.index(layer, width, height), entries);
            PaletteBaker.compositeOver(pixels, layer);
        }
        if (overlayPath != null) {
            int[] overlay = layerPixels(manager, overlayPath, width, height);
            if (overlay != null) PaletteBaker.compositeOver(pixels, overlay);
        }
        BufferedImage composited = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        composited.setRGB(0, 0, width, height, pixels, 0, width);
        return new Art(composited, base.animation());
    }

    /// The level array `loadSprite` takes for `composited`: the art in slot 0 and one null slot per mipmap level
    /// above it, exactly as vanilla's own read leaves them; `TextureUtil.generateMipmapData` fills one slot per
    /// level itself.
    static BufferedImage[] frameLadder(BufferedImage composited, int mipmapLevels) {
        BufferedImage[] levels = new BufferedImage[1 + mipmapLevels];
        levels[0] = composited;
        return levels;
    }

    private Art readBase(IResourceManager manager) {
        try {
            IResource resource = manager.getResource(texturePath(basePath));
            return new Art(readImage(resource), (AnimationMetadataSection) resource.getMetadata("animation"));
        }
        catch (IOException e) {
            MaterialLib.LOG.error("Could not read base texture {} of {}", basePath, getIconName(), e);
            return null;
        }
    }

    /// The pixels of the art at `path`, or null when that art is unreadable or is not `width` x `height`.
    private int[] layerPixels(IResourceManager manager, String path, int width, int height) {
        BufferedImage image;
        try {
            image = readImage(manager.getResource(texturePath(path)));
        }
        catch (IOException e) {
            warnLayer("Could not read layer {} of {}", path, getIconName(), e);
            return null;
        }
        if (image.getWidth() != width || image.getHeight() != height) {
            warnLayer("{} is {}x{} but the base of {} is {}x{}", path, image.getWidth(), image.getHeight(),
                getIconName(), width, height);
            return null;
        }
        return image.getRGB(0, 0, width, height, null, 0, width);
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
        MaterialLib.LOG.warn("Could not resolve column {} of {} for {}", palette.column(),
            PaletteSprites.palettePng(palette), getIconName());
    }

    private void warnLayer(String message, Object... args) {
        if (warnedLayer) return;
        warnedLayer = true;
        MaterialLib.LOG.warn(message, args);
    }
}
