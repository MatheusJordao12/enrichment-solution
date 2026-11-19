package com.enrichmentsolution.mod.common.block;

import com.enrichmentsolution.mod.common.block.entity.BeltDriveBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.ConveyorBeltBlockEntity;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Multiblock conveyor belt that connects in a line along {@link #FACING},
 * receives power from any adjacent {@link BeltDriveBlock} along the line,
 * and transports {@link net.minecraft.world.entity.item.ItemEntity} instances.
 */
public class ConveyorBeltBlock extends BaseEntityBlock {

    public static final MapCodec<ConveyorBeltBlock> CODEC = simpleCodec(ConveyorBeltBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BeltPart> PART = EnumProperty.create("part", BeltPart.class);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public static final int MAX_LINE_LENGTH = 64;
    private static final VoxelShape SHAPE = Block.box(0.0D, 1.0D, 0.0D, 16.0D, 15.0D, 16.0D);

    public ConveyorBeltBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, BeltPart.SINGLE)
                .setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BeltPart part = computePart(level, pos, facing);
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(PART, part)
                .setValue(ACTIVE, false);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (direction.getAxis().isHorizontal()) {
            BeltPart part = computePart(level, pos, state.getValue(FACING));
            if (part != state.getValue(PART)) {
                return state.setValue(PART, part);
            }
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(state.getBlock())) {
            updateConnectedLine(level, pos);
        }
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
        if (!level.isClientSide) {
            updateConnectedLine(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            Direction facing = state.getValue(FACING);
            BlockPos ahead = pos.relative(facing);
            if (level.getBlockState(ahead).getBlock() instanceof ConveyorBeltBlock) {
                updateConnectedLine(level, ahead);
            }
            BlockPos behind = pos.relative(facing.getOpposite());
            if (level.getBlockState(behind).getBlock() instanceof ConveyorBeltBlock) {
                updateConnectedLine(level, behind);
            }
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, ACTIVE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConveyorBeltBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CONVEYOR_BELT.get(), ConveyorBeltBlockEntity::serverTick);
    }

    public static BeltPart computePart(BlockGetter level, BlockPos pos, Direction facing) {
        BlockState ahead = level.getBlockState(pos.relative(facing));
        BlockState behind = level.getBlockState(pos.relative(facing.getOpposite()));
        boolean hasAhead = ahead.getBlock() instanceof ConveyorBeltBlock && ahead.getValue(FACING) == facing;
        boolean hasBehind = behind.getBlock() instanceof ConveyorBeltBlock && behind.getValue(FACING) == facing;

        if (hasAhead && hasBehind) {
            return BeltPart.MIDDLE;
        }
        if (hasAhead) {
            return BeltPart.START;
        }
        if (hasBehind) {
            return BeltPart.END;
        }
        return BeltPart.SINGLE;
    }

    /**
     * Scans up to {@link #MAX_LINE_LENGTH} connected conveyor blocks sharing the same
     * facing direction, computes each segment's {@link BeltPart}, and sets {@link #ACTIVE}
     * if any adjacent {@link BeltDriveBlock} along the line is active.
     */
    public static void updateConnectedLine(Level level, BlockPos origin) {
        if (level.isClientSide) {
            return;
        }
        BlockState originState = level.getBlockState(origin);
        if (!(originState.getBlock() instanceof ConveyorBeltBlock)) {
            return;
        }

        Direction facing = originState.getValue(FACING);
        List<BlockPos> line = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);

        boolean anyDriveActive = false;
        boolean anyDriveReverse = false;

        while (!queue.isEmpty() && line.size() < MAX_LINE_LENGTH) {
            BlockPos current = queue.poll();
            line.add(current);

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos adj = current.relative(dir);
                BlockState adjState = level.getBlockState(adj);
                if (adjState.getBlock() instanceof BeltDriveBlock && adjState.getValue(BeltDriveBlock.ACTIVE)) {
                    anyDriveActive = true;
                    if (level.getBlockEntity(adj) instanceof BeltDriveBlockEntity drive && drive.isReverse()) {
                        anyDriveReverse = true;
                    }
                }
            }

            BlockPos ahead = current.relative(facing);
            if (!visited.contains(ahead)) {
                BlockState aheadState = level.getBlockState(ahead);
                if (aheadState.getBlock() instanceof ConveyorBeltBlock && aheadState.getValue(FACING) == facing) {
                    visited.add(ahead);
                    queue.add(ahead);
                }
            }

            BlockPos behind = current.relative(facing.getOpposite());
            if (!visited.contains(behind)) {
                BlockState behindState = level.getBlockState(behind);
                if (behindState.getBlock() instanceof ConveyorBeltBlock && behindState.getValue(FACING) == facing) {
                    visited.add(behind);
                    queue.add(behind);
                }
            }
        }

        for (BlockPos beltPos : line) {
            BlockState beltState = level.getBlockState(beltPos);
            if (!(beltState.getBlock() instanceof ConveyorBeltBlock)) {
                continue;
            }
            BeltPart newPart = computePart(level, beltPos, facing);
            if (beltState.getValue(PART) != newPart || beltState.getValue(ACTIVE) != anyDriveActive) {
                level.setBlock(
                        beltPos,
                        beltState.setValue(PART, newPart).setValue(ACTIVE, anyDriveActive),
                        Block.UPDATE_ALL
                );
            }
            if (level.getBlockEntity(beltPos) instanceof ConveyorBeltBlockEntity beltBe) {
                beltBe.setReverse(anyDriveReverse);
            }
        }
    }
}
