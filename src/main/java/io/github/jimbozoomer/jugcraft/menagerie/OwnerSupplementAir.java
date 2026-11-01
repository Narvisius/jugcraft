package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;

/** Source bellows periods and airflow; native authority bounds all environmental work. */
final class OwnerSupplementAir {
    private OwnerSupplementAir(){}
    static void tick(ServerLevel l,OwnerSupplementData d){
        var p=d.getBlockPos();int power=l.getBestNeighborSignal(p);long time=l.getGameTime();
        if(power!=d.power){d.power=power;d.changed();}
        boolean manual=d.pulseUntil>time; if(power==0&&!manual)return;
        int drive=power==0?7:power,period=78-(drive-1)*3;boolean blowing=manual||Math.sin(2*Math.PI*(time%period)/period)>0;
        var facing=HouseholdBlock.facing(d.getBlockState());var actor=Authority.present(l,d.owner);
        if(actor==null||!Authority.mayChange(l,actor,p))return;
        var front=p.relative(facing);if(!l.hasChunkAt(front)||!Authority.mayChange(l,actor,front))return;
        if(time%Math.max(1,10-drive/2)==0&&l.getFluidState(front).isEmpty()&&l.getBlockEntity(front) instanceof AbstractFurnaceBlockEntity furnace){
            // An additional real furnace tick consumes fuel and obeys its normal recipe/output rules.
            var state=l.getBlockState(front);var ticker=state.getTicker(l,furnace.getType());if(ticker!=null)tickFurnace(ticker,l,front,state,furnace);
        }
        if(!blowing)return;
        int reach=0;for(int i=1;i<=5;i++){
            var q=p.relative(facing,i);if(!l.hasChunkAt(q)||!Authority.mayChange(l,actor,q))break;
            var state=l.getBlockState(q);if(Block.canSupportCenter(l,q,facing.getOpposite()))break;reach=i;
            if(state.getBlock() instanceof FireBlock&&time%(15L*i)==0)l.setBlock(q,state.setValue(FireBlock.AGE,Math.max(0,state.getValue(FireBlock.AGE)-7)),Block.UPDATE_CLIENTS);
        }
        if(reach==0)return;
        if(time%5==0){var center=net.minecraft.world.phys.Vec3.atCenterOf(front);l.sendParticles(l.getFluidState(front).is(net.minecraft.tags.FluidTags.WATER)?ParticleTypes.BUBBLE:ParticleTypes.CLOUD,center.x,center.y,center.z,1,.06,.06,.06,.015);}
        if(!l.getFluidState(front).isEmpty())return;
        var box=new AABB(front).minmax(new AABB(p.relative(facing,reach)));var dir=Vec3.atLowerCornerOf(facing.getUnitVec3i());int budget=32;
        var nearby=new java.util.ArrayList<Entity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(Entity.class),box,e->e.isAlive()&&!e.isSpectator(),nearby,32);
        for(var e:nearby){
            if(budget--<=0)break;if(e.isSpectator()||e.isPassenger()||!Authority.mayChange(l,actor,e.blockPosition()))continue;
            if(e instanceof LivingEntity living&&e!=actor&&!io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(actor,l,living))continue;
            double speed=e.getDeltaMovement().dot(dir),cap=facing==Direction.UP?1:2;if(speed>=cap)continue;
            double distance=Math.max(0,e.position().subtract(net.minecraft.world.phys.Vec3.atCenterOf(front)).dot(dir));double push=Math.min(cap-speed,5D/period*Math.max(0,(5-distance)/5));e.push(dir.x*push,dir.y*push,dir.z*push);e.syncVelocity=true;
        }
        if(time%period==1||manual&&d.pulseUntil-time==9)l.playSound(null,p,OwnerSupplementaries.SOUNDS.get("bellows_blow"),net.minecraft.sounds.SoundSource.BLOCKS,.1F,.85F+.6F*drive/15F);
    }
    @SuppressWarnings({"unchecked","rawtypes"}) private static void tickFurnace(BlockEntityTicker ticker,ServerLevel l,BlockPos p,net.minecraft.world.level.block.state.BlockState s,AbstractFurnaceBlockEntity furnace){ticker.tick(l,p,s,furnace);}
}
