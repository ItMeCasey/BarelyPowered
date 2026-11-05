package com.itmecasey.barelypowered.jade;

import com.itmecasey.barelypowered.FaceMode;
import com.itmecasey.barelypowered.Kind;
import com.itmecasey.barelypowered.block.BatteryBlock;
import com.itmecasey.barelypowered.block.BatteryBlockEntity;
import com.itmecasey.barelypowered.block.GeneratorBlock;
import com.itmecasey.barelypowered.block.GeneratorBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * The BarelyPowered lines in Jade: output, fuel or lava, the reason for the current output, and a battery's rate and face
 * modes. Jade already draws the energy bar from the energy capability. Jade finds this class by its annotation, so it is never
 * loaded unless Jade is installed; the same numbers show in Show Me What This Is.
 */
@WailaPlugin
public class BarelyPoweredJadePlugin implements IWailaPlugin {
    private static final Identifier GENERATOR = Identifier.fromNamespaceAndPath("barelypowered", "generator");
    private static final Identifier BATTERY = Identifier.fromNamespaceAndPath("barelypowered", "battery");

    private static String str(CompoundTag data, String key) {
        return data.getStringOr(key, "");
    }

    private static int num(CompoundTag data, String key) {
        return data.getIntOr(key, 0);
    }

    private static int[] arr(CompoundTag data, String key) {
        return data.getIntArray(key).orElse(new int[0]);
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(new IServerDataProvider<BlockAccessor>() {
            @Override
            public void appendServerData(CompoundTag data, BlockAccessor accessor) {
                if (!(accessor.getBlockEntity() instanceof GeneratorBlockEntity be)) {
                    return;
                }
                data.putString("kind", be.kind().name());
                data.putString("output", GeneratorBlockEntity.formatRate(be.output()));
                data.putString("note", be.note());
                if (be.kind() == Kind.COMBUSTION) {
                    data.putString("fuel", be.fuelStack().isEmpty() ? "" : be.fuelStack().getHoverName().getString());
                    data.putInt("fuelCount", be.fuelStack().getCount());
                    data.putInt("seconds", be.burnTicksLeft() / 20);
                } else if (be.kind() == Kind.LAVA) {
                    data.putInt("lava", be.lavaMillibuckets());
                    data.putInt("lavaCapacity", be.lavaCapacity());
                    data.putInt("seconds", be.burnTicksLeft() / 20);
                }
            }

            @Override
            public Identifier getUid() {
                return GENERATOR;
            }
        }, GeneratorBlockEntity.class);
        registration.registerBlockDataProvider(new IServerDataProvider<BlockAccessor>() {
            @Override
            public void appendServerData(CompoundTag data, BlockAccessor accessor) {
                if (!(accessor.getBlockEntity() instanceof BatteryBlockEntity be)) {
                    return;
                }
                data.putInt("rate", be.core().rate());
                int[] modes = new int[6];
                for (Direction face : Direction.values()) {
                    modes[face.ordinal()] = be.core().mode(face.ordinal()).ordinal();
                }
                data.putIntArray("modes", modes);
            }

            @Override
            public Identifier getUid() {
                return BATTERY;
            }
        }, BatteryBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new IBlockComponentProvider() {
            @Override
            public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
                CompoundTag data = accessor.getServerData();
                String kind = str(data, "kind");
                if (kind.isEmpty()) {
                    return;
                }
                tooltip.add(Component.translatable("smwti.barelypowered.output", str(data, "output")));
                if (kind.equals(Kind.COMBUSTION.name())) {
                    String fuel = str(data, "fuel");
                    if (fuel.isEmpty()) {
                        tooltip.add(Component.translatable("smwti.barelypowered.no_fuel"));
                    } else {
                        tooltip.add(Component.translatable("smwti.barelypowered.fuel", fuel, num(data, "fuelCount"), num(data, "seconds")));
                    }
                } else if (kind.equals(Kind.LAVA.name())) {
                    tooltip.add(Component.translatable("smwti.barelypowered.lava", num(data, "lava"), num(data, "lavaCapacity"), num(data, "seconds")));
                }
                String note = str(data, "note");
                if (!note.equals("ok") && !note.equals("idle") && !note.equals("burning") && !note.isEmpty()) {
                    tooltip.add(Component.translatable("smwti.barelypowered.note." + note));
                }
            }

            @Override
            public Identifier getUid() {
                return GENERATOR;
            }
        }, GeneratorBlock.class);
        registration.registerBlockComponent(new IBlockComponentProvider() {
            @Override
            public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
                CompoundTag data = accessor.getServerData();
                int[] modes = arr(data, "modes");
                if (modes.length != 6) {
                    return;
                }
                tooltip.add(Component.translatable("smwti.barelypowered.battery_rate", num(data, "rate")));
                FaceMode[] all = FaceMode.values();
                for (Direction face : Direction.values()) {
                    if (modes[face.ordinal()] != FaceMode.BOTH.ordinal()) {
                        tooltip.add(Component.translatable("smwti.barelypowered.face", BatteryBlock.faceName(face), all[modes[face.ordinal()]].label()));
                    }
                }
            }

            @Override
            public Identifier getUid() {
                return BATTERY;
            }
        }, BatteryBlock.class);
    }
}
