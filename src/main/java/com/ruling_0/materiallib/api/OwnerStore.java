package com.ruling_0.materiallib.api;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/// The file format shared by the owner-assignment stores: `{"version": N, "owners": {name: modid}}`, written
/// with names sorted and each owner validated as a plausible modid.
final class OwnerStore {

    private OwnerStore() {}

    static Map<String, String> read(File file, String corruptMessage) {
        if (!file.isFile()) return new LinkedHashMap<>();
        Data data = JsonStore.read(file, Data.class, corruptMessage);
        if (data == null || data.owners == null) {
            throw new IllegalStateException(corruptMessage);
        }
        validateOwners(corruptMessage, data.owners);
        return data.owners;
    }

    static void write(File file, int version, Map<String, String> owners, String failMessage) {
        Data data = new Data();
        data.version = version;
        data.owners = JsonStore.sorted(owners, Map.Entry.comparingByKey());
        JsonStore.write(file, data, failMessage);
    }

    /// Rejects an assignment whose owner is not a valid modid: null, empty, or containing ':' or whitespace.
    private static void validateOwners(String corruptMessage, Map<String, String> owners) {
        for (Map.Entry<String, String> entry : owners.entrySet()) {
            String owner = entry.getValue();
            if (owner == null || owner.isEmpty()) {
                throw new IllegalStateException(corruptMessage + " (" + entry.getKey() + " has no owner)");
            }
            for (int i = 0; i < owner.length(); i++) {
                char c = owner.charAt(i);
                if (c == ':' || Character.isWhitespace(c)) {
                    throw new IllegalStateException(
                        corruptMessage + " (" + entry.getKey() + " has an invalid owner \"" + owner + "\")");
                }
            }
        }
    }

    private static final class Data {

        int version;
        LinkedHashMap<String, String> owners;
    }
}
