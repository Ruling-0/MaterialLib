package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class FamilyMembershipTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    @Test
    void builderAddsFamiliesByObjectAndByKey() {
        Family family = registry.newFamily("testmod", "Alloys")
            .build();
        Material byObject = registry.newMaterial("testmod", "ByObject", texture)
            .addToFamily(family)
            .build();
        Material byKey = registry.newMaterial("testmod", "ByKey", texture)
            .addToFamily("testmod", "Alloys")
            .build();
        registry.resolve();

        assertEquals(Set.of(family), byObject.getFamilies());
        assertEquals(Set.of(family), byKey.getFamilies());
        assertEquals(Set.of(byObject, byKey), family.getMaterials());
    }

    @Test
    void familyBuilderClaimsMaterialsRegisteredLater() {
        registry.newFamily("testmod", "Alloys")
            .addMaterial("testmod", "Later")
            .build();
        Material material = registry.newMaterial("testmod", "Later", texture)
            .build();
        registry.resolve();

        assertEquals(Set.of(registry.getFamily("testmod", "Alloys")), material.getFamilies());
    }

    @Test
    void familyBuilderIgnoresUnregisteredMaterial() {
        registry.newFamily("testmod", "Alloys")
            .addMaterial("testmod", "Never")
            .build();
        registry.resolve();

        assertTrue(registry.getFamily("testmod", "Alloys").getMaterials().isEmpty());
    }

    @Test
    void builderAccumulatesMultipleFamilies() {
        Family alloys = registry.newFamily("testmod", "Alloys")
            .build();
        Family gems = registry.newFamily("testmod", "Gems")
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily(alloys)
            .addToFamily(gems)
            .build();
        registry.resolve();

        assertEquals(Set.of(alloys, gems), material.getFamilies());
        assertEquals(Set.of(material), alloys.getMaterials());
        assertEquals(Set.of(material), gems.getMaterials());
    }

    @Test
    void getFamiliesIteratesInKeyOrder() {
        Family zmodAaa = registry.newFamily("zmod", "Aaa")
            .build();
        Family amodZzz = registry.newFamily("amod", "Zzz")
            .build();
        Family amodAaa = registry.newFamily("amod", "Aaa")
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily(zmodAaa)
            .addToFamily(amodZzz)
            .addToFamily(amodAaa)
            .build();
        registry.resolve();

        assertEquals(List.of(amodAaa, amodZzz, zmodAaa), new ArrayList<>(material.getFamilies()));
    }

    @Test
    void membershipsAccumulateAcrossMods() {
        Family first = registry.newFamily("testmod", "First")
            .build();
        Family second = registry.newFamily("othermod", "Second")
            .build();
        Material material = registry.newMaterial("testmod", "Shared", texture)
            .addToFamily(first)
            .build();
        registry.editMaterial("testmod", "Shared")
            .addToFamily("othermod", "Second");
        registry.resolve();

        assertEquals(Set.of(first, second), material.getFamilies());
        assertEquals(Set.of(material), first.getMaterials());
        assertEquals(Set.of(material), second.getMaterials());
    }

    @Test
    void familyEditAddMaterialAddsMembership() {
        Family alloys = registry.newFamily("testmod", "Alloys")
            .build();
        Family gems = registry.newFamily("testmod", "Gems")
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily(gems)
            .build();
        registry.editFamily("testmod", "Alloys")
            .addMaterial("testmod", "TestIron");
        registry.resolve();

        assertEquals(Set.of(alloys, gems), material.getFamilies());
        assertEquals(Set.of(material), alloys.getMaterials());
        assertEquals(Set.of(material), gems.getMaterials());
    }

    @Test
    void removeFromFamilyDetachesOnlyThatFamily() {
        Family alloys = registry.newFamily("testmod", "Alloys")
            .build();
        Family gems = registry.newFamily("testmod", "Gems")
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily(alloys)
            .addToFamily(gems)
            .build();
        registry.editMaterial("testmod", "TestIron")
            .removeFromFamily("testmod", "Alloys");
        registry.resolve();

        assertEquals(Set.of(gems), material.getFamilies());
        assertTrue(alloys.getMaterials().isEmpty());
        assertEquals(Set.of(material), gems.getMaterials());
    }

    @Test
    void familyEditRemoveMaterialOnlyAffectsItsOwnMembers() {
        Family alloys = registry.newFamily("testmod", "Alloys")
            .build();
        Family gems = registry.newFamily("testmod", "Gems")
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily(alloys)
            .build();
        registry.editFamily("testmod", "Gems")
            .removeMaterial("testmod", "TestIron");
        registry.resolve();

        assertEquals(Set.of(alloys), material.getFamilies());
        assertTrue(gems.getMaterials().isEmpty());
        assertEquals(Set.of(material), alloys.getMaterials());
    }

    @Test
    void familyEditRemoveMaterialDetachesMember() {
        Family alloys = registry.newFamily("testmod", "Alloys")
            .build();
        registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily(alloys)
            .build();
        registry.editFamily("testmod", "Alloys")
            .removeMaterial("testmod", "TestIron");
        registry.resolve();

        assertTrue(
            registry.getMaterial("testmod", "TestIron")
                .getFamilies()
                .isEmpty());
        assertTrue(alloys.getMaterials().isEmpty());
    }

    @Test
    void removeThenReAddKeepsMembership() {
        Family alloys = registry.newFamily("testmod", "Alloys")
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily(alloys)
            .build();
        registry.editMaterial("testmod", "TestIron")
            .removeFromFamily("testmod", "Alloys")
            .addToFamily("testmod", "Alloys");
        registry.resolve();

        assertEquals(Set.of(alloys), material.getFamilies());
        assertEquals(Set.of(material), alloys.getMaterials());
    }

    @Test
    void removeFromUnregisteredFamilyIsSkipped() {
        Family alloys = registry.newFamily("testmod", "Alloys")
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily(alloys)
            .build();
        registry.editMaterial("testmod", "TestIron")
            .removeFromFamily("absentmod", "Missing");
        registry.resolve();

        assertEquals(Set.of(alloys), material.getFamilies());
    }

    @Test
    void materialAndFamilyEditsShareOneQueue() {
        Family family = registry.newFamily("testmod", "Alloys")
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .build();
        registry.editMaterial("testmod", "TestIron")
            .addToFamily("testmod", "Alloys");
        registry.editFamily("testmod", "Alloys")
            .removeMaterial("testmod", "TestIron");
        registry.resolve();

        assertTrue(material.getFamilies().isEmpty());
        assertTrue(family.getMaterials().isEmpty());
    }

    @Test
    void duplicateClaimsCollapseToSingleMembership() {
        Family family = registry.newFamily("testmod", "Alloys")
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily(family)
            .build();
        registry.editFamily("testmod", "Alloys")
            .addMaterial("testmod", "TestIron");
        registry.editMaterial("testmod", "TestIron")
            .removeFromFamily("testmod", "Alloys");
        registry.resolve();

        assertTrue(material.getFamilies().isEmpty());
        assertTrue(family.getMaterials().isEmpty());
    }

    @Test
    void missingFamilyClaimIsSkipped() {
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily("absentmod", "Missing")
            .build();
        registry.resolve();

        assertTrue(material.getFamilies().isEmpty());
    }
}
