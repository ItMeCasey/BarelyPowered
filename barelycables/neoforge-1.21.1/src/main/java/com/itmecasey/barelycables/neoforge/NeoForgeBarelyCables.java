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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry point for Minecraft 1.21.1. */
@Mod(BarelyCables.MODID)
public final class NeoForgeBarelyCables {
    public NeoForgeBarelyCables(IEventBus modBus) {
        BarelyCables.init(FMLPaths.CONFIGDIR::get);
        org.slf4j.LoggerFactory.getLogger("BarelyCables").info("BarelyCables module initialized");
        modBus.addListener(NeoForgeBarelyCables::register);
        modBus.addListener(NeoForgeBarelyCables::capabilities);
        CableBlockEntity.link = (level, pos, face) -> {
            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, face);
            return storage == null ? null : new Link(storage);
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
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CablesContent.CABLE, CableView::new);
    }

    /** A neighbor's energy as the network sees it. */
    private record Link(IEnergyStorage storage) implements CableMath.Port {
        @Override
        public int extract(int max, boolean simulate) {
            return storage.canExtract() ? storage.extractEnergy(max, simulate) : 0;
        }

        @Override
        public int insert(int max, boolean simulate) {
            return storage.canReceive() ? storage.receiveEnergy(max, simulate) : 0;
        }
    }

    /** A cable as other mods see it: the network buffer, open on every face that is not switched off. */
    private record CableView(CableBlockEntity be, Direction side) implements IEnergyStorage {
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
            return be.networkStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return be.networkCapacity();
        }

        @Override
        public boolean canExtract() {
            return side == null || !be.faceDisabled(side);
        }

        @Override
        public boolean canReceive() {
            return side == null || !be.faceDisabled(side);
        }
    }
}
