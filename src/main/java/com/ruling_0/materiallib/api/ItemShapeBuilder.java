package com.ruling_0.materiallib.api;

import java.util.Objects;

/// Builds and registers a simple item [Shape] backed by a [ShapeItem]. Obtained from
/// [MaterialLibAPI#newItemShape] and finished with [#build], which must be called inside the owning mod's
/// [MaterialRegistrationEvent] handler. Mods needing custom item behavior subclass [ShapeItem] instead and
/// register through [MaterialLibAPI#registerItemShape].
public final class ItemShapeBuilder {

    private final String modid;
    private final String name;
    private String[] oreDicts;
    private String displayNameFormat;
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

    /// Registers the shape and returns the shape to generate; see [ShapeRegistry#register]. Fails if called
    /// twice.
    public Shape build() {
        if (built) {
            throw new IllegalStateException("Item shape " + Names.key(modid, name) + " was already built");
        }
        built = true;
        String[] prefixes = oreDicts != null ? oreDicts : new String[] { name };
        String format = ShapeNaming.formatOrDefault(name, displayNameFormat);
        return ShapeRegistry.instance().register(new ShapeItem(modid, name, format, prefixes));
    }
}
