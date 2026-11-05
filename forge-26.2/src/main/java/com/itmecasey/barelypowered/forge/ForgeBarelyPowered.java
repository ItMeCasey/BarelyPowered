package com.itmecasey.barelypowered.forge;

import com.itmecasey.barelypowered.BarelyPowered;
import com.itmecasey.barelypowered.ModContent;
import com.itmecasey.barelypowered.PowerMath;
import com.itmecasey.barelypowered.Dev;
import com.itmecasey.barelypowered.block.BatteryBlockEntity;
import com.itmecasey.barelypowered.block.GeneratorBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Forge entry point for Minecraft 26.x. Registers the content and exposes each generator's energy. */
@Mod(BarelyPowered.MODID)
public final class ForgeBarelyPowered {
    public ForgeBarelyPowered(FMLJavaModLoadingContext context) {
        BarelyPowered.init(FMLPaths.CONFIGDIR::get);
        RegisterEvent.getBus(context.getModBusGroup()).addListener(ForgeBarelyPowered::register);
        AttachCapabilitiesEvent.BlockEntities.BUS.addListener(ForgeBarelyPowered::attach);
        Dev.call("com.itmecasey.barelypowered.forge.dev.ForgeDev", "install", context);
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
            helper.register(ModContent.id("battery"), ModContent.BATTERY);
            ModContent.extraBlockEntityTypes().forEach((name, type) -> helper.register(ModContent.id(name), type));
        });
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ModContent.id("main"), ModContent.creativeTab()));
    }

    private static void attach(AttachCapabilitiesEvent.BlockEntities event) {
        BlockEntity be = event.getObject();
        if (be instanceof BatteryBlockEntity battery) {
            @SuppressWarnings("unchecked")
            LazyOptional<IEnergyStorage>[] views = new LazyOptional[7];
            for (int i = 0; i < 7; i++) {
                Direction side = i == 6 ? null : Direction.values()[i];
                views[i] = LazyOptional.of(() -> new BatteryView(battery, side));
            }
            event.addCapability(ModContent.id("battery_energy"), new ICapabilityProvider() {
                @Override
                public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                    return cap == ForgeCapabilities.ENERGY ? views[side == null ? 6 : side.ordinal()].cast() : LazyOptional.empty();
                }
            });
            event.addListener(() -> {
                for (LazyOptional<IEnergyStorage> view : views) {
                    view.invalidate();
                }
            });
            return;
        }
        IEnergyStorage storage;
        if (be instanceof GeneratorBlockEntity generator) {
            storage = new Output(generator);
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

    /** A battery as other mods see it: each face follows its own input and output setting and the tier rate. */
    private record BatteryView(BatteryBlockEntity be, Direction side) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return be.receive(side, toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return be.extract(side, toExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return be.core().stored();
        }

        @Override
        public int getMaxEnergyStored() {
            return be.core().capacity();
        }

        @Override
        public boolean canExtract() {
            return be.core().mode(side == null ? -1 : side.ordinal()).canOutput();
        }

        @Override
        public boolean canReceive() {
            return be.core().mode(side == null ? -1 : side.ordinal()).canInput();
        }
    }
}
