package com.ruling_0.materiallib.api;

import java.util.Locale;
import java.util.Set;

/// Validates the per-material fluid names a [FluidNamer] produces, enforcing the contract documented there.
final class FluidNaming {

    private FluidNaming() {}

    /// Validates `name`, produced by a [FluidNamer] for `shape` and `material`, and records it into `usedNames` so
    /// a later duplicate within the same resolve is rejected. Returns `name` unchanged. Rejects a null or empty
    /// name, one that is not already lowercase, or one already present in `usedNames`.
    static String validate(String name, Shape shape, Material material, Set<String> usedNames) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException(
                "Fluid namer for " + shape + " and " + material.getKey() + " returned a null or empty name");
        }
        if (!name.equals(name.toLowerCase(Locale.ENGLISH))) {
            throw new IllegalArgumentException(
                "Fluid namer for " + shape + " and " + material.getKey() + " returned \"" + name +
                    "\", which is not lowercase; Forge would lowercase it silently instead of rejecting it");
        }
        if (!usedNames.add(name)) {
            throw new IllegalStateException(
                "Fluid name \"" + name + "\" for " + shape + " and " + material.getKey() +
                    " was already used by another fluid shape or material this resolve");
        }
        return name;
    }
}
