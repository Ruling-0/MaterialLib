package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class FamilyMembershipTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    @Test
    void builderAssignsFamilyByObjectAndByKey() {
        Family family = registry.newFamily("testmod", "Alloys")
            .build();
        Material byObject = registry.newMaterial("testmod", "ByObject", texture)
            .addToFamily(family)
            .build();
        Material byKey = registry.newMaterial("testmod", "ByKey", texture)
            .addToFamily("testmod", "Alloys")
            .build();
        registry.resolve();

        assertSame(family, byObject.getFamily());
        assertSame(family, byKey.getFamily());
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

        assertSame(registry.getFamily("testmod", "Alloys"), material.getFamily());
    }

    @Test
    void lastClaimWins() {
        Family first = registry.newFamily("testmod", "First")
            .build();
        Family second = registry.newFamily("testmod", "Second")
            .build();
        Material material = registry.newMaterial("testmod", "Contested", texture)
            .addToFamily(first)
            .build();
        registry.editMaterial("testmod", "Contested")
            .setFamily("testmod", "Second");
        registry.resolve();

        assertSame(second, material.getFamily());
        assertTrue(first.getMaterials().isEmpty());
        assertEquals(Set.of(material), second.getMaterials());
    }

    @Test
    void removeFromFamilyDetaches() {
        Family family = registry.newFamily("testmod", "Alloys")
            .build();
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily(family)
            .build();
        registry.editMaterial("testmod", "TestIron")
            .removeFromFamily();
        registry.resolve();

        assertNull(material.getFamily());
        assertTrue(family.getMaterials().isEmpty());
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

        assertSame(alloys, material.getFamily());
        assertTrue(gems.getMaterials().isEmpty());
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

        assertNull(registry.getMaterial("testmod", "TestIron").getFamily());
        assertTrue(alloys.getMaterials().isEmpty());
    }

    @Test
    void familyEditAddMaterialClaimsMaterial() {
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

        assertSame(alloys, material.getFamily());
        assertEquals(Set.of(material), alloys.getMaterials());
        assertTrue(gems.getMaterials().isEmpty());
    }

    @Test
    void missingFamilyClaimIsSkipped() {
        Material material = registry.newMaterial("testmod", "TestIron", texture)
            .addToFamily("absentmod", "Missing")
            .build();
        registry.resolve();

        assertNull(material.getFamily());
    }
}
