package io.github.jimbozoomer.jugcraft.diagonal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;

/** Keeps the supplied masonry's power/light state when it joins diagonally. */
public final class PoweredDiagonalWallBlock extends DiagonalWallBlock {
    public static final BooleanProperty ACTIVE=BooleanProperty.create("active");
    public PoweredDiagonalWallBlock(WallBlock wall,Properties properties){
        super(wall,properties);registerDefaultState(defaultBlockState().setValue(ACTIVE,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        super.createBlockStateDefinition(builder);builder.add(ACTIVE);
    }
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos at,Block neighbor,Orientation orientation,boolean moving){
        super.neighborChanged(state,level,at,neighbor,orientation,moving);
        if(!level.isClientSide()){
            var current=level.getBlockState(at);
            if(current.hasProperty(ACTIVE)){
                var next=current.setValue(ACTIVE,level.hasNeighborSignal(at));
                if(next!=current)level.setBlock(at,next,Block.UPDATE_ALL);
            }
        }
    }
}
