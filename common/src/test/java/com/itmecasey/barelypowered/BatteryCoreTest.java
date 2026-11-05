package com.itmecasey.barelypowered;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BatteryCoreTest {
    @BeforeEach
    void defaults() {
        Settings.reset();
    }

    @Test
    void tiersScaleCapacityAndRate() {
        assertEquals(100000, BatteryCore.capacity(1));
        assertEquals(400000, BatteryCore.capacity(2));
        assertEquals(1600000, BatteryCore.capacity(3));
        assertEquals(200, BatteryCore.rate(1));
        assertEquals(800, BatteryCore.rate(2));
        assertEquals(3200, BatteryCore.rate(3));
        assertEquals("battery_basic", BatteryCore.blockId(1));
        assertEquals("battery_steel", BatteryCore.blockId(3));
    }

    @Test
    void rateLimitsHoldPerTickAndResetEachTick() {
        BatteryCore b = new BatteryCore(1);
        assertEquals(200, b.receive(2, 5000, false));
        assertEquals(0, b.receive(2, 5000, false));
        b.startTick();
        assertEquals(150, b.receive(3, 150, false));
        assertEquals(50, b.receive(3, 150, false), "only what is left of this tick's budget");
        assertEquals(400, b.stored());
        b.startTick();
        assertEquals(200, b.extract(2, 5000, false));
        assertEquals(0, b.extract(2, 5000, false));
        assertEquals(200, b.stored());
    }

    @Test
    void simulationChangesNothing() {
        BatteryCore b = new BatteryCore(1);
        assertEquals(200, b.receive(0, 1000, true));
        assertEquals(0, b.stored());
        assertEquals(200, b.receive(0, 1000, true));
        b.receive(0, 100, false);
        assertEquals(100, b.extract(0, 1000, true));
        assertEquals(100, b.stored());
    }

    @Test
    void neverOverfillsOrGoesNegative() {
        Settings.reset();
        BatteryCore b = new BatteryCore(1);
        for (int i = 0; i < 1000; i++) {
            b.startTick();
            b.receive(-1, 1000, false);
        }
        assertEquals(100000, b.stored());
        assertEquals(0, b.receive(-1, 1000, false));
        for (int i = 0; i < 1000; i++) {
            b.startTick();
            b.extract(-1, 1000, false);
        }
        assertEquals(0, b.stored());
        assertEquals(0, b.extract(-1, 1000, false));
    }

    @Test
    void faceModesGateInputAndOutput() {
        BatteryCore b = new BatteryCore(1);
        b.setStored(1000);
        b.setMode(1, FaceMode.INPUT);
        b.setMode(2, FaceMode.OUTPUT);
        b.setMode(3, FaceMode.OFF);
        assertEquals(0, b.extract(1, 50, false), "an input face gives nothing");
        assertEquals(50, b.receive(1, 50, false));
        assertEquals(0, b.receive(2, 50, false), "an output face takes nothing");
        assertEquals(50, b.extract(2, 50, false));
        assertEquals(0, b.receive(3, 50, false));
        assertEquals(0, b.extract(3, 50, false));
        assertEquals(50, b.extract(0, 50, false), "a default face does both");
        assertEquals(50, b.receive(-1, 50, false), "no particular side is not restricted");
    }

    @Test
    void faceModeCycleAndSaving() {
        BatteryCore b = new BatteryCore(2);
        assertEquals(FaceMode.INPUT, b.cycle(4));
        assertEquals(FaceMode.OUTPUT, b.cycle(4));
        assertEquals(FaceMode.OFF, b.cycle(4));
        assertEquals(FaceMode.BOTH, b.cycle(4));
        b.setMode(0, FaceMode.OFF);
        b.setMode(5, FaceMode.OUTPUT);
        b.setMode(2, FaceMode.INPUT);
        BatteryCore c = new BatteryCore(2);
        c.unpackModes(b.packModes());
        for (int f = 0; f < BatteryCore.FACES; f++) {
            assertEquals(b.mode(f), c.mode(f));
        }
    }

    @Test
    void comparatorAndFillLevels() {
        assertEquals(0, BatteryCore.comparator(0, 1000));
        assertEquals(1, BatteryCore.comparator(1, 1000));
        assertEquals(8, BatteryCore.comparator(500, 1000));
        assertEquals(15, BatteryCore.comparator(1000, 1000));
        assertEquals(14, BatteryCore.comparator(999, 1000));
        assertEquals(0, BatteryCore.fillLevel(0, 1000, 8));
        assertEquals(1, BatteryCore.fillLevel(1, 1000, 8));
        assertEquals(4, BatteryCore.fillLevel(500, 1000, 8));
        assertEquals(8, BatteryCore.fillLevel(1000, 1000, 8));
        assertEquals(8, BatteryCore.fillLevel(990, 1000, 8));
    }

    @Test
    void energyIsConservedWhenBatteriesShuffleItAround() {
        // Three batteries pass energy between each other at random for a long time; the total never changes.
        Random rnd = new Random(7);
        BatteryCore[] bs = {new BatteryCore(1), new BatteryCore(2), new BatteryCore(3)};
        bs[0].setStored(90000);
        bs[1].setStored(300000);
        bs[2].setStored(5000);
        long total = 90000 + 300000 + 5000;
        for (int tick = 0; tick < 5000; tick++) {
            for (BatteryCore b : bs) {
                b.startTick();
            }
            for (int k = 0; k < 6; k++) {
                BatteryCore from = bs[rnd.nextInt(3)];
                BatteryCore to = bs[rnd.nextInt(3)];
                int face = rnd.nextInt(6);
                int offer = from.extract(face, rnd.nextInt(5000), true);
                int taken = to.receive(face, offer, false);
                from.extract(face, taken, false);
            }
            long sum = 0;
            for (BatteryCore b : bs) {
                assertTrue(b.stored() >= 0 && b.stored() <= b.capacity());
                sum += b.stored();
            }
            assertEquals(total, sum, "energy appeared or vanished at tick " + tick);
        }
    }
}
