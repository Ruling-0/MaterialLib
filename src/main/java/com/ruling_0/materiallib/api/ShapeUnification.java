package com.ruling_0.materiallib.api;

import java.util.Collection;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// Collapses item shapes that share a name down to a single canonical shape.
///
/// Two mods may each declare an item shape called `gear` without knowing about each other; their gears must end
/// up as one item so a single `gearIron` oredict entry exists. The first shape registered for a name becomes
/// canonical and backs the item; later shapes with that name are recorded as aliases of it. [#register] returns
/// the canonical shape so the registrant uses it directly, which keeps every material's shape set holding
/// canonical shapes; [#canonical] still maps any alias back, in case a mod kept its own reference.
///
/// Which shape wins a name is decided by registration order, which follows mod load order.
final class ShapeUnification {

    private static final Logger LOG = LogManager.getLogger("materiallib");

    private final Object2ObjectLinkedOpenHashMap<String, Shape> canonicalByName = new Object2ObjectLinkedOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<Shape, Shape> aliasToCanonical = new Reference2ObjectOpenHashMap<>();

    /// Registers a shape and returns the canonical shape for its name: the shape itself if it is the first with
    /// that name, otherwise the existing canonical shape, with this one recorded as an alias.
    Shape register(Shape shape) {
        Names.validate(shape);
        Shape canonical = canonicalByName.get(shape.getName());
        if (canonical == null) {
            canonicalByName.put(shape.getName(), shape);
            return shape;
        }
        if (canonical == shape) {
            return canonical;
        }
        if (!Set.copyOf(canonical.getOreDicts())
            .equals(Set.copyOf(shape.getOreDicts()))) {
            LOG.error(
                "Item shapes {}:{} and {}:{} share a name but declare different oredict prefixes ({} vs {}); " +
                    "unifying onto the first and registering only its prefixes, so recipes using the others " +
                    "will not resolve",
                canonical.getModId(),
                canonical.getName(),
                shape.getModId(),
                shape.getName(),
                canonical.getOreDicts(),
                shape.getOreDicts());
        }
        aliasToCanonical.put(shape, canonical);
        return canonical;
    }

    /// The canonical shape for a shape: itself if canonical or unknown, otherwise the shape it was unified onto.
    Shape canonical(Shape shape) {
        Shape canonical = aliasToCanonical.get(shape);
        return canonical != null ? canonical : shape;
    }

    /// True if the shape is the canonical one registered for its name.
    boolean isCanonical(Shape shape) {
        return canonicalByName.get(shape.getName()) == shape;
    }

    /// Every canonical shape, in registration order.
    Collection<Shape> canonicalShapes() {
        return canonicalByName.values();
    }
}
