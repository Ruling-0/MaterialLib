package com.ruling_0.materiallib.api;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// Collapses item shapes that share a name down to a single canonical shape with one owning mod.
///
/// Two mods may each declare an item shape called `gear` without knowing about each other; their gears must end up
/// as one item so a single `gearIron` oredict entry exists. Every shape registered for a name is a candidate to
/// own it; the owner is chosen once, at [#resolve], from the persisted assignment: the mod recorded as the name's
/// owner if it registered a candidate this session, otherwise the candidate whose modid sorts first. Choosing at
/// resolve rather than by registration order makes the owner independent of mod load order, and honoring the
/// persisted owner keeps it fixed when another mod that declares the same name is added. [#canonical] maps any
/// candidate back to the canonical shape, so a material that generated a non-owning shape still resolves to the
/// one backing item.
final class ShapeUnification {

    private static final Logger LOG = LogManager.getLogger("materiallib");

    private final Object2ObjectLinkedOpenHashMap<String, List<Shape>> candidatesByName = new Object2ObjectLinkedOpenHashMap<>();
    private final Object2ObjectLinkedOpenHashMap<String, Shape> canonicalByName = new Object2ObjectLinkedOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<Shape, Shape> aliasToCanonical = new Reference2ObjectOpenHashMap<>();
    private boolean resolved;

    /// Records a shape as a candidate to own its name and returns it. The owner is not chosen until [#resolve];
    /// registering the same instance twice records it once.
    Shape register(Shape shape) {
        requireRegistration("register a shape");
        Names.validate(shape);
        List<Shape> candidates = candidatesByName.get(shape.getName());
        if (candidates == null) {
            candidates = new ObjectArrayList<>();
            candidatesByName.put(shape.getName(), candidates);
        }
        if (!candidates.contains(shape)) {
            candidates.add(shape);
        }
        return shape;
    }

    /// Chooses the owner of every registered name and returns the full `name -> ownerModid` assignment to persist:
    /// the persisted owner when that mod registered a candidate this session, otherwise the candidate whose modid
    /// sorts first. A persisted name with no candidate this session keeps its owner in the returned map so it is
    /// not lost while the declaring mod is absent. Records the canonical shape and the alias mappings, and logs a
    /// name whose candidates declare differing oredict prefixes, since only the owner's prefixes are registered.
    Map<String, String> resolve(Map<String, String> persistedOwners) {
        requireRegistration("resolve shape unification");
        Map<String, String> owners = new LinkedHashMap<>(persistedOwners);
        for (Map.Entry<String, List<Shape>> entry : candidatesByName.entrySet()) {
            String name = entry.getKey();
            List<Shape> candidates = entry.getValue();
            String ownerModid = chooseOwner(name, candidates, persistedOwners.get(name));
            Shape canonical = candidateOwnedBy(candidates, ownerModid);
            canonicalByName.put(name, canonical);
            for (Shape candidate : candidates) {
                if (candidate != canonical) {
                    aliasToCanonical.put(candidate, canonical);
                    LOG.info("Unified item shape {}:{} onto owner {}", candidate.getModId(), name, ownerModid);
                }
            }
            logOreDictDivergence(name, candidates, canonical);
            owners.put(name, ownerModid);
        }
        resolved = true;
        return owners;
    }

    private static String chooseOwner(String name, List<Shape> candidates, String persistedOwner) {
        TreeSet<String> modids = new TreeSet<>();
        for (Shape candidate : candidates) {
            modids.add(candidate.getModId());
        }
        if (persistedOwner != null && modids.contains(persistedOwner)) {
            return persistedOwner;
        }
        String owner = modids.first();
        if (persistedOwner != null) {
            LOG.info(
                "Item shape {} was owned by {}, which registered no candidate this session; reassigning to {}",
                name,
                persistedOwner,
                owner);
        }
        return owner;
    }

    private static Shape candidateOwnedBy(List<Shape> candidates, String modid) {
        for (Shape candidate : candidates) {
            if (candidate.getModId()
                .equals(modid)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No candidate shape is owned by " + modid);
    }

    private void logOreDictDivergence(String name, List<Shape> candidates, Shape canonical) {
        Set<String> ownerPrefixes = Set.copyOf(canonical.getOreDicts());
        for (Shape candidate : candidates) {
            if (candidate == canonical) continue;
            if (!ownerPrefixes.equals(Set.copyOf(candidate.getOreDicts()))) {
                LOG.error(
                    "Item shapes {}:{} and {}:{} share a name but declare different oredict prefixes ({} vs {}); " +
                        "registering only the owner's prefixes, so recipes using the others will not resolve",
                    canonical.getModId(),
                    name,
                    candidate.getModId(),
                    name,
                    canonical.getOreDicts(),
                    candidate.getOreDicts());
            }
        }
    }

    /// The canonical shape for a shape: itself if it owns its name or was never registered, otherwise the shape
    /// its name unified onto. Only available after [#resolve].
    Shape canonical(Shape shape) {
        requireResolved("look up the canonical shape");
        Shape canonical = aliasToCanonical.get(shape);
        return canonical != null ? canonical : shape;
    }

    /// True if the shape is the canonical one chosen for its name. Only available after [#resolve].
    boolean isCanonical(Shape shape) {
        requireResolved("check whether a shape is canonical");
        return canonicalByName.get(shape.getName()) == shape;
    }

    /// Every canonical shape, in the order their names were first registered. Only available after [#resolve].
    Collection<Shape> canonicalShapes() {
        requireResolved("list canonical shapes");
        return canonicalByName.values();
    }

    private void requireResolved(String what) {
        if (!resolved) {
            throw new IllegalStateException("Cannot " + what + ": shape unification has not resolved yet");
        }
    }

    private void requireRegistration(String what) {
        if (resolved) {
            throw new IllegalStateException("Cannot " + what + ": shape unification has already resolved");
        }
    }
}
