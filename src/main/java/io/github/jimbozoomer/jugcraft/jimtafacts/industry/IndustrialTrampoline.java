package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** Source 1/8-block rubber sheet, adapted to the platform's native restitution and crouch handling. */
public final class IndustrialTrampoline extends Block {
    private static final VoxelShape MAT=Block.box(0,0,0,16,2,16);
    public IndustrialTrampoline(Properties p){super(p);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return MAT;}
    @Override public void fallOn(Level level,BlockState state,BlockPos pos,Entity entity,double distance){
        if(entity.isSuppressingBounce())entity.causeFallDamage(distance,1,level.damageSources().fall());
        else entity.causeFallDamage(distance,0,level.damageSources().fall());
    }
    @Override public void bounceOn(Level level,BlockState state,BlockPos pos,Entity entity,double distance){
        if(!level.isClientSide())level.playSound(null,pos,IndustrialConstruction.RUBBER_BOUNCE,net.minecraft.sounds.SoundSource.BLOCKS,.6F,1F);
    }
}
