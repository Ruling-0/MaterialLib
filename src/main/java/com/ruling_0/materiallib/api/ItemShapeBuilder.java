package com.ruling_0.materiallib.api;

import java.util.Map;
import java.util.Objects;

import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;

/// Builds and registers a simple item [Shape] backed by a [ShapeItem]. Obtained from
/// [MaterialLibAPI#newItemShape] and finished with [#build], which must be called inside the owning mod's
/// [MaterialRegistrationEvent] handler. Mods needing custom item behavior subclass [ShapeItem] instead and
/// register through [MaterialLibAPI#registerItemShape].
public final class ItemShapeBuilder {

    private final String modid;
    private final String name;
    private String[] oreDicts;
    private String displayNameFormat;
    private String iconName;
    private final Map<Property<?>, Object> properties = new Reference2ObjectLinkedOpenHashMap<>();
    private boolean built;

    ItemShapeBuilder(String modid, String name) {
        this.modid = modid;
        this.name = name;
    }

    /// Sets the oredict prefixes; the material name is appended to each (e.g. `gear` -> `gearIron`). Pass several
    /// to register the item under each, e.g. `oreDict("gear", "cog")` gives both `gearIron` and `cogIron`.
    /// Defaults to the shape name. At least one prefix is required.
    public ItemShapeBuilder oreDict(String... prefixes) {
        this.oreDicts = prefixes;
        return this;
    }

    /// Sets the display-name format applied to the material name (e.g. `"%s Gear"` -> `Iron Gear`). Defaults to
    /// the material name followed by the capitalized shape name. A lang file may override individual names; see
    /// [ShapeNaming].
    public ItemShapeBuilder displayName(String displayNameFormat) {
        this.displayNameFormat = Objects.requireNonNull(displayNameFormat, "displayNameFormat must not be null");
        return this;
    }

    /// Sets the name this shape's textures are filed under inside each texture set, defaulting to the shape name,
    /// so several shapes can share one art file (e.g. every wire gauge drawing `wire.png`). Under shape
    /// unification the owning declaration's alias wins, like every other constructor-borne attribute.
    public ItemShapeBuilder iconName(String iconName) {
        this.iconName = Objects.requireNonNull(iconName, "iconName must not be null");
        return this;
    }

    /// Sets a property value on the shape. Values are read back through [Shape#getProperty] and may be altered
    /// by another mod through [MaterialLibAPI#editShape].
    public <T> ItemShapeBuilder property(Property<T> property, T value) {
        ShapeProperties.requireSettable(property, value);
        properties.put(property, value);
        return this;
    }

    /// Registers the shape and returns the shape to generate; see [ShapeRegistry#register]. Fails if called
    /// twice.
    public Shape build() {
        if (built) {
            throw new IllegalStateException("Item shape " + Names.key(modid, name) + " was already built");
        }
        built = true;
        String[] prefixes = oreDicts != null ? oreDicts : new String[] { name };
        String format = ShapeNaming.formatOrDefault(name, displayNameFormat);
        ShapeItem shape = new ShapeItem(modid, name, format, prefixes);
        if (iconName != null) {
            shape.setIconName(iconName);
        }
        shape.properties().setAll(shape, properties);
        return ShapeRegistry.instance().register(shape);
    }
}
