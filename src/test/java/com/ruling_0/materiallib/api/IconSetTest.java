package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.IIcon;

import org.junit.jupiter.api.Test;

/// Headless coverage for the [IconSet] contract a compositing renderer relies on: a material whose art is missing
/// gets the transparent placeholder from [IconSet#getIcon] and null -- never the placeholder -- from
/// [IconSet#getOverlayIcon], so the renderer can tell "no overlay layer" from "an overlay that draws nothing".
/// As in [ShapeIconsTest], only the paths that never consult the resource manager run without a live client.
class IconSetTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final RecordingRegister register = new RecordingRegister();

    @Test
    void aMaterialWithNoArtBindsThePlaceholderAndNoOverlay() {
        Map<Property<?>, Object> properties = Map.of(StandardProperties.NAME, "Broken");
        Material material = new Material(registry, "testmod", "Broken", properties, Set.of(), List.of());
        registry.register(material);
        registry.resolve();

        IconSet set = new IconSet("testmod", "toolWrench", IconSet.Atlas.ITEMS);
        set.bind(register, new Material[] { material });

        IIcon placeholder = register.registered.get(ShapeIcons.EMPTY_ICON);
        assertNotNull(placeholder);
        assertSame(placeholder, set.getIcon(material));
        assertNull(set.getOverlayIcon(material));
    }
}
