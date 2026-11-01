package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

final class OwnerHolidayPlants {
    private OwnerHolidayPlants() { }
    static final class Snow extends SnowLayerBlock {
        Snow(Properties p) { super(p); }
        @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { }
    }
    static final class Tall extends DoublePlantBlock {
        Tall(Properties p) { super(p); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            if (context.getLevel() instanceof ServerLevel level && context.getPlayer() != null
                    && !Authority.mayChange(level, context.getPlayer(), context.getClickedPos().above())) return null;
            return super.getStateForPlacement(context);
        }
    }
    static final class Grass extends TallGrassBlock {
        private final String tall;
        Grass(Properties p, String id) { super(p); tall = id.replace("snowy_", "snowy_tall_"); }
        @Override public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
            return level.getBlockState(pos.above()).isAir();
        }
        @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state,
                BonemealSource source) {
            Block target = OwnerBuildingCollection.BLOCKS.get(tall);
            if (target instanceof DoublePlantBlock && level.getBlockState(pos.above()).isAir()
                    && target.defaultBlockState().canSurvive(level, pos))
                DoublePlantBlock.placeAt(level, target.defaultBlockState(), pos, Block.UPDATE_ALL);
        }
    }
}
