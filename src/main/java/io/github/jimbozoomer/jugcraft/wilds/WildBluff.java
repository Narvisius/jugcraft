package io.github.jimbozoomer.jugcraft.wilds;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Source Bluff encounter: ranged ground fissures, with flight when a ground route is unavailable. */
public final class WildBluff extends Monster implements GeoEntity {
    public static EntityType<WildBluff> TYPE;
    public static final int IDLE=0,START=1,FALL=2,RECOVER=3,HURT=4;
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(WildBluff.class,EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private GroundPathNavigation ground;
    private FlyingPathNavigation flying;
    private MoveControl groundControl,flyingControl;
    private boolean airborne;
    private int actionAge,cooldown=40,unseen;
    private Vec3 previousTarget;private UUID retaliation;
    public WildBluff(EntityType<? extends WildBluff> t,Level l){super(t,l);groundControl=moveControl;flyingControl=new FlyingMoveControl(this,20,true);xpReward=8;}
    static void register(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_bluff"));
        TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<WildBluff>of(WildBluff::new,MobCategory.MONSTER).sized(.6F,1.8F).clientTrackingRange(8).updateInterval(1).build(key));
        FabricDefaultAttributeRegistry.register(TYPE,Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,45).add(Attributes.ATTACK_DAMAGE,18).add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.FLYING_SPEED,.23).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.FOLLOW_RANGE,32));
        SpawnPlacements.register(TYPE,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(t,l,reason,p,r)->!l.canSeeSky(p)&&Monster.checkMonsterSpawnRules(t,l,reason,p,r));
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),MobCategory.MONSTER,TYPE,10,2,3);
        WildGeomancy.register();
    }
    @Override protected PathNavigation createNavigation(Level l){ground=new GroundPathNavigation(this,l);flying=new FlyingPathNavigation(this,l);return ground;}
    @Override protected void registerGoals(){goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,1){@Override public boolean canUse(){return action()==IDLE&&getTarget()==null&&super.canUse();}@Override public boolean canContinueToUse(){return action()==IDLE&&getTarget()==null&&super.canContinueToUse();}});goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(9,new RandomLookAroundGoal(this));}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTION,IDLE);}
    public int action(){return entityData.get(ACTION);}
    private void begin(int action){entityData.set(ACTION,action);actionAge=0;navigation.stop();if(action==START){setFlying(false);previousTarget=getTarget()==null?null:getTarget().position();playSound(OwnerWilds.sound("bluff.attack"),1,1.2F);}}
    private void setFlying(boolean value){if(value==airborne)return;navigation.stop();airborne=value;navigation=value?flying:ground;moveControl=value?flyingControl:groundControl;setNoGravity(value);}
    static boolean victim(LivingEntity e){return e instanceof Player p&&p.isAlive()&&!p.isCreative()&&!p.isSpectator();}
    private boolean enemy(LivingEntity e){return e.isAlive()&&(victim(e)||e instanceof WildSculptor||e.getUUID().equals(retaliation)&&WildSolar.canHarm(this,e));}
    @Override public boolean checkSpawnObstruction(LevelReader reader){if(!super.checkSpawnObstruction(reader))return false;if(!(level() instanceof ServerLevel server))return true;var found=new ArrayList<Entity>();server.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(Entity.class),getBoundingBox().inflate(8),e->e instanceof WildSculptor||e instanceof WildBoulder b&&b.mode()==WildBoulder.PILLAR,found,1);return found.isEmpty();}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel l)||!isAlive())return;
        if(cooldown>0)cooldown--;
        if(tickCount%10==0){var targets=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(32),this::enemy,targets,32);if(getTarget()==null||!enemy(getTarget())||distanceToSqr(getTarget())>1600)setTarget(targets.stream().filter(this::hasLineOfSight).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));}
        var target=getTarget();if(target!=null){getLookControl().setLookAt(target,30,30);setYRot(getYHeadRot());yBodyRot=getYRot();if(hasLineOfSight(target))unseen=0;else if(++unseen>100){setTarget(null);target=null;}}
        if(action()!=IDLE){navigation.stop();actionAge++;
            if(action()==START){setDeltaMovement(Vec3.ZERO);if(actionAge>=11)begin(FALL);}
            else if(action()==FALL){setDeltaMovement(0,Math.max(-7,getDeltaMovement().y-2),0);needsSync=true;if(onGround()){impact(l,target);begin(RECOVER);}else if(actionAge>80){begin(IDLE);cooldown=50;}}
            else if(actionAge>=(action()==HURT?7:34))begin(IDLE);
            return;
        }
        if(target==null){setFlying(false);return;}
        double distance=distanceToSqr(target);
        if(tickCount%20==0){var path=ground.createPath(target,0);setFlying(path==null||!path.canReach());}
        if(distance>144||!hasLineOfSight(target)){if(tickCount%10==0)navigation.moveTo(target,airborne?1.2:1);}
        else {navigation.stop();if(cooldown==0){cooldown=50+random.nextInt(40);begin(START);return;}
            var away=position().subtract(target.position()).multiply(1,0,1).normalize();var strafe=new Vec3(-away.z,0,away.x).scale((tickCount/60%2==0?1:-1)*2);var at=position().add(strafe).add(away.scale(distance<36?3:distance>100?-2:0));if(tickCount%10==0&&l.hasChunkAt(BlockPos.containing(at)))navigation.moveTo(at.x,at.y,at.z,.8);}
        if(!airborne&&!onGround()&&getDeltaMovement().y<0)setDeltaMovement(getDeltaMovement().multiply(1,.6,1));
    }
    private void impact(ServerLevel l,LivingEntity target){
        var victims=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(.4),this::enemy,victims,16);for(var e:victims)if(hasLineOfSight(e))e.hurtServer(l,damageSources().mobAttack(this),18);
        Vec3 aim=target==null?getLookAngle():target.position().add(target.position().subtract(previousTarget==null?target.position():previousTarget).multiply(.25,0,.25).scale(distanceTo(target)/.4)).subtract(position());WildGeomancy.fire(this,aim);
        l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,l.getBlockState(blockPosition().below())),getX(),getY()+.1,getZ(),30,.7,.1,.7,.15);
    }
    @Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount){boolean hit=super.hurtServer(l,source,amount);if(hit&&isAlive()){if(source.getEntity() instanceof LivingEntity attacker&&!(attacker instanceof WildBluff)&&WildSolar.canHarm(this,attacker)){retaliation=attacker.getUUID();setTarget(attacker);}if(action()==IDLE)begin(HURT);}return hit;}
    @Override public boolean causeFallDamage(double d,float m,DamageSource source){return false;}
    @Override protected SoundEvent getHurtSound(DamageSource s){return OwnerWilds.sound("bluff.hurt");}
    @Override protected SoundEvent getDeathSound(){return OwnerWilds.sound("bluff.death");}
    @Override protected SoundEvent getAmbientSound(){return OwnerWilds.sound("bluff.idle");}
    @Override protected void tickDeath(){navigation.stop();setFlying(false);if(++deathTime>=50&&!level().isClientSide())remove(RemovalReason.KILLED);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar out){out.add(new AnimationController<WildBluff>("body",0,s->{String clip=!isAlive()?"death":action()==START||action()==FALL?"attack_start":action()==RECOVER?"attack_end":action()==HURT?"hurt":"idle";return s.setAndContinue(clip.equals("idle")?RawAnimation.begin().thenLoop(clip):RawAnimation.begin().thenPlayAndHold(clip));}));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(retaliation!=null)out.store("retaliation",UUIDUtil.CODEC,retaliation);out.putInt("cooldown",cooldown);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);retaliation=in.read("retaliation",UUIDUtil.CODEC).orElse(null);cooldown=Math.clamp(in.getIntOr("cooldown",40),0,100);setFlying(false);entityData.set(ACTION,IDLE);}
}
