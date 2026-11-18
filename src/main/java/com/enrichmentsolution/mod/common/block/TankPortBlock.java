package com.enrichmentsolution.mod.common.block;

import com.enrichmentsolution.mod.common.block.entity.TankBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.TankPortBlockEntity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Water Input or Output port block for the Geothermal Multiblock Tank.
 */
public class TankPortBlock extends BaseEntityBlock {

    public enum PortMode implements StringRepresentable {
        INPUT("input"),
        OUTPUT("output");

        public static final Codec<PortMode> CODEC = StringRepresentable.fromEnum(PortMode::values);
        private final String name;

        PortMode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<TankPortBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    propertiesCodec(),
                    PortMode.CODEC.fieldOf("mode").forGetter(TankPortBlock::getMode)
            ).apply(instance, TankPortBlock::new)
    );

    private final PortMode mode;

    public TankPortBlock(Properties properties, PortMode mode) {
        super(properties);
        this.mode = mode;
    }

    public PortMode getMode() {
        return mode;
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TankPortBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (level.getBlockEntity(pos) instanceof TankPortBlockEntity port) {
            var handler = port.getFluidHandler(hitResult.getDirection());
            if (handler != null && FluidUtil.interactWithFluidHandler(player, hand, handler)) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof TankPortBlockEntity port) {
                TankBlockEntity master = port.findConnectedMaster();
                if (master != null) {
                    serverPlayer.openMenu(master, master.getBlockPos());
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
