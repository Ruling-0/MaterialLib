package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ShapeUnificationTest {

    private final ShapeUnification unification = new ShapeUnification();

    @Test
    void firstShapeForANameIsCanonical() {
        TestShape gear = new TestShape("amod", "gear");

        assertSame(gear, unification.register(gear));
        assertTrue(unification.isCanonical(gear));
    }

    @Test
    void laterShapeWithSameNameUnifiesOntoTheFirst() {
        TestShape first = new TestShape("amod", "gear");
        TestShape second = new TestShape("bmod", "gear");
        unification.register(first);

        assertSame(first, unification.register(second));
        assertFalse(unification.isCanonical(second));
        assertSame(first, unification.canonical(second));
    }

    @Test
    void canonicalOfTheCanonicalShapeIsItself() {
        TestShape gear = new TestShape("amod", "gear");
        unification.register(gear);

        assertSame(gear, unification.canonical(gear));
    }

    @Test
    void canonicalOfAnUnregisteredShapeIsItself() {
        TestShape gear = new TestShape("amod", "gear");

        assertSame(gear, unification.canonical(gear));
    }

    @Test
    void reRegisteringTheSameInstanceKeepsItCanonical() {
        TestShape gear = new TestShape("amod", "gear");
        unification.register(gear);

        assertSame(gear, unification.register(gear));
        assertTrue(unification.isCanonical(gear));
    }

    @Test
    void differentNamesProduceDistinctCanonicals() {
        TestShape gear = new TestShape("amod", "gear");
        TestShape plate = new TestShape("amod", "plate");
        unification.register(gear);
        unification.register(plate);

        assertTrue(unification.isCanonical(gear));
        assertTrue(unification.isCanonical(plate));
        assertEquals(List.of(gear, plate), new ArrayList<>(unification.canonicalShapes()));
    }

    @Test
    void unifyingShapesWithDifferentOredictPrefixesStillCollapsesToTheFirst() {
        TestShape first = new TestShape("amod", "gear", "gear");
        TestShape second = new TestShape("bmod", "gear", "wheel");
        unification.register(first);

        assertSame(first, unification.register(second));
        assertSame(first, unification.canonical(second));
    }
}
