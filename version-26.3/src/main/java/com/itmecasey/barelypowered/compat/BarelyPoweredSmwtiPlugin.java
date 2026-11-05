package com.itmecasey.barelypowered.compat;

import com.itmecasey.barelypowered.Kind;
import com.itmecasey.barelypowered.block.GeneratorBlockEntity;
import com.itmecasey.smwti.api.CommonRegistration;
import com.itmecasey.smwti.api.ISmwtiPlugin;
import com.itmecasey.smwti.api.SmwtiPlugin;
import net.minecraft.network.chat.Component;

/**
 * The BarelyPowered lines in Show Me What This Is, our own look-at overlay: each generator's current output, how full its
 * energy buffer is, its fuel or lava, and the reason for the current output. The server works the lines out, so they are
 * exact. SMWTI finds this class by its annotation; nothing calls into it, and it is never loaded unless SMWTI is installed.
 */
@SmwtiPlugin
public class BarelyPoweredSmwtiPlugin implements ISmwtiPlugin {
    private static final int GOLD = 0xF2B33D;
    private static final int LAVA = 0xE8671C;

    @Override
    public void register(CommonRegistration registration) {
        registration.serverBlock((tooltip, level, pos, entity, detailed) -> {
            if (!(entity instanceof GeneratorBlockEntity be)) {
                return;
            }
            tooltip.add(Component.translatable("smwti.barelypowered.output", GeneratorBlockEntity.formatRate(be.output())));
            // SMWTI already draws the energy bar from the generator's energy capability, so only output, fuel and the reason go here.
            if (be.kind() == Kind.COMBUSTION) {
                if (be.fuelStack().isEmpty()) {
                    tooltip.add(Component.translatable("smwti.barelypowered.no_fuel"));
                } else {
                    tooltip.item(be.fuelStack(), Component.translatable("smwti.barelypowered.fuel",
                            be.fuelStack().getHoverName(), be.fuelStack().getCount(), be.burnTicksLeft() / 20));
                }
            } else if (be.kind() == Kind.LAVA) {
                tooltip.bar(Component.translatable("smwti.barelypowered.lava", be.lavaMillibuckets(), be.lavaCapacity(),
                        be.burnTicksLeft() / 20), (double) be.lavaMillibuckets() / Math.max(1, be.lavaCapacity()), LAVA);
            }
            String note = be.note();
            if (!note.equals("ok") && !note.equals("idle") && !note.equals("burning")) {
                tooltip.add(Component.translatable("smwti.barelypowered.note." + note));
            }
        });
    }
}
