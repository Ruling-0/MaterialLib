package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;

/// The group of backing [ShapeBlock]s for one block [Shape] declared with [BlockShapeBuilder#variants], one
/// registered block per variant. Each block is named `<shapeName>_<variant>` (see [ShapeNaming#variantBlockName]),
/// so it keeps a stable save identity of its own. Materials generate the group once, through the [Shape] this class
/// implements, and every variant block serves the same materials. A shape with no declared variants is a plain
/// [ShapeBlock] instead.
final class ShapeBlockVariants implements BackedShape {

    private final String modid;
    private final String name;
    private final List<String> oreDicts;
    private final VariantSet<ShapeBlock> blocks;

    private final ServedMaterials served = new ServedMaterials();
    private final ShapeProperties props = new ShapeProperties();

    /// The per-variant blocks are separate [Shape] instances that [ShapeRegistry#getBlockShapes] hands out, so
    /// their property holders redirect to the group's: a property declared on the group reads back from any of
    /// them, and there is one map to freeze.
    private ShapeBlockVariants(String modid, String name, List<String> oreDicts, VariantSet<ShapeBlock> blocks) {
        this.modid = modid;
        this.name = name;
        this.oreDicts = oreDicts;
        this.blocks = blocks;
        for (ShapeBlock block : blocks.values()) {
            block.properties().redirectTo(props);
        }
    }

    /// Builds the variant group and its backing blocks, one per name in `variantNames`, each registered as
    /// `<name>_<variant>`. Each block draws the base texture `variantBases` declares for its variant, if any (see
    /// [BlockShapeBuilder#variantBase]). All blocks share `behavior`, `iconPather` and `iconName` (null for none).
    /// Fails when `variantBases` names a variant not in `variantNames`.
    static ShapeBlockVariants create(String modid, String name, String[] oreDicts,
                                     List<String> variantNames, Map<String, String> variantBases,
                                     BlockBehavior behavior, IconPather iconPather, String iconName) {
        List<String> validatedOreDicts = Names.validateOreDicts(oreDicts);
        requireDeclaredVariants(variantNames, variantBases.keySet(), "a variant base texture");
        String[] oreDictsArray = validatedOreDicts.toArray(new String[0]);
        VariantSet<ShapeBlock> blocks = VariantSet.of(variantNames, variant -> {
            ShapeBlock block = new ShapeBlock(
                modid,
                ShapeNaming.variantBlockName(name, variant),
                oreDictsArray,
                name,
                variant,
                variantBases.get(variant),
                behavior,
                iconPather);
            if (iconName != null) {
                block.setIconName(iconName);
            }
            return block;
        });
        return new ShapeBlockVariants(modid, name, validatedOreDicts, blocks);
    }

    /// Rejects a key in `keys` (e.g. a [BlockShapeBuilder#variantBase] target) that names a variant not in
    /// `variantNames`.
    static void requireDeclaredVariants(List<String> variantNames, Set<String> keys, String what) {
        for (String key : keys) {
            if (!variantNames.contains(key)) {
                throw new IllegalArgumentException(
                    what + " was declared for variant \"" + key + "\", which is not one of the declared variants " +
                        variantNames);
            }
        }
    }

    @Override
    public String getModId() { return modid; }

    @Override
    public String getName() { return name; }

    @Override
    public List<String> getOreDicts() { return oreDicts; }

    @Override
    public List<String> getVariants() { return blocks.names(); }

    /// The backing block of `variant`. Fails when `variant` was not declared.
    ShapeBlock blockFor(String variant) {
        return blocks.get(variant);
    }

    @Override
    public void registerWithGame() {
        for (ShapeBlock block : blocks.values()) {
            block.registerWithGame();
        }
    }

    @Override
    public void bindServedMaterials(Material[] materials) {
        served.bind(this, materials);
        for (ShapeBlock block : blocks.values()) {
            block.bindServedMaterials(materials);
        }
    }

    @Override
    public Material[] getServedMaterials() { return served.get(); }

    @Override
    public boolean serves(Material material) {
        return served.contains(material);
    }

    @Override
    public ShapeProperties properties() {
        return props;
    }

    /// The itemstack of `material` in the first declared variant; see [VariantSet#first].
    @Override
    public ItemStack getStack(Material material, int amount) {
        return blocks.first().getStack(material, amount);
    }

    /// The itemstack of `material` in the given variant, with the given stack size. Fails when `variant` was not
    /// declared.
    ItemStack getStack(Material material, String variant, int amount) {
        return blocks.get(variant).getStack(material, amount);
    }

    @Override
    public String toString() {
        return "ShapeBlockVariants[" + Names.key(modid, name) + " " + blocks.names() + "]";
    }
}
