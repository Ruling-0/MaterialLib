package com.ruling_0.materiallib.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/// A named group of [Material]s that share [Property] values and generated [Shape]s.
///
/// A material may belong to several families at once. Properties fall back member-to-family: a member's own
/// value takes precedence, and among multiple families setting the same property the alphabetically-first
/// family key wins (see [Material#getProperty]). The family's shapes are added to every member, except members
/// that removed them individually through [MaterialEdit#removeShape]. Families are created through
/// [MaterialLibAPI#newFamily] during preInit and become read-only once the registry resolves during this mod's
/// init. Membership is only available after resolution, since other mods may alter it through [FamilyEdit]s and
/// [MaterialEdit]s until then.
public final class Family {

    private final MaterialRegistry registry;
    private final String modid;
    private final String name;
    private final String key;
    private final Map<Property<?>, Object> properties;
    private final Set<Shape> shapes;

    private Set<Material> members;

    Family(MaterialRegistry registry, String modid, String name, Map<Property<?>, Object> properties,
           Set<Shape> shapes) {
        this.registry = registry;
        this.modid = modid;
        this.name = name;
        this.key = Names.key(modid, name);
        this.properties = new LinkedHashMap<>(properties);
        this.shapes = new LinkedHashSet<>(shapes);
    }

    public String getModId() { return modid; }

    public String getName() { return name; }

    /// The registry key, `modid:name`.
    public String getKey() { return key; }

    /// The materials belonging to this family. Only available after the registry has resolved.
    public Set<Material> getMaterials() {
        registry.requireResolved("query the members of ", key);
        return members;
    }

    /// The shapes this family contributes to its members; individual materials may remove them through
    /// [MaterialEdit#removeShape]. Only available after the registry has resolved.
    public Set<Shape> getShapes() {
        registry.requireResolved("query the shapes of ", key);
        return Collections.unmodifiableSet(shapes);
    }

    /// Resolves a property for this family: its own value, else the property's default. Only available after the
    /// registry has resolved.
    @SuppressWarnings("unchecked")
    public <T> T getProperty(Property<T> property) {
        registry.requireResolved("query properties of ", key);
        Object value = properties.get(property);
        if (value != null) return (T) value;
        return property.getDefaultValue();
    }

    /// True if this family sets the property explicitly (the property default does not count).
    public boolean hasProperty(Property<?> property) {
        registry.requireResolved("query properties of ", key);
        return properties.containsKey(property);
    }

    /// Properties set directly on this family, excluding default values.
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
        shapes.add(shape);
    }

    void removeShape(Shape shape) {
        requireMutable();
        shapes.remove(shape);
    }

    Set<Shape> getShapesInternal() { return shapes; }

    Map<Property<?>, Object> getOwnPropertiesInternal() { return properties; }

    void setMembersInternal(Set<Material> members) { this.members = Collections.unmodifiableSet(members); }

    private void requireMutable() {
        if (registry.isResolved()) {
            throw new IllegalStateException("Family " + key + " mutated after the registry resolved");
        }
    }

    @Override
    public String toString() {
        return "Family[" + key + "]";
    }
}
