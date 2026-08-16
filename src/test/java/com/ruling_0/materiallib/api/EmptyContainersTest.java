package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class EmptyContainersTest {

    private final EmptyContainers containers = new EmptyContainers();

    @Test
    void aSingleCandidateOwnsItsName() {
        EmptyContainerHandle cell = containers.register("amod", "cellEmpty", null);

        assertSame(cell, containers.chooseOwners().get("cellEmpty"));
    }

    @Test
    void theAlphabeticallyFirstModidOwnsAContestedNameRegardlessOfOrder() {
        EmptyContainerHandle bmod = containers.register("bmod", "cellEmpty", null);
        EmptyContainerHandle amod = containers.register("amod", "cellEmpty", null);

        assertSame(amod, containers.chooseOwners().get("cellEmpty"));
        assertEquals(List.of(bmod, amod), containers.candidatesOf("cellEmpty"));
    }

    @Test
    void chooseOwnersFollowsTheOrderNamesWereFirstRegistered() {
        containers.register("bmod", "cellEmpty", null);
        containers.register("amod", "canEmpty", null);
        containers.register("amod", "cellEmpty", null);

        assertEquals(List.of("cellEmpty", "canEmpty"), new ArrayList<>(containers.chooseOwners().keySet()));
    }

    @Test
    void registeringAfterResolveFails() {
        containers.chooseOwners();

        assertThrows(IllegalStateException.class, () -> containers.register("amod", "cellEmpty", null));
    }

    @Test
    void anEmptyIconPathIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> containers.register("amod", "cellEmpty", ""));
    }

    @Test
    void buildingAStackBeforeResolveFails() {
        EmptyContainerHandle cell = containers.register("amod", "cellEmpty", null);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> cell.getStack(1));

        assertTrue(thrown.getMessage().contains("amod:cellEmpty"));
    }

    @Test
    void theIconPathDefaultsToTheOwnersMaterialsFolder() {
        assertEquals("amod:materials/cellEmpty", containers.register("amod", "cellEmpty", null).iconPathOrDefault());
        assertEquals("bmod:items/can", containers.register("amod", "canEmpty", "bmod:items/can").iconPathOrDefault());
    }

    @Test
    void anEmptyContainerCannotReuseAShapeName() {
        ShapeRegistry registry = new ShapeRegistry();
        registry.register(new ShapeFluid("amod", "cellEmpty", "Liquid %s"));

        assertThrows(IllegalStateException.class, () -> registry.registerEmptyContainer("amod", "cellEmpty", null));
    }
}
