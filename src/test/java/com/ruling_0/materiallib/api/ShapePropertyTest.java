package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/// Pins the shape-property contract: identity-keyed lookup with a default fallback, owner-wins merging when two
/// mods declare the same shape name, and the resolve-time ordering that lets an edit override a declaration.
class ShapePropertyTest {

    private static final Property<Long> MATERIAL_AMOUNT = Property.of("testmod", "materialAmount");
    private static final Property<Integer> STACK_SIZE = Property.of("testmod", "stackSize", 64);

    private final ShapeUnification unification = new ShapeUnification();

    private static Map<String, String> noOwners() {
        return new LinkedHashMap<>();
    }

    private TestShape register(String modid, String name) {
        TestShape shape = new TestShape(modid, name);
        unification.register(shape);
        return shape;
    }

    @Test
    void anUnsetPropertyFallsBackToItsDefault() {
        TestShape gear = new TestShape("amod", "gear");
        gear.properties()
            .set(gear, MATERIAL_AMOUNT, 3628800L);

        assertEquals(3628800L, gear.getProperty(MATERIAL_AMOUNT));
        assertEquals(64, gear.getProperty(STACK_SIZE));
        assertTrue(gear.hasProperty(MATERIAL_AMOUNT));
        assertFalse(gear.hasProperty(STACK_SIZE));
    }

    /// A second [Property] with the same modid and name is a distinct key, matching materials; see
    /// [PropertyResolutionTest#propertyKeysCompareByIdentity].
    @Test
    void propertyKeysCompareByIdentity() {
        Property<Long> other = Property.of("testmod", "materialAmount");
        TestShape gear = new TestShape("amod", "gear");
        gear.properties()
            .set(gear, MATERIAL_AMOUNT, 1L);

        assertEquals(1L, gear.getProperty(MATERIAL_AMOUNT));
        assertNull(gear.getProperty(other));
    }

    /// The owner keeps its own value and adopts the ones only the merged-away declaration set, exactly as
    /// [Material#mergeFrom] does for materials.
    @Test
    void unificationKeepsTheOwnersValueAndAdoptsTheRest() {
        TestShape amod = register("amod", "gear");
        TestShape bmod = register("bmod", "gear");
        amod.properties()
            .set(amod, MATERIAL_AMOUNT, 1L);
        bmod.properties()
            .set(bmod, MATERIAL_AMOUNT, 2L);
        bmod.properties()
            .set(bmod, STACK_SIZE, 16);

        unification.resolve(noOwners());
        unification.mergeProperties();

        assertSame(amod, unification.canonical(bmod));
        assertEquals(1L, amod.getProperty(MATERIAL_AMOUNT));
        assertEquals(16, amod.getProperty(STACK_SIZE));
    }

    /// With several merged-away declarations of one name, losers fold in in modid order, so the resolved value
    /// does not depend on hash iteration.
    @Test
    void losersMergeInModidOrder() {
        TestShape owner = register("amod", "gear");
        TestShape cmod = register("cmod", "gear");
        TestShape bmod = register("bmod", "gear");
        cmod.properties().set(cmod, MATERIAL_AMOUNT, 2L);
        bmod.properties().set(bmod, MATERIAL_AMOUNT, 1L);

        unification.resolve(noOwners());
        unification.mergeProperties();

        assertEquals(1L, owner.getProperty(MATERIAL_AMOUNT));
    }

    /// The reference a mod kept from declaring the losing shape still reads the owner's values, which is what the
    /// holder's redirect buys: materials get this from their own `canonical` field, shapes cannot.
    @Test
    void aMergedAwayShapeReadsThroughToTheOwner() {
        TestShape amod = register("amod", "gear");
        TestShape bmod = register("bmod", "gear");
        amod.properties()
            .set(amod, MATERIAL_AMOUNT, 1L);

        unification.resolve(noOwners());
        unification.mergeProperties();

        assertEquals(1L, bmod.getProperty(MATERIAL_AMOUNT));
    }

    @Test
    void aFrozenShapeRejectsFurtherWrites() {
        TestShape gear = new TestShape("amod", "gear");
        gear.properties()
            .freeze();

        assertThrows(IllegalStateException.class, () -> gear.properties()
            .set(gear, MATERIAL_AMOUNT, 1L));
    }

    /// The mechanism [ShapeBlockVariants] uses to make its per-variant blocks -- separate [Shape] instances that
    /// [ShapeRegistry#getBlockShapes] hands out -- read the group's values. Exercised on the holder directly
    /// because constructing the backing blocks needs a bootstrapped Minecraft.
    @Test
    void aRedirectedHolderReadsAndWritesThroughToItsTarget() {
        TestShape group = new TestShape("amod", "ore");
        TestShape variant = new TestShape("amod", "ore_stone");
        variant.properties()
            .redirectTo(group.properties());

        group.properties()
            .set(group, MATERIAL_AMOUNT, 3628800L);
        assertEquals(3628800L, variant.getProperty(MATERIAL_AMOUNT));

        variant.properties()
            .set(variant, STACK_SIZE, 16);
        assertEquals(16, group.getProperty(STACK_SIZE));
    }

    /// The owner-facing setter, for a shape registered as a subclass rather than through a builder. Unlike
    /// [MaterialLibAPI#editShape] it writes to the shape in hand, so it declares rather than overrides.
    @Test
    void theOwnerCanSetPropertiesOnAShapeItHolds() {
        TestShape gear = new TestShape("amod", "gear");

        gear.setProperty(MATERIAL_AMOUNT, 3628800L)
            .setProperty(STACK_SIZE, 16);

        assertEquals(3628800L, gear.getProperty(MATERIAL_AMOUNT));
        assertEquals(16, gear.getProperty(STACK_SIZE));
    }
}
