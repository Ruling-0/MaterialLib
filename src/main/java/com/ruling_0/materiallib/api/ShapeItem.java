package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Objects;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

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

    private final String modid;
    private final String name;
    private final String oreDict;
    private final String displayNameFormat;

    /// Materials that generate this shape, ascending by index. Set when the registry resolves.
    private Material[] servedMaterials = new Material[0];

    private final Int2ObjectMap<IIcon> iconsByIndex = new Int2ObjectOpenHashMap<>();

    /// Creates an item shape. `oreDict` is the oredict prefix (the material name is appended, e.g. `gear` ->
    /// `gearIron`); `displayNameFormat` is applied to the material name to build the display name, e.g.
    /// `"%s Gear"`. Identifiers must be non-empty and free of ':' and whitespace.
    public ShapeItem(String modid, String name, String oreDict, String displayNameFormat) {
        this.modid = Names.validate("item shape modid", modid);
        this.name = Names.validate("item shape name", name);
        this.oreDict = Names.validate("item shape oredict", oreDict);
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

    @Override
    public String getModId() { return modid; }

    @Override
    public String getName() { return name; }

    @Override
    public String getOreDict() { return oreDict; }

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
            return super.getItemStackDisplayName(stack);
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
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return iconsByIndex.get(damage);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int renderPass) {
        Material material = materialFor(stack);
        return material != null ? material.getProperty(StandardProperties.TINT) : 0xFFFFFFFF;
    }
}
