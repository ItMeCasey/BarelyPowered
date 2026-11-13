package com.itmecasey.barelypowered.script;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

/** Events that scripts can listen to: {@code generatorStarted}, {@code generatorStopped} and {@code fuelBurned}. */
public final class ScriptEvents {
    public static final List<String> NAMES = List.of("generatorStarted", "generatorStopped", "fuelBurned");

    /** What a listener receives. {@code fuel} is the fuel category that started burning (fuelBurned only), otherwise empty. */
    public static final class Event {
        public final String name;
        public final String kind;
        public final int tier;
        public final String id;
        public final String dimension;
        public final int x;
        public final int y;
        public final int z;
        public final double output;
        public final String note;
        public final String fuel;

        public Event(String name, String kind, int tier, String id, String dimension, int x, int y, int z, double output, String note,
                String fuel) {
            this.name = name;
            this.kind = kind;
            this.tier = tier;
            this.id = id;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.output = output;
            this.note = note;
            this.fuel = fuel;
        }
    }

    private static final Logger LOG = Logger.getLogger("barelypowered");
    private static final Map<String, List<Consumer<Event>>> LISTENERS = new ConcurrentHashMap<>();

    private ScriptEvents() {
    }

    /** Adds a listener; returns false for an event name that does not exist. */
    public static boolean on(String name, Consumer<Event> listener) {
        if (!NAMES.contains(name) || listener == null) {
            return false;
        }
        LISTENERS.computeIfAbsent(name, k -> new CopyOnWriteArrayList<>()).add(listener);
        return true;
    }

    public static boolean hasListeners(String name) {
        List<Consumer<Event>> list = LISTENERS.get(name);
        return list != null && !list.isEmpty();
    }

    /** Calls every listener; a script error logs one plain line and never reaches the game. */
    public static void fire(Event event) {
        List<Consumer<Event>> list = LISTENERS.get(event.name);
        if (list == null) {
            return;
        }
        for (Consumer<Event> listener : list) {
            try {
                listener.accept(event);
            } catch (RuntimeException | LinkageError e) {
                LOG.warning("BarelyPowered: a script listener for " + event.name + " failed: " + e);
            }
        }
    }

    /** Removes every listener (tests). */
    public static void clear() {
        LISTENERS.clear();
    }
}
