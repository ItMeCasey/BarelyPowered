package com.itmecasey.barelypowered;

/** The solid fuels a combustion generator accepts. */
public enum Fuel {
    COAL("coal"),
    CHARCOAL("charcoal"),
    WOOD("wood"),
    /** Planks burn for a quarter of a log, so crafting a log into planks cannot make more energy than burning it. */
    PLANKS("planks");

    private final String key;

    Fuel(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    /** Energy produced per tick while one item of this fuel burns in a stone-tier generator. */
    public int fePerTick() {
        return Settings.getInt("combustion." + key + ".fe_per_tick");
    }

    /** Ticks one item burns in a stone-tier generator. */
    public int burnTicks() {
        return Settings.getInt("combustion." + key + ".burn_ticks");
    }
}
