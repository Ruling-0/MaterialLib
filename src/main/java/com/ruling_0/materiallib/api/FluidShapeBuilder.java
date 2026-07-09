package com.ruling_0.materiallib.api;

import java.util.Objects;

/// Builds and registers a simple fluid [Shape] backed by a [ShapeFluid]. Obtained from [MaterialLibAPI#newFluidShape]
/// and finished with [#build], which must be called inside the owning mod's [MaterialRegistrationEvent] handler.
/// Mods needing custom fluid behavior subclass [ShapeFluid] instead and register through
/// [MaterialLibAPI#registerFluidShape].
public final class FluidShapeBuilder {

    private final String modid;
    private final String name;
    private String displayNameFormat;
    private FluidNamer namer;
    private FluidConfigurer configurer;
    private FluidIconPather iconPather;
    private boolean built;

    FluidShapeBuilder(String modid, String name) {
        this.modid = modid;
        this.name = name;
    }

    /// Sets the display-name format applied to the material name (e.g. `"Molten %s"` -> `Molten Iron`). Defaults to
    /// the material name followed by the capitalized shape name. A lang file may override individual names; see
    /// [ShapeNaming].
    public FluidShapeBuilder displayName(String displayNameFormat) {
        this.displayNameFormat = Objects.requireNonNull(displayNameFormat, "displayNameFormat must not be null");
        return this;
    }

    /// Sets the function computing each served material's Forge fluid name, in place of the default
    /// `<shapeName>.<materialName>` lowercased; see [FluidNamer].
    public FluidShapeBuilder fluidName(FluidNamer namer) {
        this.namer = Objects.requireNonNull(namer, "namer must not be null");
        return this;
    }

    /// Sets the callback configuring each newly registered material fluid (temperature, gaseous, luminosity,
    /// density, viscosity, ...); see [FluidConfigurer].
    public FluidShapeBuilder configureFluid(FluidConfigurer configurer) {
        this.configurer = Objects.requireNonNull(configurer, "configurer must not be null");
        return this;
    }

    /// Sets the per-material icon path override, in place of the material's texture set; see [FluidIconPather].
    /// Returning null from `pather` for a given material falls back to that material's texture-set lookup, the
    /// same as when this is left unset.
    public FluidShapeBuilder iconPath(FluidIconPather pather) {
        this.iconPather = Objects.requireNonNull(pather, "pather must not be null");
        return this;
    }

    /// As [#iconPath(FluidIconPather)], for a single icon path shared by every served material.
    public FluidShapeBuilder iconPath(String path) {
        Objects.requireNonNull(path, "path must not be null");
        return iconPath((shape, material) -> path);
    }

    /// Registers the shape and returns the shape to generate; see [ShapeRegistry#register]. Fails if called twice.
    public Shape build() {
        if (built) {
            throw new IllegalStateException("Fluid shape " + Names.key(modid, name) + " was already built");
        }
        built = true;
        String format = ShapeNaming.formatOrDefault(name, displayNameFormat);
        return ShapeRegistry.instance()
            .register(new ShapeFluid(modid, name, format, namer, configurer, iconPather));
    }
}
