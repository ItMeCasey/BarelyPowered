package com.itmecasey.barelycables.forge.dev;

import com.itmecasey.barelycables.CablesContent;
import com.itmecasey.barelycables.dev.SelfTest;
import com.itmecasey.barelycables.dev.TestBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Development hooks for Forge: the test blocks' energy capability and the server self test. */
public final class ForgeDev {
    private ForgeDev() {
    }

    public static void install(Object context) {
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, ForgeDev::attach);
        if (SelfTest.requested()) {
            MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> SelfTest.run(e.getServer()));
        }
    }

    private static void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!(event.getObject() instanceof TestBlockEntity test)) {
            return;
        }
        LazyOptional<IEnergyStorage> lazy = LazyOptional.of(() -> new View(test));
        event.addCapability(CablesContent.id("test_energy"), new ICapabilityProvider() {
            @Override
            public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return cap == ForgeCapabilities.ENERGY ? lazy.cast() : LazyOptional.empty();
            }
        });
        event.addListener(lazy::invalidate);
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
