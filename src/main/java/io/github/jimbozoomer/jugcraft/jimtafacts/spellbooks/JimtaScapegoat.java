package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** The source running decoy, with its original geometry and run/accent clips. */
public final class JimtaScapegoat extends PathfinderMob implements GeoEntity {
    public static EntityType<JimtaScapegoat> TYPE;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private UUID owner;private BlockPos destination=BlockPos.ZERO;private int life=300;
    public JimtaScapegoat(EntityType<? extends PathfinderMob> type,Level l){super(type,l);setPersistenceRequired();}
    public static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_scapegoat"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.of(JimtaScapegoat::new,MobCategory.MISC).sized(.9F,1.3F).clientTrackingRange(12).build(key));FabricDefaultAttributeRegistry.register(TYPE,Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.35).add(Attributes.KNOCKBACK_RESISTANCE,.6).add(Attributes.STEP_HEIGHT,1));}
    public static boolean cast(ServerPlayer p){
        var level=p.level();if(level.getEntitiesOfClass(JimtaScapegoat.class,p.getBoundingBox().inflate(96),e->p.getUUID().equals(e.owner)).size()>=4)return false;
        var hit=level.clip(new ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(18)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        var target=JimtaWardEntity.ground(p,hit.getLocation());var start=JimtaWardEntity.ground(p,p.position().add(p.getLookAngle().multiply(1,0,1).normalize().scale(2)));
        if(target==null||start==null)return false;var e=TYPE.create(level,EntitySpawnReason.MOB_SUMMONED);if(e==null)return false;
        e.owner=p.getUUID();e.destination=BlockPos.containing(target);e.snapTo(start.x,start.y,start.z,p.getYRot(),0);e.damageCooldownTime=60;if(!level.noCollision(e)||!level.addFreshEntity(e))return false;
        for(var mob:level.getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(12),m->m!=e&&m.getTarget()==p&&JimtaWardSpells.permitted(p,m)).stream().limit(64).toList())mob.setTarget(e);
        return true;
    }
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel level))return;var p=owner==null?null:Authority.present(level,owner);
        if(--life<=0||p==null||p.level()!=level||!p.isAlive()||!JimtaSpellbooks.enabled()||distanceToSqr(p)>128*128||!Authority.mayChange(level,p,blockPosition())){discard();return;}
        if(tickCount%20==0&&level.hasChunkAt(destination)&&Authority.mayChange(level,p,destination)){getNavigation().moveTo(destination.getX()+.5,destination.getY(),destination.getZ()+.5,1);}
        if(tickCount%5==0)level.sendParticles(ParticleTypes.END_ROD,getX(),getY()+.5,getZ(),1,.1,.1,.1,.03);
        if(tickCount%20==0)triggerAnim("accent","run_accent");
    }
    @Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount){var p=owner==null?null:Authority.present(l,owner);if(p!=null&&source.getEntity() instanceof LivingEntity a&&JimtaSpellcasting.ally(p,a))return false;return super.hurtServer(l,source,amount);}
    @Override public void die(DamageSource source){discard();}
    @Override protected void dropAllDeathLoot(ServerLevel level,DamageSource source){}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.store("destination",BlockPos.CODEC,destination);out.putInt("life",life);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);destination=in.read("destination",BlockPos.CODEC).orElse(BlockPos.ZERO);life=Math.clamp(in.getIntOr("life",0),0,300);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<JimtaScapegoat>("accent",0,state->PlayState.STOP).triggerableAnim("run_accent",RawAnimation.begin().thenPlay("scapegoat_run_accent")));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
