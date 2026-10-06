package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;

/// The shapes, families and overrides one mod's [MaterialBuilder] declared, kept apart from the unified [Material]
/// it folds into. Unification merges every declaration's shapes onto the owner, but which [ShapeOverride] stands
/// depends on which declaration generates the shape.
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
