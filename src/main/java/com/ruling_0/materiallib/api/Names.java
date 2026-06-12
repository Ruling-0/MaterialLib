package com.ruling_0.materiallib.api;

/// Validation and key formatting for the modid/name pairs that identify materials, families, shapes, and
/// texture sets.
final class Names {

    private Names() {}

    /// Validates an identifier component and returns it. Identifiers must be non-null, non-empty, and free of
    /// whitespace and ':' (the key separator).
    static String validate(String kind, String value) {
        if (value == null) throw new IllegalArgumentException(kind + " must not be null");
        if (value.isEmpty()) throw new IllegalArgumentException(kind + " must not be empty");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ':' || Character.isWhitespace(c)) {
                throw new IllegalArgumentException(kind + " \"" + value + "\" must not contain ':' or whitespace");
            }
        }
        return value;
    }

    static String key(String modid, String name) {
        return modid + ":" + name;
    }
}
