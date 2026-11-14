package com.itmecasey.barelypowered.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Turns the one list of settings into a loader's config spec (NeoForge ModConfigSpec or Forge ForgeConfigSpec) through an adapter. */
public final class ConfigSchema {
    /** What a loader provides: define one entry in its builder and return a reader of the current value. */
    public interface Adapter {
        Supplier<Object> integer(String key, int fallback, int min, int max, String comment);

        Supplier<Object> decimal(String key, double fallback, double min, double max, String comment);

        Supplier<Object> bool(String key, boolean fallback, String comment);
    }

    private ConfigSchema() {
    }

    /** The settings with entries of one table next to each other, tables in order of first appearance. */
    public static List<ConfigDef> grouped(List<ConfigDef> defs) {
        Map<String, List<ConfigDef>> byTable = new LinkedHashMap<>();
        for (ConfigDef def : defs) {
            byTable.computeIfAbsent(def.table(), t -> new ArrayList<>()).add(def);
        }
        List<ConfigDef> out = new ArrayList<>();
        byTable.values().forEach(out::addAll);
        return out;
    }

    /** Defines every setting through the adapter, in file order; returns a reader for each key. */
    public static Map<String, Supplier<Object>> define(List<ConfigDef> defs, Adapter adapter) {
        Map<String, Supplier<Object>> readers = new LinkedHashMap<>();
        for (ConfigDef def : grouped(defs)) {
            Supplier<Object> reader = switch (def.type()) {
                case INT -> adapter.integer(def.key(), (int) Math.round(Double.parseDouble(def.fallback())), (int) def.min(), (int) def.max(), def.comment());
                case DOUBLE -> adapter.decimal(def.key(), Double.parseDouble(def.fallback()), def.min(), def.max(), def.comment());
                case BOOL -> adapter.bool(def.key(), Boolean.parseBoolean(def.fallback()), def.comment());
            };
            readers.put(def.key(), reader);
        }
        return readers;
    }

    /** The current values of all readers as text, ready for the mod's settings. */
    public static Map<String, String> read(Map<String, Supplier<Object>> readers) {
        Map<String, String> out = new LinkedHashMap<>();
        readers.forEach((key, reader) -> out.put(key, String.valueOf(reader.get())));
        return out;
    }
}
