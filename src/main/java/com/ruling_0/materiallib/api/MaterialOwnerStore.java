package com.ruling_0.materiallib.api;

import java.io.File;
import java.util.Map;

/// The instance-global store of the material name -> owning modid assignment, a JSON file under
/// `config/materiallib`.
///
/// Materials that share a name unify onto one owner (see [MaterialRegistry#resolve]). [#loadInto] feeds the
/// saved owners to the registry before it resolves, so an existing name keeps its owner even when a mod that
/// also declares it is added. [#saveFrom] writes the resolved assignment back.
public final class MaterialOwnerStore {

    private static final String FILE_NAME = "material-owners.json";
    private static final int FORMAT_VERSION = 1;

    private MaterialOwnerStore() {}

    /// Loads the saved owners from `<dir>/material-owners.json` and hands them to the registry to honor at
    /// resolve.
    public static void loadInto(MaterialRegistry registry, File dir) {
        registry.setPersistedOwners(read(new File(dir, FILE_NAME)));
    }

    /// Writes the registry's resolved owners back to `<dir>/material-owners.json`.
    public static void saveFrom(MaterialRegistry registry, File dir) {
        write(new File(dir, FILE_NAME), registry.getAssignedOwners());
    }

    static Map<String, String> read(File file) {
        return OwnerStore.read(file, corrupt(file));
    }

    static void write(File file, Map<String, String> owners) {
        OwnerStore.write(file, FORMAT_VERSION, owners, "Could not write the material owner assignment to " + file);
    }

    private static String corrupt(File file) {
        return "The material owner assignment at " + file +
            " is unreadable or malformed. Fix or remove the file; removing it recomputes material owners.";
    }
}
