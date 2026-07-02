package com.ruling_0.materiallib.api;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The item backing an item [Shape]: one [Item] whose damage value is a material's global index
/// ([Material#getIndex]), so a single item carries every material that generates the shape.
///
/// Simple shapes are created through [MaterialLibAPI#newItemShape] and need no subclass. A mod that wants custom
/// item behavior (right click logic, NBT) subclasses this and registers the instance through
/// [MaterialLibAPI#registerItemShape]; the base handles subtypes, textures from each material's [TextureSet],
/// the [StandardProperties#TINT] color, display names, and oredict. Create and register the item during the
/// owning mod's preInit. MaterialLib registers the chosen owner's item under its own domain so the shape keeps a
/// stable identity across instances. An advanced tooltip names the mod that owns the shape and the mod that
/// added the material.
public class ShapeItem extends Item implements BackedShape {

    private final String modid;
    private final String name;
    private final List<String> oreDicts;
    private final String displayNameFormat;

    private final ServedMaterials served = new ServedMaterials();
    private final ShapeIcons icons = new ShapeIcons();

    /// Creates an item shape. `oreDicts` are the oredict prefixes, at least one, each with the material name
    /// appended (e.g. `gear` -> `gearIron`); `displayNameFormat` is applied to the material name to build the
    /// display name, e.g. `"%s Gear"`. Identifiers must be non-empty and free of ':' and whitespace.
    public ShapeItem(String modid, String name, String displayNameFormat, String... oreDicts) {
        this.modid = Names.validate("item shape modid", modid);
        this.name = Names.validate("item shape name", name);
        this.oreDicts = Names.validateOreDicts(oreDicts);
        this.displayNameFormat = ShapeNaming.requireValidFormat(displayNameFormat);
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
    public List<String> getOreDicts() { return oreDicts; }

    @Override
    public String toString() {
        return "ShapeItem[" + Names.key(modid, name) + "]";
    }

    @Override
    public void registerWithGame() {
        GameRegistry.registerItem(this, name);
    }

    @Override
    public void bindServedMaterials(Material[] materials) {
        served.bind(this, materials);
    }

    @Override
    public Material[] getServedMaterials() { return served.get(); }

    @Override
    public ItemStack getStack(Material material, int amount) {
        return new ItemStack(this, amount, material.getIndex());
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return ShapeText.displayName(this, displayNameFormat, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advanced) {
        super.addInformation(stack, player, lines, advanced);
        ShapeText.appendTooltip(lines, modid, stack, advanced);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> list) {
        for (Material material : served.get()) {
            list.add(getStack(material, 1));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        icons.bind(register, served.get(), name);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return icons.get(damage);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int renderPass) {
        Material material = ShapeText.materialFor(stack);
        return material != null ? material.getProperty(StandardProperties.TINT) : 0xFFFFFFFF;
    }
}
