package com.itmecasey.barelycables;

import java.util.ArrayList;
import java.util.List;

/**
 * How one network of cables moves energy in one tick, with no Minecraft types so it can be unit tested.
 *
 * <p>Every neighbor of the network (a generator, a battery, a machine, another mod's pipe) and the network's own buffer is a
 * {@link Port}. A port that can only give is a source, one that can only take is a sink, one that can do both is storage.
 * Energy goes first from sources to sinks, then from sources to storage (charging), then from storage to sinks (discharging),
 * never from storage to storage, so batteries do not pass energy back and forth.
 *
 * <p>Energy is never created or lost: it is taken from a source before it is given to a sink, and anything a sink refuses after
 * the source already gave it goes back to the source, or failing that is returned as "spill" for the caller to keep in the
 * network buffer and deliver on a later tick.
 */
public final class CableMath {
    private CableMath() {
    }

    /** One end of a transfer. With {@code simulate} nothing may change; the return value is how much would move. */
    public interface Port {
        int extract(int max, boolean simulate);

        int insert(int max, boolean simulate);
    }

    /** The outcome of one tick. */
    public record Result(int moved, int spill) {
    }

    private enum Kind { SOURCE, SINK, STORAGE, NONE }

    private static final class Slot {
        final Port port;
        final Kind kind;
        int canGive;
        int canTake;

        Slot(Port port, int budget) {
            this.port = port;
            this.canGive = Math.max(0, port.extract(budget, true));
            this.canTake = Math.max(0, port.insert(budget, true));
            this.kind = canGive > 0 && canTake > 0 ? Kind.STORAGE : canGive > 0 ? Kind.SOURCE : canTake > 0 ? Kind.SINK : Kind.NONE;
        }
    }

    /**
     * Runs one tick.
     *
     * @param ports every neighbor of the network plus its buffer
     * @param budget the most energy the network may move this tick (its tier rate)
     * @param rotation changes every tick so no neighbor is always served first
     */
    public static Result move(List<? extends Port> ports, int budget, int rotation) {
        if (ports.size() < 2 || budget <= 0) {
            return new Result(0, 0);
        }
        List<Slot> slots = new ArrayList<>(ports.size());
        int n = ports.size();
        for (int i = 0; i < n; i++) {
            slots.add(new Slot(ports.get(Math.floorMod(i + rotation, n)), budget));
        }
        int[] left = {budget};
        int[] spill = {0};
        int moved = 0;
        moved += phase(slots, Kind.SOURCE, Kind.SINK, left, spill);
        moved += phase(slots, Kind.SOURCE, Kind.STORAGE, left, spill);
        moved += phase(slots, Kind.STORAGE, Kind.SINK, left, spill);
        return new Result(moved, spill[0]);
    }

    private static int phase(List<Slot> slots, Kind from, Kind to, int[] left, int[] spill) {
        List<Slot> givers = new ArrayList<>();
        List<Slot> takers = new ArrayList<>();
        for (Slot s : slots) {
            if (s.kind == from) {
                givers.add(s);
            } else if (s.kind == to) {
                takers.add(s);
            }
        }
        if (givers.isEmpty() || takers.isEmpty() || left[0] <= 0) {
            return 0;
        }
        int moved = 0;
        // Two passes: first an equal share for every taker, then whatever budget is left, so none is starved.
        for (int pass = 0; pass < 2 && left[0] > 0; pass++) {
            int share = pass == 0 ? Math.max(1, (left[0] + takers.size() - 1) / takers.size()) : left[0];
            for (Slot taker : takers) {
                int want = Math.min(Math.min(taker.canTake, share), left[0]);
                if (want <= 0) {
                    continue;
                }
                int pulled = 0;
                List<Slot> used = new ArrayList<>();
                int[] gave = new int[givers.size()];
                for (int g = 0; g < givers.size() && pulled < want; g++) {
                    Slot giver = givers.get(g);
                    int ask = Math.min(want - pulled, giver.canGive);
                    if (ask <= 0) {
                        continue;
                    }
                    int got = Math.max(0, giver.port.extract(ask, false));
                    giver.canGive = Math.max(0, giver.canGive - got);
                    gave[g] = got;
                    pulled += got;
                    used.add(giver);
                }
                if (pulled <= 0) {
                    continue;
                }
                int inserted = Math.max(0, Math.min(pulled, taker.port.insert(pulled, false)));
                taker.canTake = Math.max(0, taker.canTake - inserted);
                left[0] -= inserted;
                moved += inserted;
                int leftover = pulled - inserted;
                for (int g = givers.size() - 1; g >= 0 && leftover > 0; g--) {
                    if (gave[g] > 0) {
                        int back = Math.max(0, Math.min(leftover, givers.get(g).port.insert(Math.min(leftover, gave[g]), false)));
                        leftover -= back;
                    }
                }
                spill[0] += leftover;
            }
        }
        return moved;
    }

    /** A plain buffer, used for a network's own storage. */
    public static final class Buffer implements Port {
        private int stored;
        private int capacity;

        public Buffer(int capacity) {
            this.capacity = Math.max(1, capacity);
        }

        public int stored() {
            return stored;
        }

        public int capacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = Math.max(1, capacity);
        }

        public void set(int value) {
            stored = Math.max(0, value);
        }

        /** Adds energy that could not be delivered, even past the capacity, so none is lost. */
        public void keep(int amount) {
            stored += Math.max(0, amount);
        }

        @Override
        public int extract(int max, boolean simulate) {
            int given = Math.max(0, Math.min(max, stored));
            if (!simulate) {
                stored -= given;
            }
            return given;
        }

        @Override
        public int insert(int max, boolean simulate) {
            int taken = Math.max(0, Math.min(max, capacity - stored));
            if (!simulate) {
                stored += taken;
            }
            return taken;
        }
    }

    /** Per-face switches of one cable: bit {@code n} set means face {@code n} (Direction order) is disconnected. */
    public static final class Faces {
        private int disabled;

        public boolean isDisabled(int face) {
            return (disabled >> face & 1) != 0;
        }

        /** Flips a face and returns true if it is now disconnected. */
        public boolean toggle(int face) {
            disabled ^= 1 << face;
            return isDisabled(face);
        }

        public int bits() {
            return disabled;
        }

        public void setBits(int bits) {
            disabled = bits & 0x3F;
        }
    }
}
