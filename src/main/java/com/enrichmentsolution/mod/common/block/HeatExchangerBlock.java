package com.enrichmentsolution.mod.common.block;

import com.enrichmentsolution.mod.common.block.entity.HeatExchangerBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import com.enrichmentsolution.mod.registry.ModDataComponents;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * Heat Exchanger block — Stage 1 of the Geothermal Binary Cycle.
 * Reads heat from an adjacent Tank / TankPort (or internal hot-water tank)
 * to boil Liquid Isobutane into Gaseous Isobutane (1:1 mB, closed-loop).
 */
public class HeatExchangerBlock extends BaseEntityBlock {

    public static final MapCodec<HeatExchangerBlock> CODEC = simpleCodec(HeatExchangerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public HeatExchangerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        builder.add(FACING, LIT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeatExchangerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.HEAT_EXCHANGER.get(), AbstractMachineBlockEntity::serverTick);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(this)) {
            HeatExchangerBlockEntity.rebuildNetwork(level, pos, null);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof HeatExchangerBlockEntity exchanger) {
                HeatExchangerBlockEntity master = exchanger.getMasterEntity();
                HeatExchangerBlockEntity target = master != null ? master : exchanger;
                serverPlayer.openMenu(target, target.getBlockPos());
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        int waterAmount = 0;
        double waterTemp = 20.0D;
        double waterMult = 1.0D;
        int liquidAmount = 0;
        int gasAmount = 0;
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof HeatExchangerBlockEntity exchanger) {
            if (!level.isClientSide && exchanger.isMaster()) {
                FluidStack water = exchanger.getWaterTank().getFluid();
                waterAmount = water.getAmount();
                waterTemp = water.isEmpty() ? 20.0D : water.getOrDefault(ModDataComponents.FLUID_TEMPERATURE.get(), 20.0D);
                waterMult = water.isEmpty() ? 1.0D : water.getOrDefault(ModDataComponents.FLUID_MULTIPLIER.get(), 1.0D);
                liquidAmount = exchanger.getLiquidIsobutaneTank().getFluidAmount();
                gasAmount = exchanger.getGaseousIsobutaneTank().getFluidAmount();
            }
            exchanger.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            HeatExchangerBlockEntity.onBlockBroken(level, pos, waterAmount, waterTemp, waterMult, liquidAmount, gasAmount);
        }
    }
}
