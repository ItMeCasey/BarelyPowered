package com.itmecasey.barelypowered;

import com.itmecasey.barelypowered.config.ConfigDef;
import com.itmecasey.barelypowered.config.ConfigMigration;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Every tunable number in the mod, in one list with its default, range and comment. The loader builds its config file
 * ({@code config/barelypowered-common.toml}) from {@link #defs()} and pushes the values in with {@link #apply} when the file loads or
 * reloads; until then the defaults are used. Gameplay code only reads through the getters.
 */
public final class Settings {
    /** The tier columns of a per-tier setting, in order. */
    public static final String[] TIER_NAMES = {"stone", "iron", "steel"};

    private static final List<ConfigDef> DEFS = new ArrayList<>();
    private static final Map<String, ConfigDef> BY_KEY = new LinkedHashMap<>();
    private static final Map<String, List<String>> LISTS = new LinkedHashMap<>();
    private static final Map<String, List<String>> RETIRED = new LinkedHashMap<>();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    private static void add(ConfigDef def) {
        DEFS.add(def);
        BY_KEY.put(def.key(), def);
    }

    private static void whole(String key, int fallback, int min, int max, String comment) {
        add(new ConfigDef(key, ConfigDef.Type.INT, String.valueOf(fallback), min, max, comment));
    }

    private static void number(String key, double fallback, double min, double max, String comment) {
        add(new ConfigDef(key, ConfigDef.Type.DOUBLE, String.valueOf(fallback), min, max, comment));
    }

    private static void flag(String key, boolean fallback, String comment) {
        add(new ConfigDef(key, ConfigDef.Type.BOOL, String.valueOf(fallback), 0, 1, comment));
    }

    /** One number per tier, written as {@code key.stone}, {@code key.iron} and {@code key.steel}. Old files held them as "1.0,2.0,4.0". */
    private static void perTier(String key, String defaults, double min, double max, String comment) {
        String[] parts = defaults.split(",");
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < TIER_NAMES.length; i++) {
            number(key + "." + TIER_NAMES[i], Double.parseDouble(parts[i]), min, max, comment.replace("%tier%", TIER_NAMES[i]));
            keys.add(key + "." + TIER_NAMES[i]);
        }
        LISTS.put(key, keys);
    }

    static {
        whole("general.buffer_ticks", 100, 1, 72000, "Each generator stores this many ticks of its top output before it stops producing.");
        flag("general.push_to_neighbors", true, "Push energy into adjacent blocks that accept Forge Energy. Cables can also pull from the generator.");
        number("general.transfer_multiplier", 2.0, 0.1, 1000.0, "How fast a generator can hand energy out, as a multiple of its top output.");

        whole("hand_crank.fe_per_use", 200, 1, 1_000_000, "Energy added by one right-click.");
        whole("hand_crank.cooldown_ticks", 6, 0, 1200, "Ticks between cranks.");
        whole("hand_crank.max_output", 40, 1, 100_000, "Most energy the crank hands out per tick.");
        whole("hand_crank.buffer", 1000, 1, 10_000_000, "Energy the crank can hold.");

        whole("combustion.coal.fe_per_tick", 30, 1, 100_000, "Stone-tier output while coal burns.");
        whole("combustion.coal.burn_ticks", 1600, 1, 1_000_000, "Ticks one coal burns in a stone-tier generator.");
        whole("combustion.charcoal.fe_per_tick", 24, 1, 100_000, "Stone-tier output while charcoal burns.");
        whole("combustion.charcoal.burn_ticks", 1600, 1, 1_000_000, "Ticks one charcoal burns in a stone-tier generator.");
        whole("combustion.wood.fe_per_tick", 12, 1, 100_000, "Stone-tier output while logs burn.");
        whole("combustion.wood.burn_ticks", 300, 1, 1_000_000, "Ticks one log burns in a stone-tier generator.");
        whole("combustion.planks.fe_per_tick", 12, 1, 100_000, "Stone-tier output while planks burn.");
        whole("combustion.planks.burn_ticks", 75, 1, 1_000_000,
                "Ticks one plank burns in a stone-tier generator (a quarter of a log, so planks never beat the log they came from).");
        perTier("combustion.tier.output_multiplier", "1.0,2.0,4.0", 0.01, 1000.0, "Output multiplier for the %tier% combustion generator.");
        perTier("combustion.tier.burn_multiplier", "1.0,0.75,0.6", 0.01, 1000.0, "Burn time multiplier for the %tier% combustion generator (lower burns faster).");

        number("lava.fe_per_tick", 80.0, 0.1, 100_000.0, "Stone-tier output while lava burns.");
        whole("lava.bucket_ticks", 1200, 1, 1_000_000, "Ticks one bucket (1000 mB) of lava lasts in a stone-tier generator.");
        whole("lava.tank_buckets", 4, 1, 1000, "How many buckets of lava the generator holds.");
        perTier("lava.tier.output_multiplier", "1.0,2.0,4.0", 0.01, 1000.0, "Output multiplier for the %tier% lava generator.");
        perTier("lava.tier.burn_multiplier", "1.0,0.8,0.6", 0.01, 1000.0, "Burn time multiplier for the %tier% lava generator.");

        number("solar.tier1.peak", 6.0, 0.0, 100_000.0, "Tier 1 output at noon with a clear sky.");
        number("solar.tier2.peak", 24.0, 0.0, 100_000.0, "Tier 2 output at noon with a clear sky.");
        number("solar.tier1.night_floor", 0.0, 0.0, 100_000.0, "Tier 1 output at night (needs sky access).");
        number("solar.tier2.night_floor", 1.5, 0.0, 100_000.0, "Tier 2 output at night (needs sky access).");
        number("solar.tier1.rain_factor", 0.25, 0.0, 1.0, "Tier 1 share of output left in rain.");
        number("solar.tier2.rain_factor", 0.6, 0.0, 1.0, "Tier 2 share of output left in rain.");
        number("solar.tier1.thunder_factor", 0.1, 0.0, 1.0, "Tier 1 share of output left in a thunderstorm.");
        number("solar.tier2.thunder_factor", 0.4, 0.0, 1.0, "Tier 2 share of output left in a thunderstorm.");

        number("wind.tier1.base", 12.0, 0.0, 100_000.0, "Tier 1 output at full height with open sky.");
        number("wind.tier2.base", 40.0, 0.0, 100_000.0, "Tier 2 output at full height with open sky.");
        whole("wind.height.min_y", 64, -2048, 2048, "At or below this height the wind is at its weakest.");
        whole("wind.height.full_y", 192, -2048, 2048, "At or above this height the wind is at full strength.");
        number("wind.height.floor", 0.15, 0.0, 1.0, "Share of output left at the minimum height.");
        number("wind.rain_bonus", 1.2, 0.0, 100.0, "Output multiplier in rain.");
        number("wind.thunder_bonus", 1.5, 0.0, 100.0, "Output multiplier in a thunderstorm.");
        number("wind.blocked_penalty", 0.9, 0.0, 1.0, "How much of the output solid blocks around the blades can take away (1.0 = all).");
        whole("wind.turbine_radius", 6, 0, 64, "Other turbines within this many blocks steal wind.");
        number("wind.turbine_penalty", 0.2, 0.0, 1.0, "Output lost for each other turbine in range.");
        number("wind.turbine_min_factor", 0.3, 0.0, 1.0, "Output never drops below this share because of other turbines.");

        number("water_wheel.fe_per_flow", 20.0, 0.0, 100_000.0, "Output for one unit of flow. A falling stream counts as 1.0 and a flowing block counts level/8.");
        number("water_wheel.max_flow", 2.0, 0.0, 100.0, "Flow beyond this adds nothing.");
        number("water_wheel.source_flow", 0.0, 0.0, 100.0, "Flow credited for a still water source block touching the wheel (0 = needs flowing water).");

        number("geothermal.fe_per_heat", 16.0, 0.0, 100_000.0, "Output for one unit of heat.");
        number("geothermal.max_heat", 3.0, 0.0, 100.0, "Heat beyond this adds nothing.");
        number("geothermal.lava_source_heat", 1.0, 0.0, 100.0, "Heat from a lava source block touching or near the generator.");
        number("geothermal.lava_flow_heat", 0.5, 0.0, 100.0, "Heat from flowing lava.");
        number("geothermal.magma_heat", 0.75, 0.0, 100.0, "Heat from a magma block.");

        whole("battery.basic.capacity", 100_000, 1, Integer.MAX_VALUE, "Energy a basic battery holds.");
        whole("battery.iron.capacity", 400_000, 1, Integer.MAX_VALUE, "Energy an iron battery holds.");
        whole("battery.steel.capacity", 1_600_000, 1, Integer.MAX_VALUE, "Energy a steel battery holds.");
        whole("battery.basic.rate", 200, 1, 10_000_000, "Most energy a basic battery takes in, and most it gives out, each tick.");
        whole("battery.iron.rate", 800, 1, 10_000_000, "Most energy an iron battery takes in, and most it gives out, each tick.");
        whole("battery.steel.rate", 3200, 1, 10_000_000, "Most energy a steel battery takes in, and most it gives out, each tick.");

        // 2026.11.2 and older wrote 28.0 as the default; a file from then holds it whether or not the player chose it.
        RETIRED.put("water_wheel.fe_per_flow", List.of("28.0"));
    }

    private Settings() {
    }

    /** Every setting with its default, range and comment, in the order of the config file. */
    public static List<ConfigDef> defs() {
        return List.copyOf(DEFS);
    }

    /** Old list keys (from the .properties file) mapped to the new per-tier keys. */
    public static Map<String, List<String>> oldListKeys() {
        return Map.copyOf(LISTS);
    }

    /** Settings whose default changed in a release, mapped to the older defaults (see {@link ConfigMigration}). */
    public static Map<String, List<String>> retiredDefaults() {
        return Map.copyOf(RETIRED);
    }

    /** Restores every default. */
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

    /** Every setting key, in file order. */
    public static java.util.List<String> keys() {
        return new ArrayList<>(BY_KEY.keySet());
    }

    public static boolean has(String key) {
        return BY_KEY.containsKey(key);
    }

    /** The current value as text. */
    public static synchronized String get(String key) {
        return raw(key);
    }

    /** Changes a value in memory (scripts use this); the config file is not touched. Unknown keys are ignored. */
    public static synchronized void set(String key, String value) {
        if (BY_KEY.containsKey(key)) {
            VALUES.put(key, value);
        }
    }

    private static synchronized String raw(String key) {
        ConfigDef def = BY_KEY.get(key);
        if (def == null) {
            throw new IllegalArgumentException("Unknown setting " + key);
        }
        return VALUES.getOrDefault(key, def.fallback());
    }

    public static double getDouble(String key) {
        try {
            return Double.parseDouble(raw(key).trim());
        } catch (NumberFormatException ex) {
            return Double.parseDouble(BY_KEY.get(key).fallback());
        }
    }

    public static int getInt(String key) {
        return (int) Math.round(getDouble(key));
    }

    public static boolean getBool(String key) {
        return Boolean.parseBoolean(raw(key).trim());
    }

    /** Multiplier for a 1-based tier, from a per-tier setting such as {@code combustion.tier.output_multiplier}. */
    public static double tierValue(String key, int tier) {
        return getDouble(key + "." + TIER_NAMES[Math.max(0, Math.min(TIER_NAMES.length - 1, tier - 1))]);
    }

    /** Reads overrides from old-format properties text (used by tests); unknown keys are ignored and bad values fall back when read. */
    public static synchronized void load(Reader reader) throws IOException {
        Properties p = new Properties();
        p.load(reader);
        VALUES.clear();
        VALUES.putAll(ConfigMigration.expand(p, DEFS, LISTS, null));
    }
}
