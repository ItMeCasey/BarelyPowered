package com.itmecasey.barelypowered;

/** Names and registry ids for generator tiers. Tiers count from 1. */
public final class Tiers {
    private static final String[] FUELED = {"stone", "iron", "steel"};

    private Tiers() {
    }

    /** The registry name of a generator block, e.g. {@code combustion_generator_iron} or {@code solar_panel_2}. */
    public static String blockId(Kind kind, int tier) {
        if (!kind.tiered()) {
            return kind.id();
        }
        return kind.id() + "_" + (kind.fueled() ? FUELED[tier - 1] : String.valueOf(tier));
    }

    /** A short readable tier name for tooltips, e.g. "Iron" or "Tier 2". */
    public static String label(Kind kind, int tier) {
        if (!kind.tiered()) {
            return "";
        }
        if (kind.fueled()) {
            String s = FUELED[tier - 1];
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
        return "Tier " + tier;
    }
}
