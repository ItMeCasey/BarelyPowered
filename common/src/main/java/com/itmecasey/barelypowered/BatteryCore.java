package com.itmecasey.barelypowered;

/**
 * The charge, per-face modes and rate limits of one battery, with no Minecraft types so it can be unit tested. The block entity
 * wraps it; the loader layers expose it through their energy interface.
 *
 * <p>Faces are numbered 0 to 5 (down, up, north, south, west, east, the order of Minecraft's Direction). A face of -1 means
 * "no particular side" and is not restricted by the face modes. Taking in and giving out are each limited to the tier's rate per
 * tick, counted over everything that happens in one tick; {@link #startTick()} resets the counts.
 */
public final class BatteryCore {
    public static final int FACES = 6;
    public static final String[] TIER_NAMES = {"basic", "iron", "steel"};

    private final FeBuffer buffer;
    private final FaceMode[] modes = new FaceMode[FACES];
    private final int rate;
    private int inLeft;
    private int outLeft;

    public BatteryCore(int tier) {
        this.buffer = new FeBuffer(capacity(tier));
        this.rate = rate(tier);
        for (int i = 0; i < FACES; i++) {
            modes[i] = FaceMode.BOTH;
        }
        startTick();
    }

    public static int capacity(int tier) {
        return Settings.getInt("battery." + TIER_NAMES[clampTier(tier) - 1] + ".capacity");
    }

    public static int rate(int tier) {
        return Settings.getInt("battery." + TIER_NAMES[clampTier(tier) - 1] + ".rate");
    }

    private static int clampTier(int tier) {
        return Math.max(1, Math.min(TIER_NAMES.length, tier));
    }

    /** Registry name of a battery block: battery_basic, battery_iron or battery_steel. */
    public static String blockId(int tier) {
        return "battery_" + TIER_NAMES[clampTier(tier) - 1];
    }

    public int stored() {
        return buffer.stored();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public int rate() {
        return rate;
    }

    public void setStored(int value) {
        buffer.set(value);
    }

    /** Starts a new tick: both rate limits are full again. */
    public void startTick() {
        inLeft = rate;
        outLeft = rate;
    }

    public FaceMode mode(int face) {
        return face < 0 ? FaceMode.BOTH : modes[face];
    }

    public void setMode(int face, FaceMode mode) {
        modes[face] = mode;
    }

    /** Steps a face to its next mode and returns it. */
    public FaceMode cycle(int face) {
        modes[face] = modes[face].next();
        return modes[face];
    }

    /** All six modes packed two bits each, for saving. */
    public int packModes() {
        int v = 0;
        for (int i = 0; i < FACES; i++) {
            v |= modes[i].ordinal() << (i * 2);
        }
        return v;
    }

    public void unpackModes(int v) {
        FaceMode[] all = FaceMode.values();
        for (int i = 0; i < FACES; i++) {
            modes[i] = all[(v >> (i * 2)) & 3];
        }
    }

    /** Takes energy in through a face. Returns how much was taken; with {@code simulate} nothing changes. */
    public int receive(int face, int amount, boolean simulate) {
        if (amount <= 0 || !mode(face).canInput()) {
            return 0;
        }
        int taken = Math.min(Math.min(amount, inLeft), buffer.space());
        if (!simulate && taken > 0) {
            buffer.fill(taken);
            inLeft -= taken;
        }
        return taken;
    }

    /** Gives energy out through a face. Returns how much came out; with {@code simulate} nothing changes. */
    public int extract(int face, int amount, boolean simulate) {
        if (amount <= 0 || !mode(face).canOutput()) {
            return 0;
        }
        int given = Math.min(Math.min(amount, outLeft), buffer.stored());
        if (!simulate && given > 0) {
            buffer.drain(given, false);
            outLeft -= given;
        }
        return given;
    }

    /** The state a failed transfer must be able to return to: the charge and what is left of this tick's two rate limits. */
    public int[] snapshot() {
        return new int[] {buffer.stored(), inLeft, outLeft};
    }

    public void restore(int[] state) {
        buffer.set(state[0]);
        inLeft = state[1];
        outLeft = state[2];
    }

    /** Redstone comparator strength, 0 when empty and 1 to 15 in proportion to the charge (15 only when full). */
    public static int comparator(int stored, int capacity) {
        if (stored <= 0 || capacity <= 0) {
            return 0;
        }
        return Math.min(15, 1 + (int) ((long) stored * 14 / capacity));
    }

    /** How many of the {@code levels} fill marks on the model are lit: 0 when empty, 1 to {@code levels} otherwise. */
    public static int fillLevel(int stored, int capacity, int levels) {
        if (stored <= 0 || capacity <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(levels, (int) (((long) stored * levels + capacity - 1) / capacity)));
    }
}
