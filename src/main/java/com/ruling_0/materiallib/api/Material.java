package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// A registered material: a named member of the registry that generates a set of [Shape]s and carries
/// [Property] values.
///
/// Materials are created through [MaterialLibAPI#newMaterial] during preInit and become read-only once the
/// registry resolves during this mod's init. A material may belong to any number of [Family]s; membership,
/// properties, and the effective shape set are only available after resolution, since other mods may alter them
/// through [MaterialEdit]s until then.
public final class Material {

    private final MaterialRegistry registry;
    private final String modid;
    private final String name;
    private final String key;
    private final Map<Property<?>, Object> properties;
    private final Set<Shape> ownShapes;
    private final Set<Shape> removedShapes = new LinkedHashSet<>();
    private final Set<Family> families = new LinkedHashSet<>();

    private List<Family> sortedFamilies;
    private Set<Family> familiesView;
    private Set<Shape> shapes;

    Material(MaterialRegistry registry, String modid, String name, Map<Property<?>, Object> properties,
             Set<Shape> ownShapes) {
        this.registry = registry;
        this.modid = modid;
        this.name = name;
        this.key = Names.key(modid, name);
        this.properties = new LinkedHashMap<>(properties);
        this.ownShapes = new LinkedHashSet<>(ownShapes);
    }

    public String getModId() { return modid; }

    public String getName() { return name; }

    /// The registry key, `modid:name`.
    public String getKey() { return key; }

    /// The families this material belongs to, iterated in alphabetical (case-sensitive) `modid:name` key order
    /// -- the same order used to resolve property values. Empty for a standalone material. Only available after
    /// the registry has resolved.
    public Set<Family> getFamilies() {
        registry.requireResolved("query the families of ", key);
        return familiesView;
    }

    /// The shapes this material generates: its own shapes plus its families', minus any removed for this
    /// material specifically. Only available after the registry has resolved.
    public Set<Shape> getShapes() {
        registry.requireResolved("query the shapes of ", key);
        return shapes;
    }

    public boolean hasShape(Shape shape) {
        return getShapes().contains(shape);
    }

    /// Resolves a property for this material: its own value, else the value of the alphabetically-first family
    /// (by case-sensitive `modid:name` key) that sets it, else the property's default. Only available after the
    /// registry has resolved.
    @SuppressWarnings("unchecked")
    public <T> T getProperty(Property<T> property) {
        registry.requireResolved("query properties of ", key);
        Object value = properties.get(property);
        if (value != null) return (T) value;
        for (Family family : sortedFamilies) {
            if (family.hasProperty(property)) return family.getProperty(property);
        }
        return property.getDefaultValue();
    }

    /// True if this material or any of its families sets the property explicitly (the property default does not
    /// count).
    public boolean hasProperty(Property<?> property) {
        registry.requireResolved("query properties of ", key);
        if (properties.containsKey(property)) return true;
        for (Family family : sortedFamilies) {
            if (family.hasProperty(property)) return true;
        }
        return false;
    }

    /// Properties set directly on this material, excluding family-level and default values.
    public Map<Property<?>, Object> getOwnProperties() {
        registry.requireResolved("query properties of ", key);
        return Collections.unmodifiableMap(properties);
    }

    void setPropertyValue(Property<?> property, Object value) {
        requireMutable();
        properties.put(property, value);
    }

    void removePropertyValue(Property<?> property) {
        requireMutable();
        properties.remove(property);
    }

    void addShape(Shape shape) {
        requireMutable();
        ownShapes.add(shape);
        removedShapes.remove(shape);
    }

    void removeShape(Shape shape) {
        requireMutable();
        ownShapes.remove(shape);
        removedShapes.add(shape);
    }

    void addFamilyInternal(Family family) {
        requireMutable();
        families.add(family);
    }

    void removeFamilyInternal(Family family) {
        requireMutable();
        families.remove(family);
    }

    boolean isMemberOfInternal(Family family) {
        return families.contains(family);
    }

    Map<Property<?>, Object> getOwnPropertiesInternal() { return properties; }

    List<Family> getSortedFamiliesInternal() { return sortedFamilies; }

    void resolveFamilies() {
        sortedFamilies = new ArrayList<>(families);
        sortedFamilies.sort(Comparator.comparing(Family::getKey));
        familiesView = Collections.unmodifiableSet(new LinkedHashSet<>(sortedFamilies));
    }

    void resolveShapes() {
        Set<Shape> effective = new LinkedHashSet<>(ownShapes);
        for (Family family : sortedFamilies) {
            for (Shape shape : family.getShapesInternal()) {
                if (!removedShapes.contains(shape)) {
                    effective.add(shape);
                }
            }
        }
        shapes = Collections.unmodifiableSet(effective);
    }

    private void requireMutable() {
        if (registry.isResolved()) {
            throw new IllegalStateException("Material " + key + " mutated after the registry resolved");
        }
    }

    @Override
    public String toString() {
        return "Material[" + key + "]";
    }
}
