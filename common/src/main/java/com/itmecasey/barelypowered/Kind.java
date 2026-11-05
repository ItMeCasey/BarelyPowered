package com.itmecasey.barelypowered;

/** The seven kinds of generator. Tiers are separate blocks of the same kind. */
public enum Kind {
    HAND_CRANK("hand_crank"),
    COMBUSTION("combustion_generator"),
    LAVA("lava_generator"),
    SOLAR("solar_panel"),
    WIND("wind_turbine"),
    WATER_WHEEL("water_wheel"),
    GEOTHERMAL("geothermal_generator");

    private final String id;

    Kind(String id) {
        this.id = id;
    }

    /** The base registry name; tiered kinds add a tier suffix (see {@link Tiers}). */
    public String id() {
        return id;
    }

    /** True for kinds that come in more than one tier. */
    public boolean tiered() {
        return this == COMBUSTION || this == LAVA || this == SOLAR || this == WIND;
    }

    /** True for kinds that burn something the player hands in. */
    public boolean fueled() {
        return this == COMBUSTION || this == LAVA;
    }

    /** How many tiers this kind has. */
    public int tierCount() {
        return switch (this) {
            case COMBUSTION, LAVA -> 3;
            case SOLAR, WIND -> 2;
            default -> 1;
        };
    }
}
