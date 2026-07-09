package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Map;
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
/// maps, per material, from its filled stack to one of the shape's fluids through the [FluidContainerRegistry].
///
/// A material that generates this shape must also generate at least one of the fluid shapes it can hold; the
/// registry enforces that at resolve, and binds each material to the first of those shapes, in declared order, that
/// the material generates (see [FluidInContainerShapeBuilder#fluid(Shape...)]).
///
/// Renders in two passes: an untinted empty-container texture underneath the texture set's texture for this
/// shape, which supplies the fluid fill and is tinted with [StandardProperties#TINT]. The container looks the
/// same for every material, so the empty texture is a property of the shape rather than of a texture set: it
/// defaults to `<modid>:materials/<name>_empty` in the shape's own domain, or the path
/// [FluidInContainerShapeBuilder#emptyIcon] sets. A path naming a texture file that does not exist registers the
/// [ShapeIcons#EMPTY_ICON] placeholder instead, logged once, so a missing base texture degrades gracefully rather
/// than rendering missingno; see [#registerIcons].
public class ShapeFluidInContainer extends ShapeItem {

    private final List<Shape> fluidShapes;
    private final EmptyContainer emptyContainer;
    private final int volume;
    private final String emptyIconPath;
    private IIcon emptyIcon;
    private boolean warnedMissingEmptyIcon;

    /// Creates a fluid-in-container shape holding `fluidShape`, `volume` millibuckets per item. `emptyContainer` is
    /// the item returned when the fluid is drained (e.g. an empty bucket), or null for a container consumed on
    /// drain. `oreDicts` and `displayNameFormat` behave as for a [ShapeItem].
    public ShapeFluidInContainer(String modid, String name, String displayNameFormat, ShapeFluid fluidShape,
                                 ItemStack emptyContainer, int volume, String... oreDicts) {
        this(modid, name, displayNameFormat,
            List.of(Objects.requireNonNull(fluidShape, "fluidShape must not be null")),
            emptyContainer == null ? null : new EmptyContainer.Eager(emptyContainer), volume, null, oreDicts);
    }

    /// As the six-argument constructor, but with an ordered list of fluid shapes this container can hold (see
    /// [FluidInContainerShapeBuilder#fluid(Shape...)]), a possibly-deferred [EmptyContainer], and an optional base
    /// icon path override (see [FluidInContainerShapeBuilder#emptyIcon]); null uses [#emptyIconPath]'s default.
    ShapeFluidInContainer(String modid, String name, String displayNameFormat, List<Shape> fluidShapes,
                          EmptyContainer emptyContainer, int volume, String emptyIconPath, String... oreDicts) {
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
        this.emptyIconPath = emptyIconPath;
    }

    /// The icon path to register for this container's untinted base texture; see [#resolveEmptyIconPath].
    String emptyIconPath() {
        return resolveEmptyIconPath(getModId(), getName(), emptyIconPath);
    }

    /// The icon path to register for a container's untinted base texture: `override` -- the path
    /// [FluidInContainerShapeBuilder#emptyIcon] set -- when non-null, or `<modid>:materials/<name>_empty` in the
    /// container's own domain by default.
    static String resolveEmptyIconPath(String modid, String name, String override) {
        return override != null ? override : modid + ":materials/" + name + "_empty";
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

    /// Registers this container's fill icons, then its base icon at [#emptyIconPath], or the
    /// [ShapeIcons#EMPTY_ICON] placeholder -- logged once -- if that path names no existing texture file.
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
