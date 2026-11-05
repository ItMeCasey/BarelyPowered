package com.itmecasey.barelycables.dev;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Counts what a {@link TestBlock} gave or took. */
public class TestBlockEntity extends BlockEntity {
    private long sent;
    private long received;
    private final boolean source;

    public TestBlockEntity(BlockPos pos, BlockState state) {
        super(DevContent.TEST_ENTITY, pos, state);
        this.source = state.getBlock() instanceof TestBlock b && b.isSource();
    }

    public boolean isSource() {
        return source;
    }

    public long sent() {
        return sent;
    }

    public long received() {
        return received;
    }

    public long[] snapshot() {
        return new long[] {sent, received};
    }

    public void restore(long[] state) {
        sent = state[0];
        received = state[1];
    }

    /** A source gives everything asked of it; a sink gives nothing. */
    public int give(int max, boolean simulate) {
        if (!source || max <= 0) {
            return 0;
        }
        if (!simulate) {
            sent += max;
        }
        return max;
    }

    /** A sink takes everything offered; a source takes nothing. */
    public int take(int max, boolean simulate) {
        if (source || max <= 0) {
            return 0;
        }
        if (!simulate) {
            received += max;
        }
        return max;
    }
}
