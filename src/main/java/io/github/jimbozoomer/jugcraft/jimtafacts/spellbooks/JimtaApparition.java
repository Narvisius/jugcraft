package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Source spike growth, delayed jaw bite, frozen copies and authored echoing-claymore swing. */
public final class JimtaApparition extends PathfinderMob implements GeoEntity {
    public static final Map<String,EntityType<JimtaApparition>> TYPES=new LinkedHashMap<>();
    private static final EntityDataAccessor<String> COPY=SynchedEntityData.defineId(JimtaApparition.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> SIZE=SynchedEntityData.defineId(JimtaApparition.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> WAIT=SynchedEntityData.defineId(JimtaApparition.class,EntityDataSerializers.INT);
    private final String kind;
    private UUID owner,target;
    private int age,damage;
    private boolean shattered;
    private final Set<UUID> hit=new HashSet<>();
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public JimtaApparition(EntityType<? extends PathfinderMob> type,Level level,String kind){super(type,level);this.kind=kind;setNoAi(true);setNoGravity(true);setPermanentlyInvulnerable(!kind.equals("frozen_copy")&&!kind.equals("frozen_humanoid"));}
    public String kind(){return kind;}public String copiedType(){return entityData.get(COPY);}public float size(){return entityData.get(SIZE);}public int waitTicks(){return entityData.get(WAIT);}public int age(){return age;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(COPY,"minecraft:player");builder.define(SIZE,1F);builder.define(WAIT,0);}
    public static void register(){for(String kind:List.of("ice_spike","devour_jaw","frozen_copy","frozen_humanoid","echoing_sword")){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_"+kind));var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<JimtaApparition>of((t,l)->new JimtaApparition(t,l,kind),MobCategory.MISC).sized(.6F,1.8F).clientTrackingRange(12).updateInterval(1).build(key));TYPES.put(kind,type);FabricDefaultAttributeRegistry.register(type,Mob.createMobAttributes().add(Attributes.MAX_HEALTH,2).add(Attributes.MOVEMENT_SPEED,0));}}
    public static JimtaApparition create(ServerPlayer p,String kind,Vec3 at,LivingEntity target,int damage,float scale,int wait){
        if(!p.level().hasChunkAt(BlockPos.containing(at))||!Authority.mayChange(p.level(),p,BlockPos.containing(at))||p.level().getEntitiesOfClass(JimtaApparition.class,p.getBoundingBox().inflate(96),e->p.getUUID().equals(e.owner)).size()>=32)return null;
        var entity=TYPES.get(kind).create(p.level(),EntitySpawnReason.MOB_SUMMONED);if(entity==null)return null;entity.owner=p.getUUID();entity.target=target==null?null:target.getUUID();entity.damage=Math.clamp(damage,0,40);entity.entityData.set(SIZE,Math.clamp(scale,.5F,4.5F));entity.entityData.set(WAIT,Math.clamp(wait,0,20));entity.snapTo(at.x,at.y,at.z,target==null?p.getYRot():target.getYRot(),0);
        if(target!=null)entity.entityData.set(COPY,BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());return p.level().addFreshEntity(entity)?entity:null;
    }
    @Override public void tick(){
        Vec3 motion=getDeltaMovement();setDeltaMovement(Vec3.ZERO);super.tick();setDeltaMovement(motion);age++;
        if(!(level() instanceof ServerLevel l))return;if(kind.equals("frozen_humanoid"))return;var p=owner==null?null:Authority.present(l,owner);if(p==null||!p.isAlive()||!JimtaSpellbooks.enabled()||distanceToSqr(p)>128*128){discard();return;}var victim=target==null?null:l.getEntity(target);
        switch(kind){
            case "ice_spike"->{int tick=age-waitTicks();if(tick>0&&tick<6){float width=size()*.4F;var box=AABB.ofSize(position().add(0,size()*.7,0),width,size()*1.4,width);for(var e:l.getEntitiesOfClass(LivingEntity.class,box,LivingEntity::isAlive).stream().limit(16).toList())if(!hit.contains(e.getUUID())&&JimtaShot.hit(p,kind(),e,damage,"",position().add(0,.5,0))){hit.add(e.getUUID());e.setTicksFrozen(Math.min(300,e.getTicksFrozen()+(int)(40*size())));e.setDeltaMovement(e.getDeltaMovement().add(0,size()*.3,0));e.syncVelocity=true;}}if(tick>76)discard();}
            case "devour_jaw"->{if(age<5&&victim instanceof LivingEntity living)setPos(living.position());if(age==13&&victim instanceof LivingEntity living&&distanceToSqr(living)<9){float before=living.getHealth();if(JimtaShot.hit(p,kind(),living,damage,"",position().add(0,1,0))){float restore=(float)(Math.max(0,before-living.getHealth())*.15);if(restore>0)p.heal(restore);living.setDeltaMovement(living.getDeltaMovement().add(0,.5,0));living.syncVelocity=true;if(!living.isAlive()){var holder=JimtaSpellStatuses.EFFECTS.get("vigor");var old=p.getEffect(holder);p.addEffect(new MobEffectInstance(holder,1200,Math.min(9,old==null?0:old.getAmplifier()+1)));p.heal(2);}}}if(age>21)discard();}
            case "frozen_copy"->{if(age%20==0)for(var enemy:l.getEntitiesOfClass(Mob.class,getBoundingBox().inflate(10),e->e instanceof Enemy&&e.isAlive()&&TwoHanded.allowed(p,l,e)&&ConcordanceEffects.mayHarm(p,e)).stream().limit(16).toList())enemy.setTarget(this);if(age>=100)shatter(p);}
            case "echoing_sword"->{if(victim instanceof LivingEntity living){Vec3 diff=living.getBoundingBox().getCenter().subtract(position());Vec3 wanted=diff.subtract(diff.normalize().scale(1.2)).scale(.1);move(MoverType.SELF,getDeltaMovement().lerp(wanted,.05));setYRot((float)(Math.toDegrees(Math.atan2(diff.x,diff.z))));if(age==25)for(var e:l.getEntitiesOfClass(LivingEntity.class,AABB.ofSize(living.getBoundingBox().getCenter(),4,4,4),LivingEntity::isAlive).stream().limit(16).toList())if(e.distanceToSqr(living)<4)JimtaShot.hit(p,kind(),e,damage,"",position());}if(age>=45)discard();}
        }
    }
    private void shatter(ServerPlayer p){if(shattered)return;shattered=true;for(var e:p.level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(2),e->e!=this&&e.isAlive()).stream().limit(16).toList())JimtaShot.hit(p,kind(),e,Math.max(1,damage/2),"",getEyePosition());for(int i=0;i<8;i++){double a=i*Math.PI/4;JimtaShot.launch(p,"ice_shard",getEyePosition(),new Vec3(Math.sin(a),.2,Math.cos(a)),damage);}p.level().sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+1,getZ(),45,.5,1,.5,.15);discard();}
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){if(kind.equals("frozen_humanoid")){if(source.getEntity() instanceof ServerPlayer p&&!Authority.mayChange(level,p,blockPosition()))return false;level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+1,getZ(),40,.5,1,.5,.1);discard();return true;}if(!kind.equals("frozen_copy"))return false;var p=owner==null?null:Authority.present(level,owner);if(p==null)return false;if(source.getEntity() instanceof ServerPlayer attacker&&attacker!=p&&(!TwoHanded.allowed(attacker,level,p)||!ConcordanceEffects.mayHarm(attacker,p)||JimtaSpellcasting.ally(p,attacker)))return false;shatter(p);return true;}
    @Override public boolean isPushable(){return false;}
    @Override protected void registerGoals(){}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);if(target!=null)out.store("target",UUIDUtil.CODEC,target);out.putInt("age",age);out.putInt("damage",damage);out.putFloat("scale",size());out.putInt("wait",waitTicks());out.putString("copy",copiedType());out.store("struck",UUIDUtil.CODEC.listOf(),new ArrayList<>(hit));}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);target=in.read("target",UUIDUtil.CODEC).orElse(null);age=in.getIntOr("age",101);damage=Math.clamp(in.getIntOr("damage",0),0,40);entityData.set(SIZE,Math.clamp(in.getFloatOr("scale",1),.5F,4.5F));entityData.set(WAIT,Math.clamp(in.getIntOr("wait",0),0,20));entityData.set(COPY,in.getStringOr("copy",in.getStringOr("entityToCopy","minecraft:player")));hit.addAll(in.read("struck",UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().limit(16).toList());}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<JimtaApparition>("source",0,state->state.setAndContinue(RawAnimation.begin().thenPlay("summoned_sword_basic_downswing"))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
