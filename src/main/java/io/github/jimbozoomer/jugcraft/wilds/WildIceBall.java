package io.github.jimbozoomer.jugcraft.wilds;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Swept original frost projectile: burst freezes permitted targets, never damages terrain. */
public final class WildIceBall extends Projectile {
    public static EntityType<WildIceBall> TYPE;
    public WildIceBall(EntityType<? extends WildIceBall> t,Level l){super(t,l);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_ice_ball"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<WildIceBall>of(WildIceBall::new,MobCategory.MISC).sized(.7F,.7F).clientTrackingRange(12).updateInterval(1).build(key));}
    static void fire(WildFrostmaw caster,LivingEntity target){var ball=TYPE.create(caster.level(),EntitySpawnReason.TRIGGERED);if(ball==null)return;ball.setOwner(caster);ball.setPos(caster.mouth());double flight=caster.distanceTo(target)/1.6;var future=target.getBoundingBox().getCenter().add(target.getDeltaMovement().multiply(flight*.95,0,flight*.95));ball.setDeltaMovement(future.subtract(ball.position()).normalize().scale(1.6));caster.level().addFreshEntity(ball);caster.playSound(OwnerWilds.sound("frostmaw.iceballshoot"),2,1);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
    @Override public void tick(){super.tick();var start=position();var end=start.add(getDeltaMovement());if(!(level() instanceof ServerLevel l)){setPos(end);return;}if(tickCount>100||!(getOwner() instanceof LivingEntity caster)||!caster.isAlive()||!l.hasChunkAt(BlockPos.containing(end))||!l.getWorldBorder().isWithinBounds(BlockPos.containing(end))){discard();return;}
        var block=l.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));boolean impact=block.getType()!=HitResult.Type.MISS;if(impact)end=block.getLocation();var candidates=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),new AABB(start,end).inflate(.5),e->WildCold.mayHit(caster,e),candidates,32);double nearest=start.distanceToSqr(end);Vec3 hitAt=null;for(var e:candidates){var hit=e.getBoundingBox().inflate(.35).clip(start,end);if(hit.isPresent()&&hit.get().distanceToSqr(start)<=nearest){nearest=hit.get().distanceToSqr(start);hitAt=hit.get();}}if(hitAt!=null){end=hitAt;impact=true;}setPos(end);l.sendParticles(WildParticles.TYPES.get("snowflake"),getX(),getY(),getZ(),2,.2,.2,.2,.01);
        if(impact){var victims=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(3),e->WildCold.mayHit(caster,e),victims,32);for(var e:victims){if(e.distanceToSqr(this)>9||l.clip(new ClipContext(position(),e.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS)continue;if(e.hurtServer(l,damageSources().indirectMagic(this,caster),3))WildCold.chill(e,1,caster);}l.sendParticles(WildParticles.TYPES.get("snowflake"),getX(),getY(),getZ(),32,1.5,1.5,1.5,.12);playSound(net.minecraft.sounds.SoundEvents.GLASS_BREAK,1,.8F);discard();}
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("age",tickCount);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);tickCount=Math.clamp(in.getIntOr("age",0),0,101);}
}
