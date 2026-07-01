package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.renderer.texture.IIconRegister;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// The Forge fluid backing a fluid [Shape]: one registered [Fluid] per material that generates the shape, named
/// `<shape>.<material>` lowercased (e.g. `test.testiron`).
///
/// Forge's fluid registry is keyed by a flat name with no mod domain, and fluid stacks persist in NBT by that name,
/// so a fluid shape needs no metadata mapping -- only the naming convention. A fluid shape is therefore not a
/// [BackedShape]: it registers no item or block, carries no oredict, and builds [FluidStack]s rather than item
/// stacks. Materials declare it through [MaterialBuilder#generateShape]; the registry registers one fluid per
/// material at resolve and, on the client, binds each fluid's still and flowing icons from the material's
/// [TextureSet].
///
/// A bare fluid has no item form, so its material tooltip is carried by its container item (see
/// [ShapeFluidInContainer]). Each fluid takes its display name from the shape's format and its color from the
/// material's [StandardProperties#TINT], so tank and GUI renderers show the right name and tint without a custom
/// fluid block.
public class ShapeFluid implements ServedShape {

    private static final Logger LOG = LogManager.getLogger("materiallib");
    private static final List<String> NO_OREDICTS = List.of();

    private final String modid;
    private final String name;
    private final String displayNameFormat;

    private final ServedMaterials served = new ServedMaterials();

    private final Int2ObjectMap<Fluid> fluidsByIndex = new Int2ObjectOpenHashMap<>();

    /// Creates a fluid shape. `displayNameFormat` is applied to the material name to build the fluid's display
    /// name, e.g. `"Molten %s"`. Identifiers must be non-empty and free of ':' and whitespace.
    public ShapeFluid(String modid, String name, String displayNameFormat) {
        this.modid = Names.validate("fluid shape modid", modid);
        this.name = Names.validate("fluid shape name", name);
        this.displayNameFormat = ShapeNaming.requireValidFormat(displayNameFormat);
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

    /// The Forge fluid name for a material in this shape, e.g. `test.testiron`.
    String fluidName(Material material) {
        return (name + "." + material.getName()).toLowerCase(Locale.ENGLISH);
    }

    /// Registers one Forge fluid per served material.
    void registerFluids() {
        fluidsByIndex.clear();
        for (Material material : served.get()) {
            String fluidName = fluidName(material);
            Fluid fluid = new MaterialFluid(fluidName, material);
            if (!FluidRegistry.registerFluid(fluid)) {
                fluid = FluidRegistry.getFluid(fluidName);
                LOG.warn(
                    "Fluid {} of {} is already registered elsewhere; its tint, name, and icons will not apply",
                    fluidName,
                    material.getKey());
            }
            fluidsByIndex.put(material.getIndex(), fluid);
        }
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

    /// Binds each material's still and flowing fluid icon from its texture set. Fluid textures live on the block
    /// atlas, so this runs from a blocks texture-stitch on the client (see [ShapeFluidIcons]).
    @SideOnly(Side.CLIENT)
    void registerIcons(IIconRegister register) {
        for (Material material : served.get()) {
            Fluid fluid = fluidsByIndex.get(material.getIndex());
            if (!(fluid instanceof MaterialFluid)) continue;
            TextureSet textureSet = material.getProperty(StandardProperties.TEXTURE_SET);
            fluid.setIcons(register.registerIcon(textureSet.iconPath(name)));
        }
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
        public int getColor() { return material.getProperty(StandardProperties.TINT) & 0xFFFFFF; }
    }
}
