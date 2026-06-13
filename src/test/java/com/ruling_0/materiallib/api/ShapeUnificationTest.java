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
    void unifyingShapesWithDifferentOredictPrefixesUsesTheCanonicalPrefix() {
        TestShape first = new TestShape("amod", "gear", "gear");
        TestShape second = new TestShape("bmod", "gear", "wheel");
        unification.register(first);

        assertSame(first, unification.register(second));
        assertSame(first, unification.canonical(second));
        assertEquals("gear", unification.canonical(second)
            .getOreDict());
    }

    @Test
    void canonicalShapesHoldsOnlyTheCanonicalAfterAnAlias() {
        TestShape first = new TestShape("amod", "gear");
        TestShape second = new TestShape("bmod", "gear");
        unification.register(first);
        unification.register(second);

        assertEquals(List.of(first), new ArrayList<>(unification.canonicalShapes()));
    }

    @Test
    void canonicalShapesPreservesRegistrationOrderAcrossAliases() {
        TestShape plate = new TestShape("amod", "plate");
        TestShape gear = new TestShape("amod", "gear");
        TestShape gearAlias = new TestShape("bmod", "gear");
        TestShape rod = new TestShape("amod", "rod");
        unification.register(plate);
        unification.register(gear);
        unification.register(gearAlias);
        unification.register(rod);

        assertEquals(List.of(plate, gear, rod), new ArrayList<>(unification.canonicalShapes()));
    }

    @Test
    void multipleAliasesAllResolveToTheSameCanonical() {
        TestShape first = new TestShape("amod", "gear");
        TestShape second = new TestShape("bmod", "gear");
        TestShape third = new TestShape("cmod", "gear");
        unification.register(first);

        assertSame(first, unification.register(second));
        assertSame(first, unification.register(third));
        assertSame(first, unification.canonical(second));
        assertSame(first, unification.canonical(third));
        assertFalse(unification.isCanonical(second));
        assertFalse(unification.isCanonical(third));
        assertEquals(List.of(first), new ArrayList<>(unification.canonicalShapes()));
    }
}
