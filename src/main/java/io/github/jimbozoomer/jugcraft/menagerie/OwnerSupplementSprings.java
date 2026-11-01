package io.github.jimbozoomer.jugcraft.menagerie;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Original two-tick spring stroke, with a reserved physical head and owner-checked impulse. */
final class OwnerSupplementSprings {
    private OwnerSupplementSprings(){}
    static net.minecraft.world.phys.shapes.VoxelShape shape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos){
        var facing=HouseholdBlock.facing(state);double age=level.getBlockEntity(pos) instanceof OwnerSupplementData d&&d.getLevel()!=null?Math.clamp(d.getLevel().getGameTime()-d.pulseUntil,0,2):0;
        double progress=age*.5,offset=HouseholdBlock.flag(state,"extending")?progress-1:-progress;
        var headBlock=OwnerSupplementaries.BLOCKS.get("spring_launcher_head");
        if(headBlock==null)return net.minecraft.world.phys.shapes.Shapes.empty(); // Registration-time shape inspection only.
        var head=HouseholdBlock.set(headBlock.defaultBlockState(),"facing",facing.getName());
        return head.getShape(level,pos).move(facing.getStepX()*offset,facing.getStepY()*offset,facing.getStepZ()*offset);
    }
    static boolean part(BlockState s){return s.is(OwnerSupplementaries.BLOCKS.get("spring_launcher_arm"))||s.is(OwnerSupplementaries.BLOCKS.get("spring_launcher_head"));}
    static void tick(ServerLevel l,OwnerSupplementData d){
        var p=d.getBlockPos();var facing=HouseholdBlock.facing(d.getBlockState());var target=p.relative(facing);String kind=d.block().function;
        if(kind.equals("spring")){
            if(!l.hasChunkAt(target))return;var front=l.getBlockState(target);boolean extended=HouseholdBlock.flag(d.getBlockState(),"extended");
            if(extended&&!part(front)){l.setBlockAndUpdate(p,HouseholdBlock.set(d.getBlockState(),"extended","false"));extended=false;}
            var actor=Authority.present(l,d.owner);if(actor==null||!Authority.mayChange(l,actor,p)||!Authority.mayChange(l,actor,target))return;
            boolean powered=false;for(var dir:Direction.values())if(dir!=facing&&l.hasSignal(p.relative(dir),dir)){powered=true;break;}
            if(powered&&!extended){
                if(facing==Direction.UP&&front.getBlock() instanceof FallingBlock&&l.getBlockEntity(target)==null){FallingBlockEntity.fall(l,target,front);front=l.getBlockState(target);}
                // Never replace a player object, water source or block entity to create the moving part.
                if(!front.isAir())return;
                l.setBlockAndUpdate(p,HouseholdBlock.set(d.getBlockState(),"extended","true"));start(l,target,facing,true,d.owner);
                l.playSound(null,p,net.minecraft.sounds.SoundEvents.PISTON_EXTEND,net.minecraft.sounds.SoundSource.BLOCKS,.53F,.55F);
            }else if(!powered&&extended&&front.is(OwnerSupplementaries.BLOCKS.get("spring_launcher_head"))&&HouseholdBlock.facing(front)==facing){
                start(l,target,facing,false,d.owner);l.playSound(null,p,net.minecraft.sounds.SoundEvents.PISTON_CONTRACT,net.minecraft.sounds.SoundSource.BLOCKS,.53F,.55F);
            }
            return;
        }
        var base=p.relative(facing.getOpposite());if(!l.hasChunkAt(base))return;var baseState=l.getBlockState(base);
        if(!baseState.is(OwnerSupplementaries.BLOCKS.get("spring_launcher"))||HouseholdBlock.facing(baseState)!=facing||!HouseholdBlock.flag(baseState,"extended")){l.removeBlock(p,false);return;}
        if(!kind.equals("spring_arm"))return;
        boolean extending=HouseholdBlock.flag(d.getBlockState(),"extending");long age=Math.max(0,l.getGameTime()-d.pulseUntil);
        var actor=Authority.present(l,d.owner);if(actor==null||!Authority.mayChange(l,actor,p)||!Authority.mayChange(l,actor,base))return;
        if(age>=3){
            if(extending)l.setBlockAndUpdate(p,HouseholdBlock.set(OwnerSupplementaries.BLOCKS.get("spring_launcher_head").defaultBlockState(),"facing",facing.getName()));
            else{l.removeBlock(p,false);l.setBlockAndUpdate(base,HouseholdBlock.set(baseState,"extended","false"));}return;
        }
        if(!extending)return;
        double offset=Math.min(0,-1+age*.5);var dir=Vec3.atLowerCornerOf(facing.getUnitVec3i());var box=new AABB(p).move(dir.scale(offset));int budget=32;
        var nearby=new java.util.ArrayList<Entity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(Entity.class),box,e->e.isAlive()&&!e.isSpectator(),nearby,32);
        for(var entity:nearby){
            if(budget--<=0)break;if(entity.isSpectator()||entity.isPassenger()||entity.getPistonPushReaction()==net.minecraft.world.level.material.PushReaction.IGNORE_ENTITY||!Authority.mayChange(l,actor,entity.blockPosition()))continue;
            if(entity instanceof LivingEntity living&&entity!=actor&&!io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(actor,l,living))continue;
            var motion=entity.getDeltaMovement();entity.setDeltaMovement(facing.getAxis()==Direction.Axis.X?dir.x*1.5:motion.x,facing.getAxis()==Direction.Axis.Y?dir.y*1.5:motion.y,facing.getAxis()==Direction.Axis.Z?dir.z*1.5:motion.z);
            entity.move(MoverType.PISTON,dir.scale(.51));entity.syncVelocity=true;
        }
        l.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,p.getX()+.5+dir.x*offset,p.getY()+.5+dir.y*offset,p.getZ()+.5+dir.z*offset,2,.3,.3,.3,.02);
    }
    private static void start(ServerLevel l,BlockPos p,Direction facing,boolean extending,java.util.UUID owner){
        var state=HouseholdBlock.set(HouseholdBlock.set(OwnerSupplementaries.BLOCKS.get("spring_launcher_arm").defaultBlockState(),"facing",facing.getName()),"extending",Boolean.toString(extending));
        l.setBlockAndUpdate(p,state);if(l.getBlockEntity(p) instanceof OwnerSupplementData arm){arm.owner=owner;arm.pulseUntil=l.getGameTime();arm.changed();}
    }
}
