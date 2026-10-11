package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;

/// A named group of [Material]s that share [Property] values and generated [Shape]s.
///
/// A material may belong to several families at once. A member's own property value takes precedence over its
/// families' values; see [Material#getProperty] for which family wins when several set the same property. Families
/// sharing members should avoid setting the same property. Every member generates the family's shapes, except a
/// shape the member removed through [MaterialEdit#removeShape]. A family's tooltip lines appear on members' shapes
/// after the material's own lines.
///
/// Families are created through [MaterialLibAPI#newFamily] inside a [MaterialRegistrationEvent] handler. They become
/// read-only once the registry resolves at the end of MaterialLib's preInit, and their members, shapes, properties
/// and tooltip are only readable from then on.
public final class Family {

    private final MaterialRegistry registry;
    private final String modid;
    private final String name;
    private final String key;
    private final Map<Property<?>, Object> properties;
    private final Set<Shape> shapes;
    private final List<String> tooltipLines = new ArrayList<>(2);

    private Set<Material> members;
    private Set<Shape> shapesView;
    private Map<Property<?>, Object> propertiesView;

    Family(MaterialRegistry registry, String modid, String name, Map<Property<?>, Object> properties,
           Set<Shape> shapes, List<String> tooltipLines) {
        this.registry = registry;
        this.modid = modid;
        this.name = name;
        this.key = Names.key(modid, name);
        this.properties = new Reference2ObjectLinkedOpenHashMap<>(properties);
        this.shapes = new ReferenceLinkedOpenHashSet<>(shapes);
        this.tooltipLines.addAll(tooltipLines);
    }

    public String getModId() { return modid; }

    public String getName() { return name; }

    /// The registry key, `modid:name`.
    public String getKey() { return key; }

    public Set<Material> getMaterials() {
        registry.requireResolved("query the members of ", key);
        return members;
    }

    /// The shapes this family contributes to its members, including any a member removed for itself.
    public Set<Shape> getShapes() {
        registry.requireResolved("query the shapes of ", key);
        return shapesView;
    }

    /// Resolves a property for this family: its own value, else the property's default.
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
        return propertiesView;
    }

    public boolean hasCustomTooltip() {
        return !tooltipLines.isEmpty();
    }

    /// The tooltip lines this family adds to its members' shapes.
    public List<String> getTooltip() {
        registry.requireResolved("query the tooltip of ", key);
        return tooltipLines;
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

    void addTooltip(String... lines) {
        tooltipLines.addAll(Arrays.asList(lines));
    }

    void clearTooltip() {
        tooltipLines.clear();
    }

    Set<Shape> getShapesInternal() { return shapes; }

    Map<Property<?>, Object> getOwnPropertiesInternal() { return properties; }

    void resolveMembers(Set<Material> members) {
        this.members = Collections.unmodifiableSet(members);
        shapesView = Collections.unmodifiableSet(shapes);
        propertiesView = Collections.unmodifiableMap(properties);
    }

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
