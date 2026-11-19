package com.enrichmentsolution.mod.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OxidatedPlutoniumOreBlock extends Block {

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16),
            Block.box(0, 13, 0, 16, 16, 16),
            Block.box(0, 3, 0, 5, 13, 6),
            Block.box(11, 3, 0, 16, 13, 5),
            Block.box(0, 3, 10, 6, 13, 16),
            Block.box(10, 3, 11, 16, 13, 16),
            Block.box(0, 3, 6, 4, 7, 10),
            Block.box(12, 8, 5, 16, 13, 11),
            Block.box(6, 3, 12, 10, 6, 16),
            Block.box(5, 10, 0, 11, 13, 3),
            Block.box(5, 3, 5, 11, 13, 11)
    );

    public OxidatedPlutoniumOreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }
}
