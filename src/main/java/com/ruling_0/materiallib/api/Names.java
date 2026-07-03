package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.List;

/// Validation and key formatting for the modid/name pairs that identify materials, families, properties,
/// shapes, and texture sets.
final class Names {

    private Names() {}

    /// Validates a shape's oredict prefixes -- at least one, each a valid identifier -- and returns an immutable
    /// copy.
    static List<String> validateOreDicts(String... oreDicts) {
        if (oreDicts == null) throw new IllegalArgumentException("oredict prefixes must not be null");
        if (oreDicts.length == 0) throw new IllegalArgumentException("a shape requires at least one oredict prefix");
        List<String> validated = new ArrayList<>(oreDicts.length);
        for (String oreDict : oreDicts) {
            validated.add(validate("shape oredict", oreDict));
        }
        return List.copyOf(validated);
    }

    /// Validates the identifiers of a shape implementation and returns the shape.
    static Shape validate(Shape shape) {
        if (shape == null) throw new IllegalArgumentException("shape must not be null");
        validate("shape modid", shape.getModId());
        validate("shape name", shape.getName());
        return shape;
    }

    /// Validates an identifier component and returns it. Identifiers must be non-null, non-empty, and free of
    /// whitespace and ':' (the key separator).
    static String validate(String type, String value) {
        if (value == null) throw new IllegalArgumentException(type + " must not be null");
        if (value.isEmpty()) throw new IllegalArgumentException(type + " must not be empty");
        if (hasInvalidChars(value)) {
            throw new IllegalArgumentException(type + " \"" + value + "\" must not contain ':' or whitespace");
        }
        return value;
    }

    /// Whether `value` contains ':' or whitespace, the characters excluded from identifiers.
    static boolean hasInvalidChars(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ':' || Character.isWhitespace(c)) {
                return true;
            }
        }
        return false;
    }

    static String key(String modid, String name) {
        return modid + ":" + name;
    }
}
