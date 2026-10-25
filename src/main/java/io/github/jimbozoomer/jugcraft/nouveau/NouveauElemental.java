package io.github.jimbozoomer.jugcraft.nouveau;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Bounded elemental secondary effects retain the original cast's authority and work ledger. */
public final class NouveauElemental {
    private static AttachmentType<Boolean> VISUAL_FANG;
    private static final List<Pulse> PULSES=new ArrayList<>();
    private record Fang(ConcordanceEffects.Context context,Vec3 at,int power,long due){}
    private static final List<Fang> FANGS=new ArrayList<>();
    private record Pulse(ConcordanceEffects.Context context,Vec3 at,int damage,int duration,boolean ice,long due){}
    private NouveauElemental(){}
    public static void register(){
        VISUAL_FANG=AttachmentRegistry.<Boolean>builder().persistent(Codec.BOOL).buildAndRegister(Jugcraft.id("nouveau_visual_fang"));
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,amount)->!(source.getDirectEntity() instanceof EvokerFangs f&&f.getAttachedOrElse(VISUAL_FANG,false)));
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var it=PULSES.iterator();it.hasNext();){var pulse=it.next();var c=pulse.context();var l=c.level();var p=ConcordanceEffects.person(c);if(p==null||!p.isAlive()||p.level()!=l||!JugNouveau.enabled()){it.remove();continue;}if(l.getGameTime()<pulse.due())continue;it.remove();if(!l.hasChunkAt(BlockPos.containing(pulse.at())))continue;
            l.sendParticles(pulse.ice()?new BlockParticleOption(ParticleTypes.BLOCK,Blocks.ICE.defaultBlockState()):ParticleTypes.FLAME,pulse.at().x,pulse.at().y,pulse.at().z,24,.7,.6,.7,.06);
            for(var target:l.getEntitiesOfClass(LivingEntity.class,AABB.ofSize(pulse.at(),4,4,4),LivingEntity::isAlive).stream().limit(12).toList()){
                var secondary=new ConcordanceEffects.Context(l,c.cause(),p,c.ledger(),c.step()+"/shards/"+pulse.due(),pulse.at());
                if(ConcordanceEffects.apply(secondary,EffectSpec.of(EffectKind.DAMAGE,Intent.HARMFUL,pulse.damage(),0),target)!=ConcordanceEffects.Result.APPLIED)continue;
                if(pulse.ice())NouveauStatuses.give(target,"freezing",pulse.duration(),0,p);else target.igniteForSeconds(3);
            }
        }});
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var it=FANGS.iterator();it.hasNext();){var fang=it.next();var c=fang.context();var l=c.level();if(l.getGameTime()<fang.due())continue;it.remove();var p=ConcordanceEffects.person(c);if(p==null||!JugNouveau.enabled()||!p.isAlive()||!l.hasChunkAt(BlockPos.containing(fang.at())))continue;
            for(var target:l.getEntitiesOfClass(LivingEntity.class,AABB.ofSize(fang.at(),1.5,2,1.5),LivingEntity::isAlive).stream().limit(4).toList())ConcordanceEffects.apply(new ConcordanceEffects.Context(l,c.cause(),p,c.ledger(),c.step()+"/fangs",fang.at()),EffectSpec.of(EffectKind.DAMAGE,Intent.HARMFUL,fang.power(),0),target);
        }});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{PULSES.clear();FANGS.clear();});
    }
    public static boolean creature(ConcordanceEffects.Context c,EffectSpec e,ServerPlayer p,LivingEntity target,String op){
        var l=c.level();int power=Math.max(1,e.magnitude()),duration=Math.max(20,e.duration());
        if(op.equals("cut")&&target instanceof Shearable shear&&shear.readyForShearing()){shear.shear(l,SoundSource.PLAYERS,new ItemStack(Items.SHEARS));return true;}
        if(op.equals("wind_shear")){if(target.onGround())return false;int height=0;for(int i=1;i<=10;i++){var at=target.blockPosition().below(i);if(!l.hasChunkAt(at)||!l.getBlockState(at).getCollisionShape(l,at).isEmpty())break;height++;}power+=height;}
        if(op.equals("crush")&&target.isInWater())power*=2;
        if(op.equals("cold_snap")){
            if(!target.isInWater()&&!NouveauStatuses.has(target,"soaked")&&!NouveauStatuses.has(target,"freezing")&&!target.hasEffect(MobEffects.SLOWNESS)&&target.getTicksFrozen()==0)return false;
            target.setTicksFrozen(Math.max(target.getTicksFrozen(),300));NouveauStatuses.give(target,"freezing",duration,3,p);eruption(c,e,target.position(),true);
        }
        if(op.equals("flare")){
            if(!target.isOnFire()&&!NouveauStatuses.has(target,"blasting"))return false;
            if(NouveauStatuses.has(target,"blasting")){power+=3*NouveauStatuses.strength(target,"blasting");target.removeEffect(NouveauStatuses.EFFECTS.get("blasting"));}
            eruption(c,e,target.position(),false);mageFire(c,e,target.blockPosition());
        }
        if(op.equals("lightning")){power+=NouveauStatuses.strength(target,"shocked")*2+(target.isInWater()||NouveauStatuses.has(target,"soaked")?3:0);NouveauStatuses.give(target,"shocked",duration,Math.min(2,NouveauStatuses.strength(target,"shocked")),p);bolt(l,target.position());}
        if(op.equals("fangs")){fangLine(c,e,p,target.position());return true;}
        if(op.equals("explosion")){l.sendParticles(ParticleTypes.EXPLOSION,target.getX(),target.getY()+.5,target.getZ(),1,0,0,0,0);blast(c,e,p,target.position(),target);}
        boolean applied=target.hurtServer(l,l.damageSources().indirectMagic(p,p),power);
        if(op.startsWith("wind_")){var motion=target.position().subtract(c.origin()).normalize().scale(.25*power).add(0,.4,0);target.setDeltaMovement(target.getDeltaMovement().add(motion));if(target instanceof ServerPlayer player)player.connection.send(new ClientboundSetEntityMotionPacket(target));l.sendParticles(ParticleTypes.CLOUD,target.getX(),target.getY()+.5,target.getZ(),16,.6,.3,.6,.04);applied=true;}
        return applied;
    }
    public static boolean block(ConcordanceEffects.Context c,EffectSpec e,ServerPlayer p,BlockPos at,String op){
        var l=c.level();var state=l.getBlockState(at);Vec3 center=Vec3.atCenterOf(at);
        switch(op){
            case "cold_snap" -> {if(!state.is(Blocks.ICE)&&!state.is(Blocks.PACKED_ICE)&&!state.is(Blocks.BLUE_ICE)&&!state.is(Blocks.FROSTED_ICE))return false;l.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);eruption(c,e,center,true);return true;}
            case "flare" -> {if(!state.is(BlockTags.FIRE)&&!state.is(NouveauCatalog.block("magic_fire")))return false;eruption(c,e,center,false);mageFire(c,e,at);return true;}
            case "explosion" -> {l.sendParticles(ParticleTypes.EXPLOSION,center.x,center.y,center.z,1,0,0,0,0);blast(c,e,p,center,null);return true;}
            case "lightning" -> {bolt(l,center);return blastCreatures(c,e,p,center,null,"lightning");}
            case "fangs" -> {fangLine(c,e,p,center);return true;}
            case "wind_burst" -> {return blastCreatures(c,e,p,center,null,"wind_burst");}
            default -> {return false;}
        }
    }
    private static void eruption(ConcordanceEffects.Context c,EffectSpec e,Vec3 center,boolean ice){
        int count=Math.min(8,3+e.augment("radius"));for(int i=0;i<count&&PULSES.size()<128;i++){
            double angle=Math.PI*2*i/count;Vec3 at=center.add(Math.cos(angle)*1.5,0,Math.sin(angle)*1.5);
            PULSES.add(new Pulse(c,at,Math.max(1,e.magnitude()/2),e.duration(),ice,c.level().getGameTime()+8+i*2));
            c.level().sendParticles(ice?new BlockParticleOption(ParticleTypes.BLOCK,Blocks.ICE.defaultBlockState()):ParticleTypes.FLAME,center.x,center.y+1,center.z,8,.5,1,.5,.1);
        }
    }
    private static void bolt(ServerLevel l,Vec3 at){var bolt=net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(l,EntitySpawnReason.TRIGGERED);if(bolt!=null){bolt.setVisualOnly(true);bolt.snapTo(at);l.addFreshEntity(bolt);}}
    private static void fangs(ServerLevel l,ServerPlayer p,Vec3 at,float yaw){var f=new EvokerFangs(l,at.x,at.y,at.z,yaw,0,p);f.setAttached(VISUAL_FANG,true);l.addFreshEntity(f);}
    /** Original vanilla animation only; shared callers apply separately protected damage. */
    public static void displayFang(ServerLevel l,ServerPlayer p,Vec3 at,float yaw,int warmup){var f=new EvokerFangs(l,at.x,at.y,at.z,yaw,warmup,p);f.setAttached(VISUAL_FANG,true);l.addFreshEntity(f);}
    private static void fangLine(ConcordanceEffects.Context c,EffectSpec e,ServerPlayer p,Vec3 target){boolean ring=p.distanceToSqr(target)<4;var direction=target.subtract(c.origin()).multiply(1,0,1).normalize();int count=Math.min(16,8+e.augment("radius")*2);
        for(int i=0;i<count&&FANGS.size()<128;i++){double angle=Math.PI*2*i/count;var at=ring?p.position().add(Math.cos(angle)*2.5,0,Math.sin(angle)*2.5):target.add(direction.scale(i));var ground=BlockPos.containing(at);for(int down=0;down<3&&c.level().hasChunkAt(ground.below())&&!c.level().getBlockState(ground.below()).isSolidRender();down++)ground=ground.below();if(!c.level().hasChunkAt(ground)||!Authority.mayChange(c.level(),p,ground))continue;var point=Vec3.atBottomCenterOf(ground);fangs(c.level(),p,point,p.getYRot());FANGS.add(new Fang(c,point,Math.max(1,e.magnitude()),c.level().getGameTime()+8));}
    }
    private static boolean blastCreatures(ConcordanceEffects.Context c,EffectSpec e,ServerPlayer p,Vec3 center,LivingEntity primary,String op){
        boolean changed=false;for(var target:c.level().getEntitiesOfClass(LivingEntity.class,AABB.ofSize(center,4+e.augment("radius")*2,4,4+e.augment("radius")*2),v->v.isAlive()&&v!=primary).stream().limit(24).toList()){
            var child=new ConcordanceEffects.Context(c.level(),c.cause(),p,c.ledger(),c.step()+"/"+op,center);
            var result=ConcordanceEffects.apply(child,EffectSpec.of(EffectKind.DAMAGE,Intent.HARMFUL,Math.max(1,e.magnitude()),0),target);changed|=result==ConcordanceEffects.Result.APPLIED;
            if(result==ConcordanceEffects.Result.APPLIED&&op.equals("wind_burst")){target.setDeltaMovement(target.getDeltaMovement().add(target.position().subtract(center).normalize().scale(.5)).add(0,.5,0));target.syncVelocity=true;}
            if(result==ConcordanceEffects.Result.APPLIED&&op.equals("lightning"))NouveauStatuses.give(target,"shocked",e.duration(),Math.min(2,NouveauStatuses.strength(target,"shocked")),p);
        }return changed;
    }
    private static void blast(ConcordanceEffects.Context c,EffectSpec e,ServerPlayer p,Vec3 center,LivingEntity primary){
        blastCreatures(c,e,p,center,primary,"blast");int radius=Math.min(3,1+e.augment("radius"));var origin=BlockPos.containing(center);
        for(var at:BlockPos.betweenClosed(origin.offset(-radius,-radius,-radius),origin.offset(radius,radius,radius))){
            var l=c.level();if(!l.hasChunkAt(at)||at.distSqr(origin)>radius*radius||!Authority.mayChange(l,p,at))continue;var s=l.getBlockState(at);
            if(s.isAir()||s.hasBlockEntity()||s.getDestroySpeed(l,at)<0||s.getDestroySpeed(l,at)>e.magnitude())continue;
            if(!c.ledger().admit(c.step()+"/blast","block:"+at.asLong(),3).admitted())break;
            var tool=new ItemStack(Items.DIAMOND_PICKAXE);if(e.augment("extract")>0)tool.enchant(l.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);
            Block.getDrops(s,l,at,null,p,tool).forEach(drop->Block.popResource(l,at,drop));l.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
        }
    }
    private static void mageFire(ConcordanceEffects.Context c,EffectSpec e,BlockPos center){var p=ConcordanceEffects.person(c);if(p==null)return;for(var d:Direction.Plane.HORIZONTAL){var at=center.relative(d);var l=c.level();if(l.hasChunkAt(at)&&l.getBlockState(at).canBeReplaced()&&Authority.mayChange(l,p,at)&&c.ledger().admit(c.step()+"/fire","block:"+at.asLong(),3).admitted())NouveauSpellState.replace(l,at,NouveauCatalog.block("magic_fire").defaultBlockState(),100);}}
}
