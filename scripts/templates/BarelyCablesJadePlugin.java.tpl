package com.itmecasey.barelycables.jade;

import com.itmecasey.barelycables.block.CableBlock;
import com.itmecasey.barelycables.block.CableBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.__ID__;
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
 * The BarelyCables lines in Jade: what the network of the cable you look at moves, its size and the sides that are switched
 * off. Jade already draws the buffer bar from the cable's energy capability. Jade finds this class by its annotation, so it is
 * never loaded unless Jade is installed. It imports nothing from BarelyPowered.
 */
@WailaPlugin
public class BarelyCablesJadePlugin implements IWailaPlugin {
    private static final __ID__ CABLE = __ID__.fromNamespaceAndPath("barelycables", "cable");

__HELPERS__

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(new IServerDataProvider<BlockAccessor>() {
            @Override
            public void appendServerData(CompoundTag data, BlockAccessor accessor) {
                if (!(accessor.getBlockEntity() instanceof CableBlockEntity cable)) {
                    return;
                }
                data.putInt("rate", cable.networkRate());
                data.putInt("cables", cable.networkCables());
                data.putInt("endpoints", cable.networkEndpoints());
                int off = 0;
                for (Direction d : Direction.values()) {
                    if (cable.faceDisabled(d)) {
                        off |= 1 << d.ordinal();
                    }
                }
                data.putInt("off", off);
            }

            @Override
            public __ID__ getUid() {
                return CABLE;
            }
        }, CableBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new IBlockComponentProvider() {
            @Override
            public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
                CompoundTag data = accessor.getServerData();
                if (!data.contains("rate")) {
                    return;
                }
                tooltip.add(Component.translatable("smwti.barelycables.rate", num(data, "rate")));
                tooltip.add(Component.translatable("smwti.barelycables.size", num(data, "cables"), num(data, "endpoints")));
                int off = num(data, "off");
                for (Direction d : Direction.values()) {
                    if ((off & (1 << d.ordinal())) != 0) {
                        tooltip.add(Component.translatable("smwti.barelycables.face_off", Component.translatable("message.barelycables.face." + d.getName())));
                    }
                }
            }

            @Override
            public __ID__ getUid() {
                return CABLE;
            }
        }, CableBlock.class);
    }
}
