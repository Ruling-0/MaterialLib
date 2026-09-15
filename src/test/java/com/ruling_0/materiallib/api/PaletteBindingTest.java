package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import org.junit.jupiter.api.Test;

/// Pins which texture source [ShapeIcons] bakes through a [PaletteRef], the art it hands the baker, and the shape
/// and color of the stack a baked material ends up with. The baker is injected, so the whole classification runs
/// headless.
class PaletteBindingTest {

    private static final PaletteRef METALS = new PaletteRef("testmod", "metals", 2);

    private static final int TINT = 0xFFFFCC00;

    /// Accepts every path outside the resource-pack override root, so a test can run with that source missing.
    private static final Predicate<String> NO_OVERRIDE = path -> !path.startsWith(ShapeIcons.OVERRIDE_ROOT);

    /// Accepts no numbered layer sibling, bounding the layer probe of a test that otherwise accepts every path.
    private static final Predicate<String> NO_LAYERS = path -> !path.contains(ShapeIcons.LAYER_SUFFIX);

    private final MaterialRegistry registry = new MaterialRegistry();
    private final RecordingRegister register = new RecordingRegister();
    private final RecordingBinder binder = new RecordingBinder();
    private final TextureSet setA = TextureSet.of("testmod", "setA");

    /// Texture-set art bakes: the baker sees the whole discovered stack and its single icon replaces that stack,
    /// leaving no overlay layer and nothing for a tint to multiply.
    @Test
    void textureSetArtBakesIntoASingleUntintedLayer() {
        Material material = declareMaterial("Testiron", METALS);
        registry.resolve();
        String base = setA.iconPath("gear");
        String layer = base + ShapeIcons.LAYER_SUFFIX + 1;
        String overlay = base + ShapeIcons.OVERLAY_SUFFIX;
        int index = material.getIndex();

        ShapeIcons icons = new ShapeIcons(true, Set.of(base, layer, overlay)::contains, binder);
        icons.bind(register, new Material[] { material }, "gear");

        assertTrue(icons.isPaletteBaked(index));
        assertEquals(1, icons.layerCount(index));
        assertSame(register.registered.get(METALS.bakedIconName(base)), icons.get(index));
        assertFalse(icons.isOverlayLayer(index, 0));
        assertEquals(0xFFFFFFFF, icons.layerColor(material, 0));

        assertEquals(1, binder.calls.size());
        assertEquals(new BakeCall(true, new ShapeIcons.StackPaths(base, List.of(layer), overlay), METALS),
            binder.calls.get(0));
    }

    /// A material setting no palette never reaches the baker, even on a palette-enabled instance.
    @Test
    void aMaterialWithoutAPaletteNeverReachesTheBaker() {
        Material material = declareMaterialWithoutPalette("Testiron");
        registry.resolve();
        String base = setA.iconPath("gear");
        int index = material.getIndex();

        ShapeIcons icons = new ShapeIcons(true, Set.of(base)::contains, binder);
        icons.bind(register, new Material[] { material }, "gear");

        assertTrue(binder.calls.isEmpty());
        assertFalse(icons.isPaletteBaked(index));
        assertEquals(1, icons.layerCount(index));
        assertSame(register.registered.get(base), icons.get(index));
        assertEquals(TINT, icons.layerColor(material, 0));
    }

    /// A resource-pack override outranks a palette and never reaches the baker, so its art draws as authored.
    @Test
    void aResourcePackOverrideOutranksThePalette() {
        Material material = declareMaterial("Testiron", METALS);
        registry.resolve();
        int index = material.getIndex();

        ShapeIcons icons = new ShapeIcons(true, NO_LAYERS, binder);
        icons.bind(register, new Material[] { material }, "gear");

        assertTrue(icons.isOverride(index));
        assertFalse(icons.isPaletteBaked(index));
        assertTrue(binder.calls.isEmpty());
        assertSame(register.registered.get(ShapeIcons.overridePath(material, "gear")), icons.get(index));
    }

    /// A per-material icon path outranks a palette, never reaches the baker, and keeps the material's tint.
    @Test
    void aPerMaterialIconPathOutranksThePalette() {
        Material material = declareMaterial("Testiron", METALS);
        registry.resolve();
        String perMaterial = "testmod:custom/iron_gear";
        int index = material.getIndex();

        ShapeIcons icons = new ShapeIcons(true, NO_OVERRIDE.and(NO_LAYERS), binder);
        icons.bind(register, new Material[] { material }, List.of("gear"), ignored -> perMaterial);

        assertFalse(icons.isPaletteBaked(index));
        assertTrue(binder.calls.isEmpty());
        assertSame(register.registered.get(perMaterial), icons.get(index));
        assertEquals(TINT, icons.layerColor(material, 0));
    }

