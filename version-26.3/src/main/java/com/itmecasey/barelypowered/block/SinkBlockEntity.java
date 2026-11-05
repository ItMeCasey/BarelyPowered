package com.itmecasey.barelypowered.block;

import com.itmecasey.barelypowered.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Counts the energy put into a {@link SinkBlock}. */
public class SinkBlockEntity extends BlockEntity {
    private long received;

    public SinkBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.SINK_ENTITY, pos, state);
    }

    public long received() {
        return received;
    }

    public void setReceived(long value) {
        received = value;
    }

    /** Accepts up to {@code amount}; the sink never fills up. */
    public int accept(int amount, boolean simulate) {
        if (!simulate && amount > 0) {
            received += amount;
            setChanged();
        }
        return Math.max(0, amount);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("received", received);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        received = input.getLongOr("received", 0L);
    }
}
