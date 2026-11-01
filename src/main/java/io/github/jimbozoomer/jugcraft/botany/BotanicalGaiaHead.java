package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.*;

/** Floor and wall trophy retain native skull attachment and rotation behavior. */
public final class BotanicalGaiaHead extends BotanicalBlock {
    BotanicalGaiaHead(String kind,Properties p){super(kind,p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.ROTATION_16,0).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.ROTATION_16,BlockStateProperties.HORIZONTAL_FACING);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        if(!kind.equals("gaia_wall_head"))return defaultBlockState().setValue(BlockStateProperties.ROTATION_16,net.minecraft.util.Mth.floor(c.getRotation()*16/360+.5)&15);
        for(var direction:c.getNearestLookingDirections())if(direction.getAxis().isHorizontal()){
            var s=defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,direction.getOpposite());if(s.canSurvive(c.getLevel(),c.getClickedPos()))return s;
        }return null;
    }
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos at){var support=kind.equals("gaia_wall_head")?s.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite():Direction.DOWN;return l.getBlockState(at.relative(support)).isFaceSturdy(l,at.relative(support),support.getOpposite());}
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos at,Direction d,BlockPos neighbor,BlockState n,RandomSource r){return s.canSurvive(l,at)?s:Blocks.AIR.defaultBlockState();}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos at,CollisionContext c){
        if(!kind.equals("gaia_wall_head"))return Block.box(4,0,4,12,8,12);
        return switch(s.getValue(BlockStateProperties.HORIZONTAL_FACING)){case NORTH->Block.box(4,4,8,12,12,16);case SOUTH->Block.box(4,4,0,12,12,8);case WEST->Block.box(8,4,4,16,12,12);default->Block.box(0,4,4,8,12,12);};
    }
}
