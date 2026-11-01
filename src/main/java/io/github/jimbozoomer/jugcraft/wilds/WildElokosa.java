package io.github.jimbozoomer.jugcraft.wilds;
import net.minecraft.world.level.ClipContext;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Calendar;
import java.util.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Lunar jungle pack: timid day form, stalking night form, surface leaps and authored attacks. */
public final class WildElokosa extends Monster implements GeoEntity {
    public static EntityType<WildElokosa> HOWLER,FOLLOWER;
    public static final int IDLE=0,TO_NIGHT=1,TO_DAY=2,SCREAM=3,SINGLE=4,COMBO=5,COMBO_ROAR=6,SCYTHE=7,LEAP=8,LEAP_HIT=9,BACKFLIP=10,LAND=11,HURT=12,SIT=13;
    private static final EntityDataAccessor<Boolean> NIGHT=SynchedEntityData.defineId(WildElokosa.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(WildElokosa.class,EntityDataSerializers.INT),CLING=SynchedEntityData.defineId(WildElokosa.class,EntityDataSerializers.INT),START=SynchedEntityData.defineId(WildElokosa.class,EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private UUID leader;
    private int actionAge,cooldown=80,dodgeCooldown,stalking=700,watched,leapTicks,formDelay=7;
    private boolean packCreated,leapAttack;
    private Vec3 leapVelocity=Vec3.ZERO;
    public WildElokosa(EntityType<? extends WildElokosa> t,Level l){super(t,l);xpReward=t==HOWLER?8:5;}
    static void register(){HOWLER=type("elokosa_howler");FOLLOWER=type("elokosa_follower_howler");for(var type:List.of(HOWLER,FOLLOWER))FabricDefaultAttributeRegistry.register(type,Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,type==HOWLER?20:17).add(Attributes.ATTACK_DAMAGE,type==HOWLER?6:5).add(Attributes.MOVEMENT_SPEED,.2).add(Attributes.FOLLOW_RANGE,32).add(Attributes.JUMP_STRENGTH,.42).add(Attributes.GRAVITY,.095));
        SpawnPlacements.register(HOWLER,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(t,l,reason,p,r)->l.getDifficulty()!=Difficulty.PEACEFUL&&p.getY()>=60&&WildSpawns.outsideSettlements(l,p,reason,false)&&(l.getBlockState(p.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)||l.getBlockState(p.below()).is(BlockTags.LEAVES)||l.getBlockState(p.below()).is(BlockTags.LOGS)));
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_JUNGLE),MobCategory.MONSTER,HOWLER,5,1,1);
    }
    private static EntityType<WildElokosa> type(String name){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_"+name));return Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<WildElokosa>of(WildElokosa::new,MobCategory.MONSTER).sized(.9F,1.6F).clientTrackingRange(10).updateInterval(1).build(key));}
    @Override protected void registerGoals(){goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,.7){@Override public boolean canUse(){return action()==IDLE&&cling()==Direction.DOWN&&getTarget()==null&&super.canUse();}@Override public boolean canContinueToUse(){return action()==IDLE&&cling()==Direction.DOWN&&getTarget()==null&&super.canContinueToUse();}});goalSelector.addGoal(9,new RandomLookAroundGoal(this));}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(NIGHT,false);b.define(ACTION,IDLE);b.define(CLING,Direction.DOWN.get3DDataValue());b.define(START,0);}
    public boolean howler(){return getType()==HOWLER;}public boolean night(){return entityData.get(NIGHT);}public int action(){return entityData.get(ACTION);}public Direction cling(){return Direction.from3DDataValue(entityData.get(CLING));}public float actionTime(float partial){return level().getGameTime()-entityData.get(START)+partial;}
    private void form(boolean night){float health=getHealth()/Math.max(1,getMaxHealth());entityData.set(NIGHT,night);getAttribute(Attributes.MAX_HEALTH).setBaseValue((howler()?20:17)-(night?0:14));getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(night?.2:.17);setHealth(getMaxHealth()*health);refreshDimensions();if(!night)setTarget(null);}
    @Override protected EntityDimensions getDefaultDimensions(Pose pose){return night()?super.getDefaultDimensions(pose):EntityDimensions.scalable(.7F,.7F);}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(key.equals(NIGHT))refreshDimensions();}
    private void begin(int action){if(action!=LEAP&&action!=LEAP_HIT)setDiscardFriction(false);entityData.set(ACTION,action);entityData.set(START,(int)level().getGameTime());actionAge=0;navigation.stop();if(action==SCREAM)playSound(OwnerWilds.sound("elokosa.night.scream.long"),3,1);if(action==TO_NIGHT||action==TO_DAY)playSound(OwnerWilds.sound("elokosa."+(action==TO_NIGHT?"day":"night")+".transform"),1,1);}
    private static int duration(int action){return switch(action){case TO_NIGHT->55;case TO_DAY->25;case SCREAM->56;case SINGLE->26;case COMBO->39;case COMBO_ROAR->47;case SCYTHE->27;case LEAP_HIT->20;case BACKFLIP->22;case LAND->17;case HURT->12;case SIT->100;default->0;};}
    private void release(){entityData.set(CLING,Direction.DOWN.get3DDataValue());setNoGravity(false);}
    private boolean safe(Vec3 at){return WildElokosaLeaps.loaded(this,getBoundingBox().move(at.subtract(position())));}
    private void leap(Vec3 destination,boolean attack){
        if(!safe(destination)||(!onGround()&&cling()==Direction.DOWN))return;
        Vec3 velocity=attack?WildElokosaLeaps.velocity(this,destination):WildElokosaLeaps.surfaceVelocity(this,destination);
        if(velocity==null)return;
        leapVelocity=velocity;leapAttack=attack;leapTicks=0;begin(LEAP);
    }
    private boolean latch(){if(!night()&&action()!=LEAP)return false;for(Direction d:List.of(Direction.UP,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST)){var from=getBoundingBox().getCenter();var to=from.add(d.getStepX()*.9,d.getStepY()*(getBbHeight()/2+.5),d.getStepZ()*.9);var hit=level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));if(hit.getType()!=HitResult.Type.BLOCK||hit.isInside())continue;var at=hit.getLocation().subtract(new Vec3(d.getStepX(),d.getStepY(),d.getStepZ()).scale(d==Direction.UP?getBbHeight()/2+.02:getBbWidth()/2+.02)).add(0,-getBbHeight()/2,0);if(!safe(at)||!level().noCollision(this,getBoundingBox().move(at.subtract(position()))))continue;setPos(at);entityData.set(CLING,d.get3DDataValue());setNoGravity(true);setDeltaMovement(Vec3.ZERO);begin(LAND);return true;}return false;}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l,DifficultyInstance difficulty,EntitySpawnReason reason,SpawnGroupData group){var result=super.finalizeSpawn(l,difficulty,reason,group);form(wantsNight());if(howler()&&!packCreated){packCreated=true;int count=3+random.nextInt(2);for(int i=0;i<count;i++){var child=FOLLOWER.create(level(),EntitySpawnReason.MOB_SUMMONED);if(child==null)continue;double angle=i*Math.PI*2/count;var at=position().add(Math.cos(angle)*1.5,0,Math.sin(angle)*1.5);if(!safe(at))continue;child.setPos(at);if(!l.getLevel().noCollision(child))continue;child.leader=getUUID();child.packCreated=true;child.form(wantsNight());l.addFreshEntity(child);}}return result;}
    private boolean wantsNight(){long time=level().getOverworldClockTime();int hour=Calendar.tickOfDay(time);return hour>=13000&&hour<23000&&Calendar.moonPhase(time)!=4;}
    @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel l)||!isAlive())return;if(cooldown>0)cooldown--;if(dodgeCooldown>0)dodgeCooldown--;
        if(wantsNight())addEffect(new MobEffectInstance(WildMoonEffects.MOONS_CURSE,10,0,false,false));else removeEffect(WildMoonEffects.MOONS_CURSE);
        if(cling()!=Direction.DOWN){var from=getBoundingBox().getCenter();var d=cling();var hit=l.clip(new ClipContext(from,from.add(d.getStepX(),d.getStepY()*(getBbHeight()/2+.5),d.getStepZ()),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));if(hit.getType()==HitResult.Type.MISS)release();else{setDeltaMovement(Vec3.ZERO);setNoGravity(true);if(d.getAxis().isHorizontal()){setYRot(d.getOpposite().toYRot());yBodyRot=getYRot();}}}
        boolean transform=hasEffect(WildMoonEffects.MOONS_CURSE);if(action()==IDLE&&transform!=night()&&--formDelay<=0){formDelay=5+random.nextInt(5);begin(transform?TO_NIGHT:TO_DAY);}
        if(action()!=IDLE){actionAge++;var target=getTarget();if(target!=null&&cling()==Direction.DOWN){getLookControl().setLookAt(target,30,30);yBodyRot=getYRot();}
            if(action()==TO_NIGHT&&actionAge==15)form(true);if(action()==TO_DAY&&actionAge==10){release();form(false);}
            if(action()==LEAP){if(actionAge==2){release();setDiscardFriction(true);setDeltaMovement(leapVelocity.scale(1.05));needsSync=true;}if(actionAge>3){leapTicks++;if(leapAttack&&target!=null&&distanceToSqr(target)<12.25){begin(LEAP_HIT);return;}if(onGround()){begin(LAND);return;}if(leapTicks>6&&latch())return;if(leapTicks>70){begin(IDLE);return;}}return;}
            if(action()==SINGLE&&actionAge==16)strike(l,3,1);if((action()==COMBO||action()==COMBO_ROAR)&&(actionAge==12||actionAge==22))strike(l,3,1);if(action()==SCYTHE&&actionAge==11)strike(l,3.5,1.6F);if(action()==LEAP_HIT&&actionAge==4)strike(l,3.5,1.5F);
            if(action()==BACKFLIP&&actionAge==3){release();var v=getLookAngle().multiply(-1,0,-1).normalize().scale(.9);setDeltaMovement(v.x,.65,v.z);needsSync=true;}
            if((action()==COMBO||action()==COMBO_ROAR)&&howler()&&actionAge==28&&target!=null&&distanceToSqr(target)<25){begin(SCYTHE);return;}
            if(action()==SCYTHE&&actionAge>=6&&actionAge<=9)setDeltaMovement(getDeltaMovement().add(getLookAngle().multiply(.3,0,.3)));
            if(actionAge>=duration(action())){boolean screamed=action()==SCREAM;begin(IDLE);if(screamed)stalking=0;}return;
        }
        if(!night()){setTarget(null);if(tickCount%10==0){var p=l.getNearestPlayer(this,8);if(p!=null&&!p.isCreative()&&!p.isSpectator()){var away=position().subtract(p.position()).normalize();leap(position().add(away.scale(7)).add(0,2,0),false);}else if(random.nextInt(40)==0&&onGround())begin(SIT);}return;}
        WildElokosa chief=leader==null?null:l.getEntity(leader) instanceof WildElokosa e?e:null;if(chief!=null&&!chief.isAlive()){leader=null;chief=null;}
        if(tickCount%10==0){if(chief!=null&&chief.getTarget()!=null)setTarget(chief.getTarget());else if(getTarget()==null||!WildBluff.victim(getTarget())||distanceToSqr(getTarget())>1600){var players=new ArrayList<ServerPlayer>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ServerPlayer.class),getBoundingBox().inflate(32),p->WildBluff.victim(p)&&hasLineOfSight(p),players,24);setTarget(players.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));}}
        var target=getTarget();if(target==null){stalking=700;if(chief!=null&&distanceToSqr(chief)>36&&tickCount%20==0)navigation.moveTo(chief,1.2);else if(tickCount%120==0&&random.nextBoolean())leap(position().add(random.nextInt(13)-6,2,random.nextInt(13)-6),false);return;}
        getLookControl().setLookAt(target,30,30);double distance=distanceToSqr(target);
        if(stalking>0){stalking--;var toward=position().subtract(target.getEyePosition()).normalize();watched=target.getLookAngle().dot(toward)>.8&&hasLineOfSight(target)?watched+1:Math.max(0,watched-1);if(watched>=16||stalking==0||distance<16){begin(SCREAM);stalking=0;return;}if(distance>144&&tickCount%25==0)leap(target.position().add(position().subtract(target.position()).normalize().scale(10)).add(0,2,0),false);return;}
        if(distance<20.25&&dodgeCooldown==0&&random.nextInt(80)==0){dodgeCooldown=100+random.nextInt(81);begin(BACKFLIP);return;}
        if(cooldown==0&&distance<144&&hasLineOfSight(target)){cooldown=100+random.nextInt(81);if(distance>25||cling()!=Direction.DOWN)leap(target.position(),true);else begin(random.nextBoolean()?SINGLE:random.nextBoolean()?COMBO:COMBO_ROAR);}
        else if(cling()==Direction.DOWN&&tickCount%10==0)navigation.moveTo(target,distance<16?.6:1.4);
    }
    private void strike(ServerLevel l,double radius,float multiplier){var target=getTarget();if(target==null||!WildBluff.victim(target)||distanceToSqr(target)>radius*radius||!hasLineOfSight(target))return;var direction=target.position().subtract(position()).multiply(1,0,1).normalize();if(getLookAngle().multiply(1,0,1).normalize().dot(direction)<0)return;target.hurtServer(l,damageSources().mobAttack(this),(float)getAttributeValue(Attributes.ATTACK_DAMAGE)*multiplier);playSound(OwnerWilds.sound("elokosa.night.attack1"),1,1);}
    @Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount){boolean hit=super.hurtServer(l,source,amount);if(hit&&isAlive()){stalking=0;if(source.getEntity() instanceof Player p&&WildBluff.victim(p)&&night())setTarget(p);if(action()==IDLE)begin(HURT);}return hit;}
    @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource source,boolean recent){super.dropCustomDeathLoot(l,source,recent);int looting=source.getEntity() instanceof LivingEntity killer?l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.LOOTING).map(h->EnchantmentHelper.getItemEnchantmentLevel(h,killer.getMainHandItem())).orElse(0):0;if(random.nextFloat()>=.333+.2*looting)return;int moon=Calendar.moonPhase(l.getOverworldClockTime()),phase=night()?Math.min(moon,8-moon):4;spawnAtLocation(l,new ItemStack(OwnerWilds.item("elokosa_paw_"+WildMoonEffects.PAWS[phase])));}
    @Override public boolean causeFallDamage(double d,float m,DamageSource source){return false;}
    @Override protected SoundEvent getHurtSound(DamageSource s){return OwnerWilds.sound("elokosa."+(night()?"night":"day")+".hurt1");}
    @Override protected SoundEvent getDeathSound(){return OwnerWilds.sound("elokosa."+(night()?"night":"day")+".death");}
    @Override protected SoundEvent getAmbientSound(){return night()&&getTarget()!=null?null:OwnerWilds.sound("elokosa."+(night()?"night":"day")+".idle1");}
    @Override protected void tickDeath(){release();navigation.stop();if(++deathTime>=60&&!level().isClientSide())remove(RemovalReason.KILLED);}
    public String clip(){String surface=cling()==Direction.UP?"ceiling":cling()==Direction.DOWN?"ground":"wall";if(!isAlive())return deathTime<19?"death_start":"death_splat";return switch(action()){case TO_NIGHT->"transform_day_to_night"+(surface.equals("ground")?"":"_"+surface);case TO_DAY->"transform_night_to_day"+(surface.equals("ground")?"":"_"+surface);case SCREAM->"scream_"+surface;case SINGLE->"attack_single";case COMBO->"attack_combo";case COMBO_ROAR->"attack_combo_roar";case SCYTHE->"attack_tail_scythe";case LEAP->actionTime(0)<2?"leap_start_"+surface:"leap_air";case LEAP_HIT->"leap_end_slash";case BACKFLIP->"backflip";case LAND->"leap_end_"+surface;case HURT->"hurt_"+surface;case SIT->actionTime(0)<12?"sit_start":actionTime(0)>88?"sit_end":"sit";default->surface.equals("ground")?"idle":"idle_"+surface;};}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar out){out.add(new AnimationController<WildElokosa>("body",2,s->s.setAndContinue(action()==IDLE&&isAlive()?RawAnimation.begin().thenLoop(clip()):RawAnimation.begin().thenPlayAndHold(clip()))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putBoolean("night",night());out.putBoolean("pack_created",packCreated);if(leader!=null)out.store("leader",net.minecraft.core.UUIDUtil.CODEC,leader);out.putInt("cooldown",cooldown);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);leader=in.read("leader",net.minecraft.core.UUIDUtil.CODEC).orElse(null);packCreated=in.getBooleanOr("pack_created",true);cooldown=Math.clamp(in.getIntOr("cooldown",80),0,180);form(in.getBooleanOr("night",false));release();}
}
