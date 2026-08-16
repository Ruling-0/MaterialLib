package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/// Pins the pack-facing lang key the standard tint properties read, and the coded value a read falls back to with
/// no lang entry present.
class MaterialTintsTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    @Test
    void aTintReadsTheLangKeyNamedAfterItsMaterialAndProperty() {
        Material material = registry.newMaterial("testmod", "TestIron", texture).setTint(0xFFFFCC00).build();
        registry.resolve();

        assertEquals("color.resource.materiallib.TestIron.tint",
            MaterialTints.resourceFor(material, StandardProperties.TINT).getLangKey());
        assertEquals(0xFFFFCC00, MaterialTints.color(material, StandardProperties.TINT));
    }

    @Test
    void aUnifiedAlternativeReadsTheCanonicalMaterialsKey() {
        Material owner = registry.newMaterial("amod", "TestSteel", texture)
            .setProperty(StandardProperties.CELL_TINT, 0x80FF0000).build();
        Material alternative = registry.newMaterial("bmod", "TestSteel", texture).build();
        registry.resolve();

        assertEquals("color.resource.materiallib.TestSteel.cellTint",
            MaterialTints.resourceFor(alternative, StandardProperties.CELL_TINT).getLangKey());
        assertSame(MaterialTints.resourceFor(owner, StandardProperties.CELL_TINT),
            MaterialTints.resourceFor(alternative, StandardProperties.CELL_TINT));
    }
}
