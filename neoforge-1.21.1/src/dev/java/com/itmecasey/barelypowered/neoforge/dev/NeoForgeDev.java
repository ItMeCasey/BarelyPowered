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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

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
                    net.neoforged.neoforge.items.IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                    return h == null ? -1 : stack.getCount() - h.insertItem(0, stack, false).getCount();
                }

                @Override
                public int lava(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, int millibuckets) {
                    net.neoforged.neoforge.fluids.capability.IFluidHandler h = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
                    return h == null ? -1 : h.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA, millibuckets),
                            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                }
            };
            NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> SelfTest.run(e.getServer()));
        }
        if (FMLEnvironment.dist == Dist.CLIENT && System.getenv("BARELYPOWERED_SHOTS") != null) {
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ShotTour.tick());
        }
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DevContent.SINK_ENTITY, (be, side) -> new Sink(be));
    }

    /** The dev-only sink: takes any energy, gives none. */
    private record Sink(SinkBlockEntity be) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return be.accept(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
