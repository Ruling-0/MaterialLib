package com.ruling_0.materiallib.api;

/// A callback invoked once per (shape, material) pair for the shape it targets, after every mod has finished
/// registration.
///
/// Consumers let a mod generate recipes or machine processes covering every material that takes a shape,
/// including materials and shapes registered by other mods. Register from inside the mod's
/// [MaterialRegistrationEvent] handler, through [MaterialLibAPI#registerShapeConsumer] for dispatch during
/// MaterialLib's init (recipes), or [MaterialLibAPI#registerPostInitShapeConsumer] for dispatch during
/// MaterialLib's postInit (content that must observe every mod's init). A consumer receives the canonical
/// shape and the unified material and derives stacks through [MaterialLibAPI#getStack] or
/// [MaterialLibAPI#getFluidStack]. Within a phase, consumers run in registration order, and each sees a
/// shape's materials ascending by material index. A consumer that throws aborts the game load, because a
/// missing recipe discovered in a running world is harder to diagnose than a crash at load.
@FunctionalInterface
public interface ShapeConsumer {

    /// Generates content for one material in one shape.
    void consume(Shape shape, Material material);
}
