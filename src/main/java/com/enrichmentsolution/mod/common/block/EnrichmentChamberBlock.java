package com.enrichmentsolution.mod.common.block;

import com.enrichmentsolution.mod.common.block.entity.EnrichmentChamberBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class EnrichmentChamberBlock extends BaseEntityBlock {

    public enum QuadRole implements StringRepresentable {
        NONE("none"),
        MASTER("master"),
        RIGHT("right"),
        TOP_LEFT("top_left"),
        TOP_RIGHT("top_right");

        private final String name;

        QuadRole(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<EnrichmentChamberBlock> CODEC = simpleCodec(EnrichmentChamberBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final EnumProperty<QuadRole> QUAD_ROLE = EnumProperty.create("quad_role", QuadRole.class);

    public EnrichmentChamberBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false)
                .setValue(QUAD_ROLE, QuadRole.NONE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, QUAD_ROLE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(QUAD_ROLE) != QuadRole.NONE ? 1.0F : super.getShadeBrightness(state, level, pos);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(QUAD_ROLE) != QuadRole.NONE || super.propagatesSkylightDown(state, level, pos);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(QUAD_ROLE) != QuadRole.NONE ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        QuadRole role = state.getValue(QUAD_ROLE);
        if (role == QuadRole.NONE) {
            return super.getShape(state, level, pos, context);
        }
        Direction right = state.getValue(FACING).getClockWise();
        BlockPos masterPos = getMasterPos(pos, state);
        int dx = masterPos.getX() - pos.getX();
        int dy = masterPos.getY() - pos.getY();
        int dz = masterPos.getZ() - pos.getZ();

        double minX = Math.min(0.0, right.getStepX()) + dx;
        double maxX = Math.max(1.0, 1.0 + right.getStepX()) + dx;
        double minY = 0.0 + dy;
        double maxY = 2.0 + dy;
        double minZ = Math.min(0.0, right.getStepZ()) + dz;
        double maxZ = Math.max(1.0, 1.0 + right.getStepZ()) + dz;
        return Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnrichmentChamberBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(
                type,
                ModBlockEntities.ENRICHMENT_CHAMBER.get(),
                AbstractMachineBlockEntity::serverTick
        );
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && state.getValue(QUAD_ROLE) == QuadRole.NONE) {
            tryFormQuad(level, pos, state);
        }
    }

    private void tryFormQuad(Level level, BlockPos placedPos, BlockState placedState) {
        Direction facing = placedState.getValue(FACING);
        Direction right = facing.getClockWise();
        Direction left = right.getOpposite();

        BlockPos[] candidates = new BlockPos[]{
                placedPos,
                placedPos.relative(left),
                placedPos.below(),
                placedPos.below().relative(left)
        };

        for (BlockPos masterPos : candidates) {
            BlockPos rightPos = masterPos.relative(right);
            BlockPos topLeftPos = masterPos.above();
            BlockPos topRightPos = masterPos.above().relative(right);

            if (isUnformedChamber(level, masterPos, facing)
                    && isUnformedChamber(level, rightPos, facing)
                    && isUnformedChamber(level, topLeftPos, facing)
                    && isUnformedChamber(level, topRightPos, facing)) {

                if (level.getBlockEntity(masterPos) instanceof EnrichmentChamberBlockEntity masterBE) {
                    for (BlockPos slavePos : new BlockPos[]{rightPos, topLeftPos, topRightPos}) {
                        if (level.getBlockEntity(slavePos) instanceof EnrichmentChamberBlockEntity slaveBE) {
                            masterBE.absorbFromSlave(slaveBE);
                        }
                    }
                    level.setBlock(masterPos, level.getBlockState(masterPos).setValue(QUAD_ROLE, QuadRole.MASTER), Block.UPDATE_ALL);
                    level.setBlock(rightPos, level.getBlockState(rightPos).setValue(QUAD_ROLE, QuadRole.RIGHT).setValue(LIT, false), Block.UPDATE_ALL);
                    level.setBlock(topLeftPos, level.getBlockState(topLeftPos).setValue(QUAD_ROLE, QuadRole.TOP_LEFT).setValue(LIT, false), Block.UPDATE_ALL);
                    level.setBlock(topRightPos, level.getBlockState(topRightPos).setValue(QUAD_ROLE, QuadRole.TOP_RIGHT).setValue(LIT, false), Block.UPDATE_ALL);

                    masterBE.onFormQuad(level);

                    level.playSound(null, masterPos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.9F, 0.85F);
                    level.playSound(null, masterPos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.65F, 1.25F);
                    if (level instanceof ServerLevel serverLevel) {
                        double cx = (masterPos.getX() + rightPos.getX()) / 2.0 + 0.5;
                        double cy = masterPos.getY() + 1.0;
                        double cz = (masterPos.getZ() + rightPos.getZ()) / 2.0 + 0.5;
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 32, 0.8, 0.8, 0.8, 0.1);
                        serverLevel.sendParticles(ParticleTypes.END_ROD, cx, cy, cz, 18, 0.6, 0.6, 0.6, 0.05);
                    }
                }
                return;
            }
        }
    }

    private boolean isUnformedChamber(Level level, BlockPos pos, Direction facing) {
        BlockState s = level.getBlockState(pos);
        return s.is(this) && s.getValue(QUAD_ROLE) == QuadRole.NONE && s.getValue(FACING) == facing;
    }

    public static BlockPos getMasterPos(BlockPos pos, BlockState state) {
        Direction right = state.getValue(FACING).getClockWise();
        return switch (state.getValue(QUAD_ROLE)) {
            case RIGHT -> pos.relative(right.getOpposite());
            case TOP_LEFT -> pos.below();
            case TOP_RIGHT -> pos.below().relative(right.getOpposite());
            default -> pos;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockPos targetPos = state.getValue(QUAD_ROLE) != QuadRole.NONE ? getMasterPos(pos, state) : pos;
            if (level.getBlockEntity(targetPos) instanceof EnrichmentChamberBlockEntity chamber) {
                player.openMenu(chamber, targetPos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            QuadRole role = state.getValue(QUAD_ROLE);
            if (!level.isClientSide && role != QuadRole.NONE) {
                BlockPos masterPos = getMasterPos(pos, state);
                Direction right = state.getValue(FACING).getClockWise();
                if (level.getBlockEntity(masterPos) instanceof EnrichmentChamberBlockEntity masterBE) {
                    masterBE.onUnformQuad(level, pos);
                }
                BlockPos[] all = new BlockPos[]{
                        masterPos,
                        masterPos.relative(right),
                        masterPos.above(),
                        masterPos.above().relative(right)
                };
                for (BlockPos memberPos : all) {
                    if (!memberPos.equals(pos)) {
                        BlockState ms = level.getBlockState(memberPos);
                        if (ms.is(this) && ms.getValue(QUAD_ROLE) != QuadRole.NONE) {
                            level.setBlock(memberPos, ms.setValue(QUAD_ROLE, QuadRole.NONE).setValue(LIT, false), Block.UPDATE_ALL);
                        }
                    }
                }
            }
            if (level.getBlockEntity(pos) instanceof AbstractMachineBlockEntity machine) {
                machine.dropContents(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
