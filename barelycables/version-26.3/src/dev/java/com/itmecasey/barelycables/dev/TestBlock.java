package com.itmecasey.barelycables.dev;

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

/** A development-only block that either gives any amount of energy (a source) or takes any amount (a sink), and counts it. */
public class TestBlock extends BaseEntityBlock {
    private final boolean source;

    public TestBlock(boolean source, Properties properties) {
        super(properties);
        this.source = source;
    }

    public boolean isSource() {
        return source;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TestBlockEntity be) {
            player.sendOverlayMessage(Component.literal(source ? "Energy source: " + be.sent() + " FE sent" : "Energy sink: " + be.received() + " FE received"));
        }
        return InteractionResult.SUCCESS;
    }
}
