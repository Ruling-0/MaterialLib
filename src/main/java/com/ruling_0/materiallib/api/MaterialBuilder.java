package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;

/// Builds and registers a [Material]. Obtained from [MaterialLibAPI#newMaterial] and finished with [#build],
/// which registers the material and must be called inside the mod's [MaterialRegistrationEvent] handler.
public final class MaterialBuilder {

    private final MaterialRegistry registry;
    private final String modid;
    private final String name;
    private final TextureSet textureSet;
    private final Map<Property<?>, Object> properties = new Reference2ObjectLinkedOpenHashMap<>();
    private final Set<Shape> shapes = new ReferenceLinkedOpenHashSet<>();
    private final Set<Shape> removedShapes = new ReferenceLinkedOpenHashSet<>();
    private final List<String[]> familyKeys = new ArrayList<>();
    private final List<String> tooltipLines = new ArrayList<>(2);
    private final Map<Shape, ShapeOverride> shapeOverrides = new Reference2ObjectLinkedOpenHashMap<>();
    private boolean built;

    MaterialBuilder(MaterialRegistry registry, String modid, String name, TextureSet textureSet) {
        this.registry = registry;
        this.modid = Names.validate("material modid", modid);
        this.name = Names.validate("material name", name);
        this.textureSet = Objects.requireNonNull(textureSet, "textureSet must not be null");
    }

    /// Sets [StandardProperties#TINT], the ARGB tint applied to the material's textures.
    public MaterialBuilder setTint(int tint) {
        return setProperty(StandardProperties.TINT, tint);
    }

    /// Sets [StandardProperties#LAYER_TINTS], the ARGB tints of the shape textures' numbered icon layers, the
    /// first tinting `_LAYER1`.
    public MaterialBuilder setLayerTints(int... tints) {
        Integer[] boxed = new Integer[tints.length];
        for (int i = 0; i < tints.length; i++) {
            boxed[i] = tints[i];
        }
        return setProperty(StandardProperties.LAYER_TINTS, List.of(boxed));
    }

    /// Sets [StandardProperties#FLUID_TINT], the ARGB tint applied to a fluid shape's fill icon in place of
    /// [StandardProperties#TINT].
    public MaterialBuilder setFluidTint(int fluidTint) {
        return setProperty(StandardProperties.FLUID_TINT, fluidTint);
    }

    /// Sets [StandardProperties#PALETTE], baking the material's shape art through column `column` of the palette
    /// png `assets/<modid>/textures/palettes/<name>.png` in place of the tint path; see [PaletteRef].
    public MaterialBuilder usePalette(String modid, String name, int column) {
        return setProperty(StandardProperties.PALETTE, new PaletteRef(modid, name, column));
    }

    /// Sets a property value. Rejects [StandardProperties#NAME] and [StandardProperties#TEXTURE_SET].
    public <T> MaterialBuilder setProperty(Property<T> property, T value) {
        StandardProperties.requireSettable(property, value);
        properties.put(property, value);
        return this;
    }

    public MaterialBuilder generateShape(Shape shape) {
        Names.validate(shape);
        shapes.add(shape);
        removedShapes.remove(shape);
        return this;
    }

    public MaterialBuilder generateShapes(Shape... shapes) {
        for (Shape shape : shapes) {
            generateShape(shape);
        }
        return this;
    }

    /// Removes a shape from the material, masking it when a family the material joins contributes it. Queued at
    /// [#build] and otherwise identical to [MaterialEdit#removeShape] called straight after it; a [#generateShape]
    /// later in this builder cancels the removal.
    public MaterialBuilder removeShape(Shape shape) {
        Names.validate(shape);
        shapes.remove(shape);
        removedShapes.add(shape);
        return this;
    }

    public MaterialBuilder removeShapes(Shape... shapes) {
        for (Shape shape : shapes) {
            removeShape(shape);
        }
        return this;
    }

