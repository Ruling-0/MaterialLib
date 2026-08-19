package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

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
/// flowing icons from a resource-pack override, then from this shape's [IconPather] when it names an existing file
/// (see [FluidShapeBuilder#iconPath]), and otherwise from the material's texture sets -- the same chain [ShapeIcons]
/// walks for an item or block icon.
///
/// A bare fluid has no item form, so its material tooltip is carried by its container item (see
/// [ShapeFluidInContainer]). Each fluid takes its display name from the shape's format and its color from the
/// material's [StandardProperties#FLUID_TINT], or [StandardProperties#TINT] when unset, so tank and GUI renderers
/// show the right name and tint without a custom fluid block. A fluid whose icon came from the resource-pack
/// override location is untinted; see [ShapeItem#hasOverrideIcon].
public class ShapeFluid implements ServedShape {

    private static final List<String> NO_OREDICTS = List.of();
    private static final FluidConfigurer NO_OP_CONFIGURER = (material, fluid) -> {};

    private final String modid;
    private final String name;
    private final String displayNameFormat;
    private final FluidNamer namer;
    private final FluidConfigurer configurer;
    private final IconPather iconPather;

    private final ServedMaterials served = new ServedMaterials();
    private final ShapeProperties props = new ShapeProperties();

    private final Int2ObjectMap<Fluid> fluidsByIndex = new Int2ObjectOpenHashMap<>();
    private final Set<Material> warnedMissingIcon = new ReferenceOpenHashSet<>();
    private final Set<Material> overrideIcons = new ReferenceOpenHashSet<>();

    /// Creates a fluid shape. `displayNameFormat` is applied to the material name to build the fluid's display
    /// name, e.g. `"Molten %s"`. Identifiers must be non-empty and free of ':' and whitespace.
    public ShapeFluid(String modid, String name, String displayNameFormat) {
        this(modid, name, displayNameFormat, null, null);
    }

    /// As [#ShapeFluid(String, String, String)], additionally setting this shape's [FluidNamer] and
    /// [FluidConfigurer]. A null namer defaults to [FluidNamer#DEFAULT]; a null configurer performs no extra
    /// configuration.
    public ShapeFluid(String modid, String name, String displayNameFormat, FluidNamer namer,
                      FluidConfigurer configurer) {
        this(modid, name, displayNameFormat, namer, configurer, null);
    }

    /// As [#ShapeFluid(String, String, String, FluidNamer, FluidConfigurer)], additionally setting this shape's
    /// [IconPather], or null for none; see [#resolveIconPath].
    ShapeFluid(String modid, String name, String displayNameFormat, FluidNamer namer, FluidConfigurer configurer,
               IconPather iconPather) {
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

    @Override
    public ShapeProperties properties() {
        return props;
    }

    /// The Forge fluid name for a material in this shape, as produced by this shape's [FluidNamer].
    String fluidName(Material material) {
        return namer.name(this, material);
    }

    /// The first served material this shape and `candidate` name differently, or null when both name every served
    /// material identically.
    Material firstNameDivergence(ShapeFluid candidate) {
        for (Material material : served.get()) {
            if (!fluidName(material).equals(candidate.fluidName(material))) {
                return material;
            }
        }
        return null;
    }

    /// Logs where `candidate`, a fluid shape unified onto this one, would have named or configured this shape's
    /// fluids differently.
    void logCandidateDivergence(ShapeFluid candidate) {
        try {
            Material diverging = firstNameDivergence(candidate);
            if (diverging != null) {
                MaterialLib.LOG.error(
                    "Fluid shapes {} and {} share a name but name the fluid of {} differently ({} vs {}); " +
                        "registering only the owner's names, so stored fluid stacks depend on which mod owns the shape",
                    Names.key(modid, name),
                    Names.key(candidate.modid, candidate.name),
                    diverging.getKey(),
                    fluidName(diverging),
                    candidate.fluidName(diverging));
            }
        }
        catch (RuntimeException e) {
            MaterialLib.LOG.error("Fluid namer of {} failed while comparing its names against {}", candidate, this, e);
        }
        if (candidate.configurer != NO_OP_CONFIGURER && candidate.configurer != configurer) {
            MaterialLib.LOG.warn(
                "Fluid shapes {} and {} share a name but set different fluid configurers; only the owner's runs",
                Names.key(modid, name),
                Names.key(candidate.modid, candidate.name));
        }
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

    /// The fluid registered for `material` in this shape, or null when the material does not generate it.
    Fluid fluid(Material material) {
        return fluidsByIndex.get(material.getIndex());
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

    /// Binds each material's still and flowing fluid icon from [#resolveIconPath]. Fluid textures live on the
    /// block atlas, so this runs from a blocks texture-stitch on the client (see [ShapeFluidIcons]).
    @SideOnly(Side.CLIENT)
    void registerIcons(IIconRegister register) {
        for (Material material : served.get()) {
            Fluid fluid = fluidsByIndex.get(material.getIndex());
            if (!(fluid instanceof MaterialFluid)) continue;
            fluid.setIcons(register.registerIcon(resolveIconPath(material)));
        }
    }

    /// The icon path to register for `material`'s fluid, or the [ShapeIcons#EMPTY_ICON] placeholder when no source
    /// carries one; see [ShapeIcons#resolvePath] for the order. Never null.
    String resolveIconPath(Material material) {
        return resolveIconPath(material, ShapeFluid::blockTextureExists);
    }

    /// As [#resolveIconPath(Material)], with `exists` deciding whether an icon path names a file on the block
    /// atlas.
    String resolveIconPath(Material material, Predicate<String> exists) {
        String path = ShapeIcons.resolvePath(material, List.of(name), this::patherPath, exists);
        if (path != null && path.startsWith(ShapeIcons.OVERRIDE_ROOT)) overrideIcons.add(material);
        else overrideIcons.remove(material);
        if (path != null) return path;
        if (warnedMissingIcon.add(material)) {
            MaterialLib.LOG.warn(
                "Fluid shape {} of {} resolved no icon; it will render the transparent placeholder",
                this,
                material.getKey());
        }
        return ShapeIcons.EMPTY_ICON;
    }

    /// Whether `material`'s fluid icon resolved from the resource-pack override location; see
    /// [ShapeItem#hasOverrideIcon].
    boolean hasOverrideIcon(Material material) {
        return overrideIcons.contains(material);
    }

    private String patherPath(Material material) {
        return iconPather != null ? iconPather.iconPath(this, material) : null;
    }

    private static boolean blockTextureExists(String path) {
        return ResourceUtil.resourceExists(ResourceUtil.getCompleteBlockTextureResourceLocation(path));
    }

    /// A material's fluid, serving the display name and fill tint the class doc describes through overrides.
    /// 1.7.10 fluids expose no color setter, only an overridable [Fluid#getColor].
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
        public String getLocalizedName() { return ShapeText.displayName(ShapeFluid.this, displayNameFormat, material); }

        @Override
        public int getColor() {
            if (hasOverrideIcon(material)) return 0xFFFFFF;
            return tintOf(material) & 0xFFFFFF;
        }
    }

    /// The ARGB fill tint for `material`'s fluid: [StandardProperties#FLUID_TINT] when set, or
    /// [StandardProperties#TINT] otherwise.
    static int tintOf(Material material) {
        Property<Integer> tint = material.getProperty(StandardProperties.FLUID_TINT) != null ?
            StandardProperties.FLUID_TINT : StandardProperties.TINT;
        return MaterialTints.color(material, tint);
    }
}
