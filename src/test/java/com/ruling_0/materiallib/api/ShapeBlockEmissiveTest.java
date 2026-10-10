package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;

import org.junit.jupiter.api.Test;

/// Pins the emissive rule: a material's layers glow only when the material is [StandardProperties#EMISSIVE] and the
/// shape sets [StandardProperties#EMISSIVE_LAYERS].
class ShapeBlockEmissiveTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    @Test
    void onlyEmissiveMaterialsGlowOnAShapeThatOptsIn() {
        Material glowing = registry.newMaterial("testmod", "Glowing", texture)
            .setProperty(StandardProperties.EMISSIVE, true)
            .build();
        Material plain = registry.newMaterial("testmod", "Plain", texture).build();
        registry.resolve();
        TestShape ore = new TestShape("testmod", "ore");
        ore.properties().set(ore, StandardProperties.EMISSIVE_LAYERS, true);

        BitSet lit = ShapeBlock.emissiveIndices(ore, new Material[] { glowing, plain });

        assertEquals(1, lit.cardinality());
        assertTrue(lit.get(glowing.getIndex()));
    }

    @Test
    void aShapeThatDoesNotOptInLightsNoMaterial() {
        Material glowing = registry.newMaterial("testmod", "Glowing", texture)
            .setProperty(StandardProperties.EMISSIVE, true)
            .build();
        registry.resolve();

        assertTrue(ShapeBlock.emissiveIndices(new TestShape("testmod", "block"), new Material[] { glowing }).isEmpty());
    }
}
