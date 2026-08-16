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
/// ([Material#getIndex]).
///
/// Simple shapes are created through [MaterialLibAPI#newItemShape]. A mod that wants custom item behavior (right click
/// logic, NBT) subclasses this and registers the instance through [MaterialLibAPI#registerItemShape]. The base handles
/// subtypes, textures from each material's [TextureSet], the [StandardProperties#TINT] color, display names, and
/// oredict. Create and register the item inside the owning mod's [MaterialRegistrationEvent] handler. MaterialLib
/// registers the chosen owner's item under its own domain so the shape keeps a stable identity across instances. An
/// advanced tooltip names the mod that owns the shape and the mod that added the material.
public class ShapeItem extends Item implements BackedShape {

    private final String modid;
    private final String name;
    private final List<String> oreDicts;
    private final String displayNameFormat;

    private final ServedMaterials served = new ServedMaterials();
    private final ShapeProperties props = new ShapeProperties();
    private final ShapeIcons icons = new ShapeIcons(true);
    private String iconNameOverride;

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
    public ShapeProperties properties() {
        return props;
    }

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

    /// The name this shape's textures are filed under inside each texture set, defaulting to the shape name. A
    /// subclass registered through [MaterialLibAPI#registerItemShape] may override this to share another shape's
    /// art; shapes built through [ItemShapeBuilder] use [ItemShapeBuilder#iconName] instead.
    protected String iconName() {
        return iconNameOverride != null ? iconNameOverride : name;
    }

    void setIconName(String iconName) { this.iconNameOverride = Names.validate("item shape icon name", iconName); }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        icons.bind(register, served.get(), iconName());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamageForRenderPass(int damage, int renderPass) {
        return renderPass == 0 ? icons.get(damage) : icons.getOverlay(damage);
    }

    /// The material's icon, for callers that ask for a single icon. Vanilla's implementation returns the
    /// `itemIcon` field, which shape items never assign.
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return icons.get(damage);
    }

    /// The icon bound for `material` on this shape, or the transparent placeholder when none resolved. Valid only
    /// after the item atlas has stitched. Icons re-bind on every resource reload, so a caller compositing this icon
    /// itself must hold the shape and read the icon per use, never caching the returned [IIcon].
    @SideOnly(Side.CLIENT)
    public IIcon getMaterialIcon(Material material) {
        return icons.get(material.getIndex());
    }

    /// The `_OVERLAY` icon bound for `material` on this shape, or null when its resolved texture set has none;
    /// see [#getMaterialIcon] for the caching contract.
    @SideOnly(Side.CLIENT)
    public IIcon getMaterialOverlayIcon(Material material) {
        return icons.getOverlayOrNull(material.getIndex());
    }

    /// Whether `material`'s icon bound from the resource-pack override location (see [TextureSet]). Override art
    /// carries its own colors, so MaterialLib draws it untinted. A caller compositing the icon itself skips its own
    /// tint the same way.
    @SideOnly(Side.CLIENT)
    public boolean hasOverrideIcon(Material material) {
        return icons.isOverride(material.getIndex());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int renderPass) {
        if (renderPass != 0) return 0xFFFFFFFF;
        Material material = ShapeText.materialFor(stack);
        if (material == null || hasOverrideIcon(material)) return 0xFFFFFFFF;
        return material.getProperty(StandardProperties.TINT);
    }
}
