package com.ruling_0.materiallib.api;

import java.util.Objects;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.ruling_0.materiallib.MaterialLib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The item backing a fluid-in-container [Shape]: a [ShapeItem] whose damage is a material's global index and that
/// maps, per material, from its filled stack to the material's fluid through the [FluidContainerRegistry].
///
/// A material that generates this shape must also generate the [ShapeFluid] it holds; the registry enforces that at
/// resolve.
///
/// Renders in two passes: an untinted empty-container texture underneath the texture set's texture for this
/// shape, which supplies the fluid fill and is tinted with [StandardProperties#TINT]. The container looks the
/// same for every material, so the empty texture is a property of the shape rather than of a texture set: it
/// lives in the shape's own domain at `textures/items/materials/<name>_empty.png`.
public class ShapeFluidInContainer extends ShapeItem {

    private final ShapeFluid fluidShape;
    private final ItemStack emptyContainer;
    private final int volume;
    private IIcon emptyIcon;

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

    /// The fluid shape this container was built with.
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
                MaterialLib.LOG.warn(
                    "Fluid container mapping for {} of {} was rejected; the container will not fill or drain",
                    this,
                    material.getKey());
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        super.registerIcons(register);
        emptyIcon = register.registerIcon(getModId() + ":materials/" + getName() + "_empty");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamageForRenderPass(int damage, int pass) {
        return pass == 0 ? emptyIcon : getIconFromDamage(damage);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int renderPass) {
        return renderPass == 0 ? 0xFFFFFFFF : super.getColorFromItemStack(stack, renderPass);
    }
}
