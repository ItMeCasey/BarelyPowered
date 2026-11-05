package com.itmecasey.barelycables;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The tunable numbers of BarelyCables, kept in {@code config/barelycables.properties} (one {@code key=value} line each, with a
 * comment above). A missing key uses its default; the file is rewritten with every key after loading.
 */
public final class CableSettings {
    public static final String[] TIER_NAMES = {"basic", "iron", "steel"};

    private record Entry(String fallback, String comment) {
    }

    private static final Map<String, Entry> DEFS = new LinkedHashMap<>();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    private static void def(String key, Object fallback, String comment) {
        DEFS.put(key, new Entry(String.valueOf(fallback), comment));
    }

    static {
        def("cable.basic.rate", 400, "Most energy a network of basic cables moves each tick.");
        def("cable.iron.rate", 1600, "Most energy a network of iron cables moves each tick.");
        def("cable.steel.rate", 6400, "Most energy a network of steel cables moves each tick.");
        def("cable.buffer_ticks", 2, "The network keeps this many ticks of its rate in its own buffer, which other mods' pipes can fill and drain.");
    }

    private CableSettings() {
    }

    public static synchronized void reset() {
        VALUES.clear();
    }

    private static synchronized String raw(String key) {
        Entry e = DEFS.get(key);
        if (e == null) {
            throw new IllegalArgumentException("Unknown setting " + key);
        }
        return VALUES.getOrDefault(key, e.fallback());
    }

    public static int getInt(String key) {
        try {
            return (int) Math.round(Double.parseDouble(raw(key).trim()));
        } catch (NumberFormatException ex) {
            return (int) Math.round(Double.parseDouble(DEFS.get(key).fallback()));
        }
    }

    /** Throughput of a tier (1 basic, 2 iron, 3 steel) in energy per tick. */
    public static int rate(int tier) {
        return Math.max(1, getInt("cable." + TIER_NAMES[Math.max(1, Math.min(3, tier)) - 1] + ".rate"));
    }

    public static String blockId(int tier) {
        return "energy_cable_" + TIER_NAMES[Math.max(1, Math.min(3, tier)) - 1];
    }

    public static synchronized void load(Reader reader) throws IOException {
        Properties p = new Properties();
        p.load(reader);
        VALUES.clear();
        for (String key : p.stringPropertyNames()) {
            if (DEFS.containsKey(key)) {
                VALUES.put(key, p.getProperty(key));
            }
        }
    }

    public static synchronized void loadFile(Path file) {
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    load(r);
                }
            }
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                w.write("# BarelyCables settings. Energy is in Forge Energy (FE); rates are per game tick (20 per second).\n");
                w.write("# Edit a value and restart the game or server. Delete a line to restore its default.\n");
                for (Map.Entry<String, Entry> e : DEFS.entrySet()) {
                    w.write("\n# " + e.getValue().comment() + "\n");
                    w.write(e.getKey() + "=" + VALUES.getOrDefault(e.getKey(), e.getValue().fallback()) + "\n");
                }
            }
        } catch (IOException | RuntimeException ex) {
            System.err.println("[BarelyCables] Could not read " + file + ", using defaults: " + ex);
            VALUES.clear();
        }
    }
}
