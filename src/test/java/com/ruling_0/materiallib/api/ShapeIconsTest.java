package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.util.IIcon;

import org.junit.jupiter.api.Test;

/// Pins [ShapeIcons]' placeholder fallbacks and the precedence of its texture sources. [ShapeIcons#resolvePath]
/// and [ShapeIcons#ShapeIcons(boolean, Predicate)] take an existence predicate, so both run headless.
class ShapeIconsTest {

    /// Accepts every path outside the resource-pack override root, so a chain test runs with that source missing.
    private static final Predicate<String> NO_OVERRIDE = path -> !path.startsWith(ShapeIcons.OVERRIDE_ROOT);

    private final MaterialRegistry registry = new MaterialRegistry();
    private final RecordingRegister register = new RecordingRegister();
    private final TextureSet setA = TextureSet.of("testmod", "setA");
    private final TextureSet setB = TextureSet.of("testmod", "setB");
    private final TextureSet setC = TextureSet.of("testmod", "setC");

    /// Constructs a [Material] directly -- [MaterialBuilder] rejects a missing [StandardProperties#TEXTURE_SET] --
    /// to pin that binding it resolves the placeholder from both accessors instead of crashing.
    @Test
    void materialWithoutTextureSetBindsPlaceholderInsteadOfCrashing() {
        Map<Property<?>, Object> properties = Map.of(StandardProperties.NAME, "Broken");
        Material material = new Material(registry, "testmod", "Broken", properties, Set.of(), List.of());
        registry.register(material);
        registry.resolve();

        ShapeIcons icons = new ShapeIcons(true, path -> false);
        assertDoesNotThrow(
            () -> icons.bind(register, new Material[] { material }, List.of("gear"), ignored -> null));

        IIcon placeholder = register.registered.get(ShapeIcons.EMPTY_ICON);
        assertNotNull(placeholder);
        assertSame(placeholder, icons.get(material.getIndex()));
        assertSame(placeholder, icons.getOverlay(material.getIndex()));
        assertNull(icons.getOverlayOrNull(material.getIndex()));
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

    /// An index no material bound resolves to the empty placeholder from both placeholder accessors, and to null
    /// from the nullable overlay accessor.
    @Test
    void unboundIndexFallsBackToPlaceholder() {
        ShapeIcons icons = new ShapeIcons(false, path -> false);
        icons.bind(register, new Material[0], "gear");

        IIcon placeholder = register.registered.get(ShapeIcons.EMPTY_ICON);
        assertNotNull(placeholder);
        assertSame(placeholder, icons.get(7));
        assertSame(placeholder, icons.getOverlay(7));
        assertNull(icons.getOverlayOrNull(7));
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

        ShapeIcons withOverlay = new ShapeIcons(true, path -> true);
        withOverlay.bind(register, materials, "gear");
        IIcon overlay = withOverlay.getOverlayOrNull(material.getIndex());
        assertNotNull(overlay);
        assertSame(register.registered.get(override + ShapeIcons.OVERLAY_SUFFIX), overlay);

        ShapeIcons withoutOverlay = new ShapeIcons(true, path -> !path.equals(override + ShapeIcons.OVERLAY_SUFFIX));
        withoutOverlay.bind(register, materials, "gear");
        assertSame(register.registered.get(override), withoutOverlay.get(material.getIndex()));
        assertNull(withoutOverlay.getOverlayOrNull(material.getIndex()));
    }

    private Material declareMaterial(String modid, String name, TextureSet textureSet, List<TextureSet> fallbacks) {
        Map<Property<?>, Object> properties = Map.of(StandardProperties.NAME, name, StandardProperties.TEXTURE_SET,
            textureSet, StandardProperties.FALLBACK_TEXTURE_SETS, fallbacks);
        Material material = new Material(registry, modid, name, properties, Set.of(), List.of());
        registry.register(material);
        return material;
    }
}
