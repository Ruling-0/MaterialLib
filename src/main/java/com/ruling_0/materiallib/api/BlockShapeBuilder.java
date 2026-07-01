package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Objects;

/// Builds and registers a simple block [Shape] backed by a [ShapeBlock]. Obtained from
/// [MaterialLibAPI#newBlockShape] and finished with [#build], which must be called during the owning mod's
/// preInit. Mods needing custom block behavior subclass [ShapeBlock] instead and register through
/// [MaterialLibAPI#registerBlockShape].
public final class BlockShapeBuilder {

    private final String modid;
    private final String name;
    private List<String> oreDicts;
    private String displayNameFormat;
    private boolean built;

    BlockShapeBuilder(String modid, String name) {
        this.modid = Names.validate("block shape modid", modid);
        this.name = Names.validate("block shape name", name);
    }

    /// Sets the oredict prefixes; the material name is appended to each (e.g. `block` -> `blockIron`). Pass several
    /// to register the block under each. Defaults to the shape name. At least one prefix is required.
    public BlockShapeBuilder oreDict(String... prefixes) {
        this.oreDicts = Names.validateOreDicts(prefixes);
        return this;
    }

    /// Sets the display-name format applied to the material name (e.g. `"%s Block"` -> `Iron Block`). Defaults to
    /// the material name followed by the capitalized shape name. A lang file may override individual names; see
    /// [ShapeNaming].
    public BlockShapeBuilder displayName(String displayNameFormat) {
        this.displayNameFormat = Objects.requireNonNull(displayNameFormat, "displayNameFormat must not be null");
        return this;
    }

    /// Registers the shape and returns the shape to generate; see [ShapeRegistry#register]. Fails if called twice.
    public Shape build() {
        if (built) {
            throw new IllegalStateException("Block shape " + Names.key(modid, name) + " was already built");
        }
        built = true;
        String[] prefixes = oreDicts != null ? oreDicts.toArray(new String[0]) : new String[] { name };
        String format = displayNameFormat != null ? displayNameFormat : "%s " + capitalize(name);
        return ShapeRegistry.instance()
            .register(new ShapeBlock(modid, name, format, prefixes));
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
