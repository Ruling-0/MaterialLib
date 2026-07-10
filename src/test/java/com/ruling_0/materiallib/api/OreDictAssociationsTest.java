package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.oredict.OreDictionary;

import org.junit.jupiter.api.Test;

class OreDictAssociationsTest {

    private final Item mlItem = new Item();
    private final Item foreignItem = new Item();
    private final ItemStack canonicalStack = new ItemStack(mlItem, 1, 0);

    private static OreDictAssociations enabled() {
        return new OreDictAssociations(true, Set.of(), Set.of());
    }

    @Test
    void registeredCanonicalIsResolvableAndReportsCanonical() {
        OreDictAssociations associations = enabled();
        associations.registerCanonical("ingotTestIron", canonicalStack);

        ItemStack resolved = associations.resolveOreDict("ingotTestIron");
        assertEquals(mlItem, resolved.getItem());
        assertEquals(1, resolved.stackSize);
        assertTrue(associations.isCanonicalName("ingotTestIron"));
        assertTrue(associations.isCanonical(canonicalStack));
    }

    @Test
    void anUnclaimedNameResolvesToNullAndIsNotCanonical() {
        OreDictAssociations associations = enabled();

        assertNull(associations.resolveOreDict("ingotTestIron"));
        assertFalse(associations.isCanonicalName("ingotTestIron"));
    }

    @Test
    void anExcludedNameIsNeverClaimedAsCanonical() {
        OreDictAssociations associations = new OreDictAssociations(true, Set.of("ingotTestIron"), Set.of());
        associations.registerCanonical("ingotTestIron", canonicalStack);

        assertNull(associations.resolveOreDict("ingotTestIron"));
        assertFalse(associations.isCanonicalName("ingotTestIron"));
    }

    @Test
    void disablingUnificationSuppressesEveryQuery() {
        OreDictAssociations associations = new OreDictAssociations(false, Set.of(), Set.of());
        associations.registerCanonical("ingotTestIron", canonicalStack);
        associations.associate("ingotTestIron", "foreignmod", new ItemStack(foreignItem, 1, 0));

        assertNull(associations.resolveOreDict("ingotTestIron"));
        assertFalse(associations.isCanonicalName("ingotTestIron"));
        assertFalse(associations.isCanonical(canonicalStack));
        ItemStack foreign = new ItemStack(foreignItem, 1, 0);
        assertSame(foreign, associations.unify(foreign));
    }

    @Test
    void eventDrivenRegistrationAfterCanonicalIsClaimedUnifiesImmediately() {
        OreDictAssociations associations = enabled();
        associations.registerCanonical("ingotTestIron", canonicalStack);
        ItemStack foreign = new ItemStack(foreignItem, 5, 0);

        associations.associate("ingotTestIron", "foreignmod", foreign);
        ItemStack unified = associations.unify(foreign);

        assertEquals(mlItem, unified.getItem());
        assertEquals(5, unified.stackSize);
    }

    @Test
    void catchUpRegistrationBeforeCanonicalIsClaimedIsBufferedAndReplayed() {
        OreDictAssociations associations = enabled();
        ItemStack foreign = new ItemStack(foreignItem, 7, 0);

        associations.associate("ingotTestIron", "foreignmod", foreign);
        associations.registerCanonical("ingotTestIron", canonicalStack);
        ItemStack unified = associations.unify(foreign);

        assertEquals(mlItem, unified.getItem());
        assertEquals(7, unified.stackSize);
    }

    @Test
    void aBufferedEntryForANameNeverClaimedIsSimplyNeverUsed() {
        OreDictAssociations associations = enabled();
        ItemStack foreign = new ItemStack(foreignItem, 1, 0);

        associations.associate("ingotGhost", "foreignmod", foreign);

        assertFalse(associations.isCanonicalName("ingotGhost"));
        assertSame(foreign, associations.unify(foreign));
    }

    @Test
    void unifyingAStackWithNoAssociationReturnsItUnchanged() {
        OreDictAssociations associations = enabled();
        associations.registerCanonical("ingotTestIron", canonicalStack);
        ItemStack unrelated = new ItemStack(foreignItem, 1, 0);

        assertSame(unrelated, associations.unify(unrelated));
    }

    @Test
    void unifyingTheCanonicalStackItselfReturnsItUnchanged() {
        OreDictAssociations associations = enabled();
        associations.registerCanonical("ingotTestIron", canonicalStack);

        assertSame(canonicalStack, associations.unify(canonicalStack));
    }

    @Test
    void aModRegisteringMaterialLibsOwnStackUnderItsOwnNameIsNotTreatedAsAForeignAssociation() {
        OreDictAssociations associations = enabled();
        associations.registerCanonical("ingotTestIron", canonicalStack);

        associations.associate("ingotTestIron", "someothermod", new ItemStack(mlItem, 1, 0));

        ItemStack probe = new ItemStack(mlItem, 1, 0);
        assertSame(probe, associations.unify(probe));
    }

    @Test
    void anExcludedModIdKeepsItsItemCanonicalEvenUnderAClaimedName() {
        OreDictAssociations associations = new OreDictAssociations(true, Set.of(), Set.of("excludedmod"));
        associations.registerCanonical("ingotTestIron", canonicalStack);
        ItemStack foreign = new ItemStack(foreignItem, 1, 0);

        associations.associate("ingotTestIron", "excludedmod", foreign);

        assertSame(foreign, associations.unify(foreign));
    }

    @Test
    void aWildcardDamageForeignRegistrationUnifiesEveryDamageValueOfThatItem() {
        OreDictAssociations associations = enabled();
        associations.registerCanonical("ingotTestIron", canonicalStack);
        ItemStack wildcardRegistration = new ItemStack(foreignItem, 1, OreDictionary.WILDCARD_VALUE);

        associations.associate("ingotTestIron", "foreignmod", wildcardRegistration);
        ItemStack specificDamage = new ItemStack(foreignItem, 2, 3);
        ItemStack unified = associations.unify(specificDamage);

        assertEquals(mlItem, unified.getItem());
        assertEquals(2, unified.stackSize);
    }

    @Test
    void unifyCopiesNbtRatherThanSharingTheInputsReference() {
        OreDictAssociations associations = enabled();
        associations.registerCanonical("ingotTestIron", canonicalStack);
        ItemStack foreign = new ItemStack(foreignItem, 1, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("marker", "original");
        foreign.setTagCompound(tag);
        associations.associate("ingotTestIron", "foreignmod", foreign);

        ItemStack unified = associations.unify(foreign);
        unified.getTagCompound()
            .setString("marker", "mutated");

        assertEquals("original", foreign.getTagCompound()
            .getString("marker"));
    }

    @Test
    void unrelatedNamesDoNotCrossContaminateAssociations() {
        OreDictAssociations associations = enabled();
        Item otherMlItem = new Item();
        ItemStack otherCanonical = new ItemStack(otherMlItem, 1, 0);
        associations.registerCanonical("ingotTestIron", canonicalStack);
        associations.registerCanonical("ingotTestGold", otherCanonical);
        ItemStack foreignIron = new ItemStack(foreignItem, 1, 0);
        Item foreignGoldItem = new Item();
        ItemStack foreignGold = new ItemStack(foreignGoldItem, 1, 0);

        associations.associate("ingotTestIron", "foreignmod", foreignIron);
        associations.associate("ingotTestGold", "foreignmod", foreignGold);

        assertEquals(mlItem, associations.unify(foreignIron)
            .getItem());
        assertEquals(otherMlItem, associations.unify(foreignGold)
            .getItem());
    }
}
