package com.ruling_0.materiallib.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/// An ordered, named collection of one value per variant, keyed by the variant names declared through
/// [BlockShapeBuilder#variants]. Preserves declaration order so the first variant can be selected as the fallback
/// for oredict registration and the unqualified [ShapeRegistry#getStack(Material, Shape, int)].
final class VariantSet<T> {

    private final List<String> names;
    private final Map<String, T> byName;

    private VariantSet(List<String> names, Map<String, T> byName) {
        this.names = names;
        this.byName = byName;
    }

    /// Builds a variant set from ordered, unique variant names, computing each value with `valueFor`. Rejects a
    /// null or empty name list, an invalid identifier (see [Names#validate]), or a name repeated in the list.
    static <T> VariantSet<T> of(List<String> names, Function<String, T> valueFor) {
        if (names == null || names.isEmpty()) {
            throw new IllegalArgumentException("a block shape requires at least one variant name");
        }
        Map<String, T> byName = new LinkedHashMap<>();
        for (String name : names) {
            Names.validate("block shape variant", name);
            if (byName.containsKey(name)) {
                throw new IllegalArgumentException("variant \"" + name + "\" is declared more than once");
            }
            byName.put(name, valueFor.apply(name));
        }
        return new VariantSet<>(List.copyOf(names), Collections.unmodifiableMap(byName));
    }

    /// The declared variant names, in declaration order.
    List<String> names() {
        return names;
    }

    /// The value for the first declared variant, the fallback used where a caller has no variant of its own to
    /// pick (oredict, the unqualified [ShapeRegistry#getStack(Material, Shape, int)]).
    T first() {
        return byName.get(names.get(0));
    }

    /// The value for `variant`. Fails when `variant` was not declared.
    T get(String variant) {
        T value = byName.get(variant);
        if (value == null) {
            throw new IllegalArgumentException(
                "variant \"" + variant + "\" is not one of the declared variants " + names);
        }
        return value;
    }

    /// Every declared value, in declaration order.
    Collection<T> values() {
        return byName.values();
    }
}
