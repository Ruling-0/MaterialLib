package com.ruling_0.materiallib.api;

import java.io.File;
import java.util.Map;

/// The instance-global store of the shape name -> owning modid assignment, a JSON file under `config/materiallib`.
///
/// Shapes that share a name unify onto one owner (see [ShapeUnification]). [#loadInto] feeds the saved owners
/// to the registry before it resolves, so an existing name keeps its owner even when a mod that also declares it is
/// added. [#saveFrom] writes the resolved assignment back. The shape's saved identity does not depend on the
/// owner, so a wrong owner changes only which mod's item or block backs the shape, never the stored stacks.
public final class ShapeOwnerStore {

    private static final String FILE_NAME = "shape-owners.json";
    private static final int FORMAT_VERSION = 1;

    private ShapeOwnerStore() {}

    /// Loads the saved owners from `<dir>/shape-owners.json` and hands them to the registry to honor at resolve.
    public static void loadInto(ShapeRegistry registry, File dir) {
        registry.setPersistedOwners(read(new File(dir, FILE_NAME)));
    }

    /// Writes the registry's resolved owners back to `<dir>/shape-owners.json`.
    public static void saveFrom(ShapeRegistry registry, File dir) {
        write(new File(dir, FILE_NAME), registry.getAssignedOwners());
    }

    static Map<String, String> read(File file) {
        return OwnerStore.read(file, corrupt(file));
    }

    static void write(File file, Map<String, String> owners) {
        OwnerStore.write(file, FORMAT_VERSION, owners, "Could not write the shape owner assignment to " + file);
    }

    private static String corrupt(File file) {
        return "The shape owner assignment at " + file +
            " is unreadable or malformed. Fix or remove the file; removing it recomputes shape owners.";
    }
}
