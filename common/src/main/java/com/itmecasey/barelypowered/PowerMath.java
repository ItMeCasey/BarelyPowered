package com.itmecasey.barelypowered;

/** The output formulas for every generator. Pure functions of plain numbers, so they are unit tested without a game. */
public final class PowerMath {
    private PowerMath() {
    }

    public static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ---- Combustion and lava ----

    /** Energy per tick while a fuel burns in a generator of the given tier. */
    public static int combustionOutput(Fuel fuel, int tier) {
        return (int) Math.round(fuel.fePerTick() * Settings.tierValue("combustion.tier.output_multiplier", tier));
    }

    /** Ticks one item of a fuel burns in a generator of the given tier. */
    public static int combustionBurnTicks(Fuel fuel, int tier) {
        return Math.max(1, (int) Math.round(fuel.burnTicks() * Settings.tierValue("combustion.tier.burn_multiplier", tier)));
    }

    public static int lavaOutput(int tier) {
        return (int) Math.round(Settings.getDouble("lava.fe_per_tick") * Settings.tierValue("lava.tier.output_multiplier", tier));
    }

    /** Ticks one 1000 mB bucket of lava lasts. */
    public static int lavaBurnTicks(int tier) {
        return Math.max(1, (int) Math.round(Settings.getInt("lava.bucket_ticks") * Settings.tierValue("lava.tier.burn_multiplier", tier)));
    }

    /** Total energy one item of a fuel gives, for the wiki and tooltips. */
    public static long combustionEnergyPerItem(Fuel fuel, int tier) {
        return (long) combustionOutput(fuel, tier) * combustionBurnTicks(fuel, tier);
    }

    // ---- Solar ----

    /**
     * How much daylight there is, 0 to 1, from the time of day in ticks. Sunrise is tick 0, noon 6000, sunset 12000; the
     * curve is the top half of a sine so the panel ramps up and down smoothly.
     */
    public static double daylight(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        if (t >= 12000) {
            return 0.0;
        }
        return Math.sin(Math.PI * t / 12000.0);
    }

    /**
     * Solar output in energy per tick.
     *
     * @param tier 1 or 2
     * @param dayTime the world's time of day in ticks
     * @param skyAccess true when nothing solid is above the panel
     * @param raining true in rain or snow
     * @param thundering true in a thunderstorm
     */
    public static double solarOutput(int tier, long dayTime, boolean skyAccess, boolean raining, boolean thundering) {
        if (!skyAccess) {
            return 0.0;
        }
        String p = "solar.tier" + tier + ".";
        double peak = Settings.getDouble(p + "peak");
        double floor = Settings.getDouble(p + "night_floor");
        double weather = 1.0;
        if (thundering) {
            weather = Settings.getDouble(p + "thunder_factor");
        } else if (raining) {
            weather = Settings.getDouble(p + "rain_factor");
        }
        // Weather dims the sun, but the night floor (a tier 2 trickle) is only trimmed a little.
        double day = peak * daylight(dayTime) * weather;
        double night = floor * (0.5 + 0.5 * weather);
        return Math.max(day, night);
    }

    // ---- Wind ----

    /** How strong the wind is at a height: the floor at the minimum height, rising in a line to 1 at the full height. */
    public static double windHeightFactor(int y) {
        double min = Settings.getInt("wind.height.min_y");
        double full = Settings.getInt("wind.height.full_y");
        double floor = Settings.getDouble("wind.height.floor");
        if (full <= min) {
            return 1.0;
        }
        return floor + (1.0 - floor) * clamp((y - min) / (full - min), 0.0, 1.0);
    }

    /** The share of output that survives other turbines in range. */
    public static double windWakeFactor(int turbinesInRange) {
        double f = 1.0 - Settings.getDouble("wind.turbine_penalty") * Math.max(0, turbinesInRange);
        return Math.max(Settings.getDouble("wind.turbine_min_factor"), f);
    }

    /**
     * Wind turbine output in energy per tick.
     *
     * @param blockedFraction 0 to 1, the share of the checked blocks around the blades that are solid
     */
    public static double windOutput(int tier, int y, boolean skyAccess, double blockedFraction, boolean raining,
                                    boolean thundering, int turbinesInRange) {
        if (!skyAccess) {
            return 0.0;
        }
        double base = Settings.getDouble("wind.tier" + tier + ".base");
        double open = 1.0 - Settings.getDouble("wind.blocked_penalty") * clamp(blockedFraction, 0.0, 1.0);
        double storm = thundering ? Settings.getDouble("wind.thunder_bonus") : raining ? Settings.getDouble("wind.rain_bonus") : 1.0;
        return base * windHeightFactor(y) * open * storm * windWakeFactor(turbinesInRange);
    }

    // ---- Water wheel ----

    /**
     * Flow credited for one neighboring water block.
     *
     * @param amount fluid level 1 to 8 (8 for a full block)
     * @param source true for a still source block
     * @param falling true for water that is falling (also true for a stream from above)
     */
    public static double waterFlow(int amount, boolean source, boolean falling) {
        if (source && !falling) {
            return Settings.getDouble("water_wheel.source_flow");
        }
        if (falling) {
            return 1.0;
        }
        return clamp(amount / 8.0, 0.0, 1.0);
    }

    public static double waterWheelOutput(double totalFlow) {
        double flow = clamp(totalFlow, 0.0, Settings.getDouble("water_wheel.max_flow"));
        return flow * Settings.getDouble("water_wheel.fe_per_flow");
    }

    // ---- Geothermal ----

    public static double geothermalOutput(double heat) {
        return clamp(heat, 0.0, Settings.getDouble("geothermal.max_heat")) * Settings.getDouble("geothermal.fe_per_heat");
    }

    // ---- Shared ----

    /** The top output of a generator, used to size its buffer and transfer rate. */
    public static int maxOutput(Kind kind, int tier) {
        double v = switch (kind) {
            case HAND_CRANK -> Settings.getInt("hand_crank.max_output");
            case COMBUSTION -> combustionOutput(Fuel.COAL, tier);
            case LAVA -> lavaOutput(tier);
            case SOLAR -> Settings.getDouble("solar.tier" + tier + ".peak");
            case WIND -> Settings.getDouble("wind.tier" + tier + ".base") * Settings.getDouble("wind.thunder_bonus");
            case WATER_WHEEL -> waterWheelOutput(Settings.getDouble("water_wheel.max_flow"));
            case GEOTHERMAL -> geothermalOutput(Settings.getDouble("geothermal.max_heat"));
        };
        return Math.max(1, (int) Math.ceil(v));
    }

    public static int bufferSize(Kind kind, int tier) {
        if (kind == Kind.HAND_CRANK) {
            return Settings.getInt("hand_crank.buffer");
        }
        return Math.max(1000, maxOutput(kind, tier) * Settings.getInt("general.buffer_ticks"));
    }

    public static int transferRate(Kind kind, int tier) {
        return Math.max(1, (int) Math.ceil(maxOutput(kind, tier) * Settings.getDouble("general.transfer_multiplier")));
    }

    /**
     * Turns a fractional output into whole energy each tick without losing the remainder, so a 1.5 FE/t source really
     * yields 3 FE every two ticks.
     */
    public static final class Accumulator {
        private double carry;

        public int add(double amount) {
            carry += Math.max(0.0, amount);
            int whole = (int) carry;
            carry -= whole;
            return whole;
        }

        public double carry() {
            return carry;
        }

        public void set(double carry) {
            this.carry = carry;
        }
    }
}
