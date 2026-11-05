package com.itmecasey.barelypowered.dev;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A development-only block that accepts any amount of energy and counts it. It exists only when the system property
 * {@code barelypowered.dev} is set (dev runs and the self test), and stands in for a machine, battery or cable.
 */
public class SinkBlock extends BaseEntityBlock {
    public SinkBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SinkBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SinkBlockEntity sink) {
            player.sendOverlayMessage(Component.literal("Energy sink: " + sink.received() + " FE received"));
        }
        return InteractionResult.SUCCESS;
    }
}
