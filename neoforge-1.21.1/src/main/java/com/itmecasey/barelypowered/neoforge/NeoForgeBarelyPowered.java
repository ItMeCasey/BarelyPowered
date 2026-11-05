package com.itmecasey.barelypowered.neoforge;

import com.itmecasey.barelypowered.BarelyPowered;
import com.itmecasey.barelypowered.ModContent;
import com.itmecasey.barelypowered.block.GeneratorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry point for Minecraft 1.21.1. Registers the content and exposes each generator's energy. */
@Mod(BarelyPowered.MODID)
public final class NeoForgeBarelyPowered {
    public NeoForgeBarelyPowered(IEventBus modBus) {
        BarelyPowered.init(FMLPaths.CONFIGDIR::get);
        modBus.addListener(NeoForgeBarelyPowered::register);
        modBus.addListener(NeoForgeBarelyPowered::capabilities);
        GeneratorBlockEntity.pusher = (level, pos, side, amount) -> {
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
            return target != null && target.canReceive() ? target.receiveEnergy(amount, false) : 0;
        };
    }

    private static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> ModContent.registerBlocks((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.ITEM, helper -> ModContent.registerItems((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> helper.register(ModContent.id("generator"), ModContent.GENERATOR));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ModContent.id("main"), ModContent.creativeTab()));
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModContent.GENERATOR,
                (be, side) -> new Output(be));
    }

    /** What other mods see: energy that can be drawn out but never put in. */
    private record Output(GeneratorBlockEntity be) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int given = be.buffer().drain(Math.min(toExtract, com.itmecasey.barelypowered.PowerMath.transferRate(be.kind(), be.tier())), simulate);
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
}
