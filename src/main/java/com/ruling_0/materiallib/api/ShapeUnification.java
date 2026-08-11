package com.ruling_0.materiallib.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

/// Collapses shapes that share a name down to a single canonical shape with one owning mod.
///
/// Two mods may each declare a shape called `gear` without knowing about each other; their gears should end up
/// as one item. Every shape registered for a name is a candidate to own it. The owner is chosen once, at [#resolve],
/// preferring the owner stored in the instance's [ShapeOwnerStore] else the first candidate by alphabetical modid.
/// [#canonical] maps any candidate back to the canonical shape, so a material that generated a non-owning shape still
/// resolves to the one backing item.
final class ShapeUnification {

    private final Object2ObjectLinkedOpenHashMap<String, List<Shape>> candidatesByName = new Object2ObjectLinkedOpenHashMap<>();
    private final Object2ObjectLinkedOpenHashMap<String, Shape> canonicalByName = new Object2ObjectLinkedOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<Shape, Shape> aliasToCanonical = new Reference2ObjectOpenHashMap<>();
    private boolean resolved;

    /// Records a shape as a candidate to own its name and returns it. The owner is not chosen until [#resolve].
    /// Registering the same instance twice records it once.
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

    /// Chooses the owner of every registered name and returns the full `name -> ownerModid` assignment to persist,
    /// keeping a persisted name's owner even when it has no candidate this session. Records the canonical shape and
    /// the alias mappings, and logs a name whose candidates declare differing oredict prefixes.
    Map<String, String> resolve(Map<String, String> persistedOwners) {
        requireRegistration("resolve shape unification");
        Map<String, String> owners = new LinkedHashMap<>(persistedOwners);
        for (Map.Entry<String, List<Shape>> entry : candidatesByName.entrySet()) {
            String name = entry.getKey();
            List<Shape> candidates = entry.getValue();
            Shape canonical = OwnerElection.chooseCandidate("Shape", name, candidates, Shape::getModId,
                persistedOwners.get(name), "registered");
            String ownerModid = canonical.getModId();
            canonicalByName.put(name, canonical);
            for (Shape candidate : candidates) {
                if (candidate != canonical) {
                    aliasToCanonical.put(candidate, canonical);
                    MaterialLib.LOG.info("Unified shape {}:{} onto owner {}", candidate.getModId(), name, ownerModid);
                }
            }
            requireIdenticalVariants(name, candidates, canonical);
            logOreDictDivergence(name, candidates, canonical);
            owners.put(name, ownerModid);
        }
        resolved = true;
        return owners;
    }

    /// Rejects a name whose candidates declare different variant lists. Unlike oredict divergence, a mismatch is
    /// fatal: the non-owning candidates' materials would generate variants the owner's backing blocks do not have.
    private void requireIdenticalVariants(String name, List<Shape> candidates, Shape canonical) {
        List<String> ownerVariants = canonical.getVariants();
        for (Shape candidate : candidates) {
            if (candidate == canonical) continue;
            if (!ownerVariants.equals(candidate.getVariants())) {
                throw new IllegalStateException(
                    "Shapes " + canonical.getModId() + ":" + name + " and " + candidate.getModId() + ":" + name +
                        " share a name but declare different variants (" + ownerVariants + " vs " +
                        candidate.getVariants() + ")");
            }
        }
    }

    private void logOreDictDivergence(String name, List<Shape> candidates, Shape canonical) {
        Set<String> ownerPrefixes = Set.copyOf(canonical.getOreDicts());
        for (Shape candidate : candidates) {
            if (candidate == canonical) continue;
            if (!ownerPrefixes.equals(Set.copyOf(candidate.getOreDicts()))) {
                MaterialLib.LOG.error(
                    "Shapes {}:{} and {}:{} share a name but declare different oredict prefixes ({} vs {}); " +
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

    /// Every canonical shape, in the order their names were first registered. Only available after [#resolve].
    Collection<Shape> canonicalShapes() {
        requireResolved("list canonical shapes");
        return canonicalByName.values();
    }

    /// Every shape registered as a candidate for `shape`'s name, in registration order, or the shape alone when
    /// its name was never contested. Only available after [#resolve].
    List<Shape> candidatesOf(Shape shape) {
        requireResolved("list name candidates");
        List<Shape> candidates = candidatesByName.get(shape.getName());
        return candidates != null ? Collections.unmodifiableList(candidates) : List.of(shape);
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
