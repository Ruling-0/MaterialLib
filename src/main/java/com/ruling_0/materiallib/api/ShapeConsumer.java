package com.ruling_0.materiallib.api;

/// A callback invoked once per (shape, material) pair for the shape it targets, after every mod has finished
/// registration.
///
/// Consumers let a mod generate recipes or machine processes covering every material that takes a shape,
/// including materials and shapes registered by other mods. Register through
/// [MaterialLibAPI#registerShapeConsumer] during preInit; MaterialLib dispatches during its postInit handler,
/// once all mods' init handlers have registered their items, blocks, and fluids. A consumer receives the
/// canonical shape and the unified material and derives stacks through [MaterialLibAPI#getStack] or
/// [MaterialLibAPI#getFluidStack]. Consumers run in registration order, and each sees a shape's materials
/// ascending by material index. A consumer that throws aborts the game load, because a missing recipe
/// discovered in a running world is harder to diagnose than a crash at load.
@FunctionalInterface
public interface ShapeConsumer {

    /// Generates content for one material in one shape.
    void consume(Shape shape, Material material);
}
