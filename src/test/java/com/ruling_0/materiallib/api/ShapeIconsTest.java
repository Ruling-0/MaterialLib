package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.util.IIcon;

import org.junit.jupiter.api.Test;

/// Pins [ShapeIcons]' placeholder fallbacks, the precedence of its texture sources, the shape of the layer stack it
/// binds, and the color each layer takes. [ShapeIcons#resolvePath] and [ShapeIcons#ShapeIcons(boolean, Predicate)]
/// take an existence predicate, so both run headless.
class ShapeIconsTest {

    /// Accepts every path outside the resource-pack override root, so a chain test runs with that source missing.
    private static final Predicate<String> NO_OVERRIDE = path -> !path.startsWith(ShapeIcons.OVERRIDE_ROOT);

    /// Accepts no numbered layer sibling, bounding the layer probe of a test that otherwise accepts every path.
    private static final Predicate<String> NO_LAYERS = path -> !path.contains(ShapeIcons.LAYER_SUFFIX);

    private final MaterialRegistry registry = new MaterialRegistry();
    private final RecordingRegister register = new RecordingRegister();
    private final TextureSet setA = TextureSet.of("testmod", "setA");
    private final TextureSet setB = TextureSet.of("testmod", "setB");
    private final TextureSet setC = TextureSet.of("testmod", "setC");

    /// Constructs a [Material] directly -- [MaterialBuilder] rejects a missing [StandardProperties#TEXTURE_SET] --
    /// to pin that binding it resolves the placeholder instead of crashing.
    @Test
    void materialWithoutTextureSetBindsPlaceholderInsteadOfCrashing() {
        Map<Property<?>, Object> properties = Map.of(StandardProperties.NAME, "Broken");
        Material material = new Material(registry, "testmod", "Broken", properties, Set.of(), List.of());
        registry.register(material);
        registry.resolve();

        ShapeIcons icons = new ShapeIcons(true, path -> false);
        assertDoesNotThrow(() -> icons.bind(register, new Material[] { material }, List.of("gear"), ignored -> null));

        IIcon placeholder = register.registered.get(ShapeIcons.EMPTY_ICON);
        assertNotNull(placeholder);
        assertSame(placeholder, icons.get(material.getIndex()));
        assertEquals(1, icons.layerCount(material.getIndex()));
        assertSame(placeholder, icons.layer(material.getIndex(), 1));
    }

    /// An empty fallback list is equivalent to no fallback at all: the chain runs out and the material binds the
    /// placeholder.
    @Test
    void anEmptyFallbackListBindsThePlaceholderWithoutCrashing() {
        Map<Property<?>, Object> properties = Map
            .of(StandardProperties.NAME, "Broken", StandardProperties.FALLBACK_TEXTURE_SETS, List.of());
        Material material = new Material(registry, "testmod", "Broken", properties, Set.of(), List.of());
        registry.register(material);
        registry.resolve();

        ShapeIcons icons = new ShapeIcons(true, path -> false);
        assertDoesNotThrow(() -> icons.bind(register, new Material[] { material }, "gear"));

        IIcon placeholder = register.registered.get(ShapeIcons.EMPTY_ICON);
        assertNotNull(placeholder);
        assertSame(placeholder, icons.get(material.getIndex()));
    }

    /// An index no material bound reports one layer and resolves to the empty placeholder inside and outside the
    /// stack's bounds.
    @Test
    void unboundIndexFallsBackToPlaceholder() {
        ShapeIcons icons = new ShapeIcons(false, path -> false);
        icons.bind(register, new Material[0], "gear");

        IIcon placeholder = register.registered.get(ShapeIcons.EMPTY_ICON);
        assertNotNull(placeholder);
        assertSame(placeholder, icons.get(7));
        assertEquals(1, icons.layerCount(7));
        assertSame(placeholder, icons.layer(7, 1));
    }

    /// A material's own texture set outranks its fallback sets.
    @Test
    void textureSetBeatsTheFirstFallback() {
        Material material = declareMaterial("testmod", "Testiron", setA, List.of(setB));
        registry.resolve();

        assertEquals(setA.iconPath("gear"), ShapeIcons.resolvePath(material, List.of("gear"), null, NO_OVERRIDE));
    }

    /// Fallback texture sets are tried in list order.
    @Test
    void fallbackSetsAreTriedInOrder() {
        Material material = declareMaterial("testmod", "Testiron", setA, List.of(setB, setC));
        registry.resolve();
        Predicate<String> exceptSetA = NO_OVERRIDE.and(path -> !path.equals(setA.iconPath("gear")));
        Predicate<String> onlySetC = path -> path.equals(setC.iconPath("gear"));

        assertEquals(setB.iconPath("gear"), ShapeIcons.resolvePath(material, List.of("gear"), null, exceptSetA));
        assertEquals(setC.iconPath("gear"), ShapeIcons.resolvePath(material, List.of("gear"), null, onlySetC));
    }

