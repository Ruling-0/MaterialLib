package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// Renders a resolved [MaterialRegistry]'s full index assignment as a CSV table.
///
/// One row per assigned index in ascending order, including indices reserved for materials not loaded
/// this session -- the data needed when debugging persistence issues. Columns are the index, the bare
/// material name, the owning modid, whether the material is loaded, and (for loaded materials) the
/// generated shapes and family memberships as ';'-joined keys. Fields containing a comma, quote, or
/// line break are quoted with doubled inner quotes (RFC 4180); rows end with '\n'.
final class MaterialCsv {

    private MaterialCsv() {}

    static String dump(MaterialRegistry registry) {
        Map<Integer, String> namesByIndex = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : registry.getAssignedIndices()
            .entrySet()) {
            namesByIndex.put(entry.getValue(), entry.getKey());
        }
        Map<String, String> owners = registry.getAssignedOwners();
        StringBuilder csv = new StringBuilder("index,name,owner,loaded,shapes,families\n");
        for (Map.Entry<Integer, String> entry : namesByIndex.entrySet()) {
            int index = entry.getKey();
            String name = entry.getValue();
            Material material = registry.getMaterialByIndex(index);
            csv.append(index)
                .append(',')
                .append(escape(name))
                .append(',')
                .append(escape(owners.getOrDefault(name, "")))
                .append(',')
                .append(material != null ? "yes" : "no")
                .append(',')
                .append(escape(material != null ? shapeKeys(material) : ""))
                .append(',')
                .append(escape(material != null ? familyKeys(material) : ""))
                .append('\n');
        }
        return csv.toString();
    }

    private static String shapeKeys(Material material) {
        List<String> keys = new ArrayList<>();
        for (Shape shape : material.getShapes()) {
            keys.add(Names.key(shape.getModId(), shape.getName()));
        }
        keys.sort(null);
        return String.join(";", keys);
    }

    private static String familyKeys(Material material) {
        List<String> keys = new ArrayList<>();
        for (Family family : material.getFamilies()) {
            keys.add(family.getKey());
        }
        return String.join(";", keys);
    }

    private static String escape(String field) {
        if (field.indexOf(',') < 0 && field.indexOf('"') < 0 && field.indexOf('\n') < 0 && field.indexOf('\r') < 0) {
            return field;
        }
        return '"' + field.replace("\"", "\"\"") + '"';
    }
}
