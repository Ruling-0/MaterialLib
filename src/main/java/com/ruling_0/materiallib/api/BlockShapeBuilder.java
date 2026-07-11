package com.ruling_0.materiallib.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Builds and registers a simple block [Shape] backed by a [ShapeBlock]. Obtained from
/// [MaterialLibAPI#newBlockShape] and finished with [#build], which must be called inside the owning mod's
/// [MaterialRegistrationEvent] handler. Mods needing custom block behavior subclass [ShapeBlock] instead and
/// register through [MaterialLibAPI#registerBlockShape].
public final class BlockShapeBuilder {

    private final String modid;
    private final String name;
    private String[] oreDicts;
    private String displayNameFormat;
    private String[] variants;
    private final Map<String, String> variantBases = new LinkedHashMap<>();
    private BlockDropFunction dropsFn;
    private BlockFloatFunction hardnessFn;
    private BlockFloatFunction resistanceFn;
    private BlockHarvestLevelFunction harvestLevelFn;
    private BlockIconPather iconPather;
    private boolean built;

    BlockShapeBuilder(String modid, String name) {
        this.modid = modid;
        this.name = name;
    }

    /// Sets the oredict prefixes; the material name is appended to each (e.g. `block` -> `blockIron`). Pass several
    /// to register the block under each. Defaults to the shape name. At least one prefix is required.
    public BlockShapeBuilder oreDict(String... prefixes) {
        this.oreDicts = prefixes;
        return this;
    }

    /// Sets the display-name format applied to the material name (e.g. `"%s Block"` -> `Iron Block`). Defaults to
    /// the material name followed by the capitalized shape name. A lang file may override individual names; see
    /// [ShapeNaming].
    public BlockShapeBuilder displayName(String displayNameFormat) {
        this.displayNameFormat = Objects.requireNonNull(displayNameFormat, "displayNameFormat must not be null");
        return this;
    }

    /// Declares the shape's variants, e.g. the stone types an ore shape generates against. Each variant registers
    /// its own backing block, named `<shapeName>_<variant>` (see [ShapeNaming#variantBlockName]), sharing the
    /// materials the shape generates but able to differ in texture (see [ShapeIcons]) and behavior (see
    /// [#drops], [#hardness], [#resistance], [#harvestLevel]). Omit this call for a shape with no variants, the
    /// common case; a shape with variants must declare at least one, and names must be unique and valid
    /// identifiers. Shapes sharing this shape's name must declare the identical variant list, or unification fails
    /// loudly; see [ShapeUnification]. [MaterialLibAPI#getStack(Material, Shape, int)] and oredict registration
    /// use the first declared variant; [MaterialLibAPI#getStack(Material, Shape, String, int)] and
    /// [MaterialLibAPI#getBlock(Shape, String)] address a specific one.
    public BlockShapeBuilder variants(String... variants) {
        this.variants = variants;
        return this;
    }

    /// Declares the untinted background texture drawn under `variant`'s tinted material icon (e.g. the stone
    /// background of an ore), as a `domain:path` icon identifier (`"minecraft:stone"`) independent of any
    /// material's texture set -- the same convention [TextureSet#iconPath] uses, resolved the same way; do not
    /// include the `blocks/` folder, it is implicit (see
    /// [com.gtnewhorizon.gtnhlib.util.ResourceUtil#getCompleteBlockTextureResourceLocation]). `texture` is
    /// composited under the tinted material icon by [ShapeBlockRenderingHandler]; see [ShapeBlock#hasBaseTexture].
    /// Optional -- a variant with no base
    /// texture renders as a single tinted layer, as today. `variant` must be one of the names passed to
    /// [#variants].
    public BlockShapeBuilder variantBase(String variant, String texture) {
        Objects.requireNonNull(variant, "variant must not be null");
        if (texture == null || texture.isEmpty()) {
            throw new IllegalArgumentException("variant base texture must not be null or empty");
        }
        variantBases.put(variant, texture);
        return this;
    }

    /// Overrides what a block of this shape drops when broken, replacing the default of dropping the placed
    /// block itself. Called with the fortune level and whether the break was silk-touched. Needed for e.g. a
    /// small ore that drops an item, never the block.
    public BlockShapeBuilder drops(BlockDropFunction drops) {
        this.dropsFn = Objects.requireNonNull(drops, "drops must not be null");
        return this;
    }

    /// Overrides a block of this shape's hardness (mining speed), replacing the default of `5.0`.
    public BlockShapeBuilder hardness(BlockFloatFunction hardness) {
        this.hardnessFn = Objects.requireNonNull(hardness, "hardness must not be null");
        return this;
    }

    /// Overrides a block of this shape's explosion resistance, replacing the default of `10.0`.
    public BlockShapeBuilder resistance(BlockFloatFunction resistance) {
        this.resistanceFn = Objects.requireNonNull(resistance, "resistance must not be null");
        return this;
    }

    /// Overrides a block of this shape's required harvest level, replacing the vanilla default of none required.
    public BlockShapeBuilder harvestLevel(BlockHarvestLevelFunction harvestLevel) {
        this.harvestLevelFn = Objects.requireNonNull(harvestLevel, "harvestLevel must not be null");
        return this;
    }

    /// Sets the per-material icon path override, in place of the material's texture set; see [BlockIconPather].
    /// Returning null from `pather` for a given material falls back to that material's texture-set lookup, the
    /// same as when this is left unset.
    public BlockShapeBuilder iconPath(BlockIconPather pather) {
        this.iconPather = Objects.requireNonNull(pather, "pather must not be null");
        return this;
    }

    /// Registers the shape and returns the shape to generate; see [ShapeRegistry#register]. Fails if called twice.
    public Shape build() {
        if (built) {
            throw new IllegalStateException("Block shape " + Names.key(modid, name) + " was already built");
        }
        built = true;
        String[] prefixes = oreDicts != null ? oreDicts : new String[] { name };
        String format = ShapeNaming.formatOrDefault(name, displayNameFormat);
        BlockBehavior behavior = new BlockBehavior(dropsFn, hardnessFn, resistanceFn, harvestLevelFn);
        if (variants == null) {
            return ShapeRegistry.instance()
                .register(new ShapeBlock(modid, name, format, prefixes, null, null, null, behavior, iconPather));
        }
        return ShapeRegistry.instance().register(
            ShapeBlockVariants
                .create(modid, name, format, prefixes, List.of(variants), variantBases, behavior, iconPather));
    }
}
