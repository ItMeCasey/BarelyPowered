package com.itmecasey.barelycables.compat;

import com.itmecasey.barelycables.block.CableBlockEntity;
import com.itmecasey.smwti.api.CommonRegistration;
import com.itmecasey.smwti.api.ISmwtiPlugin;
import com.itmecasey.smwti.api.SmwtiPlugin;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

/**
 * The BarelyCables lines in Show Me What This Is: what the network of the cable you look at moves, its buffer, its size and the
 * sides that are switched off. SMWTI finds this class by its annotation; it is never loaded unless SMWTI is installed.
 */
@SmwtiPlugin
public class BarelyCablesSmwtiPlugin implements ISmwtiPlugin {
    @Override
    public void register(CommonRegistration registration) {
        registration.serverBlock((tooltip, level, pos, entity, detailed) -> {
            if (!(entity instanceof CableBlockEntity cable)) {
                return;
            }
            tooltip.add(Component.translatable("smwti.barelycables.rate", cable.networkRate()));
            int capacity = cable.networkCapacity();
            tooltip.bar(Component.translatable("smwti.barelycables.buffer", cable.networkStored(), capacity),
                    capacity == 0 ? 0.0 : (double) cable.networkStored() / capacity, 0xF2B33D);
            tooltip.add(Component.translatable("smwti.barelycables.size", cable.networkCables(), cable.networkEndpoints()));
            for (Direction d : Direction.values()) {
                if (cable.faceDisabled(d)) {
                    tooltip.add(Component.translatable("smwti.barelycables.face_off", Component.translatable("message.barelycables.face." + d.getName())));
                }
            }
        });
    }
}
