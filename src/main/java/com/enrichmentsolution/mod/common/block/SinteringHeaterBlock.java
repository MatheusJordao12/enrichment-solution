package com.enrichmentsolution.mod.common.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Sintering Heater module for the Pelletizer 3x3x4 multiblock.
 * Lights up (lightLevel 12) when the Pelletizer is actively processing.
 */
public class SinteringHeaterBlock extends Block {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public SinteringHeaterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }
}
