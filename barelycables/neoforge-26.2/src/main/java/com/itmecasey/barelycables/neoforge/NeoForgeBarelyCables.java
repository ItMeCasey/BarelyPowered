package com.itmecasey.barelycables.neoforge;

import com.itmecasey.barelycables.BarelyCables;
import com.itmecasey.barelycables.CableMath;
import com.itmecasey.barelycables.CablesContent;
import com.itmecasey.barelycables.Dev;
import com.itmecasey.barelycables.block.CableBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry point for Minecraft 26.x. */
@Mod(BarelyCables.MODID)
public final class NeoForgeBarelyCables {
    public NeoForgeBarelyCables(IEventBus modBus) {
        BarelyCables.init(FMLPaths.CONFIGDIR::get);
        modBus.addListener(NeoForgeBarelyCables::register);
        modBus.addListener(NeoForgeBarelyCables::capabilities);
        CableBlockEntity.link = (level, pos, face) -> {
            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, face);
            return handler == null ? null : new Link(handler);
        };
        Dev.call("com.itmecasey.barelycables.neoforge.dev.NeoForgeDev", "install", modBus);
    }

    private static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> CablesContent.registerBlocks((name, v) -> helper.register(CablesContent.id(name), v)));
        event.register(Registries.ITEM, helper -> CablesContent.registerItems((name, v) -> helper.register(CablesContent.id(name), v)));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            helper.register(CablesContent.id("cable"), CablesContent.CABLE);
            CablesContent.extraBlockEntityTypes().forEach((name, type) -> helper.register(CablesContent.id(name), type));
        });
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(CablesContent.id("main"), CablesContent.creativeTab()));
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, CablesContent.CABLE, CableView::new);
    }

    /** A neighbor's energy as the network sees it. */
    private record Link(EnergyHandler handler) implements CableMath.Port {
        @Override
        public int extract(int max, boolean simulate) {
            try (Transaction transaction = Transaction.openRoot()) {
                int taken = handler.extract(max, transaction);
                if (!simulate && taken > 0) {
                    transaction.commit();
                }
                return taken;
            }
        }

        @Override
        public int insert(int max, boolean simulate) {
            try (Transaction transaction = Transaction.openRoot()) {
                int accepted = handler.insert(max, transaction);
                if (!simulate && accepted > 0) {
                    transaction.commit();
                }
                return accepted;
            }
        }
    }

    /** A cable as other mods see it: the network buffer, open on every face that is not switched off. */
    private static final class CableView extends SnapshotJournal<Integer> implements EnergyHandler {
        private final CableBlockEntity be;
        private final Direction side;

        CableView(CableBlockEntity be, Direction side) {
            this.be = be;
            this.side = side;
        }

        @Override
        protected Integer createSnapshot() {
            return be.networkStored();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            be.restoreStored(snapshot);
        }

        @Override
        public long getAmountAsLong() {
            return be.networkStored();
        }

        @Override
        public long getCapacityAsLong() {
            return be.networkCapacity();
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
