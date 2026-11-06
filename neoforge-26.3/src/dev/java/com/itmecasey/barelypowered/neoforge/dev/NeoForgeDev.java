package com.itmecasey.barelypowered.neoforge.dev;

import com.itmecasey.barelypowered.dev.DevContent;
import com.itmecasey.barelypowered.dev.SelfTest;
import com.itmecasey.barelypowered.dev.ShotTour;
import com.itmecasey.barelypowered.dev.SinkBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Development hooks for NeoForge: the sink's energy capability, the server self test and the client screenshot tour. */
public final class NeoForgeDev {
    private NeoForgeDev() {
    }

    public static void install(IEventBus modBus) {
        modBus.addListener(NeoForgeDev::capabilities);
        if (SelfTest.requested()) {
            SelfTest.probe = new SelfTest.Probe() {
                @Override
                public int items(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, net.minecraft.world.item.ItemStack stack) {
                    net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> h =
                            level.getCapability(Capabilities.Item.BLOCK, pos, null);
                    if (h == null) {
                        return -1;
                    }
                    try (net.neoforged.neoforge.transfer.transaction.Transaction t = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                        int moved = h.insert(net.neoforged.neoforge.transfer.item.ItemResource.of(stack), stack.getCount(), t);
                        t.commit();
                        return moved;
                    }
                }

                @Override
                public int lava(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, int millibuckets) {
                    net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource> h =
                            level.getCapability(Capabilities.Fluid.BLOCK, pos, null);
                    if (h == null) {
                        return -1;
                    }
                    try (net.neoforged.neoforge.transfer.transaction.Transaction t = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                        int moved = h.insert(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.LAVA), millibuckets, t);
                        t.commit();
                        return moved;
                    }
                }
            };
            NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> SelfTest.run(e.getServer()));
        }
        if (FMLEnvironment.getDist() == Dist.CLIENT && System.getenv("BARELYPOWERED_SHOTS") != null) {
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ShotTour.tick());
        }
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, DevContent.SINK_ENTITY, (be, side) -> new Sink(be));
    }

    /** The dev-only sink: takes any energy, gives none. */
    private static final class Sink extends SnapshotJournal<Long> implements EnergyHandler {
        private final SinkBlockEntity be;

        Sink(SinkBlockEntity be) {
            this.be = be;
        }

        @Override
        protected Long createSnapshot() {
            return be.received();
        }

        @Override
        protected void revertToSnapshot(Long snapshot) {
            be.setReceived(snapshot);
        }

        @Override
        public long getAmountAsLong() {
            return 0;
        }

        @Override
        public long getCapacityAsLong() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            if (amount <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            be.accept(amount, false);
            return amount;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }
    }
}
