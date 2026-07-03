package com.ruling_0.materiallib.api;

import java.util.Objects;

import net.minecraft.item.ItemStack;

import net.minecraftforge.fluids.FluidContainerRegistry;

/// Builds and registers a simple fluid-in-container [Shape] backed by a [ShapeFluidInContainer]. Obtained from
/// [MaterialLibAPI#newFluidInContainerShape] and finished with [#build] inside the owning mod's
/// [MaterialRegistrationEvent] handler. Mods needing custom behavior subclass [ShapeFluidInContainer] instead
/// and register through [MaterialLibAPI#registerFluidInContainerShape].
public final class FluidInContainerShapeBuilder {

    private final String modid;
    private final String name;
    private Shape fluidShape;
    private ItemStack emptyContainer;
    private int volume = FluidContainerRegistry.BUCKET_VOLUME;
    private String[] oreDicts;
    private String displayNameFormat;
    private boolean built;

    FluidInContainerShapeBuilder(String modid, String name) {
        this.modid = modid;
        this.name = name;
    }

    /// Sets the fluid this container holds; required. Pass the shape returned by [MaterialLibAPI#newFluidShape].
    /// Every material generating this container must also generate that fluid shape.
    public FluidInContainerShapeBuilder fluid(Shape fluidShape) {
        this.fluidShape = Objects.requireNonNull(fluidShape, "fluidShape must not be null");
        return this;
    }

    /// Sets the item returned when the fluid is drained from the container, e.g. an empty bucket. Omit for a
    /// container consumed on drain.
    public FluidInContainerShapeBuilder emptyContainer(ItemStack emptyContainer) {
        this.emptyContainer = Objects.requireNonNull(emptyContainer, "emptyContainer must not be null");
        return this;
    }

    /// Sets the fluid volume one container holds, in millibuckets. Defaults to one bucket
    /// ([FluidContainerRegistry#BUCKET_VOLUME]).
    public FluidInContainerShapeBuilder volume(int millibuckets) {
        this.volume = millibuckets;
        return this;
    }

    /// Sets the oredict prefixes; the material name is appended to each. Defaults to the shape name. At least one.
    public FluidInContainerShapeBuilder oreDict(String... prefixes) {
        this.oreDicts = prefixes;
        return this;
    }

    /// Sets the display-name format applied to the material name (e.g. `"%s Bucket"` -> `Iron Bucket`). Defaults to
    /// the material name followed by the capitalized shape name. A lang file may override individual names; see
    /// [ShapeNaming].
    public FluidInContainerShapeBuilder displayName(String displayNameFormat) {
        this.displayNameFormat = Objects.requireNonNull(displayNameFormat, "displayNameFormat must not be null");
        return this;
    }

    /// Registers the shape and returns the shape to generate; see [ShapeRegistry#register]. Fails if called twice
    /// or if no fluid was set.
    public Shape build() {
        if (built) {
            throw new IllegalStateException(
                "Fluid container shape " + Names.key(modid, name) + " was already built");
        }
        if (fluidShape == null) {
            throw new IllegalStateException(
                "Fluid container shape " + Names.key(modid, name) + " needs a fluid; call fluid(...) before build()");
        }
        if (!(fluidShape instanceof ShapeFluid fluid)) {
            throw new IllegalArgumentException(fluidShape + " is not a fluid shape");
        }
        built = true;
        String[] prefixes = oreDicts != null ? oreDicts : new String[] { name };
        String format = ShapeNaming.formatOrDefault(name, displayNameFormat);
        return ShapeRegistry.instance()
            .register(new ShapeFluidInContainer(modid, name, format, fluid, emptyContainer, volume, prefixes));
    }
}
