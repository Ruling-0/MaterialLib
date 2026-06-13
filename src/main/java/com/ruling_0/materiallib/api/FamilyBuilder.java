package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;

/// Builds and registers a [Family]. Obtained from [MaterialLibAPI#newFamily] and finished with [#build], which
/// registers the family and must be called during preInit.
public final class FamilyBuilder {

    private final MaterialRegistry registry;
    private final String modid;
    private final String name;
    private final Map<Property<?>, Object> properties = new Reference2ObjectLinkedOpenHashMap<>();
    private final Set<Shape> shapes = new ReferenceLinkedOpenHashSet<>();
    private final List<String[]> memberKeys = new ArrayList<>();
    private boolean built;

    FamilyBuilder(MaterialRegistry registry, String modid, String name) {
        this.registry = registry;
        this.modid = Names.validate("family modid", modid);
        this.name = Names.validate("family name", name);
    }

    /// Sets [StandardProperties#TINT] for all members that do not set their own.
    public FamilyBuilder setTint(int tint) {
        properties.put(StandardProperties.TINT, tint);
        return this;
    }

    /// Sets a property value for all members that do not set their own. Rejects [StandardProperties#NAME] and
    /// [StandardProperties#TEXTURE_SET], which every material derives from its own builder arguments.
    public <T> FamilyBuilder setProperty(Property<T> property, T value) {
        Objects.requireNonNull(property, "property must not be null");
        Objects.requireNonNull(value, "value must not be null");
        StandardProperties.requireSettable(property);
        properties.put(property, value);
        return this;
    }

    public FamilyBuilder generateShape(Shape shape) {
        shapes.add(Names.validate(shape));
        return this;
    }

    public FamilyBuilder generateShapes(Shape... shapes) {
        for (Shape shape : shapes) {
            generateShape(shape);
        }
        return this;
    }

    /// Adds a material to this family. A material may belong to any number of families; memberships from all
    /// mods accumulate.
    public FamilyBuilder addMaterial(Material material) {
        Objects.requireNonNull(material, "material must not be null");
        return addMaterial(material.getModId(), material.getName());
    }

    /// Adds a material to this family by key, deferring the lookup until the registry resolves. The material may
    /// be registered by any mod at any point during preInit.
    public FamilyBuilder addMaterial(String materialModid, String materialName) {
        memberKeys.add(
            new String[] { Names.validate("material modid", materialModid),
                Names.validate("material name", materialName) });
        return this;
    }

    public FamilyBuilder addMaterials(Material... materials) {
        for (Material material : materials) {
            addMaterial(material);
        }
        return this;
    }

    /// Registers the family and returns it. Fails if a family with the same modid and name already exists or the
    /// registry has already resolved.
    public Family build() {
        if (built) {
            throw new IllegalStateException("Family " + Names.key(modid, name) + " was already built");
        }
        Family family = new Family(registry, modid, name, properties, shapes);
        registry.register(family);
        for (String[] memberKey : memberKeys) {
            registry.enqueueAddToFamily(memberKey[0], memberKey[1], modid, name);
        }
        built = true;
        return family;
    }
}
