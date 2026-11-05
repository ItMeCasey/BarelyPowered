package com.itmecasey.barelycables;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * How a cable talks to the block next to it. Forge and NeoForge have different energy interfaces, so each loader layer supplies
 * the one implementation; everything else only sees {@link CableMath.Port}.
 */
@FunctionalInterface
public interface CableLink {
    /**
     * @param pos the neighbor block
     * @param face the face of the neighbor that touches the cable
     * @return a port onto the neighbor's Forge Energy, or {@code null} if it offers none on that face
     */
    CableMath.Port portAt(Level level, BlockPos pos, Direction face);
}
