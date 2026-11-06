package com.itmecasey.barelypowered;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AutomationTest {
    @Test
    void fuelFillsTheSlotUpToTheStackLimit() {
        assertEquals(64, Automation.fuelRoom(0, 64, 64, true));
        assertEquals(10, Automation.fuelRoom(54, 64, 64, true));
        assertEquals(3, Automation.fuelRoom(0, 64, 3, true));
    }

    @Test
    void aFullSlotOrAnotherItemTakesNothing() {
        assertEquals(0, Automation.fuelRoom(64, 64, 16, true));
        assertEquals(0, Automation.fuelRoom(5, 64, 16, false));
        assertEquals(0, Automation.fuelRoom(0, 64, 0, true));
    }

    @Test
    void lavaFillsTheTankToItsCapacityInAnyAmount() {
        assertEquals(250, Automation.lavaRoom(0, 4000, 250));
        assertEquals(500, Automation.lavaRoom(3500, 4000, 1000));
        assertEquals(0, Automation.lavaRoom(4000, 4000, 1000));
        assertEquals(0, Automation.lavaRoom(0, 4000, -5));
    }
}
