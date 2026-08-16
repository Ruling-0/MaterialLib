package com.ruling_0.materiallib.api;

import java.util.Objects;
import java.util.function.Supplier;

/// A lazily-resolved reference to a [Material], written as a lambda over the field holding it, e.g.
/// `MaterialRef.of(() -> Materials.Wood)`.
///
/// Creating a reference is always safe, resolving one is not: material fields are assigned while
/// [MaterialRegistrationEvent] handlers run and handler order across mods is unspecified, so a reference resolved
/// inside a handler may read a field the owning mod has not filled in yet. Resolve no earlier than preInit, once
/// the registry has resolved.
///
/// The lambda keeps a reference compile-checked: renaming or deleting the target field breaks the build.
///
/// References compare by identity.
public final class MaterialRef {

    private final Supplier<Material> supplier;

    private MaterialRef(Supplier<Material> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "supplier must not be null");
    }

    public static MaterialRef of(Supplier<Material> supplier) {
        return new MaterialRef(supplier);
    }

    /// The referenced material, read through the supplier on every call. Fails loudly where the field the
    /// supplier reads is unassigned.
    public Material resolve() {
        Material material = supplier.get();
        if (material == null) {
            throw new IllegalStateException(
                "The referenced material field is not assigned; either its registration was removed while " +
                    "references to it remain, or the reference was resolved during material registration. " +
                    "Resolve no earlier than preInit, once the registry has resolved");
        }
        return material;
    }

    /// `MaterialRef[modid:name]`, or `MaterialRef[unresolved]` where the supplier yields null or fails. Never throws.
    @Override
    public String toString() {
        try {
            Material material = supplier.get();
            if (material != null) return "MaterialRef[" + Names.key(material.getModId(), material.getName()) + "]";
        }
        catch (RuntimeException ignored) {}
        return "MaterialRef[unresolved]";
    }
}
