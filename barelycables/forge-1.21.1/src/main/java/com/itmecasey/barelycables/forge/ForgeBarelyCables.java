package com.itmecasey.barelycables.forge;

import com.itmecasey.barelycables.BarelyCables;
import com.itmecasey.barelycables.CableMath;
import com.itmecasey.barelycables.CablesContent;
import com.itmecasey.barelycables.Dev;
import com.itmecasey.barelycables.block.CableBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Forge entry point for Minecraft 1.21.1. */
@Mod(BarelyCables.MODID)
public final class ForgeBarelyCables {
    public ForgeBarelyCables(FMLJavaModLoadingContext context) {
        BarelyCables.init(FMLPaths.CONFIGDIR::get);
        context.getModEventBus().addListener(ForgeBarelyCables::register);
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, ForgeBarelyCables::attach);
        CableBlockEntity.link = (level, pos, face) -> {
            BlockEntity target = level.getBlockEntity(pos);
            if (target == null) {
                return null;
            }
            IEnergyStorage storage = target.getCapability(ForgeCapabilities.ENERGY, face).orElse(null);
            return storage == null ? null : new Link(storage);
        };
        Dev.call("com.itmecasey.barelycables.forge.dev.ForgeDev", "install", context);
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

    private static void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!(event.getObject() instanceof CableBlockEntity cable)) {
            return;
        }
        @SuppressWarnings("unchecked")
        LazyOptional<IEnergyStorage>[] views = new LazyOptional[7];
        for (int i = 0; i < 7; i++) {
            Direction side = i == 6 ? null : Direction.values()[i];
            views[i] = LazyOptional.of(() -> new CableView(cable, side));
        }
        event.addCapability(CablesContent.id("cable_energy"), new ICapabilityProvider() {
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