    /// Within one texture source, an earlier candidate name wins.
    @Test
    void earlierCandidateNameWinsWithinASource() {
        Material material = declareMaterial("testmod", "Testiron", setA, List.of());
        registry.resolve();

        assertEquals(setA.iconPath("gear_x"),
            ShapeIcons.resolvePath(material, List.of("gear_x", "gear"), null, NO_OVERRIDE));
    }

    /// Every source of a material outranks every source of a unification alternative.
    @Test
    void ownSourcesBeatAnAlternatives() {
        Material owner = declareMaterial("amod", "Testiron", setA, List.of(setB));
        declareMaterial("bmod", "Testiron", setC, List.of());
        registry.resolve();
        Predicate<String> exceptSetA = NO_OVERRIDE.and(path -> !path.equals(setA.iconPath("gear")));

        assertEquals(setB.iconPath("gear"), ShapeIcons.resolvePath(owner, List.of("gear"), null, exceptSetA));
    }

    /// A per-material icon path outranks the texture-set chain; returning null from it falls through to the chain.
    @Test
    void aPerMaterialPathBeatsTheTextureSetChain() {
        Material material = declareMaterial("testmod", "Testiron", setA, List.of());
        registry.resolve();

        assertEquals("testmod:custom/iron_gear", ShapeIcons.resolvePath(material, List.of("gear"),
            ignored -> "testmod:custom/iron_gear", NO_OVERRIDE));
        assertEquals(setA.iconPath("gear"),
            ShapeIcons.resolvePath(material, List.of("gear"), ignored -> null, NO_OVERRIDE));
    }

    /// Pins the pack-facing override path -- the override root, the material's registry name in its exact case,
    /// and the first candidate name -- ahead of a texture set carrying the same candidates.
    @Test
    void anOverrideBeatsTheTextureSetAtItsPackFacingPath() {
        Material material = declareMaterial("testmod", "Testiron", setA, List.of());
        registry.resolve();

        assertEquals("materiallib:mloverrides/Testiron/gear_x",
            ShapeIcons.resolvePath(material, List.of("gear_x", "gear"), null, path -> true));
    }

    /// A resource-pack override outranks a per-material icon path.
    @Test
    void anOverrideBeatsThePerMaterialPath() {
        Material material = declareMaterial("testmod", "Testiron", setA, List.of());
        registry.resolve();

        assertEquals(ShapeIcons.overridePath(material, "gear"), ShapeIcons.resolvePath(material, List.of("gear"),
            ignored -> "testmod:custom/iron_gear", path -> true));
    }

    /// An override supplies its own `_OVERLAY` layer or none: a missing sibling binds no overlay even when the
    /// texture set the override outranks carries one.
    @Test
    void anOverrideSuppliesItsOwnOverlayOrNone() {
        Material material = declareMaterial("testmod", "Testiron", setA, List.of());
        registry.resolve();
        Material[] materials = { material };
        String override = ShapeIcons.overridePath(material, "gear");
        int index = material.getIndex();

        ShapeIcons withOverlay = new ShapeIcons(true, NO_LAYERS);
        withOverlay.bind(register, materials, "gear");
        int last = withOverlay.layerCount(index) - 1;
        assertTrue(withOverlay.isOverlayLayer(index, last));
        assertSame(register.registered.get(override + ShapeIcons.OVERLAY_SUFFIX), withOverlay.layer(index, last));

        ShapeIcons withoutOverlay = new ShapeIcons(true,
            NO_LAYERS.and(path -> !path.equals(override + ShapeIcons.OVERLAY_SUFFIX)));
        withoutOverlay.bind(register, materials, "gear");
        assertSame(register.registered.get(override), withoutOverlay.get(index));
        assertFalse(withoutOverlay.isOverlayLayer(index, withoutOverlay.layerCount(index) - 1));
    }

    /// Only the override tier marks a material untinted, and re-binding without the pack clears that mark.
    @Test
    void onlyAnOverrideBoundIconIsFlaggedUntinted() {
        Material material = declareMaterial("testmod", "Testiron", setA, List.of());
        registry.resolve();
        Material[] materials = { material };
        boolean[] packLoaded = { true };
        ShapeIcons icons = new ShapeIcons(true, NO_LAYERS.and(path -> packLoaded[0] || NO_OVERRIDE.test(path)));

        icons.bind(register, materials, "gear");
        assertTrue(icons.isOverride(material.getIndex()));

        packLoaded[0] = false;
        icons.bind(register, materials, List.of("gear"), ignored -> "testmod:custom/iron_gear");
        assertFalse(icons.isOverride(material.getIndex()));

        icons.bind(register, materials, "gear");
        assertFalse(icons.isOverride(material.getIndex()));
    }

