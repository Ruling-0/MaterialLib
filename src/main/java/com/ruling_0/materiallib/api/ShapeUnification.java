package com.ruling_0.materiallib.api;

import java.util.Collection;
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

    private final Object2ObjectLinkedOpenHashMap<String, List<ServedShape>> candidatesByName = new Object2ObjectLinkedOpenHashMap<>();
    private final Object2ObjectLinkedOpenHashMap<String, ServedShape> canonicalByName = new Object2ObjectLinkedOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<Shape, Shape> aliasToCanonical = new Reference2ObjectOpenHashMap<>();
    private boolean resolved;

    /// Records a shape as a candidate to own its name and returns it. The owner is not chosen until [#resolve].
    /// Registering the same instance twice records it once.
    Shape register(ServedShape shape) {
        requireRegistration("register a shape");
        Names.validate(shape);
        List<ServedShape> candidates = candidatesByName.get(shape.getName());
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
        for (Map.Entry<String, List<ServedShape>> entry : candidatesByName.entrySet()) {
            String name = entry.getKey();
            List<ServedShape> candidates = entry.getValue();
            String ownerModid = OwnerElection
                .choose("Shape", name, candidates, Shape::getModId, persistedOwners.get(name), "registered");
            ServedShape canonical = candidateOwnedBy(candidates, ownerModid);
            canonicalByName.put(name, canonical);
            for (ServedShape candidate : candidates) {
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

    /// The shape owning `name`, or null when nothing registered it. Available once [#resolve] has run, which is
    /// why [ShapeRegistry] defers an edit's lookup to that point.
    ServedShape ownerOf(String name) {
        return canonicalByName.get(name);
    }

    /// Every registered candidate, owners and merged-away declarations alike.
    List<ServedShape> allCandidates() {
        List<ServedShape> all = new ObjectArrayList<>();
        for (List<ServedShape> candidates : candidatesByName.values()) {
            for (ServedShape candidate : candidates) {
                all.add(candidate);
            }
        }
        return all;
    }

    /// Folds each merged-away declaration's property values into the shape owning the name, keeping the owner's
    /// value where both set a property, then points the loser's holder at the owner's so a stale reference reads
    /// the same values. Mirrors [Material#mergeFrom]; a conflict is logged rather than fatal, matching the
    /// oredict-divergence policy rather than the variant one, since a property cannot make a material generate
    /// against a backing object that does not match.
    void mergeProperties() {
        requireResolved("merge shape properties");
        for (Map.Entry<Shape, Shape> entry : aliasToCanonical.entrySet()) {
            ServedShape loser = (ServedShape) entry.getKey();
            ServedShape winner = (ServedShape) entry.getValue();
            winner.properties()
                .mergeFrom(winner, loser.getModId(), loser.properties());
            loser.properties()
                .redirectTo(winner.properties());
        }
    }

    private static ServedShape candidateOwnedBy(List<ServedShape> candidates, String modid) {
        for (ServedShape candidate : candidates) {
            if (candidate.getModId().equals(modid)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No candidate shape is owned by " + modid);
    }

    /// Rejects a name whose candidates declare different variant lists. Unlike oredict divergence, this is not
    /// safely ignorable: the non-owning candidates' materials would otherwise be generating variants the owner's
    /// backing blocks do not have.
    private void requireIdenticalVariants(String name, List<? extends Shape> candidates, Shape canonical) {
        List<String> ownerVariants = canonical.getVariants();
        for (Shape candidate : candidates) {
            if (candidate == canonical) continue;
            if (!ownerVariants.equals(candidate.getVariants())) {
                throw new IllegalStateException(
                    "Shapes " + canonical.getModId() + ":" + name + " and " + candidate.getModId() + ":" + name +
                        " share a name but declare different variants (" + ownerVariants + " vs " +
                        candidate.getVariants() + "); shapes sharing a name must declare identical variant lists");
            }
        }
    }

    private void logOreDictDivergence(String name, List<? extends Shape> candidates, Shape canonical) {
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
    Collection<ServedShape> canonicalShapes() {
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
