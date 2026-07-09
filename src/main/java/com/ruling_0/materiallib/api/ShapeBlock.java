package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.client.ForgeHooksClient;

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
///
/// A variant block built by [ShapeBlockVariants] additionally falls back from its own icon (`<shapeName>_<variant>`)
/// to the plain shape name, and may draw an untinted base texture (e.g. a stone background) in the solid render
/// pass, under the tinted material icon drawn in the alpha pass; see [#registerBlockIcons] and [#canRenderInPass].
/// Drops, hardness,
/// resistance, and harvest level may be overridden per material and variant through [BlockShapeBuilder]'s behavior
/// hooks; a hook left unset preserves the vanilla default it replaces.
public class ShapeBlock extends Block implements BackedShape {

    private final String modid;
    private final String name;
    private final List<String> oreDicts;
    private final String displayNameFormat;
    private final String groupName;
    private final String variant;
    private final String baseTexture;
    private final BlockBehavior behavior;

    private final ServedMaterials served = new ServedMaterials();
    private final ShapeIcons icons = new ShapeIcons(false);
    private IIcon baseIcon;

    /// Creates a block shape backed by a [net.minecraft.block.material.Material#iron] block. `oreDicts` are the
    /// oredict prefixes, at least one; `displayNameFormat` is applied to the material name to build the display
    /// name, e.g. `"%s Block"`. Identifiers must be non-empty and free of ':' and whitespace.
    public ShapeBlock(String modid, String name, String displayNameFormat, String... oreDicts) {
        this(net.minecraft.block.material.Material.iron, modid, name, displayNameFormat, oreDicts);
    }

    /// Creates a block shape backed by a block of `blockMaterial`, for subclasses needing a non-metal block.
    public ShapeBlock(net.minecraft.block.material.Material blockMaterial, String modid, String name,
                      String displayNameFormat, String... oreDicts) {
        this(blockMaterial, modid, name, displayNameFormat, oreDicts, null, null, null, BlockBehavior.NONE);
    }

    /// Creates one variant's backing block for [ShapeBlockVariants], or the sole block of a variant-less shape
    /// with behavior hooks (`groupName`, `variant`, and `baseTexture` all null). `groupName` is the plain shape
    /// name, tried as an icon fallback after this variant's own name (see [#registerBlockIcons]); `variant` is
    /// this block's variant name, passed to `behavior`'s hooks; `baseTexture` is this variant's optional untinted
    /// background icon path, independent of any material's texture set, or null for none.
    ShapeBlock(String modid, String name, String displayNameFormat, String[] oreDicts, String groupName,
               String variant, String baseTexture, BlockBehavior behavior) {
        this(
            net.minecraft.block.material.Material.iron,
            modid,
            name,
            displayNameFormat,
            oreDicts,
            groupName,
            variant,
            baseTexture,
            behavior);
    }

    private ShapeBlock(net.minecraft.block.material.Material blockMaterial, String modid, String name,
                       String displayNameFormat, String[] oreDicts, String groupName, String variant,
                       String baseTexture, BlockBehavior behavior) {
        super(blockMaterial);
        this.modid = Names.validate("block shape modid", modid);
        this.name = Names.validate("block shape name", name);
        this.oreDicts = Names.validateOreDicts(oreDicts);
        this.displayNameFormat = ShapeNaming.requireValidFormat(displayNameFormat);
        this.groupName = groupName;
        this.variant = variant;
        this.baseTexture = baseTexture;
        this.behavior = behavior;
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
        List<String> candidates = groupName != null ? List.of(name, groupName) : List.of(name);
        icons.bind(register, served.get(), candidates);
        if (baseTexture != null) {
            baseIcon = register.registerIcon(baseTexture);
        }
    }

    /// A block with no base texture renders as a single tinted layer, as always. A block with a base texture
    /// renders in two passes -- the untinted base in the solid pass 0, and the tinted material icon over it in
    /// the alpha-blended pass 1 (pass 1 draws after pass 0, and the material texture's transparent pixels let
    /// the base show through). [#getIcon] and [#colorMultiplier] tell the two passes apart through
    /// [ForgeHooksClient#getWorldRenderPass], which [ForgeHooksClient] only sets to 0 or 1 around world chunk
    /// tessellation; it is -1 everywhere else, including the item form, which always shows the tinted material
    /// icon; see [#canRenderInPass].
    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderBlockPass() { return baseTexture != null ? 1 : 0; }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean canRenderInPass(int pass) {
        return baseTexture == null ? pass == 0 : pass == 0 || pass == 1;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (baseTexture != null && ForgeHooksClient.getWorldRenderPass() == 0) {
            return baseIcon;
        }
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
        if (baseTexture != null && ForgeHooksClient.getWorldRenderPass() == 0) {
            return 0xFFFFFF;
        }
        return tintFor(world.getBlockMetadata(x, y, z));
    }

    /// The RGB tint of the material at the given metadata, or white when the metadata maps to no live material.
    /// Block render colors carry no alpha, so the material's ARGB [StandardProperties#TINT] is masked to its low
    /// 24 bits.
    private static int tintFor(int meta) {
        Material material = MaterialRegistry.instance().getMaterialByIndex(meta);
        return material != null ? material.getProperty(StandardProperties.TINT) & 0xFFFFFF : 0xFFFFFF;
    }

    @Override
    public float getBlockHardness(World world, int x, int y, int z) {
        Material material = behavior.hardness() != null ? materialAt(world, x, y, z) : null;
        return material != null ? behavior.hardness().apply(material, variant) :
            super.getBlockHardness(world, x, y, z);
    }

    @Override
    public float getExplosionResistance(Entity exploder, World world, int x, int y, int z, double explosionX,
                                        double explosionY, double explosionZ) {
        Material material = behavior.resistance() != null ? materialAt(world, x, y, z) : null;
        return material != null ? behavior.resistance().apply(material, variant) :
            super.getExplosionResistance(exploder, world, x, y, z, explosionX, explosionY, explosionZ);
    }

    @Override
    public int getHarvestLevel(int metadata) {
        Material material = behavior.harvestLevel() != null ? materialFor(metadata) : null;
        return material != null ? behavior.harvestLevel().apply(material, variant) : super.getHarvestLevel(metadata);
    }

    /// The drops for a normal (non-silk-touch) break; see [BlockShapeBuilder#drops].
    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        Material material = behavior.drops() != null ? materialFor(metadata) : null;
        if (material == null) return super.getDrops(world, x, y, z, metadata, fortune);
        return new ArrayList<>(behavior.drops().drops(material, variant, fortune, false));
    }

    /// The single stack a silk-touch break picks up; see [BlockShapeBuilder#drops].
    @Override
    public ItemStack createStackedBlock(int metadata) {
        Material material = behavior.drops() != null ? materialFor(metadata) : null;
        if (material == null) return super.createStackedBlock(metadata);
        List<ItemStack> drops = behavior.drops().drops(material, variant, 0, true);
        return drops.isEmpty() ? null : drops.get(0);
    }

    private static Material materialAt(World world, int x, int y, int z) {
        return materialFor(world.getBlockMetadata(x, y, z));
    }

    private static Material materialFor(int metadata) {
        return MaterialRegistry.instance().getMaterialByIndex(metadata);
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
