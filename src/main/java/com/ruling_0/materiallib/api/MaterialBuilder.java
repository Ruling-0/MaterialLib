package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/// Builds and registers a [Material]. Obtained from [MaterialLibAPI#newMaterial] and finished with [#build],
/// which registers the material and must be called during preInit.
public final class MaterialBuilder {

    private final MaterialRegistry registry;
    private final String modid;
    private final String name;
    private final TextureSet textureSet;
    private final Map<Property<?>, Object> properties = new LinkedHashMap<>();
    private final Set<Shape> shapes = new LinkedHashSet<>();
    private final List<String[]> familyKeys = new ArrayList<>();
    private boolean built;

    MaterialBuilder(MaterialRegistry registry, String modid, String name, TextureSet textureSet) {
        this.registry = registry;
        this.modid = Names.validate("material modid", modid);
        this.name = Names.validate("material name", name);
        this.textureSet = Objects.requireNonNull(textureSet, "textureSet must not be null");
    }

    /// Sets [StandardProperties#TINT], the ARGB tint applied to the material's textures.
    public MaterialBuilder setTint(int tint) {
        properties.put(StandardProperties.TINT, tint);
        return this;
    }

    /// Sets a property value. Rejects [StandardProperties#NAME] and [StandardProperties#TEXTURE_SET], which are
    /// derived from the [MaterialLibAPI#newMaterial] arguments.
    public <T> MaterialBuilder setProperty(Property<T> property, T value) {
        Objects.requireNonNull(property, "property must not be null");
        Objects.requireNonNull(value, "value must not be null");
        StandardProperties.requireSettable(property);
        properties.put(property, value);
        return this;
    }

    public MaterialBuilder generateShape(Shape shape) {
        shapes.add(Names.validate(shape));
        return this;
    }

    public MaterialBuilder generateShapes(Shape... shapes) {
        for (Shape shape : shapes) {
            generateShape(shape);
        }
        return this;
    }

    /// Adds the material to a family. A material may belong to any number of families; memberships from all
    /// mods accumulate.
    public MaterialBuilder addToFamily(Family family) {
        Objects.requireNonNull(family, "family must not be null");
        return addToFamily(family.getModId(), family.getName());
    }

    /// Adds the material to a family by key, deferring the lookup until the registry resolves. The family may
    /// be registered by any mod at any point during preInit.
    public MaterialBuilder addToFamily(String familyModid, String familyName) {
        familyKeys.add(
            new String[] { Names.validate("family modid", familyModid),
                Names.validate("family name", familyName) });
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
        Material material = new Material(registry, modid, name, properties, shapes);
        registry.register(material);
        for (String[] familyKey : familyKeys) {
            registry.enqueueAddToFamily(modid, name, familyKey[0], familyKey[1]);
        }
        built = true;
        return material;
    }
}
