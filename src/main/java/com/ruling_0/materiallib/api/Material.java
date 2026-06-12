package com.ruling_0.materiallib.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/// A registered material: a named member of the registry that generates a set of [Shape]s and carries
/// [Property] values.
///
/// Materials are created through [MaterialLibAPI#newMaterial] during preInit and become read-only once the
/// registry resolves during this mod's init. Family membership, properties, and the effective shape set are
/// only available after resolution, since other mods may alter them through [MaterialEdit]s until then.
public final class Material {

    private final MaterialRegistry registry;
    private final String modid;
    private final String name;
    private final Map<Property<?>, Object> properties;
    private final Set<Shape> ownShapes;
    private final Set<Shape> removedShapes = new LinkedHashSet<>();

    private Family family;
    private Set<Shape> shapes;

    Material(MaterialRegistry registry, String modid, String name, Map<Property<?>, Object> properties,
             Set<Shape> ownShapes) {
        this.registry = registry;
        this.modid = modid;
        this.name = name;
        this.properties = new LinkedHashMap<>(properties);
        this.ownShapes = new LinkedHashSet<>(ownShapes);
    }

    public String getModId() { return modid; }

    public String getName() { return name; }

    /// The registry key, `modid:name`.
    public String getKey() { return Names.key(modid, name); }

    /// The family this material belongs to, or null for a standalone material. Only available after the registry
    /// has resolved.
    public Family getFamily() {
        registry.requireResolved("query the family of " + getKey());
        return family;
    }

    /// The shapes this material generates: its own shapes plus its family's, minus any removed for this material
    /// specifically. Only available after the registry has resolved.
    public Set<Shape> getShapes() {
        registry.requireResolved("query the shapes of " + getKey());
        return shapes;
    }

    public boolean hasShape(Shape shape) {
        return getShapes().contains(shape);
    }

    /// Resolves a property for this material: its own value, else its family's value, else the property's
    /// default. Only available after the registry has resolved.
    @SuppressWarnings("unchecked")
    public <T> T getProperty(Property<T> property) {
        registry.requireResolved("query properties of " + getKey());
        Object value = properties.get(property);
        if (value != null) return (T) value;
        if (family != null && family.hasProperty(property)) return family.getProperty(property);
        return property.getDefaultValue();
    }

    /// True if this material or its family sets the property explicitly (the property default does not count).
    public boolean hasProperty(Property<?> property) {
        registry.requireResolved("query properties of " + getKey());
        return properties.containsKey(property) || (family != null && family.hasProperty(property));
    }

    /// Properties set directly on this material, excluding family-level and default values.
    public Map<Property<?>, Object> getOwnProperties() {
        registry.requireResolved("query properties of " + getKey());
        return Collections.unmodifiableMap(properties);
    }

    void setPropertyValue(Property<?> property, Object value) {
        properties.put(property, value);
    }

    void removePropertyValue(Property<?> property) {
        properties.remove(property);
    }

    void addShape(Shape shape) {
        ownShapes.add(shape);
        removedShapes.remove(shape);
    }

    void removeShape(Shape shape) {
        ownShapes.remove(shape);
        removedShapes.add(shape);
    }

    Family getFamilyInternal() { return family; }

    void setFamilyInternal(Family family) { this.family = family; }

    void resolveShapes() {
        Set<Shape> effective = new LinkedHashSet<>(ownShapes);
        if (family != null) {
            for (Shape shape : family.getShapesInternal()) {
                if (!removedShapes.contains(shape)) {
                    effective.add(shape);
                }
            }
        }
        shapes = Collections.unmodifiableSet(effective);
    }

    @Override
    public String toString() {
        return "Material[" + getKey() + "]";
    }
}
