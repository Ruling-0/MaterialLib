package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;

/// What one mod's [MaterialBuilder] declared about shapes, kept apart from the unified [Material] it may fold
/// into. Unification unions shapes and families onto the owner and so forgets which declaration contributed a
/// shape; a [ShapeOverride] only stands when its own declaration would have produced the shape it replaces, so
/// that origin has to survive the merge.
record MaterialDeclaration(Set<Shape> shapes, Set<Shape> removedShapes, List<String[]> familyKeys,
                           Map<Shape, ShapeOverride> overrides) {

    static final MaterialDeclaration EMPTY = new MaterialDeclaration(Set.of(), Set.of(), List.of(), Map.of());

    MaterialDeclaration {
        shapes = new ReferenceLinkedOpenHashSet<>(shapes);
        removedShapes = new ReferenceLinkedOpenHashSet<>(removedShapes);
        familyKeys = List.copyOf(familyKeys);
        overrides = new Reference2ObjectLinkedOpenHashMap<>(overrides);
    }

    /// Whether this declaration alone generates `canonicalShape`: through its own shapes or a family it joins,
    /// and not removed by it. `canonical` maps a declared shape onto the elected shape of its name.
    boolean generates(Shape canonicalShape, UnaryOperator<Shape> canonical, MaterialRegistry registry) {
        for (Shape removed : removedShapes) {
            if (canonical.apply(removed) == canonicalShape) return false;
        }
        for (Shape shape : shapes) {
            if (canonical.apply(shape) == canonicalShape) return true;
        }
        for (String[] familyKey : familyKeys) {
            Family family = registry.getFamily(familyKey[0], familyKey[1]);
            if (family == null) continue;
            for (Shape shape : family.getShapesInternal()) {
                if (canonical.apply(shape) == canonicalShape) return true;
            }
        }
        return false;
    }

    /// The override this declaration names for `canonicalShape`, or null.
    ShapeOverride overrideFor(Shape canonicalShape, UnaryOperator<Shape> canonical) {
        for (Map.Entry<Shape, ShapeOverride> entry : overrides.entrySet()) {
            if (canonical.apply(entry.getKey()) == canonicalShape) return entry.getValue();
        }
        return null;
    }
}
