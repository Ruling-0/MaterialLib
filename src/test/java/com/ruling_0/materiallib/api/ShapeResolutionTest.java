package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class ShapeResolutionTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");
    private final Shape ingot = new TestShape("testmod", "ingot");
    private final Shape gear = new TestShape("testmod", "gear");
    private final Shape frame = new TestShape("testmod", "frame");

    @Test
    void shapesCombineOwnAndFamilyMinusRemoved() {
        registry.newFamily("testmod", "Metals")
            .generateShapes(ingot, frame)
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .generateShape(gear)
            .addToFamily("testmod", "Metals")
            .build();
        registry.editMaterial("testmod", "TestIron")
            .removeShape(frame);
        registry.resolve();

        assertEquals(Set.of(gear, ingot), material.getShapes());
        assertTrue(material.hasShape(gear));
        assertFalse(material.hasShape(frame));
    }

    @Test
    void removeThenReAddKeepsShape() {
        registry.newFamily("testmod", "Metals")
            .generateShape(ingot)
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily("testmod", "Metals")
            .build();
        registry.editMaterial("testmod", "TestIron")
            .removeShape(ingot)
            .generateShape(ingot);
        registry.resolve();

        assertTrue(material.hasShape(ingot));
    }

    @Test
    void addThenRemoveDropsShape() {
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .generateShape(gear)
            .build();
        registry.editMaterial("testmod", "TestIron")
            .removeShape(gear);
        registry.resolve();

        assertFalse(material.hasShape(gear));
        assertTrue(material.getShapes().isEmpty());
    }

    @Test
    void familyEditsApplyToAllMembers() {
        registry.newFamily("testmod", "Metals")
            .build();
        Material iron = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily("testmod", "Metals")
            .build();
        Material gold = registry.newMaterial("testmod", "TestGold", texture)
            .addToFamily("testmod", "Metals")
            .build();
        registry.editFamily("testmod", "Metals")
            .generateShapes(ingot, gear);
        registry.editFamily("testmod", "Metals")
            .removeShape(gear);
        registry.resolve();

        assertEquals(Set.of(ingot), iron.getShapes());
        assertEquals(Set.of(ingot), gold.getShapes());
    }

    @Test
    void familyShapeRemovalDoesNotAffectOwnShapes() {
        registry.newFamily("testmod", "Metals")
            .generateShape(gear)
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .generateShape(gear)
            .addToFamily("testmod", "Metals")
            .build();
        registry.editFamily("testmod", "Metals")
            .removeShape(gear);
        registry.resolve();

        assertTrue(material.hasShape(gear));
    }

    @Test
    void familyShapesVisibleOnFamily() {
        Family family = registry.newFamily("testmod", "Metals")
            .generateShapes(ingot, frame)
            .build();
        registry.resolve();

        assertEquals(Set.of(ingot, frame), family.getShapes());
    }
}
