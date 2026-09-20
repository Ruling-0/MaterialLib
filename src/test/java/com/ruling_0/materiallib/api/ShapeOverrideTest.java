package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

/// Pins which declaration's shape override stands once same-name materials unify: an override replaces the
/// version of a shape its own declaration would have produced, never another mod's.
class ShapeOverrideTest {

    private static final Predicate<String> ALL_LOADED = modid -> true;

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("amod", "shiny");
    private final TestShape ingot = new TestShape("amod", "ingot");

    @Test
    void theOwnersOverrideStands() {
        registry.newMaterial("amod", "Iron", texture).generateShape(ingot).addShapeOverride(ingot, "ownermod", "bar", 0)
            .build();
        registry.newMaterial("bmod", "Iron", texture).generateShape(ingot).addShapeOverride(ingot, "losermod", "bar", 0)
            .build();
        registry.resolve();

        assertEquals("ownermod:bar:0", chosen("amod", "Iron", ALL_LOADED));
    }

    @Test
    void aLosersOverrideIsIgnoredForAShapeTheOwnerGenerates() {
        registry.newFamily("amod", "Metals").generateShape(ingot).build();
        registry.newMaterial("amod", "Iron", texture).addToFamily("amod", "Metals").build();
        registry.newMaterial("bmod", "Iron", texture).generateShape(ingot).addShapeOverride(ingot, "losermod", "bar", 0)
            .build();
        registry.resolve();

        assertNull(chosen("amod", "Iron", ALL_LOADED));
    }

    @Test
    void aLosersOverrideStandsForAShapeOnlyItGenerates() {
        registry.newMaterial("amod", "Iron", texture).build();
        registry.newMaterial("bmod", "Iron", texture).generateShape(ingot).addShapeOverride(ingot, "losermod", "bar", 0)
            .build();
        registry.resolve();

        assertEquals("losermod:bar:0", chosen("amod", "Iron", ALL_LOADED));
    }

    @Test
    void anEditBeatsTheOwnersDeclaration() {
        registry.newMaterial("amod", "Iron", texture).generateShape(ingot).addShapeOverride(ingot, "ownermod", "bar", 0)
            .build();
        registry.editMaterial("amod", "Iron").addShapeOverride(ingot, "packmod", "bar", 3);
        registry.resolve();

        assertEquals("packmod:bar:3", chosen("amod", "Iron", ALL_LOADED));
    }

    @Test
    void anOverrideNamingAnAbsentModIsTreatedAsUndeclared() {
        registry.newMaterial("amod", "Iron", texture).generateShape(ingot).addShapeOverride(ingot, "ownermod", "bar", 0)
            .build();
        registry.resolve();

        assertNull(chosen("amod", "Iron", modid -> !modid.equals("ownermod")));
    }

    private String chosen(String modid, String name, Predicate<String> modLoaded) {
        ShapeOverride override = registry.getMaterial(modid, name)
            .chooseOverride(ingot, UnaryOperator.identity(), modLoaded);
        return override == null ? null : override.describe();
    }
}
