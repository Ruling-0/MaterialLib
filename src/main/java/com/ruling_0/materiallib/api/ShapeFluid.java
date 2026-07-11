package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Set;

import net.minecraft.client.renderer.texture.IIconRegister;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.ruling_0.materiallib.MaterialLib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/// The Forge fluid backing a fluid [Shape]: one registered [Fluid] per material that generates the shape, named by
/// this shape's [FluidNamer] (by default `<shape>.<material>` lowercased, e.g. `test.testiron`).
///
/// A fluid shape is not a [BackedShape], as fluids are registered by name and not numeric ID. Materials declare it
/// through [MaterialBuilder#generateShape]; the registry registers one fluid per material at resolve, configures
/// newly registered fluids through this shape's [FluidConfigurer], and, on the client, binds each fluid's still and
/// flowing icons from [#iconPath] -- the material's [TextureSet] by default, or this shape's [FluidIconPather] when
/// set (see [FluidShapeBuilder#iconPath]).
///
/// A bare fluid has no item form, so its material tooltip is carried by its container item (see
/// [ShapeFluidInContainer]). Each fluid takes its display name from the shape's format and its color from the
/// material's [StandardProperties#FLUID_TINT], or [StandardProperties#TINT] when unset, so tank and GUI renderers
/// show the right name and tint without a custom fluid block.
public class ShapeFluid implements ServedShape {

    private static final List<String> NO_OREDICTS = List.of();
    private static final FluidConfigurer NO_OP_CONFIGURER = (material, fluid) -> {};

    private final String modid;
    private final String name;
    private final String displayNameFormat;
    private final FluidNamer namer;
    private final FluidConfigurer configurer;
    private final FluidIconPather iconPather;

    private final ServedMaterials served = new ServedMaterials();

    private final Int2ObjectMap<Fluid> fluidsByIndex = new Int2ObjectOpenHashMap<>();
    private final Set<Material> warnedMissingIcon = new ReferenceOpenHashSet<>();

    /// Creates a fluid shape. `displayNameFormat` is applied to the material name to build the fluid's display
    /// name, e.g. `"Molten %s"`. Identifiers must be non-empty and free of ':' and whitespace.
    public ShapeFluid(String modid, String name, String displayNameFormat) {
        this(modid, name, displayNameFormat, null, null);
    }

    /// As the three-argument constructor, additionally setting this shape's [FluidNamer] and [FluidConfigurer]. A
    /// null namer defaults to [FluidNamer#DEFAULT]; a null configurer performs no extra configuration.
    public ShapeFluid(String modid, String name, String displayNameFormat, FluidNamer namer,
                      FluidConfigurer configurer) {
        this(modid, name, displayNameFormat, namer, configurer, null);
    }

    /// As the five-argument constructor, additionally setting this shape's [FluidIconPather]. A null pather, or one
    /// that returns null for a given material, falls back to that material's texture-set lookup; see
    /// [#registerIcons].
    ShapeFluid(String modid, String name, String displayNameFormat, FluidNamer namer, FluidConfigurer configurer,
               FluidIconPather iconPather) {
        this.modid = Names.validate("fluid shape modid", modid);
        this.name = Names.validate("fluid shape name", name);
        this.displayNameFormat = ShapeNaming.requireValidFormat(displayNameFormat);
        this.namer = namer != null ? namer : FluidNamer.DEFAULT;
        this.configurer = configurer != null ? configurer : NO_OP_CONFIGURER;
        this.iconPather = iconPather;
    }

    @Override
    public String getModId() { return modid; }

    @Override
    public String getName() { return name; }

    @Override
    public List<String> getOreDicts() { return NO_OREDICTS; }

    @Override
    public String toString() {
        return "ShapeFluid[" + Names.key(modid, name) + "]";
    }

    @Override
    public void bindServedMaterials(Material[] materials) {
        served.bind(this, materials);
    }

    @Override
    public Material[] getServedMaterials() { return served.get(); }

    /// The Forge fluid name for a material in this shape, as produced by this shape's [FluidNamer] (e.g.
    /// `molten.testiron` by default).
    String fluidName(Material material) {
        return namer.name(this, material);
    }

    /// Registers one Forge fluid per served material, validating and reserving each material's fluid name against
    /// `usedFluidNames`, shared across every fluid shape resolving this session.
    void registerFluids(Set<String> usedFluidNames) {
        fluidsByIndex.clear();
        for (Material material : served.get()) {
            String fluidName = FluidNaming.validate(fluidName(material), this, material, usedFluidNames);
            fluidsByIndex.put(material.getIndex(), registerOrReuse(fluidName, material));
        }
    }

