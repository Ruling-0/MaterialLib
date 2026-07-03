package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/// A registered material: a named member of the registry that generates a set of [Shape]s and carries
/// [Property] values.
///
/// Materials are created through [MaterialLibAPI#newMaterial] inside a [MaterialRegistrationEvent] handler and
/// become read-only once the registry resolves at the end of MaterialLib's preInit. A material may belong to
/// any number of [Family]s; membership, properties, and the effective shape set are only available after
/// resolution, since other mods may alter them through [MaterialEdit]s until then.
///
/// Two mods may declare a material with the same name without coordinating; at resolve such declarations unify
/// into one material carrying the union of both -- shapes, families, tooltip lines, and properties, with the
/// owner's value kept for a property both declare. The owner is the mod recorded in the persisted owner store
/// when it declares the name this session, else the alphabetically-first declaring modid; it supplies the
/// modid, key, texture set, and lang key. A reference to a non-owning declaration reads through to the unified
/// material.
public final class Material {

    private static final Comparator<Family> FAMILY_KEY_ORDER = Comparator.comparing(Family::getKey);

    private final MaterialRegistry registry;
    private final String modid;
    private final String name;
    private final String key;
    private final Map<Property<?>, Object> properties;
    private final Set<Shape> ownShapes;
    // Plain (unordered) sets: removedShapes is only membership-tested, and families is sorted into
    // sortedFamilies before any iteration order can be observed.
    private final Set<Shape> removedShapes = new ReferenceOpenHashSet<>(4);
    private final Set<Family> families = new ReferenceOpenHashSet<>(4);
    private final List<String> tooltipLines = new ArrayList<>(2);

    private Family[] sortedFamilies;
    private Set<Family> familiesView;
    private Set<Shape> shapes;
    private Map<Property<?>, Object> propertiesView;
    private int index = -1;
    private Material canonical = this;

    Material(MaterialRegistry registry, String modid, String name, Map<Property<?>, Object> properties,
             Set<Shape> ownShapes, List<String> tooltipLines) {
        this.registry = registry;
        this.modid = modid;
        this.name = name;
        this.key = Names.key(modid, name);
        this.properties = new Reference2ObjectLinkedOpenHashMap<>(properties);
        this.ownShapes = new ReferenceLinkedOpenHashSet<>(ownShapes);
        this.tooltipLines.addAll(tooltipLines);
    }

    public String getModId() {
        if (canonical != this) return canonical.getModId();
        return modid;
    }

    public String getName() {
        if (canonical != this) return canonical.getName();
        return name;
    }

    /// The registry key, `modid:name`.
    public String getKey() {
        if (canonical != this) return canonical.getKey();
        return key;
    }

    /// The material's global metadata index: a stable per-material number used as the item damage in every shape
    /// and as the worldgen/ore id. Assigned at resolve from the instance's persisted assignment, which is
    /// append-only, so a material keeps its index across launches and mod-set changes. Only available after the
    /// registry has resolved.
    public int getIndex() {
        if (canonical != this) return canonical.getIndex();
        registry.requireResolved("query the index of ", key);
        return index;
    }

    /// The families this material belongs to, iterated in alphabetical (case-sensitive) `modid:name` key order
    /// -- the same order used to resolve property values. Empty for a standalone material. Only available after
    /// the registry has resolved.
    public Set<Family> getFamilies() {
        if (canonical != this) return canonical.getFamilies();
        registry.requireResolved("query the families of ", key);
        return familiesView;
    }

    /// The shapes this material generates: its own shapes plus its families', minus any removed for this
    /// material specifically. Only available after the registry has resolved.
    public Set<Shape> getShapes() {
        if (canonical != this) return canonical.getShapes();
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
        if (canonical != this) return canonical.getProperty(property);
        registry.requireResolved("query properties of ", key);
        Object value = properties.get(property);
        if (value != null) return (T) value;
        for (Family family : sortedFamilies) {
            Object inherited = family.getOwnPropertiesInternal()
                .get(property);
            if (inherited != null) return (T) inherited;
        }
        return property.getDefaultValue();
    }

    /// True if this material or any of its families sets the property explicitly (the property default does not
    /// count).
    public boolean hasProperty(Property<?> property) {
        if (canonical != this) return canonical.hasProperty(property);
        registry.requireResolved("query properties of ", key);
        if (properties.containsKey(property)) return true;
        for (Family family : sortedFamilies) {
            if (family.getOwnPropertiesInternal()
                .containsKey(property)) return true;
        }
        return false;
    }

    /// Properties set directly on this material, excluding family-level and default values.
    public Map<Property<?>, Object> getOwnProperties() {
        if (canonical != this) return canonical.getOwnProperties();
        registry.requireResolved("query properties of ", key);
        return propertiesView;
    }

    /// True if this material has a custom tooltip.
    public boolean hasCustomTooltip() {
        if (canonical != this) return canonical.hasCustomTooltip();
        return !tooltipLines.isEmpty();
    }

    /// The added tooltip lines of this material.
    public List<String> getTooltip() {
        if (canonical != this) return canonical.getTooltip();
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
        ownShapes.add(shape);
        removedShapes.remove(shape);
    }

    void removeShape(Shape shape) {
        requireMutable();
        ownShapes.remove(shape);
        removedShapes.add(shape);
    }

    void addTooltip(String... lines) {
        tooltipLines.addAll(Arrays.asList(lines));
    }

    void clearTooltip() {
        tooltipLines.clear();
    }

    /// Folds another declaration of this material's name into this material: unions shapes, removed shapes,
    /// families, and tooltip lines, keeps the already-present value for any property both declare, and reroutes
    /// the other declaration's reads here.
    void mergeFrom(Material loser) {
        for (Map.Entry<Property<?>, Object> entry : loser.properties.entrySet()) {
            Object existing = properties.putIfAbsent(entry.getKey(), entry.getValue());
            if (existing != null && !existing.equals(entry.getValue())) {
                MaterialLib.LOG.warn(
                    "Material {} keeps {} = {}; {} declares conflicting value {}",
                    name,
                    entry.getKey(),
                    existing,
                    loser.modid,
                    entry.getValue());
            }
        }
        ownShapes.addAll(loser.ownShapes);
        removedShapes.addAll(loser.removedShapes);
        removedShapes.removeAll(ownShapes);
        families.addAll(loser.families);
        tooltipLines.addAll(loser.tooltipLines);
        loser.canonical = this;
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

    Family[] getSortedFamiliesInternal() { return sortedFamilies; }

    /// The unified material carrying this name, or this material when it owns its name.
    Material canonical() {
        return canonical;
    }

    void resolveIndex(int index) {
        this.index = index;
    }

    void resolveFamilies() {
        sortedFamilies = families.toArray(new Family[0]);
        Arrays.sort(sortedFamilies, FAMILY_KEY_ORDER);
        familiesView = Collections.unmodifiableSet(new ReferenceArraySet<>(sortedFamilies));
        propertiesView = Collections.unmodifiableMap(properties);
    }

    void resolveShapes() {
        Set<Shape> effective = new ReferenceLinkedOpenHashSet<>(ownShapes);
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
