package com.itmecasey.barelypowered;

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
 * Every tunable number in the mod. The file {@code config/barelypowered.properties} holds one {@code key=value} line per
 * setting; a missing key uses its default, and the file is rewritten with every key and a comment after loading.
 */
public final class Settings {
    private record Entry(String fallback, String comment) {
    }

    private static final Map<String, Entry> DEFS = new LinkedHashMap<>();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    private static void def(String key, Object fallback, String comment) {
        DEFS.put(key, new Entry(String.valueOf(fallback), comment));
    }

    static {
        def("general.buffer_ticks", 100, "Each generator stores this many ticks of its top output before it stops producing.");
        def("general.push_to_neighbors", true, "Push energy into adjacent blocks that accept Forge Energy. Cables can also pull from the generator.");
        def("general.transfer_multiplier", 2.0, "How fast a generator can hand energy out, as a multiple of its top output.");

        def("hand_crank.fe_per_use", 200, "Energy added by one right-click.");
        def("hand_crank.cooldown_ticks", 6, "Ticks between cranks.");
        def("hand_crank.max_output", 40, "Most energy the crank hands out per tick.");
        def("hand_crank.buffer", 1000, "Energy the crank can hold.");

        def("combustion.coal.fe_per_tick", 30, "Stone-tier output while coal burns.");
        def("combustion.coal.burn_ticks", 1600, "Ticks one coal burns in a stone-tier generator.");
        def("combustion.charcoal.fe_per_tick", 24, "Stone-tier output while charcoal burns.");
        def("combustion.charcoal.burn_ticks", 1600, "Ticks one charcoal burns in a stone-tier generator.");
        def("combustion.wood.fe_per_tick", 12, "Stone-tier output while logs or planks burn.");
        def("combustion.wood.burn_ticks", 300, "Ticks one log or plank burns in a stone-tier generator.");
        def("combustion.tier.output_multiplier", "1.0,2.0,4.0", "Output multiplier for stone, iron and steel generators.");
        def("combustion.tier.burn_multiplier", "1.0,0.75,0.6", "Burn time multiplier for stone, iron and steel generators (lower burns faster).");

        def("lava.fe_per_tick", 80, "Stone-tier output while lava burns.");
        def("lava.bucket_ticks", 1200, "Ticks one bucket (1000 mB) of lava lasts in a stone-tier generator.");
        def("lava.tank_buckets", 4, "How many buckets of lava the generator holds.");
        def("lava.tier.output_multiplier", "1.0,2.0,4.0", "Output multiplier for stone, iron and steel lava generators.");
        def("lava.tier.burn_multiplier", "1.0,0.8,0.6", "Burn time multiplier for stone, iron and steel lava generators.");

        def("solar.tier1.peak", 6.0, "Tier 1 output at noon with a clear sky.");
        def("solar.tier2.peak", 24.0, "Tier 2 output at noon with a clear sky.");
        def("solar.tier1.night_floor", 0.0, "Tier 1 output at night (needs sky access).");
        def("solar.tier2.night_floor", 1.5, "Tier 2 output at night (needs sky access).");
        def("solar.tier1.rain_factor", 0.25, "Tier 1 share of output left in rain.");
        def("solar.tier2.rain_factor", 0.6, "Tier 2 share of output left in rain.");
        def("solar.tier1.thunder_factor", 0.1, "Tier 1 share of output left in a thunderstorm.");
        def("solar.tier2.thunder_factor", 0.4, "Tier 2 share of output left in a thunderstorm.");

        def("wind.tier1.base", 12.0, "Tier 1 output at full height with open sky.");
        def("wind.tier2.base", 40.0, "Tier 2 output at full height with open sky.");
        def("wind.height.min_y", 64, "At or below this height the wind is at its weakest.");
        def("wind.height.full_y", 192, "At or above this height the wind is at full strength.");
        def("wind.height.floor", 0.15, "Share of output left at the minimum height.");
        def("wind.rain_bonus", 1.2, "Output multiplier in rain.");
        def("wind.thunder_bonus", 1.5, "Output multiplier in a thunderstorm.");
        def("wind.blocked_penalty", 0.9, "How much of the output solid blocks around the blades can take away (1.0 = all).");
        def("wind.turbine_radius", 6, "Other turbines within this many blocks steal wind.");
        def("wind.turbine_penalty", 0.2, "Output lost for each other turbine in range.");
        def("wind.turbine_min_factor", 0.3, "Output never drops below this share because of other turbines.");

        def("water_wheel.fe_per_flow", 28.0, "Output for one unit of flow. A falling stream counts as 1.0 and a flowing block counts level/8.");
        def("water_wheel.max_flow", 2.0, "Flow beyond this adds nothing.");
        def("water_wheel.source_flow", 0.0, "Flow credited for a still water source block touching the wheel (0 = needs flowing water).");

        def("geothermal.fe_per_heat", 16.0, "Output for one unit of heat.");
        def("geothermal.max_heat", 3.0, "Heat beyond this adds nothing.");
        def("geothermal.lava_source_heat", 1.0, "Heat from a lava source block touching or near the generator.");
        def("geothermal.lava_flow_heat", 0.5, "Heat from flowing lava.");
        def("geothermal.magma_heat", 0.75, "Heat from a magma block.");
    }

    private Settings() {
    }

    /** Restores every default. */
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

    public static double getDouble(String key) {
        try {
            return Double.parseDouble(raw(key).trim());
        } catch (NumberFormatException ex) {
            return Double.parseDouble(DEFS.get(key).fallback());
        }
    }

    public static int getInt(String key) {
        return (int) Math.round(getDouble(key));
    }

    public static boolean getBool(String key) {
        return Boolean.parseBoolean(raw(key).trim());
    }

    /** A comma-separated list of numbers; a short or broken list is padded from the default. */
    public static double[] getList(String key, int length) {
        String[] parts = raw(key).split(",");
        String[] defaults = DEFS.get(key).fallback().split(",");
        double[] out = new double[length];
        for (int i = 0; i < length; i++) {
            String p = i < parts.length ? parts[i] : defaults[Math.min(i, defaults.length - 1)];
            try {
                out[i] = Double.parseDouble(p.trim());
            } catch (NumberFormatException ex) {
                out[i] = Double.parseDouble(defaults[Math.min(i, defaults.length - 1)].trim());
            }
        }
        return out;
    }

    /** Multiplier for a 1-based tier, from a list setting. */
    public static double tierValue(String key, int tier) {
        return getList(key, 3)[Math.max(0, Math.min(2, tier - 1))];
    }

    /** Reads overrides from a properties text (also used by tests). */
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

    /** Loads the file if there is one, then rewrites it with every key so players can see what they can change. */
    public static synchronized void loadFile(Path file) {
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    load(r);
                }
            }
            write(file);
        } catch (IOException | RuntimeException ex) {
            System.err.println("[BarelyPowered] Could not read " + file + ", using defaults: " + ex);
            VALUES.clear();
        }
    }

    private static void write(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("# BarelyPowered settings. Energy is in Forge Energy (FE); rates are per game tick (20 per second).\n");
            w.write("# Edit a value and restart the game or server. Delete a line to restore its default.\n");
            for (Map.Entry<String, Entry> e : DEFS.entrySet()) {
                w.write("\n# " + e.getValue().comment() + "\n");
                w.write(e.getKey() + "=" + VALUES.getOrDefault(e.getKey(), e.getValue().fallback()) + "\n");
            }
        }
    }
}
