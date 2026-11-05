package com.itmecasey.barelycables.block;

import com.itmecasey.barelycables.CablesContent;
import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** An energy cable: a small core with an arm toward every neighbor it is joined to. */
public class CableBlock extends BaseEntityBlock {
    private static final Map<Direction, BooleanProperty> PROPERTIES = new EnumMap<>(Direction.class);
    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);
    private static final VoxelShape[] SHAPES = new VoxelShape[64];
    private static final MapCodec<CableBlock> CODEC = simpleCodec(p -> new CableBlock(1, p));

    static {
        PROPERTIES.put(Direction.DOWN, BlockStateProperties.DOWN);
        PROPERTIES.put(Direction.UP, BlockStateProperties.UP);
        PROPERTIES.put(Direction.NORTH, BlockStateProperties.NORTH);
        PROPERTIES.put(Direction.SOUTH, BlockStateProperties.SOUTH);
        PROPERTIES.put(Direction.WEST, BlockStateProperties.WEST);
        PROPERTIES.put(Direction.EAST, BlockStateProperties.EAST);
        ARMS.put(Direction.DOWN, Block.box(6, 0, 6, 10, 6, 10));
        ARMS.put(Direction.UP, Block.box(6, 10, 6, 10, 16, 10));
        ARMS.put(Direction.NORTH, Block.box(6, 6, 0, 10, 10, 6));
        ARMS.put(Direction.SOUTH, Block.box(6, 6, 10, 10, 10, 16));
        ARMS.put(Direction.WEST, Block.box(0, 6, 6, 6, 10, 10));
        ARMS.put(Direction.EAST, Block.box(10, 6, 6, 16, 10, 10));
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = CORE;
            for (Direction d : Direction.values()) {
                if ((mask >> d.ordinal() & 1) != 0) {
                    shape = Shapes.or(shape, ARMS.get(d));
                }
            }
            SHAPES[mask] = shape;
        }
    }

    private final int tier;

    public CableBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
        BlockState state = stateDefinition.any();
        for (BooleanProperty p : PROPERTIES.values()) {
            state = state.setValue(p, false);
        }
        registerDefaultState(state);
    }

    public static BooleanProperty property(Direction d) {
        return PROPERTIES.get(d);
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
        for (Direction d : Direction.values()) {
            builder.add(PROPERTIES.get(d));
        }
    }

    private static int mask(BlockState state) {
        int mask = 0;
        for (Direction d : Direction.values()) {
            if (state.getValue(PROPERTIES.get(d))) {
                mask |= 1 << d.ordinal();
            }
        }
        return mask;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[mask(state)];
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, CablesContent.CABLE, CableBlockEntity::serverTick);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean moved) {
        super.neighborChanged(state, level, pos, block, orientation, moved);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CableBlockEntity be) {
            be.refreshConnections();
        }
    }

    /** Right-click an arm with an empty hand to switch that face off, and again to switch it back on. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CableBlockEntity be) {
            Direction face = armAt(state, hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)), hit.getDirection());
            boolean off = be.toggleFace(face);
            player.sendOverlayMessage(Component.translatable(off ? "message.barelycables.face_off" : "message.barelycables.face_on",
                    Component.translatable("message.barelycables.face." + face.getName())));
        }
        return InteractionResult.SUCCESS;
    }

    /** Which face the click was meant for: the arm that contains the point, otherwise the face of the core that was clicked. */
    private static Direction armAt(BlockState state, Vec3 local, Direction clicked) {
        for (Direction d : Direction.values()) {
            if (state.getValue(PROPERTIES.get(d))) {
                for (net.minecraft.world.phys.AABB box : ARMS.get(d).toAabbs()) {
                    if (box.inflate(0.002).contains(local)) {
                        return d;
                    }
                }
            }
        }
        return clicked;
    }
}
