package com.itmecasey.barelycables;

import com.itmecasey.barelycables.config.ConfigDef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The tunable numbers of BarelyCables, in one list with default, range and comment. The loader builds its config file
 * ({@code config/barelycables-common.toml}) from {@link #defs()} and pushes the values in with {@link #apply} when the file loads or
 * reloads; until then the defaults are used.
 */
public final class CableSettings {
    public static final String[] TIER_NAMES = {"basic", "iron", "steel"};

    private static final List<ConfigDef> DEFS = new ArrayList<>();
    private static final Map<String, ConfigDef> BY_KEY = new LinkedHashMap<>();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    private static void whole(String key, int fallback, int min, int max, String comment) {
        ConfigDef def = new ConfigDef(key, ConfigDef.Type.INT, String.valueOf(fallback), min, max, comment);
        DEFS.add(def);
        BY_KEY.put(key, def);
    }

    static {
        whole("cable.basic.rate", 400, 1, 100_000_000, "Most energy a network of basic cables moves each tick.");
        whole("cable.iron.rate", 1600, 1, 100_000_000, "Most energy a network of iron cables moves each tick.");
        whole("cable.steel.rate", 6400, 1, 100_000_000, "Most energy a network of steel cables moves each tick.");
        whole("cable.buffer_ticks", 2, 1, 1200, "The network keeps this many ticks of its rate in its own buffer, which other mods' pipes can fill and drain.");
    }

    private CableSettings() {
    }

    /** Every setting with its default, range and comment, in the order of the config file. */
    public static List<ConfigDef> defs() {
        return List.copyOf(DEFS);
    }

    /** Old list keys: BarelyCables never had any. */
    public static Map<String, List<String>> oldListKeys() {
        return Map.of();
    }

    /** Settings whose default changed in a release: none so far. */
    public static Map<String, List<String>> retiredDefaults() {
        return Map.of();
    }

    public static synchronized void reset() {
        VALUES.clear();
    }

    /** Replaces all values with the ones the loader read from the config file (key to text). */
    public static synchronized void apply(Map<String, String> values) {
        VALUES.clear();
        values.forEach((key, value) -> {
            if (BY_KEY.containsKey(key)) {
                VALUES.put(key, value);
            }
        });
    }

    private static synchronized String raw(String key) {
        ConfigDef def = BY_KEY.get(key);
        if (def == null) {
            throw new IllegalArgumentException("Unknown setting " + key);
        }
        return VALUES.getOrDefault(key, def.fallback());
    }

    public static int getInt(String key) {
        try {
            return (int) Math.round(Double.parseDouble(raw(key).trim()));
        } catch (NumberFormatException ex) {
            return (int) Math.round(Double.parseDouble(BY_KEY.get(key).fallback()));
        }
    }

    /** Throughput of a tier (1 basic, 2 iron, 3 steel) in energy per tick. */
    public static int rate(int tier) {
        return Math.max(1, getInt("cable." + TIER_NAMES[Math.max(1, Math.min(3, tier)) - 1] + ".rate"));
    }

    public static String blockId(int tier) {
        return "energy_cable_" + TIER_NAMES[Math.max(1, Math.min(3, tier)) - 1];
    }
}
