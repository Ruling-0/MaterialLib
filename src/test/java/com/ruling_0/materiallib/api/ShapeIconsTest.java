package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.renderer.texture.IIconRegister;
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
    }

    /// An index no material bound resolves to the empty placeholder from both accessors.
    @Test
    void unboundIndexFallsBackToPlaceholder() {
        ShapeIcons icons = new ShapeIcons(false);
        icons.bind(register, new Material[0], "gear");

        IIcon placeholder = register.registered.get(ShapeIcons.EMPTY_ICON);
        assertNotNull(placeholder);
        assertSame(placeholder, icons.get(7));
        assertSame(placeholder, icons.getOverlay(7));
    }

    private record FakeIcon(String name) implements IIcon {

        @Override
        public int getIconWidth() { return 16; }

        @Override
        public int getIconHeight() { return 16; }

        @Override
        public float getMinU() { return 0; }

        @Override
        public float getMaxU() { return 1; }

        @Override
        public float getInterpolatedU(double u) {
            return 0;
        }

        @Override
        public float getMinV() { return 0; }

        @Override
        public float getMaxV() { return 1; }

        @Override
        public float getInterpolatedV(double v) {
            return 0;
        }

        @Override
        public String getIconName() { return name; }
    }

    private static final class RecordingRegister implements IIconRegister {

        final Map<String, IIcon> registered = new HashMap<>();

        @Override
        public IIcon registerIcon(String path) {
            return registered.computeIfAbsent(path, FakeIcon::new);
        }
    }
}