    /// The numbered layers are probed upward from 1 and stop at the first absent number, so art skipping a number
    /// leaves everything above it out of the stack.
    @Test
    void aGapInTheLayerNumbersEndsTheStack() {
        Material material = declareMaterial("testmod", "Testiron", setA, List.of());
        registry.resolve();
        String base = setA.iconPath("gear");
        Set<String> art = Set.of(base, base + ShapeIcons.LAYER_SUFFIX + 1, base + ShapeIcons.LAYER_SUFFIX + 3);
        int index = material.getIndex();

        ShapeIcons icons = new ShapeIcons(true, art::contains);
        icons.bind(register, new Material[] { material }, "gear");

        assertEquals(2, icons.layerCount(index));
        assertSame(register.registered.get(base), icons.layer(index, 0));
        assertSame(register.registered.get(base + ShapeIcons.LAYER_SUFFIX + 1), icons.layer(index, 1));
        assertFalse(icons.isOverlayLayer(index, 1));
    }

    /// `_OVERLAY` binds as the stack's last layer.
    @Test
    void anOverlayBindsAsTheStacksLastLayer() {
        Material material = declareMaterial("testmod", "Testiron", setA, List.of());
        registry.resolve();
        String base = setA.iconPath("gear");
        Set<String> art = Set.of(base, base + ShapeIcons.OVERLAY_SUFFIX);
        int index = material.getIndex();

        ShapeIcons icons = new ShapeIcons(true, art::contains);
        icons.bind(register, new Material[] { material }, "gear");

        assertEquals(2, icons.layerCount(index));
        assertSame(register.registered.get(base), icons.get(index));
        assertSame(register.registered.get(base + ShapeIcons.OVERLAY_SUFFIX), icons.layer(index, 1));
        assertTrue(icons.isOverlayLayer(index, 1));
    }

    /// Art with no siblings is a one-layer stack carrying no overlay.
    @Test
    void artWithNoSiblingsBindsASingleLayer() {
        Material material = declareMaterial("testmod", "Testiron", setA, List.of());
        registry.resolve();
        String base = setA.iconPath("gear");
        int index = material.getIndex();

        ShapeIcons icons = new ShapeIcons(true, base::equals);
        icons.bind(register, new Material[] { material }, "gear");

        assertEquals(1, icons.layerCount(index));
        assertSame(register.registered.get(base), icons.get(index));
        assertFalse(icons.isOverlayLayer(index, 0));
        assertSame(register.registered.get(ShapeIcons.EMPTY_ICON), icons.layer(index, 1));
    }

    /// A stack resolves at one location: an override's numbered layers come from the override root, not from the
    /// texture set the override outranks.
    @Test
    void anOverrideStacksLayersComeFromTheOverrideRoot() {
        Material material = declareMaterial("testmod", "Testiron", setA, List.of());
        registry.resolve();
        String override = ShapeIcons.overridePath(material, "gear");
        String base = setA.iconPath("gear");
        Set<String> art = Set.of(override, override + ShapeIcons.LAYER_SUFFIX + 1, base,
            base + ShapeIcons.LAYER_SUFFIX + 1, base + ShapeIcons.LAYER_SUFFIX + 2);
        int index = material.getIndex();

        ShapeIcons icons = new ShapeIcons(true, art::contains);
        icons.bind(register, new Material[] { material }, "gear");

        assertTrue(icons.isOverride(index));
        assertEquals(2, icons.layerCount(index));
        assertSame(register.registered.get(override), icons.layer(index, 0));
        assertSame(register.registered.get(override + ShapeIcons.LAYER_SUFFIX + 1), icons.layer(index, 1));
    }

    /// The trailing `_OVERLAY` layer draws untinted even where [StandardProperties#LAYER_TINTS] codes an element at
    /// its index, while a numbered layer takes its coded element and layer 0 takes [StandardProperties#TINT].
    @Test
    void anOverlayLayerReportsWhiteWhileANumberedLayerReportsItsCodedTint() {
        Map<Property<?>, Object> properties = Map.of(StandardProperties.NAME, "Testlayered",
            StandardProperties.TEXTURE_SET, setA, StandardProperties.TINT, 0xFFFFCC00,
            StandardProperties.LAYER_TINTS, List.of(0xFF00FF00, 0xFF0000FF));
        Material material = new Material(registry, "testmod", "Testlayered", properties, Set.of(), List.of());
        registry.register(material);
        registry.resolve();
        String base = setA.iconPath("gear");
        Set<String> art = Set.of(base, base + ShapeIcons.LAYER_SUFFIX + 1, base + ShapeIcons.OVERLAY_SUFFIX);

        ShapeIcons icons = new ShapeIcons(true, art::contains);
        icons.bind(register, new Material[] { material }, "gear");

        assertTrue(icons.isOverlayLayer(material.getIndex(), 2));
        assertEquals(0xFFFFCC00, icons.layerColor(material, 0));
        assertEquals(0xFF00FF00, icons.layerColor(material, 1));
        assertEquals(0xFFFFFFFF, icons.layerColor(material, 2));
    }

    private Material declareMaterial(String modid, String name, TextureSet textureSet, List<TextureSet> fallbacks) {
        Map<Property<?>, Object> properties = Map.of(StandardProperties.NAME, name, StandardProperties.TEXTURE_SET,
            textureSet, StandardProperties.FALLBACK_TEXTURE_SETS, fallbacks);
        Material material = new Material(registry, modid, name, properties, Set.of(), List.of());
        registry.register(material);
        return material;
    }
}
