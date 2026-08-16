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

/// Pins [ShapeIcons]' placeholder fallbacks and the precedence of its texture-source chain. [ShapeIcons#resolve]
/// takes an existence predicate, so its chain runs headless. The binding paths that consult the resource manager
/// need a live client; see the example content.
class ShapeIconsTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final RecordingRegister register = new RecordingRegister();
    private final TextureSet setA = TextureSet.of("testmod", "setA");
    private final TextureSet setB = TextureSet.of("testmod", "setB");
    private final TextureSet setC = TextureSet.of("testmod", "setC");

    /// Constructs a [Material] directly -- [MaterialBuilder] rejects a missing [StandardProperties#TEXTURE_SET] --
    /// to pin that binding it resolves the placeholder from both accessors instead of crashing. A pather
    /// returning null falls through to the same candidate chain; a pather returning a path needs a live client to
    /// check the file exists, so only this branch runs headless.
    @Test
    void materialWithoutTextureSetBindsPlaceholderInsteadOfCrashing() {
        Map<Property<?>, Object> properties = Map.of(StandardProperties.NAME, "Broken");
        Material material = new Material(registry, "testmod", "Broken", properties, Set.of(), List.of());
        registry.register(material);
        registry.resolve();

        ShapeIcons icons = new ShapeIcons(true);
        assertDoesNotThrow(
            () -> icons.bind(register, new Material[] { material }, List.of("gear"), ignored -> null));

        IIcon placeholder = register.registered.get(ShapeIcons.EMPTY_ICON);
        assertNotNull(placeholder);
        assertSame(placeholder, icons.get(material.getIndex()));
        assertSame(placeholder, icons.getOverlay(material.getIndex()));
        assertNull(icons.getOverlayOrNull(material.getIndex()));
    }

    /// An empty fallback list is equivalent to no fallback at all: the chain runs out and the material takes the
    /// placeholder, rather than the list walk failing on the way.
    @Test
    void anEmptyFallbackListBindsThePlaceholderWithoutCrashing() {
        Map<Property<?>, Object> properties = Map
            .of(StandardProperties.NAME, "Broken", StandardProperties.FALLBACK_TEXTURE_SETS, List.of());
        Material material = new Material(registry, "testmod", "Broken", properties, Set.of(), List.of());
        registry.register(material);
        registry.resolve();

        ShapeIcons icons = new ShapeIcons(true);
        assertDoesNotThrow(() -> icons.bind(register, new Material[] { material }, "gear"));

        IIcon placeholder = register.registered.get(ShapeIcons.EMPTY_ICON);
        assertNotNull(placeholder);
        assertSame(placeholder, icons.get(material.getIndex()));
    }

    /// An index no material bound resolves to the empty placeholder from both placeholder accessors, and to null
    /// from the nullable overlay accessor.
    @Test
    void unboundIndexFallsBackToPlaceholder() {
        ShapeIcons icons = new ShapeIcons(false);
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

        assertSame(setA, ShapeIcons.resolve(material, List.of("gear"), path -> true).set());
    }

    /// Fallback texture sets are tried in list order.
    @Test
    void fallbackSetsAreTriedInOrder() {
        Material material = declareMaterial("testmod", "Testiron", setA, List.of(setB, setC));
        registry.resolve();
        Predicate<String> exceptSetA = path -> !path.equals(setA.iconPath("gear"));
        Predicate<String> onlySetC = path -> path.equals(setC.iconPath("gear"));

        assertSame(setB, ShapeIcons.resolve(material, List.of("gear"), exceptSetA).set());
        assertSame(setC, ShapeIcons.resolve(material, List.of("gear"), onlySetC).set());
    }

    /// Within one texture source, an earlier candidate name wins.
    @Test
    void earlierCandidateNameWinsWithinASource() {
        Material material = declareMaterial("testmod", "Testiron", setA, List.of());
        registry.resolve();

        ShapeIcons.ResolvedTexture resolved = ShapeIcons.resolve(material, List.of("gear_x", "gear"), path -> true);

        assertEquals("gear_x", resolved.shapeName());
    }

    /// Every source of a material outranks every source of a unification alternative.
    @Test
    void ownSourcesBeatAnAlternatives() {
        Material owner = declareMaterial("amod", "Testiron", setA, List.of(setB));
        declareMaterial("bmod", "Testiron", setC, List.of());
        registry.resolve();
        Predicate<String> exceptSetA = path -> !path.equals(setA.iconPath("gear"));

        assertSame(setB, ShapeIcons.resolve(owner, List.of("gear"), exceptSetA).set());
    }

    private Material declareMaterial(String modid, String name, TextureSet textureSet, List<TextureSet> fallbacks) {
        Map<Property<?>, Object> properties = Map.of(StandardProperties.NAME, name, StandardProperties.TEXTURE_SET,
            textureSet, StandardProperties.FALLBACK_TEXTURE_SETS, fallbacks);
        Material material = new Material(registry, modid, name, properties, Set.of(), List.of());
        registry.register(material);
        return material;
    }
}
