package com.ruling_0.materiallib.api;

import java.util.Map;
import java.util.Objects;

import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;

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
    private IconPather iconPather;
    private final Map<Property<?>, Object> properties = new Reference2ObjectLinkedOpenHashMap<>();
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

    /// Sets the per-material icon path override, tried ahead of the material's texture set; see [IconPather].
    public FluidShapeBuilder iconPath(IconPather pather) {
        this.iconPather = Objects.requireNonNull(pather, "pather must not be null");
        return this;
    }

    /// As [#iconPath(IconPather)], for a single icon path shared by every served material.
    public FluidShapeBuilder iconPath(String path) {
        Objects.requireNonNull(path, "path must not be null");
        return iconPath((shape, material) -> path);
    }

    /// Sets a property value on the shape. Values are read back through [Shape#getProperty] and may be altered
    /// by another mod through [MaterialLibAPI#editShape].
    public <T> FluidShapeBuilder property(Property<T> property, T value) {
        ShapeProperties.requireSettable(property, value);
        properties.put(property, value);
        return this;
    }

    /// Registers the shape and returns the shape to generate; see [ShapeRegistry#register]. Fails if called twice.
    public Shape build() {
        if (built) {
            throw new IllegalStateException("Fluid shape " + Names.key(modid, name) + " was already built");
        }
        built = true;
        String format = ShapeNaming.formatOrDefault(name, displayNameFormat);
        ShapeFluid shape = new ShapeFluid(modid, name, format, namer, configurer, iconPather);
        shape.properties().setAll(shape, properties);
        return ShapeRegistry.instance().register(shape);
    }
}
