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

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.ruling_0.materiallib.MaterialLib;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The block backing a block [Shape]: one [Block] whose metadata is a material's global index
/// ([Material#getIndex]), so a single block carries every material that generates the shape.
///
/// Simple shapes are created through [MaterialLibAPI#newBlockShape]. A mod wanting custom block behavior subclasses
/// this and registers the instance through [MaterialLibAPI#registerBlockShape]. The base handles per-material textures
/// from each material's [TextureSet], the [StandardProperties#TINT] color, creative-tab variants, and dropping the
/// placed metadata; a material may instead be given a per-material icon ahead of its texture set through
/// [#iconPathFor] (a subclass override) or a [BlockShapeBuilder#iconPath] pather. Create and register the block
/// inside the owning mod's [MaterialRegistrationEvent] handler.
/// MaterialLib registers the chosen owner's block under its own domain so the shape keeps a stable identity across
/// instances, and the block's item shows the same display name and advanced-tooltip attribution as an item shape.
///
/// A variant block built by [ShapeBlockVariants] additionally falls back from its own icon (`<shapeName>_<variant>`)
/// to the plain shape name, and may draw an untinted base texture (e.g. a stone background) under the tinted
/// material icon, composited by [ShapeBlockRenderingHandler]; see [#registerBlockIcons] and [#hasBaseTexture].
/// Drops, hardness, resistance, and harvest level may be overridden per material and variant, and the harvest tool
/// class per shape, through [BlockShapeBuilder]'s behavior hooks; a hook left unset preserves the vanilla default
/// it replaces.
public class ShapeBlock extends Block implements BackedShape {

    private final String modid;
    private final String name;
    private final List<String> oreDicts;
    private final String displayNameFormat;
    private final String groupName;
    private final String variant;
    private final String baseTexture;
    private final BlockBehavior behavior;
    private final BlockIconPather iconPather;

    private final ServedMaterials served = new ServedMaterials();
    private final ShapeProperties props = new ShapeProperties();
    private final ShapeIcons icons = new ShapeIcons(false);
    private String iconNameOverride;
    private IIcon baseIcon;
    private boolean warnedMissingBaseTexture;
    private int renderType = 0;

    /// Creates a block shape backed by a [net.minecraft.block.material.Material#iron] block. `oreDicts` are the
    /// oredict prefixes, at least one; `displayNameFormat` is applied to the material name to build the display
    /// name, e.g. `"%s Block"`. Identifiers must be non-empty and free of ':' and whitespace.
    public ShapeBlock(String modid, String name, String displayNameFormat, String... oreDicts) {
        this(net.minecraft.block.material.Material.iron, modid, name, displayNameFormat, oreDicts);
    }

    /// Creates a block shape backed by a block of `blockMaterial`, for subclasses needing a non-metal block.
    public ShapeBlock(net.minecraft.block.material.Material blockMaterial, String modid, String name,
                      String displayNameFormat, String... oreDicts) {
        this(blockMaterial, modid, name, displayNameFormat, oreDicts, null, null, null, BlockBehavior.NONE, null);
    }

    /// Creates one variant's backing block for [ShapeBlockVariants], or the sole block of a variant-less shape
    /// with behavior hooks (`groupName`, `variant`, and `baseTexture` all null). `groupName` is the plain shape
    /// name, tried as an icon fallback after this variant's own name (see [#registerBlockIcons]); `variant` is
    /// this block's variant name, passed to `behavior`'s hooks; `baseTexture` is this variant's optional untinted
    /// background icon path, independent of any material's texture set, or null for none; `iconPather` is this
    /// shape's optional [BlockShapeBuilder#iconPath] override, or null for none.
    ShapeBlock(String modid, String name, String displayNameFormat, String[] oreDicts, String groupName,
               String variant, String baseTexture, BlockBehavior behavior, BlockIconPather iconPather) {
        this(
            net.minecraft.block.material.Material.iron,
            modid,
            name,
            displayNameFormat,
            oreDicts,
            groupName,
            variant,
            baseTexture,
            behavior,
            iconPather);
    }

    private ShapeBlock(net.minecraft.block.material.Material blockMaterial, String modid, String name,
                       String displayNameFormat, String[] oreDicts, String groupName, String variant,
                       String baseTexture, BlockBehavior behavior, BlockIconPather iconPather) {
        super(blockMaterial);
        this.modid = Names.validate("block shape modid", modid);
        this.name = Names.validate("block shape name", name);
        this.oreDicts = Names.validateOreDicts(oreDicts);
        this.displayNameFormat = ShapeNaming.requireValidFormat(displayNameFormat);
        this.groupName = groupName;
        this.variant = variant;
        this.baseTexture = baseTexture;
        this.behavior = behavior;
        this.iconPather = iconPather;
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

    /// The declared shape name of the [ShapeBlockVariants] group this block backs, or null when this block is not
    /// a variant's backing block.
    String getGroupName() { return groupName; }

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
    public ShapeProperties properties() {
        return props;
    }

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

    /// The name this block's textures are filed under inside each texture set, defaulting to the shape name -- or,
    /// for one variant's backing block, to the variant group's name, which [#registerBlockIcons] then extends with
    /// the variant. A subclass registered through [MaterialLibAPI#registerBlockShape] may override this to share
    /// another shape's art; shapes built through [BlockShapeBuilder] use [BlockShapeBuilder#iconName] instead.
    protected String iconName() {
        if (iconNameOverride != null) return iconNameOverride;
        return groupName != null ? groupName : name;
    }

    void setIconName(String iconName) { this.iconNameOverride = Names.validate("block shape icon name", iconName); }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        String iconName = iconName();
        List<String> candidates = groupName != null ?
            List.of(ShapeNaming.variantBlockName(iconName, variant), iconName) : List.of(iconName);
        icons.bind(register, served.get(), candidates, this::iconPathFor);
        if (baseTexture != null) {
            baseIcon = registerBaseIcon(register);
        }
    }

