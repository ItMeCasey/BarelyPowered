package com.itmecasey.barelycables.neoforge.dev;

import com.itmecasey.barelycables.dev.DevContent;
import com.itmecasey.barelycables.dev.SelfTest;
import com.itmecasey.barelycables.dev.TestBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Development hooks for NeoForge: the test blocks' energy capability and the server self test. */
public final class NeoForgeDev {
    private NeoForgeDev() {
    }

    public static void install(IEventBus modBus) {
        modBus.addListener(NeoForgeDev::capabilities);
        if (SelfTest.requested()) {
            NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> SelfTest.run(e.getServer()));
        }
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DevContent.TEST_ENTITY, (be, side) -> new View(be));
    }

    private record View(TestBlockEntity be) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return be.take(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return be.give(toExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return be.isSource() ? Integer.MAX_VALUE : 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return be.isSource();
        }

        @Override
        public boolean canReceive() {
            return !be.isSource();
        }
    }
}
