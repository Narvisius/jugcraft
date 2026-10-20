package io.github.jimbozoomer.jugcraft.menagerie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.redstone.Orientation;

/** Native counterparts for stateful supplied meshes; keeps their visual variants usable. */
final class OwnerBuildingStates {
    static final BooleanProperty ACTIVE=io.github.jimbozoomer.jugcraft.diagonal.PoweredDiagonalWallBlock.ACTIVE;
    private OwnerBuildingStates(){}
    private static BlockState powered(BlockState s,Level level,BlockPos at){return s==null?null:s.setValue(ACTIVE,level.hasNeighborSignal(at));}
    private static void refresh(BlockState s,Level level,BlockPos at){
        if(!s.hasProperty(ACTIVE))return;
        if(!level.isClientSide()){var next=powered(s,level,at);if(next!=s)level.setBlock(at,next,Block.UPDATE_ALL);}
    }
    static final class ActiveStair extends StairBlock {
        ActiveStair(Properties p){super(Blocks.STONE.defaultBlockState(),p.lightLevel(s->s.getValue(ACTIVE)?6:0));registerDefaultState(defaultBlockState().setValue(ACTIVE,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(ACTIVE);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return powered(super.getStateForPlacement(c),c.getLevel(),c.getClickedPos());}
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,Orientation o,boolean moving){super.neighborChanged(s,l,p,n,o,moving);refresh(l.getBlockState(p),l,p);}
    }
    static final class ActiveSlab extends SlabBlock {
        ActiveSlab(Properties p){super(p.lightLevel(s->s.getValue(ACTIVE)?6:0));registerDefaultState(defaultBlockState().setValue(ACTIVE,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(ACTIVE);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return powered(super.getStateForPlacement(c),c.getLevel(),c.getClickedPos());}
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,Orientation o,boolean moving){super.neighborChanged(s,l,p,n,o,moving);refresh(l.getBlockState(p),l,p);}
    }
    static final class ActiveWall extends WallBlock {
        ActiveWall(Properties p){super(p.lightLevel(s->s.getValue(ACTIVE)?6:0));registerDefaultState(defaultBlockState().setValue(ACTIVE,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(ACTIVE);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return powered(super.getStateForPlacement(c),c.getLevel(),c.getClickedPos());}
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,Orientation o,boolean moving){super.neighborChanged(s,l,p,n,o,moving);refresh(l.getBlockState(p),l,p);}
    }
    static final class ConnectedGlass extends TransparentBlock {
        ConnectedGlass(Properties p){super(p);var s=defaultBlockState();for(var property:PipeBlock.PROPERTY_BY_DIRECTION.values())s=s.setValue(property,false);registerDefaultState(s);}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);for(var property:PipeBlock.PROPERTY_BY_DIRECTION.values())b.add(property);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){var s=defaultBlockState();for(var d:Direction.values())s=s.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d),c.getLevel().getBlockState(c.getClickedPos().relative(d)).is(this));return s;}
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){
            super.onPlace(s,l,p,old,moving);if(old.is(this))return;var next=s;
            for(var d:Direction.values()){var at=p.relative(d);next=next.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d),l.hasChunkAt(at)&&l.getBlockState(at).is(this));}
            if(next!=s)l.setBlock(p,next,Block.UPDATE_ALL);
        }
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos neighbor,BlockState other,RandomSource random){return super.updateShape(s,l,ticks,p,d,neighbor,other,random).setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d),other.is(this));}
    }
    static final class FacingLeaves extends TintedParticleLeavesBlock {
        FacingLeaves(Properties p){super(.01F,p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.FACING,Direction.UP));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return super.getStateForPlacement(c).setValue(BlockStateProperties.FACING,c.getClickedFace());}
        @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(BlockStateProperties.FACING,r.rotate(s.getValue(BlockStateProperties.FACING)));}
        @Override protected BlockState mirror(BlockState s,Mirror m){return s.setValue(BlockStateProperties.FACING,m.mirror(s.getValue(BlockStateProperties.FACING)));}
    }
}
