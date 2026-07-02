package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ShapeConsumersTest {

    private final ShapeConsumers consumers = new ShapeConsumers();
    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    private Material material(String name) {
        return registry.newMaterial("testmod", name, texture)
            .build();
    }

    private static TestServedShape shape(String name, Material... materials) {
        TestServedShape shape = new TestServedShape("amod", name);
        shape.bindServedMaterials(materials);
        return shape;
    }

    @Test
    void consumersRunInRegistrationOrderAndMaterialsInBoundOrder() {
        Material iron = material("Iron");
        Material gold = material("Gold");
        TestServedShape gear = shape("gear", iron, gold);
        TestServedShape plate = shape("plate", gold);

        List<String> calls = new ArrayList<>();
        consumers.register("amod", "plate", (s, m) -> calls.add("first " + s.getName() + " " + m.getName()));
        consumers.register("amod", "gear", (s, m) -> calls.add("second " + s.getName() + " " + m.getName()));

        consumers.run(Map.of("gear", gear, "plate", plate));

        assertEquals(List.of("first plate Gold", "second gear Iron", "second gear Gold"), calls);
    }

    @Test
    void anUnknownShapeNameIsSkippedAndLaterConsumersStillRun() {
        Material iron = material("Iron");
        TestServedShape gear = shape("gear", iron);

        List<String> calls = new ArrayList<>();
        consumers.register("amod", "missing", (s, m) -> calls.add("missing " + m.getName()));
        consumers.register("amod", "gear", (s, m) -> calls.add("gear " + m.getName()));

        consumers.run(Map.of("gear", gear));

        assertEquals(List.of("gear Iron"), calls);
    }

    @Test
    void aThrowingConsumerFailsNamingTheModidShapeAndMaterial() {
        Material iron = material("Iron");
        TestServedShape gear = shape("gear", iron);
        RuntimeException boom = new RuntimeException("boom");
        consumers.register("cmod", "gear", (s, m) -> {
            throw boom;
        });

        IllegalStateException e = assertThrows(
            IllegalStateException.class,
            () -> consumers.run(Map.of("gear", gear)));

        assertEquals(
            "Failed to run shape consumer from cmod on shape TestServedShape[amod:gear] and material testmod:Iron",
            e.getMessage());
        assertSame(boom, e.getCause());
    }

    @Test
    void runningTwiceFails() {
        consumers.run(Map.of());

        assertThrows(IllegalStateException.class, () -> consumers.run(Map.of()));
    }

    @Test
    void registeringAfterTheRunFails() {
        consumers.run(Map.of());

        assertThrows(IllegalStateException.class, () -> consumers.register("cmod", "gear", (s, m) -> {}));
    }

    @Test
    void anInvalidRegistrationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> consumers.register("cmod", "gear", null));
        assertThrows(IllegalArgumentException.class, () -> consumers.register(null, "gear", (s, m) -> {}));
        assertThrows(IllegalArgumentException.class, () -> consumers.register("cmod", "bad name", (s, m) -> {}));
    }
}
