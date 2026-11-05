package com.itmecasey.barelypowered;

/** A plain energy buffer with no loader types; each loader wraps it in its own energy interface. */
public final class FeBuffer {
    private int stored;
    private int capacity;

    public FeBuffer(int capacity) {
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
        stored = Math.min(stored, this.capacity);
    }

    public void set(int value) {
        stored = Math.max(0, Math.min(capacity, value));
    }

    public int space() {
        return capacity - stored;
    }

    /** Adds as much as fits; returns how much was added. */
    public int fill(int amount) {
        int taken = Math.max(0, Math.min(amount, space()));
        stored += taken;
        return taken;
    }

    /** Takes up to {@code amount}; with {@code simulate} nothing changes. Returns how much came out. */
    public int drain(int amount, boolean simulate) {
        int given = Math.max(0, Math.min(amount, stored));
        if (!simulate) {
            stored -= given;
        }
        return given;
    }
}
