package com.enrichmentsolution.mod.common.block;

import com.enrichmentsolution.mod.common.block.entity.FluidPipeBlockEntity;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Six-way auto-connecting fluid pipe block with distinct pipe and machine-plug endpoints.
 */
public class FluidPipeBlock extends BaseEntityBlock {

    public static final MapCodec<FluidPipeBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(),
            Codec.STRING.fieldOf("tier").forGetter(block -> block.tier.getSerializedName())
    ).apply(instance, (properties, tierName) -> new FluidPipeBlock(properties, FluidConduitTier.bySerializedName(tierName))));

    private final FluidConduitTier tier;

    public enum ConnectionType implements StringRepresentable {
        NONE("none"),
        PIPE("pipe"),
        MACHINE("machine"),
        PUMP("pump");

        private final String serializedName;

        ConnectionType(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public static final EnumProperty<ConnectionType> NORTH = EnumProperty.create("north", ConnectionType.class);
    public static final EnumProperty<ConnectionType> EAST = EnumProperty.create("east", ConnectionType.class);
    public static final EnumProperty<ConnectionType> SOUTH = EnumProperty.create("south", ConnectionType.class);
    public static final EnumProperty<ConnectionType> WEST = EnumProperty.create("west", ConnectionType.class);
    public static final EnumProperty<ConnectionType> UP = EnumProperty.create("up", ConnectionType.class);
    public static final EnumProperty<ConnectionType> DOWN = EnumProperty.create("down", ConnectionType.class);

    private static final Map<Direction, EnumProperty<ConnectionType>> PROPERTY_BY_DIRECTION = Map.of(
            Direction.NORTH, NORTH,
            Direction.EAST, EAST,
            Direction.SOUTH, SOUTH,
            Direction.WEST, WEST,
            Direction.UP, UP,
            Direction.DOWN, DOWN
    );

    private static final VoxelShape[] SHAPE_CACHE = new VoxelShape[64];
    private static final VoxelShape CORE_SHAPE = Block.box(6, 6, 6, 10, 10, 10);
    /** Extra reach so arms touch the 14px fluid pump model (1px inset). */
    private static final Map<Direction, VoxelShape> PUMP_EXTENSIONS = Map.of(
            Direction.NORTH, Block.box(5, 5, -1, 11, 11, 0),
            Direction.SOUTH, Block.box(5, 5, 16, 11, 11, 17),
            Direction.WEST, Block.box(-1, 5, 5, 0, 11, 11),
            Direction.EAST, Block.box(16, 5, 5, 17, 11, 11),
            Direction.DOWN, Block.box(5, -1, 5, 11, 0, 11),
            Direction.UP, Block.box(5, 16, 5, 11, 17, 11));
    private static final Map<Direction, VoxelShape> ARM_SHAPES = Map.of(
            Direction.NORTH, Block.box(6, 6, 0, 10, 10, 6),
            Direction.SOUTH, Block.box(6, 6, 10, 10, 10, 16),
            Direction.WEST, Block.box(0, 6, 6, 6, 10, 10),
            Direction.EAST, Block.box(10, 6, 6, 16, 10, 10),
            Direction.DOWN, Block.box(6, 0, 6, 10, 6, 10),
            Direction.UP, Block.box(6, 10, 6, 10, 16, 10)
    );

    public FluidPipeBlock(Properties properties, FluidConduitTier tier) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, ConnectionType.NONE)
                .setValue(EAST, ConnectionType.NONE)
                .setValue(SOUTH, ConnectionType.NONE)
                .setValue(WEST, ConnectionType.NONE)
                .setValue(UP, ConnectionType.NONE)
                .setValue(DOWN, ConnectionType.NONE));
    }

    public static EnumProperty<ConnectionType> property(Direction dir) {
        return PROPERTY_BY_DIRECTION.get(dir);
    }

    public FluidConduitTier getTier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), getConnectionType(level, pos, dir));
        }
        return state;
    }

    /** Recomputes the arm toward {@code direction}, dropping cached pipe routes when the connection pattern changes. */
    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        ConnectionType type = getConnectionType(level, pos, direction);
        if (type != state.getValue(PROPERTY_BY_DIRECTION.get(direction))
                && level instanceof Level serverLevel && !serverLevel.isClientSide) {
            FluidPipeBlockEntity.invalidateNetwork(serverLevel);
        }
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), type);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide) {
            return;
        }
        BlockState updated = state;
        for (Direction dir : Direction.values()) {
            updated = updated.setValue(PROPERTY_BY_DIRECTION.get(dir), getConnectionType(level, pos, dir));
        }
        if (!updated.equals(state)) {
            FluidPipeBlockEntity.invalidateNetwork(level);
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
    }

    private ConnectionType getConnectionType(LevelReader level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        if (level.getBlockState(neighborPos).getBlock() instanceof FluidPipeBlock) {
            return ConnectionType.PIPE;
        }
        if (level.getBlockState(neighborPos).getBlock() instanceof FluidPumpBlock) {
            return ConnectionType.PUMP;
        }
        if (level instanceof Level realLevel) {
            if (realLevel.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, direction.getOpposite()) != null) {
                return ConnectionType.MACHINE;
            }
        }
        return ConnectionType.NONE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        Direction[] dirs = Direction.values();
        for (int i = 0; i < dirs.length; i++) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(dirs[i])) != ConnectionType.NONE) {
                mask |= 1 << i;
            }
        }
        VoxelShape cached = SHAPE_CACHE[mask];
        if (cached == null) {
            VoxelShape shape = CORE_SHAPE;
            for (int i = 0; i < dirs.length; i++) {
                if ((mask & (1 << i)) != 0) {
                    shape = Shapes.or(shape, ARM_SHAPES.get(dirs[i]));
                }
            }
            cached = shape.optimize();
            SHAPE_CACHE[mask] = cached;
        }
        return cached;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(pos, state);
    }

    /** Placing a pipe changes the network shape, so every cached pipe route is dropped. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            FluidPipeBlockEntity.invalidateNetwork(level);
        }
    }

    /** Removing a pipe changes the network shape, so every cached pipe route is dropped. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide) {
            FluidPipeBlockEntity.invalidateNetwork(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(
                type,
                ModBlockEntities.FLUID_PIPE.get(),
                FluidPipeBlockEntity::serverTick
        );
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            pipe.returnFluidToNeighbors(level, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
