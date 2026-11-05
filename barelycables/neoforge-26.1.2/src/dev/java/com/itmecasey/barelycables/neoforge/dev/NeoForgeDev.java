package com.itmecasey.barelycables.neoforge.dev;

import com.itmecasey.barelycables.dev.DevContent;
import com.itmecasey.barelycables.dev.SelfTest;
import com.itmecasey.barelycables.dev.TestBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
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
        event.registerBlockEntity(Capabilities.Energy.BLOCK, DevContent.TEST_ENTITY, (be, side) -> new View(be));
    }

    /** The test blocks as other mods see them: a source gives any amount, a sink takes any amount. */
    private static final class View extends SnapshotJournal<long[]> implements EnergyHandler {
        private final TestBlockEntity be;

        View(TestBlockEntity be) {
            this.be = be;
        }

        @Override
        protected long[] createSnapshot() {
            return be.snapshot();
        }

        @Override
        protected void revertToSnapshot(long[] snapshot) {
            be.restore(snapshot);
        }

        @Override
        public long getAmountAsLong() {
            return be.isSource() ? Integer.MAX_VALUE : 0;
        }

        @Override
        public long getCapacityAsLong() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            int taken = be.take(amount, true);
            if (taken > 0) {
                updateSnapshots(transaction);
                be.take(taken, false);
            }
            return taken;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            int given = be.give(amount, true);
            if (given > 0) {
                updateSnapshots(transaction);
                be.give(given, false);
            }
            return given;
        }
    }
}
