package io.github.jimbozoomer.jugcraft.menagerie;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.Vec3;

/** Native path volumes for lava, acid and soda swimmers; no water-only goal assumptions. */
final class LavaSwimming {
    private LavaSwimming(){}
    static boolean species(Menagerie.Species s){return s.aquatic()&&(s.habitat().equals("nether")||s.source().equals("radgill")||s.source().equals("sweetish_fish"));}
    static boolean fluid(Menagerie.Species s,net.minecraft.world.level.material.FluidState fluid){
        if(s.habitat().equals("nether"))return fluid.is(FluidTags.LAVA);
        if(fluid.is(FluidTags.WATER))return true;
        String name=s.source().equals("radgill")?"cavern_acid":s.source().equals("sweetish_fish")?"cavern_purple_soda":"";var entry=io.github.jimbozoomer.jugcraft.chemistry.PetroFluids.FLUIDS.get(name);return entry!=null&&entry.source().isSame(fluid.getType());
    }
    private static boolean home(Mob mob,net.minecraft.world.level.material.FluidState state){return mob instanceof MenagerieAnimal a&&fluid(a.species(),state);}
    static final class Navigation extends WaterBoundPathNavigation {
        Navigation(Mob mob,Level level){super(mob,level);}
        @Override protected PathFinder createPathFinder(int limit){
            nodeEvaluator=new SwimNodeEvaluator(false){
                @Override public PathType getPathType(PathfindingContext c,int x,int y,int z){return home(mob,c.getBlockState(new BlockPos(x,y,z)).getFluidState())?PathType.WATER:PathType.BLOCKED;}
                @Override public PathType getPathTypeOfMob(PathfindingContext c,int x,int y,int z,Mob creature){
                    for(int dx=0;dx<Math.ceil(creature.getBbWidth());dx++)for(int dy=0;dy<Math.ceil(creature.getBbHeight());dy++)for(int dz=0;dz<Math.ceil(creature.getBbWidth());dz++)if(getPathType(c,x+dx,y+dy,z+dz)!=PathType.WATER)return PathType.BLOCKED;
                    return PathType.WATER;
                }
            };return new PathFinder(nodeEvaluator,limit);
        }
        @Override protected boolean canUpdatePath(){return home(mob,level.getFluidState(mob.blockPosition()));}
        @Override public boolean isStableDestination(BlockPos at){return home(mob,level.getFluidState(at));}
        @Override protected boolean canMoveDirectly(Vec3 from,Vec3 to){return false;}
    }
    static final class Control extends MoveControl<Mob> {
        Control(Mob mob){super(mob);}
        @Override public void tick(){
            if(operation!=Operation.MOVE_TO||mob.getNavigation().isDone()){mob.setSpeed(0);return;}
            Vec3 delta=new Vec3(wantedX-mob.getX(),wantedY-mob.getY(),wantedZ-mob.getZ());if(delta.lengthSqr()<.04){mob.setSpeed(0);return;}
            mob.setYRot(rotlerp(mob.getYRot(),(float)(Math.atan2(delta.z,delta.x)*180/Math.PI)-90,10));mob.yBodyRot=mob.getYRot();mob.setSpeed((float)(speedModifier*.25));
            mob.setDeltaMovement(mob.getDeltaMovement().add(delta.normalize().scale(.025*speedModifier)));
        }
    }
    static final class Wander extends Goal {
        private final Mob mob;private BlockPos target;
        Wander(Mob mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){
            if(mob.isVehicle()||mob.getTarget()!=null||!mob.getNavigation().isDone()||mob.getRandom().nextInt(40)!=0)return false;
            for(int i=0;i<8;i++){var at=mob.blockPosition().offset(mob.getRandom().nextInt(17)-8,mob.getRandom().nextInt(7)-3,mob.getRandom().nextInt(17)-8);if(mob.level().hasChunkAt(at)&&home(mob,mob.level().getFluidState(at))){target=at;return true;}}return false;
        }
        @Override public void start(){mob.getNavigation().moveTo(target.getX()+.5,target.getY()+.5,target.getZ()+.5,1);}
        @Override public boolean canContinueToUse(){return !mob.getNavigation().isDone()&&!mob.isVehicle();}
    }
}
