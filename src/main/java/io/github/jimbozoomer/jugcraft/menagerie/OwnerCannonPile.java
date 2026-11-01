package io.github.jimbozoomer.jugcraft.menagerie;

import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.shapes.*;

/** The original one-to-four cannonball piles, also used as the cannon's paid ammunition. */
final class OwnerCannonPile extends Block implements SimpleWaterloggedBlock {
    static final IntegerProperty BALLS=IntegerProperty.create("balls",1,4);
    OwnerCannonPile(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(BALLS,1).setValue(BlockStateProperties.WATERLOGGED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BALLS,BlockStateProperties.WATERLOGGED);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(BALLS)==1?box(4,0,4,12,8,12):box(0,0,0,16,s.getValue(BALLS)==4?16:8,16);}
    @Override protected boolean canBeReplaced(BlockState s,BlockPlaceContext c){return !c.isSecondaryUseActive()&&c.getItemInHand().is(asItem())&&s.getValue(BALLS)<4||super.canBeReplaced(s,c);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){var old=c.getLevel().getBlockState(c.getClickedPos());return old.is(this)?old.setValue(BALLS,Math.min(4,old.getValue(BALLS)+1)):defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).getType()==Fluids.WATER);}
    @Override protected FluidState getFluidState(BlockState s){return s.getValue(BlockStateProperties.WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(s);}
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos q,BlockState n,net.minecraft.util.RandomSource random){if(s.getValue(BlockStateProperties.WATERLOGGED))ticks.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));return super.updateShape(s,l,ticks,p,d,q,n,random);}
}
