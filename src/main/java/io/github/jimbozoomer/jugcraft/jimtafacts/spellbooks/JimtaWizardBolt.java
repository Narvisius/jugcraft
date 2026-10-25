package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Actual hostile projectiles collide with terrain and only affect their mage's selected foe. */
public final class JimtaWizardBolt extends Projectile {
    public static EntityType<JimtaWizardBolt> TYPE;
    private static final EntityDataAccessor<String> SCHOOL=SynchedEntityData.defineId(JimtaWizardBolt.class,EntityDataSerializers.STRING);
    private UUID target;private int damage;
    public JimtaWizardBolt(EntityType<? extends Projectile> type,Level level){super(type,level);setNoGravity(true);}
    public String school(){return entityData.get(SCHOOL);}
    public static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_wizard_bolt"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<JimtaWizardBolt>of(JimtaWizardBolt::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(12).updateInterval(1).build(key));}
    public static void launch(Mob mage,LivingEntity victim,String school,int damage,double angle){if(!(mage.level() instanceof ServerLevel level))return;var bolt=TYPE.create(level,EntitySpawnReason.TRIGGERED);if(bolt==null)return;bolt.entityData.set(SCHOOL,school);bolt.damage=damage;bolt.target=victim.getUUID();bolt.setOwner(mage);bolt.setPos(mage.getEyePosition());bolt.setDeltaMovement(victim.getBoundingBox().getCenter().subtract(mage.getEyePosition()).normalize().yRot((float)angle).scale(.65));level.addFreshEntity(bolt);}
    public static void spawn(Mob mage,LivingEntity target,String school,float damage){launch(mage,target,school,Math.clamp((int)Math.ceil(damage),0,15),0);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(SCHOOL,"evocation");}
    @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel level))return;if(tickCount>100||!(getOwner() instanceof Mob mage)||!mage.isAlive()||!JimtaSpellbooks.enabled()){discard();return;}var victim=target==null?null:level.getEntity(target);if(!(victim instanceof LivingEntity living)||!living.isAlive()){discard();return;}Vec3 from=position(),next=from.add(getDeltaMovement());if(!level.hasChunkAt(BlockPos.containing(next))){discard();return;}var hit=level.clip(new ClipContext(from,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));Vec3 end=hit.getType()==HitResult.Type.BLOCK?hit.getLocation():next;if(living.getBoundingBox().inflate(.2).clip(from,end).isPresent()){
            boolean changed=living.hurtServer(level,level.damageSources().mobProjectile(this,mage),damage);
            if(changed)switch(school()){case "fire"->living.igniteForSeconds(2);case "ice"->{living.setTicksFrozen(Math.min(300,living.getTicksFrozen()+80));living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,60,0));}case "blood"->mage.heal(damage*.15F);case "poison"->living.addEffect(new MobEffectInstance(MobEffects.POISON,100,0));case "wither"->living.addEffect(new MobEffectInstance(MobEffects.WITHER,60,0));default->{}}
            discard();return;}setPos(end);if(hit.getType()==HitResult.Type.BLOCK)discard();}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(target!=null)out.store("target",UUIDUtil.CODEC,target);out.putString("school",school());out.putInt("damage",damage);out.putInt("age",tickCount);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);target=in.read("target",UUIDUtil.CODEC).orElse(null);entityData.set(SCHOOL,in.getStringOr("school","evocation"));damage=Math.clamp(in.getIntOr("damage",0),0,15);tickCount=in.getIntOr("age",101);}
}
