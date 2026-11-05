package com.itmecasey.barelypowered.forge;

import com.itmecasey.barelypowered.BarelyPowered;
import com.itmecasey.barelypowered.ModContent;
import com.itmecasey.barelypowered.PowerMath;
import com.itmecasey.barelypowered.SelfTest;
import com.itmecasey.barelypowered.block.GeneratorBlockEntity;
import com.itmecasey.barelypowered.block.SinkBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Forge entry point for Minecraft 1.21.1. Registers the content and exposes each generator's energy. */
@Mod(BarelyPowered.MODID)
public final class ForgeBarelyPowered {
    public ForgeBarelyPowered(FMLJavaModLoadingContext context) {
        BarelyPowered.init(FMLPaths.CONFIGDIR::get);
        context.getModEventBus().addListener(ForgeBarelyPowered::register);
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, ForgeBarelyPowered::attach);
        if (FMLEnvironment.dist.isClient()) {
            ForgeClientSetup.install();
        }
        if (SelfTest.requested()) {
            MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> SelfTest.run(e.getServer()));
        }
        GeneratorBlockEntity.pusher = (level, pos, side, amount) -> {
            BlockEntity target = level.getBlockEntity(pos);
            if (target == null) {
                return 0;
            }
            IEnergyStorage storage = target.getCapability(ForgeCapabilities.ENERGY, side).orElse(null);
            return storage != null && storage.canReceive() ? storage.receiveEnergy(amount, false) : 0;
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

    private static void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        BlockEntity be = event.getObject();
        IEnergyStorage storage;
        if (be instanceof GeneratorBlockEntity generator) {
            storage = new Output(generator);
        } else if (be instanceof SinkBlockEntity sink) {
            storage = new Sink(sink);
        } else {
            return;
        }
        LazyOptional<IEnergyStorage> lazy = LazyOptional.of(() -> storage);
        event.addCapability(ModContent.id("energy"), new ICapabilityProvider() {
            @Override
            public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return cap == ForgeCapabilities.ENERGY ? lazy.cast() : LazyOptional.empty();
            }
        });
        event.addListener(lazy::invalidate);
    }

    /** What other mods see: energy that can be drawn out but never put in. */
    private record Output(GeneratorBlockEntity be) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int given = be.buffer().drain(Math.min(toExtract, PowerMath.transferRate(be.kind(), be.tier())), simulate);
            if (given > 0 && !simulate) {
                be.setChanged();
            }
            return given;
        }

        @Override
        public int getEnergyStored() {
            return be.buffer().stored();
        }

        @Override
        public int getMaxEnergyStored() {
            return be.buffer().capacity();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
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
