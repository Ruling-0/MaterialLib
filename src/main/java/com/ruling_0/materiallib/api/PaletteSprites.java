package com.ruling_0.materiallib.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.client.event.TextureStitchEvent;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The palette pngs behind [PaletteRef]: where they live, their pixels for the sprites baking against them, and
/// the [#bake] entry point that puts such a sprite on an atlas.
///
/// One png serves every material whose palette names it, each material reading its own column. A parsed png is
/// held until the atlas stitches again, which drops it -- a stitch begins before the sprite loads that read it.
/// Registered on the Forge event bus from the client proxy.
@SideOnly(Side.CLIENT)
public final class PaletteSprites {

    private record ParsedPalette(int[] argb, int width, int height) {}

    private static final Map<String, ParsedPalette> PARSED = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        clearCache();
    }

    /// The atlas sprite baking `stack`'s art through `palette`, or null when `register` is not a texture atlas or
    /// `palette` names no png, both of which leave the caller on the tint path.
    ///
    /// The sprite is named after the art and the palette column, so materials sharing both share one sprite, and
    /// it is inserted rather than registered: the atlas clears its sprites before every bind, and a name it
    /// registered itself would refuse the insert and read a file that does not exist.
    static IIcon bake(IIconRegister register, boolean isItem, ShapeIcons.StackPaths stack, PaletteRef palette) {
        if (!(register instanceof TextureMap map)) return null;
        if (!paletteExists(palette)) return null;
        String name = palette.bakedIconName(stack.base());
        TextureAtlasSprite bound = map.getTextureExtry(name);
        if (bound != null) return bound;
        PaletteSprite sprite = new PaletteSprite(name, isItem, stack.base(), stack.layers(), stack.overlay(),
            palette);
        map.setTextureEntry(name, sprite);
        return sprite;
    }

    static ResourceLocation palettePng(PaletteRef palette) {
        return new ResourceLocation(palette.modid(), "textures/palettes/" + palette.name() + ".png");
    }

    static boolean paletteExists(PaletteRef palette) {
        return ResourceUtil.resourceExists(palettePng(palette));
    }

    /// The entries of `palette`'s column, top to bottom, or null when the png cannot be read or the column carries
    /// no entries. See [PaletteBaker#paletteColumn] for how a column's length is read.
    static int[] paletteEntries(IResourceManager manager, PaletteRef palette) {
        String key = palette.modid() + ":" + palette.name();
        ParsedPalette parsed = PARSED.get(key);
        if (parsed == null) {
            parsed = parse(manager, palettePng(palette));
            if (parsed == null) return null;
            PARSED.put(key, parsed);
        }
        return PaletteBaker.paletteColumn(parsed.argb(), parsed.width(), parsed.height(), palette.column());
    }

    static void clearCache() {
        PARSED.clear();
    }

    private static ParsedPalette parse(IResourceManager manager, ResourceLocation location) {
        try (InputStream stream = manager.getResource(location).getInputStream()) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) return null;
            int width = image.getWidth();
            int height = image.getHeight();
            return new ParsedPalette(image.getRGB(0, 0, width, height, null, 0, width), width, height);
        }
        catch (IOException e) {
            return null;
        }
    }
}