    /// Registers [#baseTexture] if it names an existing file, or the [ShapeIcons#EMPTY_ICON] placeholder if it
    /// does not.
    private IIcon registerBaseIcon(IIconRegister register) {
        if (ResourceUtil.resourceExists(ResourceUtil.getCompleteBlockTextureResourceLocation(baseTexture))) {
            return register.registerIcon(baseTexture);
        }
        if (!warnedMissingBaseTexture) {
            warnedMissingBaseTexture = true;
            MaterialLib.LOG.warn(
                "Block shape {} variant {} has no base texture at {}; it will render the empty placeholder instead",
                name,
                variant,
                baseTexture);
        }
        return register.registerIcon(ShapeIcons.EMPTY_ICON);
    }

    /// Whether this variant draws a base texture layer under the tinted material icon; see [#registerBlockIcons].
    public boolean hasBaseTexture() {
        return baseTexture != null;
    }

    /// Sets the render type [#getRenderType] reports: [ShapeBlockRenderingHandler]'s render ID for a
    /// [#hasBaseTexture] composite, or the vanilla full-cube default (0).
    @SideOnly(Side.CLIENT)
    public void setRenderType(int renderType) { this.renderType = renderType; }

    @Override
    public int getRenderType() { return renderType; }

    /// This variant's base texture icon, or null when it declares none.
    @SideOnly(Side.CLIENT)
    IIcon baseIcon() {
        return baseIcon;
    }

    /// The material icon bound at the given metadata; see [ShapeIcons#get].
    @SideOnly(Side.CLIENT)
    IIcon materialIcon(int meta) {
        return icons.get(meta);
    }

    /// The icon path to try for `material` before this shape's texture-set candidates, or null to skip straight
    /// to them. The default implementation defers to this block's [BlockIconPather]. A subclass may override this
    /// directly.
    protected String iconPathFor(Material material) {
        return iconPather != null ? iconPather.iconPath(this, material) : null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (baseTexture != null) {
            return baseIcon;
        }
        return icons.get(meta);
    }

    /// The icon bound for `material` on this shape, or the transparent placeholder when none resolved. Valid only
    /// after the block atlas has stitched. Icons re-bind on every resource reload, so a caller compositing this
    /// icon itself must hold the shape and call this per use rather than cache the returned [IIcon].
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

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int meta) {
        if (baseTexture != null) {
            return 0xFFFFFF;
        }
        return tintFor(meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        if (baseTexture != null) {
            return 0xFFFFFF;
        }
        return tintFor(world.getBlockMetadata(x, y, z));
    }

    /// The RGB tint of the material at the given metadata, or white when the metadata maps to no live material:
    /// [StandardProperties#BLOCK_OVERLAY_TINT] for a [#hasBaseTexture] composite's overlay layer,
    /// [StandardProperties#BLOCK_TINT] for a plain block, [StandardProperties#TINT] when the specific property is
    /// unset. Block render colors carry no alpha, so the resolved ARGB value is masked to its low 24 bits.
    @SideOnly(Side.CLIENT)
    int tintFor(int meta) {
        Material material = MaterialRegistry.instance().getMaterialByIndex(meta);
        if (material == null) return 0xFFFFFF;
        Integer override = material.getProperty(
            baseTexture != null ? StandardProperties.BLOCK_OVERLAY_TINT : StandardProperties.BLOCK_TINT);
        if (override != null) return override & 0xFFFFFF;
        return material.getProperty(StandardProperties.TINT) & 0xFFFFFF;
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
        // The hook takes setResistance units; vanilla stores blockResistance * 3 and getExplosionResistance
        // divides by 5, so the same conversion keeps hook values comparable to setResistance calls.
        return material != null ? behavior.resistance().apply(material, variant) * 3.0F / 5.0F :
            super.getExplosionResistance(exploder, world, x, y, z, explosionX, explosionY, explosionZ);
    }

    /// Forge's harvest gate only consults [#getHarvestLevel] when a harvest tool class is set (see
    /// ForgeHooks.canHarvestBlock), so a harvest level hook with no declared tool defaults to pickaxe,
    /// matching legacy GregTech ore blocks.
    @Override
    public String getHarvestTool(int metadata) {
        if (behavior.harvestTool() != null) return behavior.harvestTool();
        return behavior.harvestLevel() != null ? "pickaxe" : super.getHarvestTool(metadata);
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
