package com.itmecasey.barelypowered;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Hands energy to whatever block sits at a position. Forge and NeoForge each have their own energy interface, so each loader
 * layer supplies the one implementation and the generators only call this.
 */
@FunctionalInterface
public interface EnergyPusher {
    /**
     * @param target the block that may accept energy
     * @param side the face of {@code target} the energy enters through
     * @param amount the most to hand over
     * @return how much the target took
     */
    int push(Level level, BlockPos target, Direction side, int amount);
}
