package com.itmecasey.barelypowered.neoforge;

import com.itmecasey.barelypowered.BarelyPowered;
import com.itmecasey.barelypowered.FeBuffer;
import com.itmecasey.barelypowered.ModContent;
import com.itmecasey.barelypowered.PowerMath;
import com.itmecasey.barelypowered.Dev;
import com.itmecasey.barelypowered.block.BatteryBlockEntity;
import com.itmecasey.barelypowered.block.GeneratorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
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
        Dev.call("com.itmecasey.barelypowered.neoforge.dev.NeoForgeDev", "install", modBus);
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
            helper.register(ModContent.id("battery"), ModContent.BATTERY);
            helper.register(ModContent.id("wind_head"), ModContent.WIND_HEAD);
            ModContent.extraBlockEntityTypes().forEach((name, type) -> helper.register(ModContent.id(name), type));
        });
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ModContent.id("main"), ModContent.creativeTab()));
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModContent.GENERATOR, (be, side) -> new Output(be));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModContent.BATTERY, BatteryView::new);
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

    /** A battery as other mods see it: each face follows its own input and output setting and the tier rate. */
    private static final class BatteryView extends SnapshotJournal<int[]> implements EnergyHandler {
        private final BatteryBlockEntity be;
        private final net.minecraft.core.Direction side;

        BatteryView(BatteryBlockEntity be, net.minecraft.core.Direction side) {
            this.be = be;
            this.side = side;
        }

        @Override
        protected int[] createSnapshot() {
            return be.core().snapshot();
        }

        @Override
        protected void revertToSnapshot(int[] snapshot) {
            be.core().restore(snapshot);
        }

        @Override
        protected void onRootCommit(int[] original) {
            be.setChanged();
        }

        @Override
        public long getAmountAsLong() {
            return be.core().stored();
        }

        @Override
        public long getCapacityAsLong() {
            return be.core().capacity();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            int taken = be.receive(side, amount, true);
            if (taken > 0) {
                updateSnapshots(transaction);
                be.receive(side, taken, false);
            }
            return taken;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            int given = be.extract(side, amount, true);
            if (given > 0) {
                updateSnapshots(transaction);
                be.extract(side, given, false);
            }
            return given;
        }
    }
}
