package com.ruling_0.materiallib.api;

import java.util.Objects;

/// Builds and registers a simple fluid [Shape] backed by a [ShapeFluid]. Obtained from [MaterialLibAPI#newFluidShape]
/// and finished with [#build], which must be called during the owning mod's preInit. Mods needing custom fluid
/// behavior subclass [ShapeFluid] instead and register through [MaterialLibAPI#registerFluidShape].
public final class FluidShapeBuilder {

    private final String modid;
    private final String name;
    private String displayNameFormat;
    private boolean built;

    FluidShapeBuilder(String modid, String name) {
        this.modid = Names.validate("fluid shape modid", modid);
        this.name = Names.validate("fluid shape name", name);
    }

    /// Sets the display-name format applied to the material name (e.g. `"Molten %s"` -> `Molten Iron`). Defaults to
    /// the material name followed by the capitalized shape name. A lang file may override individual names; see
    /// [ShapeNaming].
    public FluidShapeBuilder displayName(String displayNameFormat) {
        this.displayNameFormat = Objects.requireNonNull(displayNameFormat, "displayNameFormat must not be null");
        return this;
    }

    /// Registers the shape and returns the shape to generate; see [ShapeRegistry#register]. Fails if called twice.
    public Shape build() {
        if (built) {
            throw new IllegalStateException("Fluid shape " + Names.key(modid, name) + " was already built");
        }
        built = true;
        String format = displayNameFormat != null ? displayNameFormat : "%s " + ShapeNaming.capitalize(name);
        return ShapeRegistry.instance()
            .register(new ShapeFluid(modid, name, format));
    }
}
