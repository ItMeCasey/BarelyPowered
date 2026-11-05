package com.itmecasey.barelypowered.block;

import com.itmecasey.barelypowered.Kind;
import com.itmecasey.barelypowered.ModContent;
import com.itmecasey.barelypowered.Tiers;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A piece of a wind turbine's tower: a mast segment, or the head (nacelle and rotor) at the top. The turbine block itself
 * (a {@link GeneratorBlock} of kind WIND) builds these above itself when it is placed and holds the energy; the pieces have
 * no item, no drops and no block entity. A piece that loses its support, or the piece above it, removes itself, so the
 * whole tower goes when the base is broken; breaking a piece by hand breaks the base (and drops it) instead.
 */
public class WindTowerBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty HEAD = BooleanProperty.create("head");

    private static final VoxelShape SEGMENT_SHAPE = Block.box(5, 0, 5, 11, 16, 11);
    private static final VoxelShape HEAD_SHAPE = Block.box(3, 0, 3, 13, 14, 13);

    private final int tier;

    public WindTowerBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HEAD, false).setValue(LIT, false));
    }

    public int tier() {
        return tier;
    }

    /** Only the head has a block entity, for the renderer that turns the rotor. */
    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HEAD) ? new WindHeadBlockEntity(pos, state) : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HEAD, LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HEAD) ? HEAD_SHAPE : SEGMENT_SHAPE;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos,
                                     Direction dir, BlockPos neighborPos, BlockState neighbor, net.minecraft.util.RandomSource random) {
        return check(state, dir, neighbor);
    }

    private BlockState check(BlockState state, Direction toNeighbor, BlockState neighbor) {
        if (toNeighbor == Direction.DOWN && !supports(neighbor)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (toNeighbor == Direction.UP && !state.getValue(HEAD)
                && !(neighbor.getBlock() instanceof WindTowerBlock t && t.tier == tier)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    /** What a piece may stand on: the turbine block of its tier, or a mast segment of its tier. */
    private boolean supports(BlockState below) {
        if (below.getBlock() instanceof GeneratorBlock g) {
            return g.kind() == Kind.WIND && g.tier() == tier;
        }
        return below.getBlock() instanceof WindTowerBlock t && t.tier == tier && !below.getValue(HEAD);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos p = pos;
        while (level.getBlockState(p.below()).getBlock() instanceof WindTowerBlock) {
            p = p.below();
        }
        BlockPos base = p.below();
        if (!level.isClientSide() && level.getBlockState(base).getBlock() instanceof GeneratorBlock g && g.kind() == Kind.WIND) {
            level.destroyBlock(base, !player.isCreative(), player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** True when the blocks above a turbine placed at pos are free for its tower. */
    public static boolean hasRoom(LevelReader level, BlockPos pos, int tier) {
        for (int i = 1; i < Tiers.windHeight(tier); i++) {
            BlockPos p = pos.above(i);
            if (level.isOutsideBuildHeight(p) || !level.getBlockState(p).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    /** Builds the mast segments and the head above a freshly placed turbine block. */
    public static void build(Level level, BlockPos base, BlockState baseState, int tier) {
        int height = Tiers.windHeight(tier);
        WindTowerBlock piece = ModContent.towers().get("wind_tower_" + tier);
        for (int i = 1; i < height; i++) {
            BlockPos p = base.above(i);
            if (level.isOutsideBuildHeight(p) || !level.getBlockState(p).canBeReplaced()) {
                break;
            }
            level.setBlock(p, piece.defaultBlockState().setValue(FACING, baseState.getValue(GeneratorBlock.FACING))
                    .setValue(HEAD, i == height - 1), 3);
        }
    }

    /** Copies the turbine's lit state to its tower, so the rotor turns while it makes power. */
    public static void syncLit(Level level, BlockPos base, int tier, boolean lit) {
        for (int i = 1; i < Tiers.windHeight(tier); i++) {
            BlockPos p = base.above(i);
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof WindTowerBlock && s.getValue(LIT) != lit) {
                level.setBlock(p, s.setValue(LIT, lit), 2);
            }
        }
    }
}
