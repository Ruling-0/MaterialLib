package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.IIcon;

import org.junit.jupiter.api.Test;

/// Pins [ShapeIcons]' placeholder fallbacks. Only paths that never consult the resource manager run here; paths
/// that check whether a texture file exists need a live client; see the example content.
class ShapeIconsTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final RecordingRegister register = new RecordingRegister();

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
}
