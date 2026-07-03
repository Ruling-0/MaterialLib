package com.ruling_0.materiallib.api;

/// A callback invoked once per (shape, material) pair for the shape it targets, after every mod has finished
/// registration.
///
/// Consumers allow for automatically generating recipes or other processes simply over an entire shape set, with
/// per-material granularity. For example, a consumer could target `gear`, and would run over all `gear`+material
/// pairs. For each pair, it could create a recipe turning an `ingot` into a `gear`, checking if `ingot` exists and
/// using the properties on the material.
///
/// Register from inside the mod's [MaterialRegistrationEvent] handler, through [MaterialLibAPI#registerShapeConsumer]
/// for dispatch during MaterialLib's init, or [MaterialLibAPI#registerPostInitShapeConsumer] for dispatch during
/// MaterialLib's postInit.
@FunctionalInterface
public interface ShapeConsumer {

    /// Generates content for one material in one shape.
    void consume(Shape shape, Material material);
}
