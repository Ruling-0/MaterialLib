package com.ruling_0.materiallib.api;

import java.util.Locale;

/// Names the Forge fluid MaterialLib registers for one material in one fluid [Shape].
///
/// Set through [FluidShapeBuilder#fluidName], invoked once per served material at resolve, immediately before that
/// material's fluid is registered or reused. The returned name must be non-null, non-empty, lowercase (Forge
/// lowercases silently instead of rejecting), and unique across every fluid shape resolving this session. A name
/// already present in Forge's fluid registry is reused rather than re-skinned, the same as when no namer is set.
@FunctionalInterface
public interface FluidNamer {

    /// The default namer: the shape and material names, joined by '.' and lowercased, e.g. `molten.testiron`.
    FluidNamer DEFAULT = (shape, material) -> (shape.getName() + "." + material.getName())
        .toLowerCase(Locale.ENGLISH);

    /// Returns the Forge fluid name for `material` in `shape`.
    String name(Shape shape, Material material);
}
