package com.ruling_0.materiallib.api;

import java.util.Objects;

import net.minecraft.item.ItemStack;

import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// The item backing a fluid-in-container [Shape]: a [ShapeItem] whose damage is a material's global index and that
/// maps, per material, from its filled stack to the material's fluid through the [FluidContainerRegistry].
///
/// A material that generates this shape must also generate the [ShapeFluid] it holds; the registry enforces that at
/// resolve. The filled item renders and reads like any item shape (per-material texture, tint, display name, and
/// tooltip); the added behavior is the container mapping linking each filled stack to the material's fluid, with an
/// optional empty container returned when the fluid is drained.
public class ShapeFluidInContainer extends ShapeItem {

    private static final Logger LOG = LogManager.getLogger("materiallib");

    private final ShapeFluid fluidShape;
    private final ItemStack emptyContainer;
    private final int volume;

    /// Creates a fluid-in-container shape holding `fluidShape`, `volume` millibuckets per item. `emptyContainer` is
    /// the item returned when the fluid is drained (e.g. an empty bucket), or null for a container consumed on
    /// drain. `oreDicts` and `displayNameFormat` behave as for a [ShapeItem].
    public ShapeFluidInContainer(String modid, String name, String displayNameFormat, ShapeFluid fluidShape,
                                 ItemStack emptyContainer, int volume, String... oreDicts) {
        super(modid, name, displayNameFormat, oreDicts);
        this.fluidShape = Objects.requireNonNull(fluidShape, "fluidShape must not be null");
        this.emptyContainer = emptyContainer == null ? null : emptyContainer.copy();
        if (volume <= 0) {
            throw new IllegalArgumentException("container volume must be positive, was " + volume);
        }
        this.volume = volume;
    }

    /// The fluid shape this container was built with. The registry follows unification to the canonical fluid at
    /// resolve.
    ShapeFluid getFluidShape() { return fluidShape; }

    /// Registers a [FluidContainerRegistry] mapping for each served material, filling this item at the material's
    /// index from `canonicalFluid`. Called at resolve, after fluids are registered.
    void registerContainers(ShapeFluid canonicalFluid) {
        for (Material material : getServedMaterials()) {
            FluidStack fluidStack = canonicalFluid.fluidStack(material, volume);
            ItemStack filled = getStack(material, 1);
            boolean registered = emptyContainer != null ?
                FluidContainerRegistry.registerFluidContainer(fluidStack, filled, emptyContainer.copy()) :
                FluidContainerRegistry.registerFluidContainer(fluidStack, filled);
            if (!registered) {
                LOG.warn(
                    "Fluid container mapping for {} of {} was rejected; the container will not fill or drain",
                    this,
                    material.getKey());
            }
        }
    }
}
