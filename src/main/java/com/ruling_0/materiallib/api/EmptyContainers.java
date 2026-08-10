package com.ruling_0.materiallib.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/// Collapses empty container registrations that share a name down to a single owning mod, electing the owner the
/// same way [ShapeUnification] elects a shape's.
///
/// Unlike for shapes, no owner is persisted: the item registers as `materiallib:<name>` regardless of owner, so
/// its identity is already stable across sessions, and the owner decides only the icon and the lang key.
final class EmptyContainers {

    private final Object2ObjectLinkedOpenHashMap<String, List<EmptyContainerHandle>> candidatesByName = new Object2ObjectLinkedOpenHashMap<>();
    private boolean resolved;

    /// Records a mod's claim on an empty container name and returns its handle. A null `iconPath` takes the default
    /// icon path; a given one must be non-empty.
    EmptyContainerHandle register(String modid, String name, String iconPath) {
        requireRegistration("register an empty container");
        Names.validate("empty container modid", modid);
        Names.validate("empty container name", name);
        if (iconPath != null && iconPath.isEmpty()) {
            throw new IllegalArgumentException("empty container icon path must not be empty");
        }
        List<EmptyContainerHandle> candidates = candidatesByName.get(name);
        if (candidates == null) {
            candidates = new ObjectArrayList<>();
            candidatesByName.put(name, candidates);
        }
        EmptyContainerHandle handle = new EmptyContainerHandle(modid, name, iconPath);
        candidates.add(handle);
        return handle;
    }

    /// Elects the owner of every registered name and returns each name's canonical handle, in the order the names
    /// were first registered.
    Map<String, EmptyContainerHandle> chooseOwners() {
        requireRegistration("choose empty container owners");
        Map<String, EmptyContainerHandle> owners = new LinkedHashMap<>();
        for (Map.Entry<String, List<EmptyContainerHandle>> entry : candidatesByName.entrySet()) {
            String name = entry.getKey();
            EmptyContainerHandle owner = OwnerElection.chooseCandidate("Empty container", name, entry.getValue(),
                EmptyContainerHandle::getModId, null, "registered");
            owners.put(name, owner);
        }
        resolved = true;
        return owners;
    }

    /// Every handle registered for `name`, in registration order.
    List<EmptyContainerHandle> candidatesOf(String name) {
        List<EmptyContainerHandle> candidates = candidatesByName.get(name);
        return candidates != null ? candidates : List.of();
    }

    private void requireRegistration(String what) {
        if (resolved) {
            throw new IllegalStateException("Cannot " + what + ": empty containers have already resolved");
        }
    }
}
