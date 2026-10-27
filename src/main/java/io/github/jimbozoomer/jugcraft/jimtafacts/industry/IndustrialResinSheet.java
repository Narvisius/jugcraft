package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;

/** Supplied two-pixel resin sheet: supported, non-solid and sticky rather than a full rubber cube. */
public final class IndustrialResinSheet extends Block {
    private static final VoxelShape OUTLINE=Block.box(0,0,0,16,2,16);
    private static final Vec3 SLOW=new Vec3(.6,.85,.6);
    public IndustrialResinSheet(Properties properties){super(properties);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return OUTLINE;}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP);}
    @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction side,BlockPos neighbor,BlockState other,RandomSource random){
        return side==Direction.DOWN&&!canSurvive(state,level,pos)?Blocks.AIR.defaultBlockState():state;
    }
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity,InsideBlockEffectApplier effects,boolean pastEdges){
        entity.makeStuckInBlock(state,SLOW);
        entity.fallDistance*=.75;
    }
}
