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

/// Headless coverage for [ShapeIcons]' placeholder fallbacks. Only the paths that never consult the resource
/// manager run here: a material with no texture set performs no resource lookup, so [ShapeIcons#bind] completes
/// without a Minecraft client. Paths that check whether a texture file exists need a live client and are covered
/// by the in-game example content instead.
class ShapeIconsTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final RecordingRegister register = new RecordingRegister();

    /// A material missing the mandatory [StandardProperties#TEXTURE_SET] (constructed around [MaterialBuilder],
    /// which forbids that) must bind without crashing, and every icon accessor must fall back to the registered
    /// empty placeholder rather than return null into a renderer.
    @Test
    void materialWithoutTextureSetBindsPlaceholderInsteadOfCrashing() {
        Map<Property<?>, Object> properties = Map.of(StandardProperties.NAME, "Broken");
        Material material = new Material(registry, "testmod", "Broken", properties, Set.of(), List.of());
        registry.register(material);
        registry.resolve();

        ShapeIcons icons = new ShapeIcons(true);
        assertDoesNotThrow(() -> icons.bind(register, new Material[] { material }, "gear"));

        IIcon placeholder = register.registered.get(ShapeIcons.EMPTY_ICON);
        assertNotNull(placeholder);
        assertSame(placeholder, icons.get(material.getIndex()));
        assertSame(placeholder, icons.getOverlay(material.getIndex()));
    }

    /// An index no material bound (an unloaded material's stack, or a stray damage value) resolves to the empty
    /// placeholder from both accessors.
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
