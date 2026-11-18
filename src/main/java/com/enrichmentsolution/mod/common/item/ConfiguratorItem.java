package com.enrichmentsolution.mod.common.item;

import com.enrichmentsolution.mod.common.block.ConduitSideConfig;
import com.enrichmentsolution.mod.common.block.EnergyCableBlock;
import com.enrichmentsolution.mod.common.block.FluidPipeBlock;
import com.enrichmentsolution.mod.common.block.IConduitConfigurable;
import com.enrichmentsolution.mod.common.block.ISideConfigurableBlockEntity;
import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;

/**
 * Mekanism-style configurator: right-click a cable/pipe connection to cycle push/pull/none,
 * or a machine face to cycle input/output/none. Shift reverses the cycle.
 */
public class ConfiguratorItem extends Item {

    public ConfiguratorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        boolean reverse = player != null && player.isShiftKeyDown();

        if (be instanceof IConduitConfigurable conduit) {
            Direction arm = findArm(ctx.getClickLocation().subtract(Vec3.atLowerCornerOf(pos)), ctx.getClickedFace());
            if (!isMachineConnection(state, arm)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                ConduitSideConfig cfg = conduit.getConduitConfig();
                ConduitSideConfig.Mode mode = reverse ? cfg.get(arm).previous() : cfg.get(arm).next();
                cfg.set(arm, mode);
                conduit.onConduitConfigChanged();
                level.invalidateCapabilities(pos);
                level.sendBlockUpdated(pos, state, state, 3);
                message(player, Component.translatable("message.enrichment_solution.configurator.conduit",
                        Component.translatable("direction." + arm.getName()),
                        Component.translatable("mode.enrichment_solution.conduit." + mode.getKey())));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (be instanceof ISideConfigurableBlockEntity machine) {
            Direction facing = state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                    ? state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                    : Direction.NORTH;
            RelativeSide side = RelativeSide.fromWorldDirection(ctx.getClickedFace(), facing);
            if (!level.isClientSide) {
                machine.cycleRelativeSideMode(side.ordinal());
                if (reverse) {
                    machine.cycleRelativeSideMode(side.ordinal());
                    machine.cycleRelativeSideMode(side.ordinal());
                }
                SideConfigMode mode = machine.getRelativeSideMode(side);
                level.sendBlockUpdated(pos, state, state, 3);
                message(player, Component.translatable("message.enrichment_solution.configurator.machine",
                        side.getDisplayName(), mode.getKey().toUpperCase()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static void message(Player player, Component c) {
        if (player != null) {
            player.displayClientMessage(c, true);
        }
    }

    private static boolean isMachineConnection(BlockState state, Direction dir) {
        if (state.getBlock() instanceof EnergyCableBlock) {
            return state.getValue(EnergyCableBlock.property(dir)) == EnergyCableBlock.ConnectionType.MACHINE
                    || state.getValue(EnergyCableBlock.property(dir)) == EnergyCableBlock.ConnectionType.PUMP;
        }
        if (state.getBlock() instanceof FluidPipeBlock) {
            return state.getValue(FluidPipeBlock.property(dir)) == FluidPipeBlock.ConnectionType.MACHINE
                    || state.getValue(FluidPipeBlock.property(dir)) == FluidPipeBlock.ConnectionType.PUMP;
        }
        return false;
    }

    /** Picks the arm under the cursor; falls back to the clicked face when the core was hit. */
    private static Direction findArm(Vec3 local, Direction face) {
        double lo = 6.0 / 16.0, hi = 10.0 / 16.0, eps = 1.0E-4;
        if (local.x < lo - eps) return Direction.WEST;
        if (local.x > hi + eps) return Direction.EAST;
        if (local.y < lo - eps) return Direction.DOWN;
        if (local.y > hi + eps) return Direction.UP;
        if (local.z < lo - eps) return Direction.NORTH;
        if (local.z > hi + eps) return Direction.SOUTH;
        return face;
    }
}
