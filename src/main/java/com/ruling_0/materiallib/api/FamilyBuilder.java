package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;

/// Builds and registers a [Family]. Obtained from [MaterialLibAPI#newFamily] and finished with [#build], which
/// registers the family and must be called inside the mod's [MaterialRegistrationEvent] handler.
public final class FamilyBuilder {

    private final MaterialRegistry registry;
    private final String modid;
    private final String name;
    private final Map<Property<?>, Object> properties = new Reference2ObjectLinkedOpenHashMap<>();
    private final Set<Shape> shapes = new ReferenceLinkedOpenHashSet<>();
    private final List<String[]> memberKeys = new ArrayList<>();
    private final List<String> tooltipLines = new ArrayList<>(2);
    private boolean built;

    FamilyBuilder(MaterialRegistry registry, String modid, String name) {
        this.registry = registry;
        this.modid = Names.validate("family modid", modid);
        this.name = Names.validate("family name", name);
    }

    /// Sets [StandardProperties#TINT] for all members that do not set their own.
    public FamilyBuilder setTint(int tint) {
        return setProperty(StandardProperties.TINT, tint);
    }

    /// Sets a property value for all members that do not set their own. Rejects [StandardProperties#NAME] and
    /// [StandardProperties#TEXTURE_SET].
    public <T> FamilyBuilder setProperty(Property<T> property, T value) {
        StandardProperties.requireSettable(property, value);
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

    /// Adds a material to this family.
    public FamilyBuilder addMaterial(Material material) {
        Objects.requireNonNull(material, "material must not be null");
        return addMaterial(material.getModId(), material.getName());
    }

    /// Adds a material to this family by key, deferring the lookup until the registry resolves. The material may
    /// be registered by any mod at any point during the registration event.
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

    /// Adds tooltip lines shown on every [Shape] of the family's member materials.
    public FamilyBuilder addTooltip(String... lines) {
        tooltipLines.addAll(Arrays.asList(lines));
        return this;
    }

    /// Registers the family and returns it. Fails if a family with the same modid and name already exists or the
    /// registry has already resolved.
    public Family build() {
        if (built) {
            throw new IllegalStateException("Family " + Names.key(modid, name) + " was already built");
        }
        Family family = new Family(registry, modid, name, properties, shapes, tooltipLines);
        registry.register(family);
        for (String[] memberKey : memberKeys) {
            registry.enqueueAddToFamily(memberKey[0], memberKey[1], modid, name);
        }
        built = true;
        return family;
    }
}
