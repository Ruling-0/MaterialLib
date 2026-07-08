package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmptyContainerTest {

    @Test
    void deferredResolveFailsLoudlyWhenNoItemIsFound() {
        EmptyContainer.Deferred deferred = new EmptyContainer.Deferred(
            "gregtech", "gt.metaitem.01", 32000, (modid, name) -> null);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, deferred::resolve);

        assertTrue(thrown.getMessage()
            .contains("gregtech:gt.metaitem.01"));
    }
}
