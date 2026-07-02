package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Map;

import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/// Holds the shape consumers registered during preInit and dispatches each one over its targeted shape's
/// materials once shapes have resolved.
///
/// Consumers target shapes by name rather than by [Shape] reference: requiring a reference would force a
/// consuming mod to declare its own same-name shape candidate just to obtain a handle, which would make it an
/// ownership-election candidate as a side effect. The name resolves to the canonical shape at dispatch, and a
/// name no mod registered is skipped with a warning so a consumer may target a shape from an optional mod.
/// Dispatch runs once, from [ShapeRegistry#runConsumers].
final class ShapeConsumers {

    private final List<Registration> registrations = new ObjectArrayList<>();
    private boolean ran;

    private record Registration(String modid, String shapeName, ShapeConsumer consumer) {}

    /// Records a consumer to invoke for every material generating the shape registered under `shapeName`.
    void register(String modid, String shapeName, ShapeConsumer consumer) {
        if (ran) {
            throw new IllegalStateException("Cannot register a shape consumer: consumers have already run");
        }
        Names.validate("shape consumer modid", modid);
        Names.validate("shape consumer shape name", shapeName);
        if (consumer == null) {
            throw new IllegalArgumentException("shape consumer must not be null");
        }
        registrations.add(new Registration(modid, shapeName, consumer));
    }

    /// Invokes every consumer once per material generating its targeted shape, resolved through `shapesByName`.
    /// Runs once; rethrows a consumer's failure wrapped with the registering modid, shape, and material.
    void run(Map<String, ServedShape> shapesByName) {
        if (ran) {
            throw new IllegalStateException("Cannot run shape consumers: they have already run");
        }
        ran = true;
        int dispatched = 0;
        int pairs = 0;
        for (Registration registration : registrations) {
            ServedShape shape = shapesByName.get(registration.shapeName());
            if (shape == null) {
                MaterialLib.LOG.warn(
                    "Skipping shape consumer from {} targeting {}: no such shape is registered",
                    registration.modid(),
                    registration.shapeName());
                continue;
            }
            dispatched++;
            for (Material material : shape.getServedMaterials()) {
                try {
                    registration.consumer()
                        .consume(shape, material);
                }
                catch (RuntimeException e) {
                    throw new IllegalStateException(
                        "Failed to run shape consumer from " + registration.modid() + " on shape " + shape +
                            " and material " + material.getKey(),
                        e);
                }
                pairs++;
            }
        }
        MaterialLib.LOG.info("Ran {} shape consumers over {} shape-material pairs", dispatched, pairs);
    }
}