    /// Serves this material's `shape` with `item` at metadata 0 instead of an item MaterialLib mints.
    /// [MaterialLibAPI#getStack] returns it, it is registered under the shape's oredict names, and the shape's own
    /// item or block does not carry this material. The material must still generate the shape. [Material] documents
    /// which override stands when several mods declare the material. Only block shapes without variants and item
    /// shapes other than fluid containers can be overridden. Any other override is ignored with a warning.
    ///
    /// During the registration event only vanilla items are guaranteed to exist. Another mod's item is named through
    /// [#addShapeOverride(Shape, String, String, int)].
    public MaterialBuilder addShapeOverride(Shape shape, Item item) {
        return addShapeOverride(shape, item, 0);
    }

    /// [#addShapeOverride(Shape, Item)] with an explicit item metadata.
    public MaterialBuilder addShapeOverride(Shape shape, Item item, int meta) {
        Objects.requireNonNull(item, "item must not be null");
        return addShapeOverride(shape, new ShapeOverride.Eager(new ItemStack(item, 1, meta)));
    }

    /// [#addShapeOverride(Shape, Item)] for a block's item form, at metadata 0.
    public MaterialBuilder addShapeOverride(Shape shape, Block block) {
        return addShapeOverride(shape, block, 0);
    }

    /// [#addShapeOverride(Shape, Item)] for a block's item form with an explicit metadata.
    public MaterialBuilder addShapeOverride(Shape shape, Block block, int meta) {
        Objects.requireNonNull(block, "block must not be null");
        return addShapeOverride(shape, new ShapeOverride.Eager(new ItemStack(block, 1, meta)));
    }

    /// [#addShapeOverride(Shape, Item)] for another mod's item or block, named by registry name. The override is
    /// ignored when `itemModid` is not loaded. The name binds at MaterialLib's init, which throws
    /// [IllegalStateException] when no item or block registered during preInit matches it. [MaterialLibAPI#getStack]
    /// serves the pair from MaterialLib's init on.
    public MaterialBuilder addShapeOverride(Shape shape, String itemModid, String itemName, int meta) {
        Names.validate("override item modid", itemModid);
        Objects.requireNonNull(itemName, "itemName must not be null");
        return addShapeOverride(shape, new ShapeOverride.Named(itemModid, itemName, meta));
    }

    private MaterialBuilder addShapeOverride(Shape shape, ShapeOverride override) {
        Names.validate(shape);
        shapeOverrides.put(shape, override);
        return this;
    }

    /// Adds the material to a family.
    public MaterialBuilder addToFamily(Family family) {
        Objects.requireNonNull(family, "family must not be null");
        return addToFamily(family.getModId(), family.getName());
    }

    /// Adds the material to a family by key, deferring the lookup until the registry resolves. The family may
    /// be registered by any mod at any point during the registration event.
    public MaterialBuilder addToFamily(String familyModid, String familyName) {
        familyKeys.add(
            new String[] { Names.validate("family modid", familyModid),
                Names.validate("family name", familyName) });
        return this;
    }

    /// Adds tooltip lines shown on every [Shape] of this material.
    public MaterialBuilder addTooltip(String... lines) {
        tooltipLines.addAll(Arrays.asList(lines));
        return this;
    }

    /// Registers the material and returns it. Fails if a material with the same modid and name already exists or
    /// the registry has already resolved.
    public Material build() {
        if (built) {
            throw new IllegalStateException("Material " + Names.key(modid, name) + " was already built");
        }
        properties.put(StandardProperties.NAME, name);
        properties.put(StandardProperties.TEXTURE_SET, textureSet);
        MaterialDeclaration declaration = new MaterialDeclaration(shapes, removedShapes, familyKeys, shapeOverrides);
        Material material = new Material(registry, modid, name, properties, shapes, tooltipLines, declaration);
        registry.register(material);
        for (String[] familyKey : familyKeys) {
            registry.enqueueAddToFamily(modid, name, familyKey[0], familyKey[1]);
        }
        for (Shape shape : removedShapes) {
            registry
                .enqueueMaterialOp(modid, name, "remove shape " + shape + " from material", m -> m.removeShape(shape));
        }
        built = true;
        return material;
    }
}
