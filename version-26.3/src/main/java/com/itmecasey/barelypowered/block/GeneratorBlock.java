package com.itmecasey.barelypowered.block;

import com.itmecasey.barelypowered.Kind;
import com.itmecasey.barelypowered.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** One block class for every generator; the kind and tier decide what its block entity does. */
public class GeneratorBlock extends BaseEntityBlock {
    public static final EnumProperty<net.minecraft.core.Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final net.minecraft.world.phys.shapes.VoxelShape SOLAR_SHAPE = Block.box(0, 0, 0, 16, 13, 16);
    private static final net.minecraft.world.phys.shapes.VoxelShape SOLAR_SHAPE_2 = Block.box(0, 0, 0, 16, 16, 16);
    private static final net.minecraft.world.phys.shapes.VoxelShape WIND_SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    private final Kind kind;
    private final int tier;

    public GeneratorBlock(Kind kind, int tier, Properties properties) {
        super(properties);
        this.kind = kind;
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(LIT, false));
    }

    public Kind kind() {
        return kind;
    }

    public int tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (kind == Kind.WIND && !WindTowerBlock.hasRoom(context.getLevel(), context.getClickedPos(), tier)) {
            if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
                context.getPlayer().sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                        "message.barelypowered.wind_space", com.itmecasey.barelypowered.Tiers.windHeight(tier) - 1));
            }
            return null;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** A wind turbine builds its tower when it appears (placed by hand, by a command or by a structure). */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (kind == Kind.WIND && !level.isClientSide() && !oldState.is(state.getBlock())) {
            WindTowerBlock.build(level, pos, state, tier);
        }
    }

    /** The solar panels and wind turbines have hand-built models that do not fill the block, so their outline follows the model. */
    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                                                   BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return switch (kind) {
            case SOLAR -> tier >= 2 ? SOLAR_SHAPE_2 : SOLAR_SHAPE;
            case WIND -> WIND_SHAPE;
            default -> net.minecraft.world.phys.shapes.Shapes.block();
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModContent.GENERATOR, GeneratorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity be && be.acceptsItem(stack)) {
            if (!level.isClientSide()) {
                be.insertFrom(player, hand, stack);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof GeneratorBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            be.interact(player);
        }
        return InteractionResult.SUCCESS;
    }
}
