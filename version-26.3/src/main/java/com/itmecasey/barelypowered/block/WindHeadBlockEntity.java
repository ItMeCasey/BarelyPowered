package com.itmecasey.barelypowered.block;

import com.itmecasey.barelypowered.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block entity of a wind turbine's head. It stores nothing: it exists so that a renderer can draw the turning rotor.
 * The rotor's angle and speed are client-side animation state, advanced by the renderer in real time so the turn is smooth
 * at any frame rate; the turbine's lit state (copied from the base) decides whether it spins up or slows down.
 */
public class WindHeadBlockEntity extends BlockEntity {
    /** Current rotor angle in degrees. Client-side animation only. */
    public float angle;
    /** Current speed in degrees per second. Client-side animation only. */
    public float speed;
    /** The last time the renderer advanced the animation. */
    public long lastNanos;

    public WindHeadBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.WIND_HEAD, pos, state);
    }
}
