package io.github.jimbozoomer.jugcraft.wilds;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Original short-lived ballistic acid; bounded victims and no terrain destruction. */
public final class WildPoisonBall extends Projectile {
    public static EntityType<WildPoisonBall> TYPE;
    public WildPoisonBall(EntityType<? extends WildPoisonBall> t,Level l){super(t,l);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_poison_ball"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<WildPoisonBall>of(WildPoisonBall::new,MobCategory.MISC).sized(.5F,.5F).clientTrackingRange(10).updateInterval(1).build(key));}
    static void fire(WildNaga caster,LivingEntity target){var ball=TYPE.create(caster.level(),EntitySpawnReason.TRIGGERED);if(ball==null)return;ball.setOwner(caster);ball.setPos(caster.position().add(caster.getLookAngle().scale(2)).add(0,.5,0));Vec3 difference=target.getEyePosition().subtract(ball.position());double flight=Math.sqrt(2*Math.max(1,-difference.y)/.05);ball.setDeltaMovement(difference.x/flight,.1,difference.z/flight);caster.level().addFreshEntity(ball);caster.playSound(OwnerWilds.sound("naga.acidspit"),2,1);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
    @Override public void tick(){super.tick();var motion=getDeltaMovement().add(0,-.05,0);var end=position().add(motion);if(!(level() instanceof ServerLevel l)){setPos(end);setDeltaMovement(motion);return;}if(tickCount>50||!l.hasChunkAt(BlockPos.containing(end))||!l.getWorldBorder().isWithinBounds(BlockPos.containing(end))){discard();return;}var hit=l.clip(new ClipContext(position(),end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));setPos(hit.getType()==HitResult.Type.BLOCK?hit.getLocation():end);setDeltaMovement(motion);boolean impact=hit.getType()==HitResult.Type.BLOCK;var victims=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(impact?2:1),e->e.isAlive()&&e!=getOwner()&&!(e instanceof WildNaga),victims,24);for(var target:victims){if(getOwner() instanceof ServerPlayer p&&!TwoHanded.allowed(p,l,target)||getOwner() instanceof WildNaga&&!(target instanceof net.minecraft.world.entity.player.Player)||l.clip(new ClipContext(position(),target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS)continue;if(target.hurtServer(l,l.damageSources().indirectMagic(this,getOwner()),3))target.addEffect(new MobEffectInstance(MobEffects.POISON,80,impact?0:1),getOwner());}l.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,getX(),getY(),getZ(),2,.1,.1,.1,.01);if(impact){playSound(OwnerWilds.sound("naga.acidhit"),1,1);discard();}}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("age",tickCount);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);tickCount=Math.clamp(in.getIntOr("age",0),0,51);}
}
