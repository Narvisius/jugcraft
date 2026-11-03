package io.github.jimbozoomer.jugcraft.menagerie;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/** Imported families include both waterloggable and dry-only source definitions. */
interface OptionalWaterloggedBlock extends SimpleWaterloggedBlock {
    @Override default boolean canPlaceLiquid(LivingEntity actor, BlockGetter level, BlockPos pos,
                                            BlockState state, Fluid fluid) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED)
            && SimpleWaterloggedBlock.super.canPlaceLiquid(actor, level, pos, state, fluid);
    }

    @Override default boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        // StructureTemplate calls this directly, without checking canPlaceLiquid first.
        return state.hasProperty(BlockStateProperties.WATERLOGGED)
            && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluid);
    }

    @Override default ItemStack pickupBlock(LivingEntity actor, LevelAccessor level, BlockPos pos, BlockState state) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED)
            ? SimpleWaterloggedBlock.super.pickupBlock(actor, level, pos, state) : ItemStack.EMPTY;
    }
}
