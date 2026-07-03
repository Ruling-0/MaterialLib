package com.ruling_0.materiallib.api;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/// The file machinery shared by MaterialLib's instance-global JSON stores: Gson parsing with a wrapped
/// corrupt-file error, and an atomic write through a temp file so a crash mid-write never truncates the store.
final class JsonStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonStore() {}

    /// Parses the file as `type`, or throws [IllegalStateException] with `corruptMessage` when unreadable or
    /// malformed. May return null for an empty file; callers validate fields.
    static <D> D read(File file, Class<D> type, String corruptMessage) {
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        }
        catch (IOException | JsonParseException e) {
            throw new IllegalStateException(corruptMessage, e);
        }
    }

    /// Writes `data` as pretty-printed JSON via a sibling `.tmp` file and an atomic move, or throws
    /// [IllegalStateException] with `failMessage`.
    static void write(File file, Object data, String failMessage) {
        try {
            File parent = file.getParentFile();
            if (parent != null) Files.createDirectories(parent.toPath());
            File temp = new File(parent, file.getName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e) {
            throw new IllegalStateException(failMessage, e);
        }
    }

    /// A copy of `map` whose iteration order follows `order`, for stable file output.
    static <K, V> LinkedHashMap<K, V> sorted(Map<K, V> map, Comparator<Map.Entry<K, V>> order) {
        List<Map.Entry<K, V>> entries = new ObjectArrayList<>(map.entrySet());
        entries.sort(order);
        LinkedHashMap<K, V> sorted = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : entries) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        return sorted;
    }
}
