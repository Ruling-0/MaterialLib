package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;

import com.ruling_0.materiallib.MaterialLib;

/// The owner-election policy shared by material and shape unification, kept in one place so the two cannot
/// drift: a name is owned by its persisted owner when that mod supplied a candidate this session, else by the
/// alphabetically first candidate modid.
final class OwnerElection {

    private OwnerElection() {}

    /// Elects the owning modid for `name`, logging when a persisted owner supplied no candidate. `kind` and
    /// `candidateVerb` fill the log message, e.g. "Material" and "declared".
    static <T> String choose(String kind, String name, List<T> candidates, Function<T, String> modidOf,
                             String persistedOwner, String candidateVerb) {
        TreeSet<String> modids = new TreeSet<>();
        for (T candidate : candidates) {
            modids.add(modidOf.apply(candidate));
        }
        if (persistedOwner != null && modids.contains(persistedOwner)) {
            return persistedOwner;
        }
        String owner = modids.first();
        if (persistedOwner != null) {
            MaterialLib.LOG.info(
                "{} {} was owned by {}, which {} no candidate this session; reassigning to {}",
                kind,
                name,
                persistedOwner,
                candidateVerb,
                owner);
        }
        return owner;
    }
}
