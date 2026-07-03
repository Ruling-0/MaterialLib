package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    private final List<String[]> familyKeys = new ArrayList<>();
    private final List<String> tooltipLines = new ArrayList<>(2);
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

    /// Sets a property value. Rejects [StandardProperties#NAME] and [StandardProperties#TEXTURE_SET].
    public <T> MaterialBuilder setProperty(Property<T> property, T value) {
        StandardProperties.requireSettable(property, value);
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
        Material material = new Material(registry, modid, name, properties, shapes, tooltipLines);
        registry.register(material);
        for (String[] familyKey : familyKeys) {
            registry.enqueueAddToFamily(modid, name, familyKey[0], familyKey[1]);
        }
        built = true;
        return material;
    }
}
