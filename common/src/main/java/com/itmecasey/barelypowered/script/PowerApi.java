package com.itmecasey.barelypowered.script;

import com.itmecasey.barelypowered.Kind;
import com.itmecasey.barelypowered.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * The scripting surface, exposed to ModJS scripts as {@code BarelyPowered}. It uses plain strings and numbers so any script engine
 * can call it. Reads are for server scripts; they return null when the position holds no such block or no world is running.
 */
public final class PowerApi {
    private static final Logger LOG = Logger.getLogger("barelypowered");

    /** A generator's state at one moment. */
    public static final class Generator {
        public final String kind;
        public final int tier;
        public final String id;
        public final double output;
        public final int stored;
        public final int capacity;
        public final boolean lit;
        public final String note;
        public final String fuel;
        public final int fuelCount;
        public final int lavaMb;
        public final int lavaCapacityMb;
        public final int burnTicksLeft;

        public Generator(String kind, int tier, String id, double output, int stored, int capacity, boolean lit, String note, String fuel,
                int fuelCount, int lavaMb, int lavaCapacityMb, int burnTicksLeft) {
            this.kind = kind;
            this.tier = tier;
            this.id = id;
            this.output = output;
            this.stored = stored;
            this.capacity = capacity;
            this.lit = lit;
            this.note = note;
            this.fuel = fuel;
            this.fuelCount = fuelCount;
            this.lavaMb = lavaMb;
            this.lavaCapacityMb = lavaCapacityMb;
            this.burnTicksLeft = burnTicksLeft;
        }
    }

    /** A battery's state at one moment. */
    public static final class Battery {
        public final int tier;
        public final String id;
        public final int stored;
        public final int capacity;
        public final int rate;

        public Battery(int tier, String id, int stored, int capacity, int rate) {
            this.tier = tier;
            this.id = id;
            this.stored = stored;
            this.capacity = capacity;
            this.rate = rate;
        }
    }

    /** Reads blocks in the running world; each Minecraft version supplies one. */
    public interface World {
        Generator generatorAt(String dimension, int x, int y, int z);

        Battery batteryAt(String dimension, int x, int y, int z);
    }

    private static volatile World world;

    public static final PowerApi INSTANCE = new PowerApi();

    private PowerApi() {
    }

    public static void attach(World w) {
        world = w;
    }

    /** Listens to an event ({@link #events()}); the listener gets one event object. Returns false for an unknown event name. */
    public boolean on(String event, Consumer<ScriptEvents.Event> listener) {
        boolean ok = ScriptEvents.on(event, listener);
        if (!ok) {
            LOG.warning("BarelyPowered: no script event named " + event + " (events: " + ScriptEvents.NAMES + ")");
        }
        return ok;
    }

    public List<String> events() {
        return ScriptEvents.NAMES;
    }

    /** The generator kinds, such as combustion_generator and wind_turbine. */
    public List<String> kinds() {
        List<String> out = new ArrayList<>();
        for (Kind k : Kind.values()) {
            out.add(k.id());
        }
        return out;
    }

    /** The generator at a position, or null. {@code dimension} is an id such as minecraft:overworld. */
    public Generator generatorAt(String dimension, int x, int y, int z) {
        World w = world;
        return w == null ? null : safe(() -> w.generatorAt(dimension, x, y, z));
    }

    /** The battery at a position, or null. */
    public Battery batteryAt(String dimension, int x, int y, int z) {
        World w = world;
        return w == null ? null : safe(() -> w.batteryAt(dimension, x, y, z));
    }

    /** Every setting key of config/barelypowered.properties. */
    public List<String> settingKeys() {
        return Settings.keys();
    }

    /** A setting's current value as text, or null for an unknown key. */
    public String setting(String key) {
        return Settings.has(key) ? Settings.get(key) : null;
    }

    /** A setting's current value as a number (0 for an unknown key or a non-number). */
    public double settingNumber(String key) {
        if (!Settings.has(key)) {
            return 0;
        }
        try {
            return Double.parseDouble(Settings.get(key).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Changes a setting for this session only (the file is not written; the change applies to what happens next). Use it from a
     * startup script. Returns false for an unknown key.
     */
    public boolean setSetting(String key, Object value) {
        if (key == null || !Settings.has(key) || value == null) {
            LOG.warning("BarelyPowered: no setting named " + key);
            return false;
        }
        Object v = value;
        if (value instanceof Double d && d == Math.rint(d) && Math.abs(d) < 1e15) {
            v = d.longValue();
        }
        Settings.set(key, String.valueOf(v));
        return true;
    }

    private interface Read<T> {
        T get();
    }

    private static <T> T safe(Read<T> read) {
        try {
            return read.get();
        } catch (RuntimeException e) {
            LOG.warning("BarelyPowered: a script read failed: " + e);
            return null;
        }
    }
}
