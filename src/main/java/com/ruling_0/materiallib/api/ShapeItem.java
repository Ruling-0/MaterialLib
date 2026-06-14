package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import com.ruling_0.materiallib.MaterialLib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/// The item backing an item [Shape]: one [Item] whose damage value is a material's global index
/// ([Material#getIndex]), so a single item carries every material that generates the shape.
///
/// Simple shapes are created through [MaterialLibAPI#newItemShape] and need no subclass. A mod that wants custom
/// item behavior (right click logic, NBT) subclasses this and registers the instance through
/// [MaterialLibAPI#registerItemShape]; the base handles subtypes, textures from each material's [TextureSet],
/// the [StandardProperties#TINT] color, display names, and oredict. The item must be created and registered
/// during the owning mod's preInit so FML attributes it to that mod.
public class ShapeItem extends Item implements Shape {

    private static final String MISSING_ICON = MaterialLib.MODID + ":missing_material";

    private final String modid;
    private final String name;
    private final List<String> oreDicts;
    private final String displayNameFormat;

    /// Materials that generate this shape, ascending by index. Set when the registry resolves.
    private Material[] servedMaterials = new Material[0];

    private final Int2ObjectMap<IIcon> iconsByIndex = new Int2ObjectOpenHashMap<>();

    /// The placeholder icon shown for a damage value that maps to no live material (a reserved or unknown index).
    private IIcon missingIcon;

    /// Creates an item shape. `oreDicts` are the oredict prefixes, at least one, each with the material name
    /// appended (e.g. `gear` -> `gearIron`); `displayNameFormat` is applied to the material name to build the
    /// display name, e.g. `"%s Gear"`. Identifiers must be non-empty and free of ':' and whitespace.
    public ShapeItem(String modid, String name, String displayNameFormat, String... oreDicts) {
        this.modid = Names.validate("item shape modid", modid);
        this.name = Names.validate("item shape name", name);
        this.oreDicts = validateOreDicts(oreDicts);
        this.displayNameFormat = Objects.requireNonNull(displayNameFormat, "displayNameFormat must not be null");
        try {
            ShapeNaming.format(displayNameFormat, "");
        }
        catch (RuntimeException e) {
            throw new IllegalArgumentException(
                "displayNameFormat \"" + displayNameFormat + "\" is not a valid format string", e);
        }
        setHasSubtypes(true);
        setMaxDamage(0);
        setCreativeTab(CreativeTabs.tabMaterials);
        setUnlocalizedName(modid + "." + name);
    }

    private static List<String> validateOreDicts(String... oreDicts) {
        Objects.requireNonNull(oreDicts, "oreDicts must not be null");
        if (oreDicts.length == 0) {
            throw new IllegalArgumentException("item shape requires at least one oredict prefix");
        }
        List<String> validated = new ArrayList<>(oreDicts.length);
        for (String oreDict : oreDicts) {
            validated.add(Names.validate("item shape oredict", oreDict));
        }
        return List.copyOf(validated);
    }

    @Override
    public String getModId() { return modid; }

    @Override
    public String getName() { return name; }

    @Override
    public List<String> getOreDicts() { return oreDicts; }

    @Override
    public String toString() {
        return "ShapeItem[" + Names.key(modid, name) + "]";
    }

    void bindServedMaterials(Material[] materials) {
        this.servedMaterials = materials;
    }

    Material[] getServedMaterials() { return servedMaterials; }

    /// The itemstack of `material` in this shape, with the given stack size. The damage is the material's global
    /// index, so the stack is the same across launches for the same material set.
    public ItemStack getStack(Material material, int amount) {
        return new ItemStack(this, amount, material.getIndex());
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        Material material = materialFor(stack);
        if (material == null) {
            return missingMaterialName(stack.getItemDamage());
        }
        String overrideKey = ShapeNaming.overrideKey(this, material);
        if (StatCollector.canTranslate(overrideKey)) {
            return StatCollector.translateToLocal(overrideKey);
        }
        return ShapeNaming.format(displayNameFormat, localizedMaterialName(material));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean aF3_H) {
        super.addInformation(stack, player, lines, aF3_H);
        Material material = materialFor(stack);
        if (material == null) return;
        if (material.hasCustomTooltip()) {
            for (String line : material.getTooltip()) {
                lines.add(StatCollector.translateToLocal(line));
            }
        }
        for (Family family : material.getFamilies()) {
            if (family.hasCustomTooltip()) {
                for (String line : family.getTooltip()) {
                    lines.add(StatCollector.translateToLocal(line));
                }
            }
        }
    }

    /// The material a stack of this shape represents, decoding the damage value, or null if the damage maps to no
    /// material (a stale or hand-edited stack).
    private static Material materialFor(ItemStack stack) {
        return MaterialRegistry.instance()
            .getMaterialByIndex(stack.getItemDamage());
    }

    private static String localizedMaterialName(Material material) {
        String key = ShapeNaming.materialNameKey(material);
        return StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key) : material.getName();
    }

    /// The display name for a stack whose damage maps to no live material: the material was removed but its index
    /// stays reserved, so the stack is preserved under a visible placeholder name rather than transformed.
    private static String missingMaterialName(int index) {
        String key = "item.materiallib.missingMaterial";
        return StatCollector.canTranslate(key) ? StatCollector.translateToLocalFormatted(key, index) :
            "Missing Material #" + index;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> list) {
        for (Material material : servedMaterials) {
            list.add(getStack(material, 1));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        iconsByIndex.clear();
        for (Material material : servedMaterials) {
            TextureSet textureSet = material.getProperty(StandardProperties.TEXTURE_SET);
            iconsByIndex.put(material.getIndex(), register.registerIcon(textureSet.iconPath(name)));
        }
        missingIcon = register.registerIcon(MISSING_ICON);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        IIcon icon = iconsByIndex.get(damage);
        return icon != null ? icon : missingIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int renderPass) {
        Material material = materialFor(stack);
        return material != null ? material.getProperty(StandardProperties.TINT) : 0xFFFFFFFF;
    }
}
