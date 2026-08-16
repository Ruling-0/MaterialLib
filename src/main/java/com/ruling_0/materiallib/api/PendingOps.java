package com.ruling_0.materiallib.api;

import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/// The queued cross-mod edits of a registry, applied in call order when the registry resolves.
final class PendingOps {

    private final List<PendingOp> ops = new ObjectArrayList<>();

    void add(String description, Runnable action) {
        ops.add(new PendingOp(description, action));
    }

    /// Runs every queued operation in call order, then clears the queue. Fails on the first operation that
    /// throws, naming the edit.
    void drain() {
        for (PendingOp op : ops) {
            try {
                op.action.run();
            }
            catch (RuntimeException e) {
                throw new IllegalStateException("Failed to apply queued edit \"" + op.description + "\"", e);
            }
        }
        ops.clear();
    }

    private record PendingOp(String description, Runnable action) {}
}
