package com.itmecasey.barelypowered.neoforge;

import com.itmecasey.barelypowered.BarelyPowered;
import com.itmecasey.barelypowered.FeBuffer;
import com.itmecasey.barelypowered.ModContent;
import com.itmecasey.barelypowered.PowerMath;
import com.itmecasey.barelypowered.Dev;
import com.itmecasey.barelypowered.block.BatteryBlockEntity;
import com.itmecasey.barelypowered.block.GeneratorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
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
        // hoppers and pipes: coal, charcoal, logs and planks into a combustion generator, lava into a lava generator
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModContent.GENERATOR,
                (be, side) -> be.kind() == com.itmecasey.barelypowered.Kind.COMBUSTION ? new FuelSlot(be) : null);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModContent.GENERATOR,
                (be, side) -> be.kind() == com.itmecasey.barelypowered.Kind.LAVA ? new LavaTank(be) : null);
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

    /** The fuel slot as other mods see it: fuel can go in, nothing comes out. */
    private static final class FuelSlot extends SnapshotJournal<ItemStack> implements ResourceHandler<ItemResource> {
        private final GeneratorBlockEntity be;

        FuelSlot(GeneratorBlockEntity be) {
            this.be = be;
        }

        @Override
        protected ItemStack createSnapshot() {
            return be.fuelStack().copy();
        }

        @Override
        protected void revertToSnapshot(ItemStack snapshot) {
            be.restoreFuel(snapshot);
        }

        @Override
        protected void onRootCommit(ItemStack original) {
            be.setChanged();
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemResource getResource(int index) {
            return ItemResource.of(be.fuelStack());
        }

        @Override
        public long getAmountAsLong(int index) {
            return be.fuelStack().getCount();
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return resource.getMaxStackSize();
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return com.itmecasey.barelypowered.block.FuelRules.of(resource.toStack()) != null;
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            ItemStack offered = resource.toStack(amount);
            int moved = be.insertFuel(offered, true);
            if (moved > 0) {
                updateSnapshots(transaction);
                be.insertFuel(offered, false);
            }
            return moved;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }

    /** The lava tank as other mods see it: lava can be pumped in, nothing comes out. */
    private static final class LavaTank extends SnapshotJournal<Integer> implements ResourceHandler<FluidResource> {
        private final GeneratorBlockEntity be;

        LavaTank(GeneratorBlockEntity be) {
            this.be = be;
        }

        @Override
        protected Integer createSnapshot() {
            return be.lavaMillibuckets();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            be.restoreLava(snapshot);
        }

        @Override
        protected void onRootCommit(Integer original) {
            be.setChanged();
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public FluidResource getResource(int index) {
            return be.lavaMillibuckets() > 0 ? FluidResource.of(Fluids.LAVA) : FluidResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            return be.lavaMillibuckets();
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return be.lavaCapacity();
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.getFluid().isSame(Fluids.LAVA);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (!resource.getFluid().isSame(Fluids.LAVA)) {
                return 0;
            }
            int moved = be.insertLava(amount, true);
            if (moved > 0) {
                updateSnapshots(transaction);
                be.insertLava(moved, false);
            }
            return moved;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }
}
