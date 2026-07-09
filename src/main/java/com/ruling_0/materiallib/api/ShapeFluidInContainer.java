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
/// The container holds one fluid shape. A material that generates this shape must also generate that fluid shape;
/// the registry enforces that at resolve.
///
/// Renders in two passes: an untinted empty-container texture underneath the texture set's texture for this
/// shape, which supplies the fluid fill and is tinted with [StandardProperties#TINT]. The container looks the
/// same for every material, so the empty texture is a property of the shape rather than of a texture set: it
/// lives in the shape's own domain at `textures/items/materials/<name>_empty.png`.
public class ShapeFluidInContainer extends ShapeItem {

    private final Shape fluidShape;
    private final EmptyContainer emptyContainer;
    private final int volume;
    private IIcon emptyIcon;

    /// Creates a fluid-in-container shape holding `fluidShape`, `volume` millibuckets per item. `emptyContainer` is
    /// the item returned when the fluid is drained (e.g. an empty bucket), or null for a container consumed on
    /// drain. `oreDicts` and `displayNameFormat` behave as for a [ShapeItem].
    public ShapeFluidInContainer(String modid, String name, String displayNameFormat, ShapeFluid fluidShape,
                                 ItemStack emptyContainer, int volume, String... oreDicts) {
        this(modid, name, displayNameFormat, fluidShape,
            emptyContainer == null ? null : new EmptyContainer.Eager(emptyContainer), volume, oreDicts);
    }

    /// As [#ShapeFluidInContainer(String, String, String, ShapeFluid, ItemStack, int, String...)], but draining to
    /// an empty container item registered through [MaterialLibAPI#registerEmptyContainer(String, String)].
    protected ShapeFluidInContainer(String modid, String name, String displayNameFormat, Shape fluidShape,
                                    EmptyContainerHandle emptyContainer, int volume, String... oreDicts) {
        this(modid, name, displayNameFormat, fluidShape,
            new EmptyContainer.Registered(Objects.requireNonNull(emptyContainer, "emptyContainer must not be null")),
            volume, oreDicts);
    }

    /// As [#ShapeFluidInContainer(String, String, String, ShapeFluid, ItemStack, int, String...)], accepting any
    /// form of [EmptyContainer].
    ShapeFluidInContainer(String modid, String name, String displayNameFormat, Shape fluidShape,
                          EmptyContainer emptyContainer, int volume, String... oreDicts) {
        super(modid, name, displayNameFormat, oreDicts);
        Objects.requireNonNull(fluidShape, "fluidShape must not be null");
        if (!(fluidShape instanceof ShapeFluid)) {
            throw new IllegalArgumentException(fluidShape + " is not a fluid shape");
        }
        this.fluidShape = fluidShape;
        this.emptyContainer = emptyContainer;
        if (volume <= 0) {
            throw new IllegalArgumentException("container volume must be positive, was " + volume);
        }
        this.volume = volume;
    }

    /// The fluid shape this container was built with.
    Shape getFluidShape() { return fluidShape; }

    /// Registers a [FluidContainerRegistry] mapping for each served material, filling this item at the material's
    /// index from `fluid`. Called at resolve, after fluids and any registered empty container items exist.
    void registerContainers(ShapeFluid fluid) {
        ItemStack empty = emptyContainer != null ? emptyContainer.resolve() : null;
        for (Material material : getServedMaterials()) {
            FluidStack fluidStack = fluid.fluidStack(material, volume);
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

    /// The untinted container base for pass 0, and the material's fill icon -- [ShapeItem]'s pass-0 icon, with its
    /// placeholder fallback -- for every later pass. The fill pass must not fall through to vanilla
    /// [net.minecraft.item.Item#getIconFromDamage]'s `itemIcon` field, which no shape item ever assigns.
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamageForRenderPass(int damage, int pass) {
        return pass == 0 ? emptyIcon : super.getIconFromDamageForRenderPass(damage, 0);
    }

    /// The single-icon form simple renderers use, showing the container base; see [ShapeItem#getIconFromDamage].
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return emptyIcon != null ? emptyIcon : super.getIconFromDamage(damage);
    }

    /// White for the untinted container base in pass 0, and the material tint -- [ShapeItem]'s pass-0 color -- for
    /// the fill passes, keeping each pass's tint aligned with the icon [#getIconFromDamageForRenderPass] returns
    /// for it.
    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int renderPass) {
        return renderPass == 0 ? 0xFFFFFFFF : super.getColorFromItemStack(stack, 0);
    }
}
