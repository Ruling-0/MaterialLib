package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Objects;

import net.minecraft.item.ItemStack;

import net.minecraftforge.fluids.FluidContainerRegistry;

import cpw.mods.fml.common.registry.GameRegistry;

/// Builds and registers a simple fluid-in-container [Shape] backed by a [ShapeFluidInContainer]. Obtained from
/// [MaterialLibAPI#newFluidInContainerShape] and finished with [#build] inside the owning mod's
/// [MaterialRegistrationEvent] handler. Mods needing custom behavior subclass [ShapeFluidInContainer] instead
/// and register through [MaterialLibAPI#registerFluidInContainerShape].
public final class FluidInContainerShapeBuilder {

    private final String modid;
    private final String name;
    private List<Shape> fluidShapes;
    private EmptyContainer emptyContainer;
    private int volume = FluidContainerRegistry.BUCKET_VOLUME;
    private String[] oreDicts;
    private String displayNameFormat;
    private boolean built;

    FluidInContainerShapeBuilder(String modid, String name) {
        this.modid = modid;
        this.name = name;
    }

    /// Sets the ordered fluid shapes this container can hold; required, at least one. The container binds, per
    /// material, to the first listed shape that material generates (GT's `cell`, for example, holds a material's
    /// liquid or gas); a material generating none of them fails validation at resolve. Pass the shape returned by
    /// [MaterialLibAPI#newFluidShape].
    public FluidInContainerShapeBuilder fluid(Shape... fluidShapes) {
        if (fluidShapes == null || fluidShapes.length == 0) {
            throw new IllegalArgumentException("fluid(...) needs at least one fluid shape");
        }
        for (Shape fluidShape : fluidShapes) {
            Objects.requireNonNull(fluidShape, "fluidShape must not be null");
        }
        this.fluidShapes = List.of(fluidShapes);
        return this;
    }

    /// Sets the item returned when the fluid is drained from the container, e.g. an empty bucket. Omit for a
    /// container consumed on drain.
    public FluidInContainerShapeBuilder emptyContainer(ItemStack emptyContainer) {
        this.emptyContainer = new EmptyContainer.Eager(
            Objects.requireNonNull(emptyContainer, "emptyContainer must not be null"));
        return this;
    }

    /// Sets the item returned when the fluid is drained from the container by identifier (`modid:name`) and
    /// metadata, resolved once at MaterialLib's init -- after every mod's items exist -- instead of immediately.
    /// Use this over [#emptyContainer(ItemStack)] for an item registered by a mod that has not loaded yet at
    /// MaterialLib's preInit. Fails loudly at resolution if no such item is registered.
    public FluidInContainerShapeBuilder emptyContainer(String itemName, int meta) {
        Objects.requireNonNull(itemName, "itemName must not be null");
        int colon = itemName.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("itemName \"" + itemName + "\" must be of the form modid:name");
        }
        String itemModid = itemName.substring(0, colon);
        String itemOnlyName = itemName.substring(colon + 1);
        this.emptyContainer = new EmptyContainer.Deferred(itemModid, itemOnlyName, meta, GameRegistry::findItem);
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
        if (fluidShapes == null) {
            throw new IllegalStateException(
                "Fluid container shape " + Names.key(modid, name) + " needs a fluid; call fluid(...) before build()");
        }
        for (Shape fluidShape : fluidShapes) {
            if (!(fluidShape instanceof ShapeFluid)) {
                throw new IllegalArgumentException(fluidShape + " is not a fluid shape");
            }
        }
        built = true;
        String[] prefixes = oreDicts != null ? oreDicts : new String[] { name };
        String format = ShapeNaming.formatOrDefault(name, displayNameFormat);
        return ShapeRegistry.instance()
            .register(new ShapeFluidInContainer(modid, name, format, fluidShapes, emptyContainer, volume, prefixes));
    }
}
