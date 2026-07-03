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
/// A material may belong to several families at once. Properties fall back member-to-family: a member's own
/// value takes precedence, and among multiple families setting the same property the alphabetically-first
/// family key wins (see [Material#getProperty]). Property conflict should not be relied on; wherever possible, keys
/// should be unique. The family's shapes are added to every member, except members that removed them individually
/// through [MaterialEdit#removeShape]. A family's tooltip lines appear on members' shapes after the material's own
/// lines. Families are created through [MaterialLibAPI#newFamily] inside a [MaterialRegistrationEvent] handler
/// and become read-only once the registry resolves at the end of MaterialLib's preInit. Membership is only
/// available after resolution, since other mods may alter it through [FamilyEdit]s and [MaterialEdit]s until then.
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

    /// The materials belonging to this family. Only available after the registry has resolved.
    public Set<Material> getMaterials() {
        registry.requireResolved("query the members of ", key);
        return members;
    }

    /// The shapes this family contributes to its members; individual materials may remove them through
    /// [MaterialEdit#removeShape]. Only available after the registry has resolved.
    public Set<Shape> getShapes() {
        registry.requireResolved("query the shapes of ", key);
        return shapesView;
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
        return propertiesView;
    }

    /// True if this family has a custom tooltip.
    public boolean hasCustomTooltip() {
        return !tooltipLines.isEmpty();
    }

    /// The added tooltip lines of this family.
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
