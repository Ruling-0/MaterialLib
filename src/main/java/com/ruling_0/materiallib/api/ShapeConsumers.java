package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Map;

import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/// Holds the registered shape consumers and dispatches each one over its targeted shape's materials once
/// shapes have resolved.
///
/// Consumers target shapes by name rather than by [Shape] reference: requiring a reference would force a
/// consuming mod to declare its own same-name shape candidate just to obtain a handle, which would make it an
/// ownership-election candidate as a side effect. The name resolves to the canonical shape at dispatch, and a
/// name no mod registered is skipped with a warning so a consumer may target a shape from an optional mod.
/// Consumers register into one of two phases, each dispatched once from [ShapeRegistry]: the init phase for
/// recipes, and the postInit phase for content that must observe every mod's init.
final class ShapeConsumers {

    /// The dispatch phases, named for the MaterialLib lifecycle handler that runs them.
    enum Phase {

        INIT("init"),
        POST_INIT("postInit");

        private final String label;

        Phase(String label) {
            this.label = label;
        }
    }

    private final List<Registration> initRegistrations = new ObjectArrayList<>();
    private final List<Registration> postInitRegistrations = new ObjectArrayList<>();
    private boolean initRan;
    private boolean postInitRan;

    private record Registration(String modid, String shapeName, ShapeConsumer consumer) {}

    /// Records a consumer to invoke during `phase` for every material generating the shape registered under
    /// `shapeName`.
    void register(Phase phase, String modid, String shapeName, ShapeConsumer consumer) {
        if (ran(phase)) {
            throw new IllegalStateException(
                "Cannot register a shape consumer: the " + phase.label + " consumers have already run");
        }
        Names.validate("shape consumer modid", modid);
        Names.validate("shape consumer shape name", shapeName);
        if (consumer == null) {
            throw new IllegalArgumentException("shape consumer must not be null");
        }
        registrations(phase).add(new Registration(modid, shapeName, consumer));
    }

    /// Invokes every `phase` consumer once per material generating its targeted shape, resolved through
    /// `shapesByName`.
    void run(Phase phase, Map<String, ServedShape> shapesByName) {
        if (ran(phase)) {
            throw new IllegalStateException(
                "Cannot run the " + phase.label + " shape consumers: they have already run");
        }
        if (phase == Phase.INIT) {
            initRan = true;
        }
        else {
            postInitRan = true;
        }
        int dispatched = 0;
        int pairs = 0;
        for (Registration registration : registrations(phase)) {
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
        MaterialLib.LOG.info(
            "Ran {} {} shape consumers over {} shape-material pairs",
            dispatched,
            phase.label,
            pairs);
    }

    private boolean ran(Phase phase) {
        return phase == Phase.INIT ? initRan : postInitRan;
    }

    private List<Registration> registrations(Phase phase) {
        return phase == Phase.INIT ? initRegistrations : postInitRegistrations;
    }
}
