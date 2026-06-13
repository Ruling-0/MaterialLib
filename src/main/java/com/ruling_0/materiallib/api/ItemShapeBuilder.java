package com.ruling_0.materiallib.api;

/// Builds and registers a simple item [Shape] backed by a [ShapeItem]. Obtained from
/// [MaterialLibAPI#newItemShape] and finished with [#build], which must be called during the owning mod's
/// preInit. Mods needing custom item behavior subclass [ShapeItem] instead and register through
/// [MaterialLibAPI#registerItemShape].
public final class ItemShapeBuilder {

    private final String modid;
    private final String name;
    private String oreDict;
    private String displayNameFormat;
    private boolean built;

    ItemShapeBuilder(String modid, String name) {
        this.modid = Names.validate("item shape modid", modid);
        this.name = Names.validate("item shape name", name);
    }

    /// Sets the oredict prefix; the material name is appended to it (e.g. `gear` -> `gearIron`). Defaults to the
    /// shape name.
    public ItemShapeBuilder oreDict(String oreDict) {
        this.oreDict = Names.validate("item shape oredict", oreDict);
        return this;
    }

    /// Sets the display-name format applied to the material name (e.g. `"%s Gear"` -> `Iron Gear`). Defaults to
    /// the material name followed by the capitalized shape name. A lang file may override individual names; see
    /// [ShapeNaming].
    public ItemShapeBuilder displayName(String displayNameFormat) {
        this.displayNameFormat = displayNameFormat;
        return this;
    }

    /// Registers the shape and returns the canonical shape to generate; see [ItemShapeRegistry#register]. Fails
    /// if called twice.
    public Shape build() {
        if (built) {
            throw new IllegalStateException("Item shape " + Names.key(modid, name) + " was already built");
        }
        built = true;
        String prefix = oreDict != null ? oreDict : name;
        String format = displayNameFormat != null ? displayNameFormat : "%s " + capitalize(name);
        return ItemShapeRegistry.instance()
            .register(new ShapeItem(modid, name, prefix, format));
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
