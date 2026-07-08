package com.ruling_0.materiallib.api;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The block backing a block [Shape]: one [Block] whose metadata is a material's global index
/// ([Material#getIndex]), so a single block carries every material that generates the shape.
///
/// Simple shapes are created through [MaterialLibAPI#newBlockShape]. A mod wanting custom block behavior subclasses
/// this and registers the instance through [MaterialLibAPI#registerBlockShape]. The base handles per-material textures
/// from each material's [TextureSet], the [StandardProperties#TINT] color, creative-tab variants, and dropping the
/// placed metadata. Create and register the block inside the owning mod's [MaterialRegistrationEvent] handler.
/// MaterialLib registers the chosen owner's block under its own domain so the shape keeps a stable identity across
/// instances, and the block's item shows the same display name and advanced-tooltip attribution as an item shape.
public class ShapeBlock extends Block implements BackedShape {

    private final String modid;
    private final String name;
    private final List<String> oreDicts;
    private final String displayNameFormat;

    private final ServedMaterials served = new ServedMaterials();
    private final ShapeIcons icons = new ShapeIcons(false);

    /// Creates a block shape backed by a [net.minecraft.block.material.Material#iron] block. `oreDicts` are the
    /// oredict prefixes, at least one; `displayNameFormat` is applied to the material name to build the display
    /// name, e.g. `"%s Block"`. Identifiers must be non-empty and free of ':' and whitespace.
    public ShapeBlock(String modid, String name, String displayNameFormat, String... oreDicts) {
        this(net.minecraft.block.material.Material.iron, modid, name, displayNameFormat, oreDicts);
    }

    /// Creates a block shape backed by a block of `blockMaterial`, for subclasses needing a non-metal block.
    public ShapeBlock(net.minecraft.block.material.Material blockMaterial, String modid, String name,
                      String displayNameFormat, String... oreDicts) {
        super(blockMaterial);
        this.modid = Names.validate("block shape modid", modid);
        this.name = Names.validate("block shape name", name);
        this.oreDicts = Names.validateOreDicts(oreDicts);
        this.displayNameFormat = ShapeNaming.requireValidFormat(displayNameFormat);
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setCreativeTab(CreativeTabs.tabBlock);
        setBlockName(modid + "." + name);
    }

    @Override
    public String getModId() { return modid; }

    @Override
    public String getName() { return name; }

    @Override
    public List<String> getOreDicts() { return oreDicts; }

    @Override
    public String toString() {
        return "ShapeBlock[" + Names.key(modid, name) + "]";
    }

    @Override
    public void registerWithGame() {
        GameRegistry.registerBlock(this, ShapeBlockItem.class, name);
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
    public int damageDropped(int meta) {
        return meta;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {
        for (Material material : served.get()) {
            list.add(getStack(material, 1));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons.bind(register, served.get(), name);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return icons.get(meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int meta) {
        return tintFor(meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return tintFor(world.getBlockMetadata(x, y, z));
    }

    /// The RGB tint of the material at the given metadata, or white when the metadata maps to no live material.
    /// Block render colors carry no alpha, so the material's ARGB [StandardProperties#TINT] is masked to its low
    /// 24 bits.
    private static int tintFor(int meta) {
        Material material = MaterialRegistry.instance().getMaterialByIndex(meta);
        return material != null ? material.getProperty(StandardProperties.TINT) & 0xFFFFFF : 0xFFFFFF;
    }

    /// The item form of a [ShapeBlock], carrying the placed metadata onto the stack so each material is a separate
    /// item, and showing the same display name and tooltip as an item shape.
    public static class ShapeBlockItem extends ItemBlock {

        private final ShapeBlock shape;

        public ShapeBlockItem(Block block) {
            super(block);
            if (!(block instanceof ShapeBlock shapeBlock)) {
                throw new IllegalArgumentException("ShapeBlockItem must back a ShapeBlock, got " + block);
            }
            this.shape = shapeBlock;
            setHasSubtypes(true);
            setMaxDamage(0);
        }

        @Override
        public int getMetadata(int damage) {
            return damage;
        }

        @Override
        public String getItemStackDisplayName(ItemStack stack) {
            return ShapeText.displayName(shape, shape.displayNameFormat, stack);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advanced) {
            super.addInformation(stack, player, lines, advanced);
            ShapeText.appendTooltip(lines, shape.getModId(), stack, advanced);
        }
    }
}
