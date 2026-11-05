package com.itmecasey.barelypowered;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PowerMathTest {
    @BeforeEach
    void defaults() {
        Settings.reset();
    }

    @Test
    void daylightPeaksAtNoonAndIsZeroAtNight() {
        assertEquals(0.0, PowerMath.daylight(0), 1e-9);
        assertEquals(1.0, PowerMath.daylight(6000), 1e-9);
        assertEquals(0.0, PowerMath.daylight(12000), 1e-9);
        assertEquals(0.0, PowerMath.daylight(18000), 1e-9);
        assertEquals(1.0, PowerMath.daylight(6000 + 24000L * 5), 1e-9);
        assertTrue(PowerMath.daylight(3000) > 0.5 && PowerMath.daylight(3000) < 1.0);
    }

    @Test
    void solarNeedsSkyAndDimsInWeather() {
        assertEquals(0.0, PowerMath.solarOutput(1, 6000, false, false, false));
        assertEquals(6.0, PowerMath.solarOutput(1, 6000, true, false, false), 1e-9);
        assertEquals(1.5, PowerMath.solarOutput(1, 6000, true, true, false), 1e-9);
        assertEquals(0.6, PowerMath.solarOutput(1, 6000, true, true, true), 1e-9);
        assertEquals(24.0, PowerMath.solarOutput(2, 6000, true, false, false), 1e-9);
        assertEquals(14.4, PowerMath.solarOutput(2, 6000, true, true, false), 1e-9);
    }

    @Test
    void solarTierTwoKeepsATrickleAtNightAndInRainTierOneDoesNot() {
        assertEquals(0.0, PowerMath.solarOutput(1, 18000, true, false, false));
        assertEquals(1.5, PowerMath.solarOutput(2, 18000, true, false, false), 1e-9);
        assertTrue(PowerMath.solarOutput(2, 18000, true, true, true) > 0.0);
        assertTrue(PowerMath.solarOutput(2, 6000, true, true, true) > PowerMath.solarOutput(1, 6000, true, true, true));
    }

    @Test
    void windRisesWithHeight() {
        assertEquals(0.15, PowerMath.windHeightFactor(0), 1e-9);
        assertEquals(0.15, PowerMath.windHeightFactor(64), 1e-9);
        assertEquals(1.0, PowerMath.windHeightFactor(192), 1e-9);
        assertEquals(1.0, PowerMath.windHeightFactor(300), 1e-9);
        assertTrue(PowerMath.windHeightFactor(128) > PowerMath.windHeightFactor(100));
        assertEquals(0.575, PowerMath.windHeightFactor(128), 1e-9);
    }

    @Test
    void windIsHurtByBlocksAndNeighborsAndHelpedByStorms() {
        double open = PowerMath.windOutput(1, 192, true, 0.0, false, false, 0);
        assertEquals(12.0, open, 1e-9);
        assertEquals(0.0, PowerMath.windOutput(1, 192, false, 0.0, false, false, 0));
        assertTrue(PowerMath.windOutput(1, 192, true, 0.5, false, false, 0) < open);
        assertTrue(PowerMath.windOutput(1, 192, true, 0.0, false, false, 2) < open);
        assertEquals(open * 1.2, PowerMath.windOutput(1, 192, true, 0.0, true, false, 0), 1e-9);
        assertEquals(open * 1.5, PowerMath.windOutput(1, 192, true, 0.0, true, true, 0), 1e-9);
        assertEquals(0.3, PowerMath.windWakeFactor(50), 1e-9);
        assertEquals(40.0, PowerMath.windOutput(2, 192, true, 0.0, false, false, 0), 1e-9);
    }

    @Test
    void waterFollowsFlow() {
        assertEquals(0.0, PowerMath.waterFlow(8, true, false), 1e-9);
        assertEquals(1.0, PowerMath.waterFlow(8, false, true), 1e-9);
        assertEquals(0.5, PowerMath.waterFlow(4, false, false), 1e-9);
        assertEquals(28.0, PowerMath.waterWheelOutput(1.0), 1e-9);
        assertEquals(56.0, PowerMath.waterWheelOutput(5.0), 1e-9);
        assertEquals(0.0, PowerMath.waterWheelOutput(0.0), 1e-9);
    }

    @Test
    void geothermalIsCapped() {
        assertEquals(16.0, PowerMath.geothermalOutput(1.0), 1e-9);
        assertEquals(48.0, PowerMath.geothermalOutput(10.0), 1e-9);
        assertEquals(0.0, PowerMath.geothermalOutput(-1.0), 1e-9);
    }

    @Test
    void fuelBurnScalesWithTier() {
        assertEquals(30, PowerMath.combustionOutput(Fuel.COAL, 1));
        assertEquals(60, PowerMath.combustionOutput(Fuel.COAL, 2));
        assertEquals(120, PowerMath.combustionOutput(Fuel.COAL, 3));
        assertEquals(1600, PowerMath.combustionBurnTicks(Fuel.COAL, 1));
        assertEquals(1200, PowerMath.combustionBurnTicks(Fuel.COAL, 2));
        assertEquals(960, PowerMath.combustionBurnTicks(Fuel.COAL, 3));
        assertEquals(48000, PowerMath.combustionEnergyPerItem(Fuel.COAL, 1));
        assertTrue(PowerMath.combustionEnergyPerItem(Fuel.COAL, 3) > PowerMath.combustionEnergyPerItem(Fuel.COAL, 1));
        assertTrue(PowerMath.combustionEnergyPerItem(Fuel.WOOD, 1) < PowerMath.combustionEnergyPerItem(Fuel.CHARCOAL, 1));
        assertEquals(80, PowerMath.lavaOutput(1));
        assertEquals(320, PowerMath.lavaOutput(3));
        assertEquals(1200, PowerMath.lavaBurnTicks(1));
        assertEquals(720, PowerMath.lavaBurnTicks(3));
    }

    @Test
    void settingsOverridesAndBadValuesFallBack() throws IOException {
        Settings.load(new StringReader("solar.tier1.peak=10\nwind.height.min_y=oops\nunknown.key=5\n"));
        assertEquals(10.0, Settings.getDouble("solar.tier1.peak"), 1e-9);
        assertEquals(64, Settings.getInt("wind.height.min_y"));
        Settings.load(new StringReader("combustion.tier.output_multiplier=1,3\n"));
        assertEquals(3.0, Settings.tierValue("combustion.tier.output_multiplier", 2), 1e-9);
        assertEquals(4.0, Settings.tierValue("combustion.tier.output_multiplier", 3), 1e-9);
    }

    @Test
    void accumulatorKeepsFractions() {
        PowerMath.Accumulator acc = new PowerMath.Accumulator();
        int total = 0;
        for (int i = 0; i < 4; i++) {
            total += acc.add(1.5);
        }
        assertEquals(6, total);
        assertEquals(0, new PowerMath.Accumulator().add(-3));
    }

    @Test
    void bufferNeverOverfillsOrGoesNegative() {
        FeBuffer b = new FeBuffer(100);
        assertEquals(100, b.fill(250));
        assertEquals(0, b.fill(5));
        assertEquals(30, b.drain(30, true));
        assertEquals(100, b.stored());
        assertEquals(100, b.drain(500, false));
        assertEquals(0, b.drain(1, false));
    }

    @Test
    void sizesFollowTheTopOutput() {
        assertEquals(40, PowerMath.maxOutput(Kind.HAND_CRANK, 1));
        assertEquals(120, PowerMath.maxOutput(Kind.COMBUSTION, 3));
        assertEquals(12000, PowerMath.bufferSize(Kind.COMBUSTION, 3));
        assertEquals(240, PowerMath.transferRate(Kind.COMBUSTION, 3));
        assertEquals("combustion_generator_steel", Tiers.blockId(Kind.COMBUSTION, 3));
        assertEquals("solar_panel_2", Tiers.blockId(Kind.SOLAR, 2));
        assertEquals("water_wheel", Tiers.blockId(Kind.WATER_WHEEL, 1));
    }
}
