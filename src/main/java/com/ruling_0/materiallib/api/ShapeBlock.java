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
/// material icon; see [#registerBlockIcons]. [ShapeBlockRenderingHandler] composites both layers -- in world and in
/// every item form (GUI slot, hotbar, held, and dropped) -- in a single draw per block, driven by [#getIcon],
/// [#getRenderColor], and [#colorMultiplier] through [#layerOverride]; see [#hasBaseTexture]. Drops, hardness,
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
    private final BlockIconPather iconPather;

    private final ServedMaterials served = new ServedMaterials();
    private final ShapeIcons icons = new ShapeIcons(false);
    private IIcon baseIcon;
    private boolean warnedMissingBaseTexture;
    private int layerOverride = -1;
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
        icons.bind(register, served.get(), candidates, this::iconPathFor);
        if (baseTexture != null) {
            baseIcon = registerBaseIcon(register);
        }
    }

    /// Registers [#baseTexture] if it names an existing file, or the [ShapeIcons#EMPTY_ICON] placeholder -- logged
    /// once -- if it does not, the same existence-checked fallback [ShapeIcons] uses for a material's texture-set
    /// icon. Without this check a bad base texture path renders as Minecraft's own unlogged missing-texture
    /// checkerboard instead of a diagnosable warning. The existence check and the registration both resolve
    /// [#baseTexture] itself -- never a derived or re-formatted copy of it -- so they always agree; see
    /// [com.gtnewhorizon.gtnhlib.util.ResourceUtil#getCompleteBlockTextureResourceLocation] for how that single
    /// string maps to the file the check looks for.
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
    /// [ClientProxy][com.ruling_0.materiallib.ClientProxy] uses this to decide which block shapes need
    /// [#setRenderType] pointed at [ShapeBlockRenderingHandler] -- a plain block shape has nothing to composite and
    /// keeps the vanilla full-cube render type (0).
    public boolean hasBaseTexture() {
        return baseTexture != null;
    }

    /// This block's [#getRenderType]; [ClientProxy][com.ruling_0.materiallib.ClientProxy] sets this once, at
    /// preInit, to [ShapeBlockRenderingHandler]'s render ID for every block [#hasBaseTexture]. Never called on the
    /// dedicated server, where [ShapeBlockRenderingHandler] itself must never classload.
    public void setRenderType(int renderType) { this.renderType = renderType; }

    @Override
    public int getRenderType() { return renderType; }

    /// Forces [#getIcon], [#getRenderColor], and [#colorMultiplier] to resolve the base layer (0) or the tinted
    /// overlay layer (1) regardless of metadata, or -1 to resolve the base layer by default.
    /// [ShapeBlockRenderingHandler] toggles this around each of the two draws its single [#hasBaseTexture]
    /// composite needs -- both the world and the item-form (GUI slot, hotbar, held, and dropped) renderers set it
    /// explicitly before every draw, so -1 is only reached by other code that calls [#getIcon] or [#getRenderColor]
    /// directly, e.g. a mod inspecting an icon outside `RenderItem` entirely. The untinted base is a recognizable
    /// icon in that case, where the tinted overlay alone renders as a transparent slot with a few floating tinted
    /// flecks, since the overlay icon is a sparse, mostly-transparent layer meant to be drawn over the base, never
    /// standalone.
    void setLayerOverride(int layer) { layerOverride = layer; }

    /// The icon path to try for `material` before this shape's texture-set candidates, or null to skip straight
    /// to them; consulted once per served material in [#registerBlockIcons]. The default implementation defers to
    /// this block's [BlockIconPather], set through [BlockShapeBuilder#iconPath] and null when unset. A shape
    /// registered as a [ShapeBlock] subclass through [MaterialLibAPI#registerBlockShape] may override this
    /// directly instead of going through a pather.
    protected String iconPathFor(Material material) {
        return iconPather != null ? iconPather.iconPath(this, material) : null;
    }

    /// Signals to vanilla and Forge's item-rendering dispatch (`RenderItem`, `ItemRenderer`) that this block's item
    /// form has translucent content whenever it [#hasBaseTexture] -- those callers enable alpha blending around the
    /// composite draw precisely when this returns nonzero, and [ShapeBlockRenderingHandler]'s single world draw
    /// participates in exactly the render pass this names ([net.minecraft.block.Block#canRenderInPass] defaults to
    /// `pass == getRenderBlockPass()`), so the overlay's transparent pixels are blended, not just alpha-tested.
    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderBlockPass() { return baseTexture != null ? 1 : 0; }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (baseTexture != null && layerOverride != 1) {
            return baseIcon;
        }
        return icons.get(meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int meta) {
        if (baseTexture != null && layerOverride != 1) {
            return 0xFFFFFF;
        }
        return tintFor(meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        if (baseTexture != null && layerOverride != 1) {
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
