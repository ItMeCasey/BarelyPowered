package com.itmecasey.barelypowered.neoforge;

import com.itmecasey.barelypowered.BarelyPowered;
import com.itmecasey.barelypowered.FeBuffer;
import com.itmecasey.barelypowered.ModContent;
import com.itmecasey.barelypowered.PowerMath;
import com.itmecasey.barelypowered.SelfTest;
import com.itmecasey.barelypowered.block.GeneratorBlockEntity;
import com.itmecasey.barelypowered.block.SinkBlockEntity;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** NeoForge entry point for Minecraft 26.x. Registers the content and exposes each generator's energy. */
@Mod(BarelyPowered.MODID)
public final class NeoForgeBarelyPowered {
    public NeoForgeBarelyPowered(IEventBus modBus) {
        BarelyPowered.init(FMLPaths.CONFIGDIR::get);
        modBus.addListener(NeoForgeBarelyPowered::register);
        modBus.addListener(NeoForgeBarelyPowered::capabilities);
        if (SelfTest.requested()) {
            NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> SelfTest.run(e.getServer()));
        }
        GeneratorBlockEntity.pusher = (level, pos, side, amount) -> {
            EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
            if (target == null) {
                return 0;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                int taken = target.insert(amount, transaction);
                if (taken > 0) {
                    transaction.commit();
                }
                return taken;
            }
        };
    }

    private static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> ModContent.registerBlocks((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.ITEM, helper -> ModContent.registerItems((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            helper.register(ModContent.id("generator"), ModContent.GENERATOR);
            if (ModContent.SINK_ENTITY != null) {
                helper.register(ModContent.id("test_energy_sink"), ModContent.SINK_ENTITY);
            }
        });
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ModContent.id("main"), ModContent.creativeTab()));
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModContent.GENERATOR, (be, side) -> new Output(be));
        if (ModContent.SINK_ENTITY != null) {
            event.registerBlockEntity(Capabilities.Energy.BLOCK, ModContent.SINK_ENTITY, (be, side) -> new Sink(be));
        }
    }

    /** What other mods see: energy that can be drawn out but never put in. */
    private static final class Output extends SnapshotJournal<Integer> implements EnergyHandler {
        private final GeneratorBlockEntity be;

        Output(GeneratorBlockEntity be) {
            this.be = be;
        }

        private FeBuffer buffer() {
            return be.buffer();
        }

        @Override
        protected Integer createSnapshot() {
            return buffer().stored();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            buffer().set(snapshot);
        }

        @Override
        protected void onRootCommit(Integer original) {
            be.setChanged();
        }

        @Override
        public long getAmountAsLong() {
            return buffer().stored();
        }

        @Override
        public long getCapacityAsLong() {
            return buffer().capacity();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            int given = buffer().drain(Math.min(amount, PowerMath.transferRate(be.kind(), be.tier())), true);
            if (given > 0) {
                updateSnapshots(transaction);
                buffer().drain(given, false);
            }
            return given;
        }
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
