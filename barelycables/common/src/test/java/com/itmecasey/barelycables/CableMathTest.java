package com.itmecasey.barelycables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CableMathTest {
    /** A test neighbor. {@code acceptsLess} makes it take less than its simulation promised, like a badly behaved mod. */
    private static final class Box implements CableMath.Port {
        int stored;
        final int capacity;
        final boolean canGive;
        final boolean canTake;
        final boolean acceptsLess;

        Box(int stored, int capacity, boolean canGive, boolean canTake, boolean acceptsLess) {
            this.stored = stored;
            this.capacity = capacity;
            this.canGive = canGive;
            this.canTake = canTake;
            this.acceptsLess = acceptsLess;
        }

        @Override
        public int extract(int max, boolean simulate) {
            if (!canGive) {
                return 0;
            }
            int n = Math.min(max, stored);
            if (!simulate) {
                stored -= n;
            }
            return n;
        }

        @Override
        public int insert(int max, boolean simulate) {
            if (!canTake) {
                return 0;
            }
            int n = Math.min(max, capacity - stored);
            if (!simulate && acceptsLess) {
                n = n / 2;
            }
            if (!simulate) {
                stored += n;
            }
            return n;
        }
    }

    @BeforeEach
    void defaults() {
        CableSettings.reset();
    }

    private static long total(List<Box> boxes, CableMath.Buffer buffer, int spill) {
        long t = buffer.stored() + spill;
        for (Box b : boxes) {
            t += b.stored;
        }
        return t;
    }

    @Test
    void tiersHaveGrowingThroughputFromConfig() {
        assertEquals(400, CableSettings.rate(1));
        assertEquals(1600, CableSettings.rate(2));
        assertEquals(6400, CableSettings.rate(3));
        assertEquals("energy_cable_iron", CableSettings.blockId(2));
    }

    @Test
    void sourceFeedsSinkUpToTheBudget() {
        Box gen = new Box(100000, 100000, true, false, false);
        Box machine = new Box(0, 100000, false, true, false);
        CableMath.Result r = CableMath.move(List.of(gen, machine), 400, 0);
        assertEquals(400, r.moved());
        assertEquals(400, machine.stored);
        assertEquals(99600, gen.stored);
        assertEquals(0, r.spill());
    }

    @Test
    void throughputNeverExceedsTheTierRateOverManyTicks() {
        Box gen = new Box(1_000_000, 1_000_000, true, false, false);
        Box machine = new Box(0, 1_000_000, false, true, false);
        int rate = CableSettings.rate(1);
        for (int tick = 0; tick < 100; tick++) {
            int before = machine.stored;
            CableMath.move(List.of(gen, machine), rate, tick);
            assertTrue(machine.stored - before <= rate);
        }
        assertEquals(rate * 100, machine.stored);
    }

    @Test
    void sinksShareTheBudgetFairly() {
        Box gen = new Box(100000, 100000, true, false, false);
        Box a = new Box(0, 100000, false, true, false);
        Box b = new Box(0, 100000, false, true, false);
        Box c = new Box(0, 100000, false, true, false);
        CableMath.move(List.of(gen, a, b, c), 300, 0);
        assertEquals(100, a.stored);
        assertEquals(100, b.stored);
        assertEquals(100, c.stored);
    }

    @Test
    void smallSinkDoesNotStarveBigOnes() {
        Box gen = new Box(100000, 100000, true, false, false);
        Box small = new Box(0, 50, false, true, false);
        Box big = new Box(0, 100000, false, true, false);
        CableMath.move(List.of(gen, small, big), 400, 0);
        assertEquals(50, small.stored);
        assertEquals(350, big.stored);
    }

    @Test
    void batteriesChargeAfterSinksAndNeverPassEnergyToEachOther() {
        Box gen = new Box(100000, 100000, true, false, false);
        Box machine = new Box(0, 100, false, true, false);
        Box batA = new Box(500, 10000, true, true, false);
        Box batB = new Box(9000, 10000, true, true, false);
        CableMath.move(List.of(gen, machine, batA, batB), 1000, 0);
        assertEquals(100, machine.stored, "the machine is served first");
        assertEquals(1000, 100000 - gen.stored, "the generator gave the whole budget");
        long gains = (batA.stored - 500) + (batB.stored - 9000);
        assertEquals(900, gains, "the rest of the budget only charged the batteries, no battery to battery moves");
    }

    @Test
    void storageDischargesIntoSinksWhenNoSourceIsLeft() {
        Box machine = new Box(0, 100000, false, true, false);
        Box battery = new Box(5000, 10000, true, true, false);
        CableMath.move(List.of(machine, battery), 400, 0);
        assertEquals(400, machine.stored);
        assertEquals(4600, battery.stored);
    }

    @Test
    void emptyOrFullNetworkMovesNothing() {
        Box gen = new Box(0, 1000, true, false, false);
        Box machine = new Box(1000, 1000, false, true, false);
        assertEquals(0, CableMath.move(List.of(gen, machine), 400, 0).moved());
        assertEquals(0, CableMath.move(List.of(machine), 400, 0).moved());
    }

    @Test
    void energyIsConservedEvenWhenANeighborLies() {
        Random rnd = new Random(11);
        for (int round = 0; round < 200; round++) {
            List<Box> boxes = new ArrayList<>();
            int count = 2 + rnd.nextInt(6);
            for (int i = 0; i < count; i++) {
                boolean give = rnd.nextBoolean();
                boolean take = rnd.nextBoolean();
                boxes.add(new Box(rnd.nextInt(5000), 5000, give, take, rnd.nextInt(4) == 0));
            }
            CableMath.Buffer buffer = new CableMath.Buffer(800);
            buffer.set(rnd.nextInt(800));
            List<CableMath.Port> ports = new ArrayList<>(boxes);
            ports.add(buffer);
            long before = total(boxes, buffer, 0);
            int budget = 1 + rnd.nextInt(2000);
            CableMath.Result r = CableMath.move(ports, budget, round);
            buffer.keep(r.spill());
            assertEquals(before, total(boxes, buffer, 0), "energy appeared or vanished in round " + round);
            assertTrue(r.moved() <= budget);
        }
    }

    @Test
    void spillStaysInTheBufferAndIsDeliveredLater() {
        Box gen = new Box(10000, 10000, true, false, false);
        Box liar = new Box(0, 10000, false, true, true);
        CableMath.Buffer buffer = new CableMath.Buffer(10);
        List<CableMath.Port> ports = List.of(gen, liar, buffer);
        long start = gen.stored;
        for (int tick = 0; tick < 50; tick++) {
            CableMath.Result r = CableMath.move(ports, 400, tick);
            buffer.keep(r.spill());
            assertEquals(start, gen.stored + liar.stored + buffer.stored());
        }
    }

    @Test
    void faceSwitchesToggleAndSave() {
        CableMath.Faces f = new CableMath.Faces();
        assertTrue(!f.isDisabled(2));
        assertTrue(f.toggle(2));
        assertTrue(f.isDisabled(2));
        assertTrue(!f.toggle(2));
        f.toggle(0);
        f.toggle(5);
        CableMath.Faces g = new CableMath.Faces();
        g.setBits(f.bits());
        assertTrue(g.isDisabled(0) && g.isDisabled(5) && !g.isDisabled(1));
    }
}
