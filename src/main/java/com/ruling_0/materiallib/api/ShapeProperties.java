package com.ruling_0.materiallib.api;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;

/// The property values attached to a shape.
///
/// Each shape type ([ShapeItem], [ShapeBlock], [ShapeFluid]) holds one of these by composition, for the same reason
/// [ServedMaterials] is held that way.
///
/// A shape that loses unification points its holder at the winner's through [#redirectTo], so a stale reference kept
/// by the mod that declared it still reads the canonical values.
final class ShapeProperties {

    private final Map<Property<?>, Object> properties = new Reference2ObjectLinkedOpenHashMap<>();
    private Map<Property<?>, Object> propertiesView;
    private ShapeProperties canonical = this;

    /// Resolves a property: this shape's own value, else the property's default.
    @SuppressWarnings("unchecked")
    <T> T get(Property<T> property) {
        if (canonical != this) return canonical.get(property);
        Object value = properties.get(property);
        return value != null ? (T) value : property.getDefaultValue();
    }

    /// True if this shape sets the property explicitly; the property default does not count.
    boolean has(Property<?> property) {
        if (canonical != this) return canonical.has(property);
        return properties.containsKey(property);
    }

    /// The values set on this shape, unmodifiable once the registry has resolved.
    Map<Property<?>, Object> view() {
        if (canonical != this) return canonical.view();
        return propertiesView != null ? propertiesView : Collections.unmodifiableMap(properties);
    }

    /// Rejects a null property or value.
    static void requireSettable(Property<?> property, Object value) {
        Objects.requireNonNull(property, "property must not be null");
        Objects.requireNonNull(value, "value must not be null");
    }

    void setAll(Object owner, Map<Property<?>, Object> values) {
        for (Map.Entry<Property<?>, Object> entry : values.entrySet()) {
            set(owner, entry.getKey(), entry.getValue());
        }
    }

    void set(Object owner, Property<?> property, Object value) {
        if (canonical != this) {
            canonical.set(owner, property, value);
            return;
        }
        requireMutable(owner);
        properties.put(property, value);
    }

    void remove(Object owner, Property<?> property) {
        if (canonical != this) {
            canonical.remove(owner, property);
            return;
        }
        requireMutable(owner);
        properties.remove(property);
    }

    /// Copies every value the losing declaration sets and this one does not, keeping this shape's own value where
    /// both set the property and logging a discarded value that differs. Mirrors [Material#mergeFrom]. `owner` and
    /// `loserModid` name the shapes in that warning.
    void mergeFrom(Object owner, String loserModid, ShapeProperties loser) {
        for (Map.Entry<Property<?>, Object> entry : loser.properties.entrySet()) {
            Object existing = properties.putIfAbsent(entry.getKey(), entry.getValue());
            if (existing != null && !existing.equals(entry.getValue())) {
                MaterialLib.LOG.warn(
                    "Shape {} keeps {} = {}; {} declares conflicting value {}",
                    owner,
                    entry.getKey(),
                    existing,
                    loserModid,
                    entry.getValue());
            }
        }
    }

    /// Routes every read to `winner`, for a shape that lost unification.
    void redirectTo(ShapeProperties winner) {
        canonical = winner;
    }

    void freeze() {
        propertiesView = Collections.unmodifiableMap(properties);
    }

    private void requireMutable(Object owner) {
        if (propertiesView != null) {
            throw new IllegalStateException("Shape " + owner + " mutated after shapes resolved");
        }
    }
}
