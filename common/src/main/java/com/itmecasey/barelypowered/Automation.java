package com.itmecasey.barelypowered;

/**
 * The room rules for what hoppers, pipes and other mods' item and fluid transfer may put into a generator. Kept free of
 * Minecraft classes so the rules can be unit tested; the block entity applies them to its real stacks and tank.
 */
public final class Automation {
    /** Millibuckets in one bucket; the lava generator burns lava in whole buckets. */
    public static final int BUCKET = 1000;

    private Automation() {
    }

    /**
     * How many items of a stack may go into the fuel slot.
     *
     * @param stored   items already in the slot
     * @param maxStack the stack limit of the item
     * @param offered  items offered
     * @param sameItem true when the slot is empty or holds the same item as the offered stack
     */
    public static int fuelRoom(int stored, int maxStack, int offered, boolean sameItem) {
        if (!sameItem || offered <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(maxStack - stored, offered));
    }

    /** How many millibuckets of lava may go into the tank. */
    public static int lavaRoom(int storedMb, int capacityMb, int offeredMb) {
        if (offeredMb <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(capacityMb - storedMb, offeredMb));
    }
}
