package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ShapeUnificationTest {

    private final ShapeUnification unification = new ShapeUnification();

    private static Map<String, String> noOwners() {
        return new LinkedHashMap<>();
    }

    @Test
    void aSingleCandidateOwnsItsName() {
        TestShape gear = new TestShape("amod", "gear");
        unification.register(gear);

        Map<String, String> owners = unification.resolve(noOwners());

        assertSame(gear, unification.canonical(gear));
        assertEquals(Map.of("gear", "amod"), owners);
    }

    @Test
    void theAlphabeticallyFirstModidOwnsAContestedNameRegardlessOfOrder() {
        TestShape bmod = new TestShape("bmod", "gear");
        TestShape amod = new TestShape("amod", "gear");
        unification.register(bmod);
        unification.register(amod);

        Map<String, String> owners = unification.resolve(noOwners());

        assertSame(amod, unification.canonical(amod));
        assertSame(amod, unification.canonical(bmod));
        assertEquals("amod", owners.get("gear"));
    }

    @Test
    void aPersistedOwnerWinsOverTheAlphabeticalDefault() {
        TestShape amod = new TestShape("amod", "gear");
        TestShape bmod = new TestShape("bmod", "gear");
        unification.register(amod);
        unification.register(bmod);

        Map<String, String> owners = unification.resolve(Map.of("gear", "bmod"));

        assertSame(bmod, unification.canonical(bmod));
        assertSame(bmod, unification.canonical(amod));
        assertEquals("bmod", owners.get("gear"));
    }

    @Test
    void aPersistedOwnerAbsentThisSessionFallsBackToTheAlphabeticalDefault() {
        TestShape amod = new TestShape("amod", "gear");
        TestShape bmod = new TestShape("bmod", "gear");
        unification.register(amod);
        unification.register(bmod);

        Map<String, String> owners = unification.resolve(Map.of("gear", "zmod"));

        assertSame(amod, unification.canonical(amod));
        assertEquals("amod", owners.get("gear"));
    }

    @Test
    void aPersistedNameWithNoCandidateKeepsItsOwner() {
        TestShape gear = new TestShape("amod", "gear");
        unification.register(gear);

        Map<String, String> persisted = new LinkedHashMap<>();
        persisted.put("gear", "amod");
        persisted.put("plate", "cmod");
        Map<String, String> owners = unification.resolve(persisted);

        assertEquals("amod", owners.get("gear"));
        assertEquals("cmod", owners.get("plate"));
    }

    @Test
    void canonicalShapesFollowsTheOrderNamesWereFirstRegistered() {
        TestShape plate = new TestShape("amod", "plate");
        TestShape gear = new TestShape("amod", "gear");
        TestShape gearAlias = new TestShape("bmod", "gear");
        TestShape rod = new TestShape("amod", "rod");
        unification.register(plate);
        unification.register(gear);
        unification.register(gearAlias);
        unification.register(rod);

        unification.resolve(noOwners());

        assertEquals(List.of(plate, gear, rod), new ArrayList<>(unification.canonicalShapes()));
    }

    @Test
    void theOwnersOredictPrefixesAreKeptWhenCandidatesDiverge() {
        TestShape amod = new TestShape("amod", "gear", "gear");
        TestShape bmod = new TestShape("bmod", "gear", "wheel");
        unification.register(bmod);
        unification.register(amod);

        unification.resolve(noOwners());

        assertSame(amod, unification.canonical(bmod));
        assertEquals(List.of("gear"), unification.canonical(bmod)
            .getOreDicts());
    }

    @Test
    void registeringTheSameInstanceTwiceRecordsItOnce() {
        TestShape gear = new TestShape("amod", "gear");
        assertSame(gear, unification.register(gear));
        assertSame(gear, unification.register(gear));

        unification.resolve(noOwners());

        assertSame(gear, unification.canonical(gear));
        assertEquals(List.of(gear), new ArrayList<>(unification.canonicalShapes()));
    }

    @Test
    void canonicalOfAnUnregisteredShapeIsItself() {
        unification.resolve(noOwners());
        TestShape gear = new TestShape("amod", "gear");

        assertSame(gear, unification.canonical(gear));
    }

    @Test
    void registeringAfterResolveFails() {
        unification.resolve(noOwners());

        assertThrows(IllegalStateException.class, () -> unification.register(new TestShape("amod", "gear")));
    }

    @Test
    void queryingBeforeResolveFails() {
        TestShape gear = new TestShape("amod", "gear");
        unification.register(gear);

        assertThrows(IllegalStateException.class, () -> unification.canonical(gear));
        assertThrows(IllegalStateException.class, () -> unification.canonicalShapes());
    }
}
