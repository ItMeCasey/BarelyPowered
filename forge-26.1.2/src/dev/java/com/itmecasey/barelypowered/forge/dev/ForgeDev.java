package com.itmecasey.barelypowered.forge.dev;

import com.itmecasey.barelypowered.ModContent;
import com.itmecasey.barelypowered.dev.SelfTest;
import com.itmecasey.barelypowered.dev.ShotTour;
import com.itmecasey.barelypowered.dev.SinkBlockEntity;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Development hooks for Forge: the sink's energy capability, the server self test and the client screenshot tour. */
public final class ForgeDev {
    private ForgeDev() {
    }

    public static void install(Object context) {
        AttachCapabilitiesEvent.BlockEntities.BUS.addListener(ForgeDev::attach);
        if (SelfTest.requested()) {
            ServerStartedEvent.BUS.addListener((ServerStartedEvent e) -> SelfTest.run(e.getServer()));
        }
        if (FMLEnvironment.dist.isClient() && System.getenv("BARELYPOWERED_SHOTS") != null) {
            TickEvent.ClientTickEvent.Post.BUS.addListener((TickEvent.ClientTickEvent.Post e) -> ShotTour.tick());
        }
    }

    private static void attach(AttachCapabilitiesEvent.BlockEntities event) {
        if (!(event.getObject() instanceof SinkBlockEntity sink)) {
            return;
        }
        LazyOptional<IEnergyStorage> lazy = LazyOptional.of(() -> new Sink(sink));
        event.addCapability(ModContent.id("sink_energy"), new ICapabilityProvider() {
            @Override
            public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return cap == ForgeCapabilities.ENERGY ? lazy.cast() : LazyOptional.empty();
            }
        });
        event.addListener(lazy::invalidate);
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
