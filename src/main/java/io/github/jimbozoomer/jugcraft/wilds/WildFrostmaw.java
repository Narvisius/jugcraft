package io.github.jimbozoomer.jugcraft.wilds;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Original sleeping snow guardian with crystal theft, staged melee and ranged frost attacks. */
public final class WildFrostmaw extends Monster {
    public static EntityType<WildFrostmaw> TYPE;
    public static final int IDLE=0,SWIPE=1,TWICE=2,ROAR=3,BREATH=4,BALL=5,WAKE=6,WAKE_EMPTY=7,SLEEP=8,LAND=9,SLAM=10,DODGE=11;
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(WildFrostmaw.class,EntityDataSerializers.INT),START=SynchedEntityData.defineId(WildFrostmaw.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ACTIVE=SynchedEntityData.defineId(WildFrostmaw.class,EntityDataSerializers.BOOLEAN),CRYSTAL=SynchedEntityData.defineId(WildFrostmaw.class,EntityDataSerializers.BOOLEAN);
    private final ServerBossEvent bar=new ServerBossEvent(UUID.randomUUID(),Component.translatable("entity.jugcraft.wilds_frostmaw"),BossEvent.BossBarColor.BLUE,BossEvent.BossBarOverlay.PROGRESS);
    private BlockPos home;private int age,breathWait,ballWait,slamWait,dodgeWait,attackWait,empty;private float recentDamage;
    public WildFrostmaw(EntityType<? extends WildFrostmaw> t,Level l){super(t,l);setPersistenceRequired();xpReward=40;}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_frostmaw"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<WildFrostmaw>of(WildFrostmaw::new,MobCategory.MONSTER).sized(4,4).clientTrackingRange(12).updateInterval(1).build(key));FabricDefaultAttributeRegistry.register(TYPE,Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,250).add(Attributes.ATTACK_DAMAGE,10).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.FOLLOW_RANGE,50).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.STEP_HEIGHT,1.5));WildCold.register();WildIceBall.register();}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTION,0);b.define(START,0);b.define(ACTIVE,false);b.define(CRYSTAL,true);}
    public int action(){return entityData.get(ACTION);}public boolean active(){return entityData.get(ACTIVE);}public boolean crystal(){return entityData.get(CRYSTAL);}public float actionTime(float partial){return level().getGameTime()-entityData.get(START)+partial;}
    private void begin(int a){entityData.set(ACTION,a);entityData.set(START,(int)level().getGameTime());age=0;getNavigation().stop();if(a==WAKE||a==WAKE_EMPTY)entityData.set(ACTIVE,true);if(a==SLEEP)entityData.set(ACTIVE,false);}
    private static int duration(int a){return switch(a){case SWIPE->28;case TWICE->57;case ROAR->76;case BREATH->92;case BALL->50;case WAKE->118;case WAKE_EMPTY->100;case SLEEP->25;case LAND->14;case SLAM->113;case DODGE->15;default->0;};}
    private void face(LivingEntity e,float speed){float desired=(float)Math.toDegrees(Math.atan2(e.getZ()-getZ(),e.getX()-getX()))-90;setYRot(Mth.approachDegrees(getYRot(),desired,speed));yBodyRot=getYRot();yHeadRot=getYRot();}
    public Vec3 mouth(){return position().add(new Vec3(0,2.65,2.3).yRot((float)Math.toRadians(-getYRot())));}
    public Vec3 sleepingCrystal(){return position().add(new Vec3(1.6,.4,1.8).yRot((float)Math.toRadians(-getYRot()-90)));}
    private List<ServerPlayer> players(ServerLevel l,double range){var result=new ArrayList<ServerPlayer>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ServerPlayer.class),getBoundingBox().inflate(range,8,range),p->p.isAlive()&&!p.isSpectator(),result,48);return result;}
    private void strike(ServerLevel l,Vec3 origin,double range,float arc,float damage){var forward=new Vec3(0,0,1).yRot((float)Math.toRadians(-getYRot()));for(var p:players(l,range+4)){var delta=p.position().subtract(origin);if(p.isCreative()||delta.lengthSqr()>range*range||!hasLineOfSight(p)||delta.horizontalDistanceSqr()>.01&&new Vec3(delta.x,0,delta.z).normalize().dot(forward)<Math.cos(Math.toRadians(arc/2)))continue;if(p.hurtServer(l,damageSources().mobAttack(this),damage)){var push=delta.normalize().scale(.8);p.setDeltaMovement(p.getDeltaMovement().add(push.x,.2,push.z));p.needsSync=true;}}}
    @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel l)||!isAlive())return;if(home==null)home=blockPosition();bar.setProgress(getHealth()/getMaxHealth());bar.setVisible(active());breathWait=Math.max(0,breathWait-1);ballWait=Math.max(0,ballWait-1);slamWait=Math.max(0,slamWait-1);dodgeWait=Math.max(0,dodgeWait-1);attackWait=Math.max(0,attackWait-1);recentDamage=Math.max(0,recentDamage-.1F);
        if(l.getDifficulty()==Difficulty.PEACEFUL){setTarget(null);getNavigation().stop();if(active())begin(SLEEP);heal(.3F);return;}
        if(action()!=IDLE&&++age>=duration(action())){entityData.set(ACTION,IDLE);age=0;attackWait=12;}
        if(tickCount%10==0){var target=players(l,40).stream().filter(p->!p.isCreative()&&(!p.isInvisible()||active())&&p.blockPosition().distSqr(home)<4096&&hasLineOfSight(p)).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);setTarget(target);}
        var target=getTarget();
        if(!active()){getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);heal(.3F);if(tickCount%118==1)playSound(OwnerWilds.sound("frostmaw.breath1"),1.5F,1.1F);
            if(crystal()&&action()==IDLE&&tickCount>20)for(var p:players(l,8)){if(!(p.isInvisible()||p.isCreative())||p.position().distanceToSqr(sleepingCrystal())>3.24||!TwoHanded.allowed(p,l,this))continue;var reward=new ItemStack(OwnerWilds.item("ice_crystal"));if(p.getInventory().getFreeSlot()<0)continue;if(p.getInventory().add(reward)){entityData.set(CRYSTAL,false);setTarget(p);begin(WAKE_EMPTY);break;}}
            if(action()==IDLE&&target!=null&&distanceToSqr(target)<64)begin(crystal()?WAKE:WAKE_EMPTY);return;
        }
        if(action()!=IDLE){if(action()!=DODGE)setDeltaMovement(0,getDeltaMovement().y,0);if(target!=null&&(action()==BREATH||action()==BALL||action()==SWIPE&&age<7||action()==TWICE&&age<19||action()==SLAM&&age<70))face(target,action()==BALL?15:30);
            if(action()==SWIPE&&age==9)strike(l,position(),6.5,135,20);if(action()==TWICE&&(age==9||age==21))strike(l,position(),6.5,135,10);
            if((action()==SWIPE||action()==TWICE)&&(age==6||action()==TWICE&&age==16))playSound(OwnerWilds.sound("frostmaw.whoosh"),2,.8F);
            if(action()==BREATH){if(age==13)playSound(OwnerWilds.sound("frostmaw.icebreathstart"),2,1);if(age>=23&&age<=78){var direction=target==null?getLookAngle():target.getEyePosition().subtract(mouth()).normalize();WildCold.breath(this,mouth(),direction,age-23);if(age%20==3)playSound(OwnerWilds.sound("frostmaw.icebreath"),2,1);}}
            if(action()==BALL){if(age==10)playSound(OwnerWilds.sound("frostmaw.iceballcharge"),2,1);if(age==33&&target!=null)WildIceBall.fire(this,target);}
            if(action()==SLAM&&age==87){strike(l,position().add(getLookAngle().multiply(4,0,4)),3.5,360,30);playSound(OwnerWilds.sound("frostmaw.land"),3,1);l.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+.2,getZ(),48,4,.2,4,.1);}
            if(action()==DODGE&&age==2&&target!=null){var away=position().subtract(target.position()).multiply(1,0,1).normalize().scale(1.7);var dest=BlockPos.containing(position().add(away.scale(3)));if(l.hasChunkAt(dest)&&l.getWorldBorder().isWithinBounds(dest)&&l.noCollision(this,getBoundingBox().move(away)))setDeltaMovement(away.x,.6,away.z);}
            if((action()==WAKE&&age==52)||(action()==WAKE_EMPTY&&age==34)||(action()==ROAR&&age==10))playSound(OwnerWilds.sound("frostmaw.roar"),4,1);return;
        }
        if(target==null){if(++empty>1200){if(blockPosition().distSqr(home)>16)getNavigation().moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,1);else begin(SLEEP);}return;}empty=0;face(target,10);double d=distanceTo(target);
        if(d<4&&recentDamage>=16&&dodgeWait==0){begin(DODGE);dodgeWait=200;recentDamage=0;breathWait=0;return;}
        if(attackWait==0){if(d<=8.5&&getHealth()<getMaxHealth()*.6&&slamWait==0&&random.nextInt(4)==0){begin(SLAM);slamWait=500;}else if(d<=6.5)begin(random.nextInt(4)==0?TWICE:SWIPE);else if(d<=13.5&&crystal()&&breathWait==0){begin(BREATH);breathWait=260;}else if(d>=14.5&&crystal()&&ballWait==0){begin(BALL);ballWait=200;}}
        if(action()==IDLE&&d>5.5)getNavigation().moveTo(target,1);else getNavigation().stop();if(tickCount%16==5&&getDeltaMovement().horizontalDistanceSqr()>.0025)playSound(OwnerWilds.sound("frostmaw.step"),3,1);
    }
    @Override public boolean hurtServer(ServerLevel l,DamageSource source,float damage){boolean hit=super.hurtServer(l,source,damage);if(hit&&isAlive()){recentDamage+=damage;if(!active()&&l.getDifficulty()!=Difficulty.PEACEFUL)begin(crystal()?WAKE:WAKE_EMPTY);}return hit;}
    @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource source,boolean recentlyHit){super.dropCustomDeathLoot(l,source,recentlyHit);if(recentlyHit){if(crystal())spawnAtLocation(l,new ItemStack(OwnerWilds.item("ice_crystal")));else if(l.isRaining())spawnAtLocation(l,new ItemStack(OwnerWilds.item("music_disc_petiole")));}entityData.set(CRYSTAL,false);}
    @Override protected void tickDeath(){bar.setVisible(false);deathTime++;if(deathTime>=94&&!level().isClientSide())remove(RemovalReason.KILLED);}
    @Override public void checkDespawn(){if(level().getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL)super.checkDespawn();}
    @Override public boolean canFreeze(){return false;}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);bar.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
    @Override public void remove(RemovalReason r){bar.removeAllPlayers();super.remove(r);}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putBoolean("active",active());out.putBoolean("has_crystal",crystal());if(home!=null)out.store("home",BlockPos.CODEC,home);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);entityData.set(ACTIVE,in.getBooleanOr("active",false));entityData.set(CRYSTAL,in.getBooleanOr("has_crystal",true));home=in.read("home",BlockPos.CODEC).orElse(blockPosition());}
}
