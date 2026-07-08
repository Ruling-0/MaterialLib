package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Map;
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
/// maps, per material, from its filled stack to one of the shape's fluids through the [FluidContainerRegistry].
///
/// A material that generates this shape must also generate at least one of the fluid shapes it can hold; the
/// registry enforces that at resolve, and binds each material to the first of those shapes, in declared order, that
/// the material generates (see [FluidInContainerShapeBuilder#fluid(Shape...)]).
///
/// Renders in two passes: an untinted empty-container texture underneath the texture set's texture for this
/// shape, which supplies the fluid fill and is tinted with [StandardProperties#TINT]. The container looks the
/// same for every material, so the empty texture is a property of the shape rather than of a texture set: it
/// lives in the shape's own domain at `textures/items/materials/<name>_empty.png`.
public class ShapeFluidInContainer extends ShapeItem {

    private final List<Shape> fluidShapes;
    private final EmptyContainer emptyContainer;
    private final int volume;
    private IIcon emptyIcon;

    /// Creates a fluid-in-container shape holding `fluidShape`, `volume` millibuckets per item. `emptyContainer` is
    /// the item returned when the fluid is drained (e.g. an empty bucket), or null for a container consumed on
    /// drain. `oreDicts` and `displayNameFormat` behave as for a [ShapeItem].
    public ShapeFluidInContainer(String modid, String name, String displayNameFormat, ShapeFluid fluidShape,
                                 ItemStack emptyContainer, int volume, String... oreDicts) {
        this(modid, name, displayNameFormat,
            List.of(Objects.requireNonNull(fluidShape, "fluidShape must not be null")),
            emptyContainer == null ? null : new EmptyContainer.Eager(emptyContainer), volume, oreDicts);
    }

    /// As the six-argument constructor, but with an ordered list of fluid shapes this container can hold (see
    /// [FluidInContainerShapeBuilder#fluid(Shape...)]) and a possibly-deferred [EmptyContainer].
    ShapeFluidInContainer(String modid, String name, String displayNameFormat, List<Shape> fluidShapes,
                          EmptyContainer emptyContainer, int volume, String... oreDicts) {
        super(modid, name, displayNameFormat, oreDicts);
        if (fluidShapes == null || fluidShapes.isEmpty()) {
            throw new IllegalArgumentException("fluidShapes must not be null or empty");
        }
        this.fluidShapes = List.copyOf(fluidShapes);
        this.emptyContainer = emptyContainer;
        if (volume <= 0) {
            throw new IllegalArgumentException("container volume must be positive, was " + volume);
        }
        this.volume = volume;
    }

    /// The fluid shapes this container was built with, in fallback order.
    List<Shape> getFluidShapes() { return fluidShapes; }

    /// The first of `fluidShapes` that `material` generates, or null if none do.
    static ShapeFluid selectFluid(Material material, List<ShapeFluid> fluidShapes) {
        for (ShapeFluid fluid : fluidShapes) {
            for (Material served : fluid.getServedMaterials()) {
                if (served == material) return fluid;
            }
        }
        return null;
    }

    /// Registers a [FluidContainerRegistry] mapping for each served material, filling this item at the material's
    /// index from its fluid in `fluidByMaterial`. Called from MaterialLib's init, before init-phase shape
    /// consumers run and after fluids are registered at resolve; resolves this container's [EmptyContainer] once.
    void registerContainers(Map<Material, ShapeFluid> fluidByMaterial) {
        ItemStack empty = emptyContainer != null ? emptyContainer.resolve() : null;
        for (Material material : getServedMaterials()) {
            FluidStack fluidStack = fluidByMaterial.get(material)
                .fluidStack(material, volume);
            ItemStack filled = getStack(material, 1);
            boolean registered = empty != null ?
                FluidContainerRegistry.registerFluidContainer(fluidStack, filled, empty.copy()) :
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
