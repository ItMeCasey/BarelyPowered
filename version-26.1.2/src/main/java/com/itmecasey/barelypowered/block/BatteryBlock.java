package com.itmecasey.barelypowered.block;

import com.itmecasey.barelypowered.BatteryCore;
import com.itmecasey.barelypowered.FaceMode;
import com.itmecasey.barelypowered.ModContent;
import com.mojang.serialization.MapCodec;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** A battery: stores energy, with per-face input and output. The charge property drives the gauge on the model. */
public class BatteryBlock extends BaseEntityBlock {
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 8);
    private static final MapCodec<BatteryBlock> CODEC = simpleCodec(p -> new BatteryBlock(1, p));

    private final int tier;

    public BatteryBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(CHARGE, 0));
    }

    public int tier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatteryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModContent.BATTERY, BatteryBlockEntity::serverTick);
    }

    /** Right-click a face with an empty hand to step it through both, input, output and off. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BatteryBlockEntity be) {
            Direction face = hit.getDirection();
            FaceMode mode = be.cycleFace(face);
            player.sendOverlayMessage(Component.translatable("message.barelypowered.battery",
                    faceName(face), mode.label(), be.core().stored(), be.core().capacity()));
        }
        return InteractionResult.SUCCESS;
    }

    public static String faceName(Direction face) {
        String n = face.getName();
        return n.substring(0, 1).toUpperCase(Locale.ROOT) + n.substring(1);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof BatteryBlockEntity be
                ? BatteryCore.comparator(be.core().stored(), be.core().capacity()) : 0;
    }
}
