package com.ruling_0.materiallib.api;

import java.util.Objects;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.ruling_0.materiallib.MaterialLib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The item backing a fluid-in-container [Shape]: a [ShapeItem] whose damage is a material's global index and that
/// maps, per material, from its filled stack to the material's fluid through the [FluidContainerRegistry].
///
/// The container holds one fluid shape. A material that generates this shape must also generate that fluid shape;
/// the registry enforces that at resolve.
///
/// Renders the fluid fill first -- the texture set's texture for this shape, one pass per icon layer as [ShapeItem]
/// draws it -- then the untinted container base in a final pass over them. Container base art leaves its fill
/// window fully transparent. The fill's first layer takes [StandardProperties#CELL_TINT] when the material sets it,
/// or the fluid's fill tint (see [ShapeFluid#tintOf]) otherwise; every later fill layer takes the color [ShapeItem]
/// gives it. The container looks the same for every material, so the empty texture is a property of the shape
/// rather than of a texture set: it defaults to `<modid>:materials/<name>_empty` in the shape's own domain, or the
/// path [FluidInContainerShapeBuilder#emptyIcon] sets. For a path naming no existing texture file, see
/// [#registerIcons].
public class ShapeFluidInContainer extends ShapeItem {

    private final Shape fluidShape;
    private final EmptyContainer emptyContainer;
    private final int volume;
    private final String emptyIconOverride;
    private IIcon emptyIcon;
    private boolean warnedMissingEmptyIcon;

    /// Creates a fluid-in-container shape holding `fluidShape`, `volume` millibuckets per item. `emptyContainer` is
    /// the item returned when the fluid is drained (e.g. an empty bucket), or null for a container consumed on
    /// drain. `oreDicts` and `displayNameFormat` behave as for a [ShapeItem].
    public ShapeFluidInContainer(String modid, String name, String displayNameFormat, ShapeFluid fluidShape,
                                 ItemStack emptyContainer, int volume, String... oreDicts) {
        this(modid, name, displayNameFormat, fluidShape,
            emptyContainer == null ? null : new EmptyContainer.Eager(emptyContainer), volume, null, oreDicts);
    }

    /// As [#ShapeFluidInContainer(String, String, String, ShapeFluid, ItemStack, int, String...)], but draining to
    /// an empty container item registered through [MaterialLibAPI#registerEmptyContainer(String, String, String)].
    protected ShapeFluidInContainer(String modid, String name, String displayNameFormat, Shape fluidShape,
                                    EmptyContainerHandle emptyContainer, int volume, String... oreDicts) {
        this(modid, name, displayNameFormat, fluidShape,
            new EmptyContainer.Registered(Objects.requireNonNull(emptyContainer, "emptyContainer must not be null")),
            volume, null, oreDicts);
    }

    /// As [#ShapeFluidInContainer(String, String, String, ShapeFluid, ItemStack, int, String...)], accepting any
    /// form of [EmptyContainer] and an optional base icon path override (see
    /// [FluidInContainerShapeBuilder#emptyIcon]); null uses [#emptyIconPath]'s default.
    ShapeFluidInContainer(String modid, String name, String displayNameFormat, Shape fluidShape,
                          EmptyContainer emptyContainer, int volume, String emptyIconOverride, String... oreDicts) {
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
        this.emptyIconOverride = emptyIconOverride;
    }

    /// The icon path registered for this container's untinted base texture: the
    /// [FluidInContainerShapeBuilder#emptyIcon] override when set, otherwise `<modid>:materials/<name>_empty`.
    String emptyIconPath() {
        return emptyIconOverride != null ? emptyIconOverride : getModId() + ":materials/" + getName() + "_empty";
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

    /// Registers this container's fill icons, then its base icon at [#emptyIconPath], or the
    /// [ShapeIcons#EMPTY_ICON] placeholder if that path names no existing texture file.
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        super.registerIcons(register);
        String path = emptyIconPath();
        if (ResourceUtil.resourceExists(ResourceUtil.getCompleteItemTextureResourceLocation(path))) {
            emptyIcon = register.registerIcon(path);
            return;
        }
        if (!warnedMissingEmptyIcon) {
            warnedMissingEmptyIcon = true;
            MaterialLib.LOG.warn(
                "Fluid container {} has no base icon at {}; it will render the empty placeholder instead",
                this,
                path);
        }
        emptyIcon = register.registerIcon(ShapeIcons.EMPTY_ICON);
    }

    /// One pass per fill layer, plus the container base's final pass.
    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderPasses(int meta) {
        return super.getRenderPasses(meta) + 1;
    }

    /// The fill's layer at `pass`, and the untinted container base for the final pass.
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamageForRenderPass(int damage, int pass) {
        if (pass < super.getRenderPasses(damage)) return super.getIconFromDamageForRenderPass(damage, pass);
        return emptyIcon;
    }

    /// The container base, not the fill, for callers that ask for a single icon; see [ShapeItem#getIconFromDamage].
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return emptyIcon;
    }

    /// The fill tint (see the class doc) for the first pass, each later fill layer's own color (see
    /// [ShapeItem#getColorFromItemStack]) for the passes after it, and white for the container base's final pass.
    /// The first pass is white for a damage value carrying no live material and for an override-bound fill icon;
    /// see [ShapeItem#hasOverrideIcon].
    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int renderPass) {
        if (renderPass >= super.getRenderPasses(stack.getItemDamage())) return 0xFFFFFFFF;
        if (renderPass > 0) return super.getColorFromItemStack(stack, renderPass);
        Material material = ShapeText.materialFor(stack);
        if (material == null || hasOverrideIcon(material)) return 0xFFFFFFFF;
        if (material.getProperty(StandardProperties.CELL_TINT) == null) return ShapeFluid.tintOf(material);
        return MaterialTints.color(material, StandardProperties.CELL_TINT);
    }
}