    /// A baker that declines -- the palette png is missing -- leaves the material on the full tinted stack.
    @Test
    void aDeclinedBakeFallsBackToTheTintedStack() {
        Material material = declareMaterial("Testiron", METALS);
        registry.resolve();
        String base = setA.iconPath("gear");
        String layer = base + ShapeIcons.LAYER_SUFFIX + 1;
        String overlay = base + ShapeIcons.OVERLAY_SUFFIX;
        int index = material.getIndex();
        binder.bakes = false;

        ShapeIcons icons = new ShapeIcons(true, Set.of(base, layer, overlay)::contains, binder);
        icons.bind(register, new Material[] { material }, "gear");

        assertFalse(icons.isPaletteBaked(index));
        assertEquals(3, icons.layerCount(index));
        assertSame(register.registered.get(base), icons.layer(index, 0));
        assertSame(register.registered.get(layer), icons.layer(index, 1));
        assertSame(register.registered.get(overlay), icons.layer(index, 2));
        assertTrue(icons.isOverlayLayer(index, 2));
        assertEquals(TINT, icons.layerColor(material, 0));
    }

    /// A palette-disabled instance never reads the property.
    @Test
    void aPaletteDisabledInstanceIgnoresTheProperty() {
        Material material = declareMaterial("Testiron", METALS);
        registry.resolve();
        String base = setA.iconPath("gear");
        int index = material.getIndex();

        ShapeIcons icons = new ShapeIcons(true, Set.of(base)::contains);
        icons.bind(register, new Material[] { material }, "gear");

        assertFalse(icons.isPaletteBaked(index));
        assertSame(register.registered.get(base), icons.get(index));
        assertEquals(TINT, icons.layerColor(material, 0));
    }

    /// The deferred placeholder bind drops the baked mark along with the stacks it replaces.
    @Test
    void bindingThePlaceholderClearsTheBakedMark() {
        Material material = declareMaterial("Testiron", METALS);
        registry.resolve();
        String base = setA.iconPath("gear");
        int index = material.getIndex();

        ShapeIcons icons = new ShapeIcons(true, Set.of(base)::contains, binder);
        icons.bind(register, new Material[] { material }, "gear");
        assertTrue(icons.isPaletteBaked(index));

        icons.bindPlaceholder(register);

        assertFalse(icons.isPaletteBaked(index));
        assertSame(register.registered.get(ShapeIcons.EMPTY_ICON), icons.get(index));
    }

    private Material declareMaterial(String name, PaletteRef palette) {
        return declare(name, Map.of(StandardProperties.NAME, name, StandardProperties.TEXTURE_SET, setA,
            StandardProperties.TINT, TINT, StandardProperties.PALETTE, palette));
    }

    private Material declareMaterialWithoutPalette(String name) {
        return declare(name,
            Map.of(StandardProperties.NAME, name, StandardProperties.TEXTURE_SET, setA, StandardProperties.TINT, TINT));
    }

    private Material declare(String name, Map<Property<?>, Object> properties) {
        Material material = new Material(registry, "testmod", name, properties, Set.of(), List.of());
        registry.register(material);
        return material;
    }

    private record BakeCall(boolean isItem, ShapeIcons.StackPaths stack, PaletteRef palette) {}

    /// A [ShapeIcons.PaletteBinder] recording what it was handed, standing in for the client atlas. While `bakes`
    /// it hands back the icon a real bake would put on the atlas under [PaletteRef#bakedIconName]; otherwise it
    /// declines the way a missing palette png does.
    private static final class RecordingBinder implements ShapeIcons.PaletteBinder {

        private final List<BakeCall> calls = new ArrayList<>();
        private boolean bakes = true;

        @Override
        public IIcon bake(IIconRegister register, boolean isItem, ShapeIcons.StackPaths stack, PaletteRef palette) {
            calls.add(new BakeCall(isItem, stack, palette));
            return bakes ? register.registerIcon(palette.bakedIconName(stack.base())) : null;
        }
    }
}
