package com.ruling_0.materiallib.api;

import java.util.Locale;
import java.util.Set;

/// Validates the per-material fluid names a [FluidNamer] produces, enforcing the contract documented there.
final class FluidNaming {

    private FluidNaming() {}

    /// Validates `name`, produced by a [FluidNamer] for `shape` and `material`, records it into `usedNames`, and
    /// returns it. Rejects a null or empty name, one that is not lowercase, one containing ':', or one already
    /// present in `usedNames`.
    static String validate(String name, Shape shape, Material material, Set<String> usedNames) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException(
                "Fluid namer for " + shape + " and " + material.getKey() + " returned a null or empty name");
        }
        if (!name.equals(name.toLowerCase(Locale.ENGLISH))) {
            throw new IllegalArgumentException(
                "Fluid namer for " + shape + " and " + material.getKey() + " returned \"" + name +
                    "\", which is not lowercase");
        }
        if (name.indexOf(':') >= 0) {
            throw new IllegalArgumentException(
                "Fluid namer for " + shape + " and " + material.getKey() + " returned \"" + name +
                    "\", which contains ':'");
        }
        if (!usedNames.add(name)) {
            throw new IllegalStateException(
                "Fluid name \"" + name + "\" for " + shape + " and " + material.getKey() +
                    " was already used by another fluid shape or material this resolve");
        }
        return name;
    }
}