    /// Registers a newly created [MaterialFluid] under `fluidName` and configures it, or -- if that name is already
    /// registered by another mod -- reuses the existing fluid unmodified.
    private Fluid registerOrReuse(String fluidName, Material material) {
        Fluid created = new MaterialFluid(fluidName, material);
        if (FluidRegistry.registerFluid(created)) {
            configurer.configure(material, created);
            return created;
        }
        MaterialLib.LOG.warn(
            "Fluid {} of {} is already registered elsewhere; its tint, name, and icons will not apply",
            fluidName,
            material.getKey());
        return FluidRegistry.getFluid(fluidName);
    }

    /// The fluid stack of `material` in this shape, with the given volume in millibuckets. The material must
    /// generate this shape.
    FluidStack fluidStack(Material material, int amount) {
        Fluid fluid = fluidsByIndex.get(material.getIndex());
        if (fluid == null) {
            throw new IllegalArgumentException(
                "Material " + material.getKey() + " does not generate fluid shape " + this);
        }
        return new FluidStack(fluid, amount);
    }

    /// Binds each material's still and flowing fluid icon from [#iconPath], or the [ShapeIcons#EMPTY_ICON]
    /// placeholder -- logged once per material -- if that path names no existing texture file, the same
    /// existence-checked fallback [ShapeIcons] uses for a block or item shape's icon. Fluid textures live on the
    /// block atlas, so this runs from a blocks texture-stitch on the client (see [ShapeFluidIcons]).
    @SideOnly(Side.CLIENT)
    void registerIcons(IIconRegister register) {
        for (Material material : served.get()) {
            Fluid fluid = fluidsByIndex.get(material.getIndex());
            if (!(fluid instanceof MaterialFluid)) continue;
            String path = iconPath(material);
            if (!ResourceUtil.resourceExists(ResourceUtil.getCompleteBlockTextureResourceLocation(path))) {
                if (warnedMissingIcon.add(material)) {
                    MaterialLib.LOG.warn(
                        "Fluid shape {} of {} has no icon at {}; it will render the empty placeholder instead",
                        this,
                        material.getKey(),
                        path);
                }
                path = ShapeIcons.EMPTY_ICON;
            }
            fluid.setIcons(register.registerIcon(path));
        }
    }

    /// The icon path to register for `material`'s fluid: this shape's [FluidIconPather] when set and it returns a
    /// path for `material`, otherwise `material`'s texture set (see [StandardProperties#TEXTURE_SET]), or the
    /// empty placeholder if it has none, since a [Fluid] left iconless returns null from [Fluid#getIcon] and
    /// crashes tank and NEI renderers; [MaterialRegistry#resolve] already warns about that condition when the
    /// registry resolves.
    String iconPath(Material material) {
        if (iconPather != null) {
            String path = iconPather.iconPath(this, material);
            if (path != null) return path;
        }
        TextureSet textureSet = material.getProperty(StandardProperties.TEXTURE_SET);
        return textureSet != null ? textureSet.iconPath(name) : ShapeIcons.EMPTY_ICON;
    }

    /// A material's fluid, naming itself from the shape's display format and coloring itself with the material's
    /// [StandardProperties#TINT] so renderers that read [Fluid#getColor] tint the fluid per material. 1.7.10 fluids
    /// expose no color setter, only an overridable [Fluid#getColor], which is why this is a subclass.
    private final class MaterialFluid extends Fluid {

        private final Material material;

        MaterialFluid(String fluidName, Material material) {
            super(fluidName);
            this.material = material;
            setUnlocalizedName(Names.key(modid, name));
        }

        @Override
        public String getLocalizedName(FluidStack stack) {
            return ShapeText.displayName(ShapeFluid.this, displayNameFormat, material);
        }

        @Override
        public int getColor() { return tintOf(material) & 0xFFFFFF; }
    }

    /// The ARGB fill tint for `material`'s fluid: [StandardProperties#FLUID_TINT] when set, or
    /// [StandardProperties#TINT] otherwise. Shared with [ShapeFluidInContainer] so a container's fill layer
    /// matches the fluid it holds; a caller reading a fluid (rather than an item) color masks off the alpha byte
    /// itself, as [Fluid#getColor] expects (see [MaterialFluid#getColor]).
    static int tintOf(Material material) {
        Integer fluidTint = material.getProperty(StandardProperties.FLUID_TINT);
        return fluidTint != null ? fluidTint : material.getProperty(StandardProperties.TINT);
    }
}
