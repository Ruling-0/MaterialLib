package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class MaterialUnificationTest {

    private static final Property<Integer> MELTING_POINT = Property.of("testmod", "meltingPoint");
    private static final Property<Integer> PROPERTY_A = Property.of("testmod", "propertyA");
    private static final Property<Integer> PROPERTY_B = Property.of("testmod", "propertyB");

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet ownerTexture = TextureSet.of("amod", "shiny");
    private final TextureSet otherTexture = TextureSet.of("bmod", "dull");

    @Test
    void sameNameMaterialsFromDifferentModsUnifyIntoOne() {
        registry.newMaterial("amod", "testiron", ownerTexture)
            .build();
        registry.newMaterial("bmod", "testiron", otherTexture)
            .build();
        registry.resolve();

        assertEquals(1, registry.getMaterials().size());
        assertSame(registry.getMaterial("amod", "testiron"), registry.getMaterial("bmod", "testiron"));
    }

    @Test
    void theAlphabeticallyFirstModidOwnsAContestedNameRegardlessOfOrder() {
        registry.newMaterial("bmod", "testiron", otherTexture)
            .build();
        registry.newMaterial("amod", "testiron", ownerTexture)
            .build();
        registry.resolve();

        Material merged = registry.getMaterial("amod", "testiron");
        assertEquals("amod", merged.getModId());
        assertEquals("amod:testiron", merged.getKey());
        assertEquals(Map.of("testiron", "amod"), registry.getAssignedOwners());
    }

    @Test
    void aPersistedOwnerWinsOverTheAlphabeticalDefault() {
        registry.setPersistedOwners(Map.of("testiron", "bmod"));
        registry.newMaterial("amod", "testiron", ownerTexture)
            .build();
        registry.newMaterial("bmod", "testiron", otherTexture)
            .build();
        registry.resolve();

        assertEquals("bmod", registry.getMaterial("amod", "testiron")
            .getModId());
    }

    @Test
    void aPersistedOwnerAbsentThisSessionFallsBackToTheAlphabeticalDefault() {
        registry.setPersistedOwners(Map.of("testiron", "zmod"));
        registry.newMaterial("bmod", "testiron", otherTexture)
            .build();
        registry.newMaterial("amod", "testiron", ownerTexture)
            .build();
        registry.resolve();

        assertEquals("amod", registry.getMaterial("amod", "testiron")
            .getModId());
        assertEquals("amod", registry.getAssignedOwners()
            .get("testiron"));
    }

    @Test
    void aPersistedNameWithNoCandidateKeepsItsOwner() {
        registry.setPersistedOwners(Map.of("ghost", "cmod"));
        registry.newMaterial("amod", "testiron", ownerTexture)
            .build();
        registry.resolve();

        assertEquals("cmod", registry.getAssignedOwners()
            .get("ghost"));
    }

    @Test
    void everyMaterialNameRecordsAnOwner() {
        registry.newMaterial("amod", "testiron", ownerTexture)
            .build();
        registry.resolve();

        assertEquals(Map.of("testiron", "amod"), registry.getAssignedOwners());
    }

    @Test
    void aMergedMaterialCarriesTheUnionOfDeclarations() {
        TestShape ownerShape = new TestShape("amod", "gear");
        TestShape otherShape = new TestShape("bmod", "plate");
        Family alloys = registry.newFamily("amod", "Alloys")
            .build();
        Family heavy = registry.newFamily("bmod", "Heavy")
            .build();
        registry.newMaterial("amod", "testiron", ownerTexture)
            .generateShape(ownerShape)
            .setProperty(PROPERTY_A, 1)
            .addToFamily("amod", "Alloys")
            .addTooltip("owner line")
            .build();
        registry.newMaterial("bmod", "testiron", otherTexture)
            .generateShape(otherShape)
            .setProperty(PROPERTY_B, 2)
            .addToFamily("bmod", "Heavy")
            .addTooltip("loser line")
            .build();
        registry.resolve();

        Material merged = registry.getMaterial("amod", "testiron");
        assertEquals(Set.of(ownerShape, otherShape), merged.getShapes());
        assertEquals(1, merged.getProperty(PROPERTY_A));
        assertEquals(2, merged.getProperty(PROPERTY_B));
        assertEquals(Set.of(alloys, heavy), merged.getFamilies());
        assertEquals(List.of("owner line", "loser line"), merged.getTooltip());
    }

    @Test
    void conflictingPropertyValuesResolveToTheOwner() {
        registry.newMaterial("amod", "testiron", ownerTexture)
            .setProperty(MELTING_POINT, 1000)
            .build();
        registry.newMaterial("bmod", "testiron", otherTexture)
            .setProperty(MELTING_POINT, 2000)
            .build();
        registry.resolve();

        Material merged = registry.getMaterial("amod", "testiron");
        assertEquals(1000, merged.getProperty(MELTING_POINT));
        assertEquals(ownerTexture, merged.getProperty(StandardProperties.TEXTURE_SET));
    }

    @Test
    void tooltipLinesAppendOwnerFirstThenLosersInModidOrder() {
        registry.newMaterial("cmod", "testiron", TextureSet.of("cmod", "c"))
            .addTooltip("c-line")
            .build();
        registry.newMaterial("bmod", "testiron", TextureSet.of("bmod", "b"))
            .addTooltip("b-line")
            .build();
        registry.newMaterial("amod", "testiron", ownerTexture)
            .addTooltip("a-line")
            .build();
        registry.resolve();

        Material merged = registry.getMaterial("amod", "testiron");
        assertEquals(List.of("a-line", "b-line", "c-line"), merged.getTooltip());
    }

    @Test
    void anEditToTheLoserKeyOverridesTheOwnersDeclaredValue() {
        registry.newMaterial("amod", "testiron", ownerTexture)
            .setProperty(MELTING_POINT, 1000)
            .build();
        registry.newMaterial("bmod", "testiron", otherTexture)
            .build();
        registry.editMaterial("bmod", "testiron")
            .setProperty(MELTING_POINT, 2000);
        registry.resolve();

        assertEquals(
            2000,
            registry.getMaterial("amod", "testiron")
                .getProperty(MELTING_POINT));
    }

    @Test
    void editsToBothConstituentKeysApplyInCallOrder() {
        registry.newMaterial("amod", "testiron", ownerTexture)
            .build();
        registry.newMaterial("bmod", "testiron", otherTexture)
            .build();
        registry.editMaterial("amod", "testiron")
            .setProperty(MELTING_POINT, 1000);
        registry.editMaterial("bmod", "testiron")
            .setProperty(MELTING_POINT, 2000);
        registry.resolve();

        assertEquals(
            2000,
            registry.getMaterial("amod", "testiron")
                .getProperty(MELTING_POINT));
    }

    @Test
    void aStaleLoserReferenceReadsThroughToTheMergedMaterial() {
        Material winner = registry.newMaterial("amod", "testiron", ownerTexture)
            .setProperty(MELTING_POINT, 1000)
            .build();
        Material loser = registry.newMaterial("bmod", "testiron", otherTexture)
            .build();
        registry.resolve();

        assertEquals(winner.getModId(), loser.getModId());
        assertEquals(winner.getKey(), loser.getKey());
        assertEquals(winner.getIndex(), loser.getIndex());
        assertEquals(winner.getShapes(), loser.getShapes());
        assertEquals(winner.getProperty(MELTING_POINT), loser.getProperty(MELTING_POINT));
        assertEquals(winner.getTooltip(), loser.getTooltip());
        assertSame(winner, loser.canonical());
    }
}
