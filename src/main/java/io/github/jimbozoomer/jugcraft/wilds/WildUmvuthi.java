package io.github.jimbozoomer.jugcraft.wilds;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Stationary sun guardian: authored attack windows, healer phase and renewable blessing trade. */
public final class WildUmvuthi extends Monster implements GeoEntity {
    public static EntityType<WildUmvuthi> TYPE;
    public static final int IDLE=0,STRIKE=1,FLARE=2,BEAM=3,NOVA=4,SPAWN=5,HEALERS=6,BLESS=7,BELLY=8,ROAR=9,HURT=10;
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(WildUmvuthi.class,EntityDataSerializers.INT),START=SynchedEntityData.defineId(WildUmvuthi.class,EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar=new ServerBossEvent(UUID.randomUUID(),getDisplayName(),BossEvent.BossBarColor.YELLOW,BossEvent.BossBarOverlay.PROGRESS);
    private final Set<UUID> traded=new HashSet<>();
    private UUID anger,customer;
    private static final EntityDataAccessor<Boolean> COMBAT=SynchedEntityData.defineId(WildUmvuthi.class,EntityDataSerializers.BOOLEAN);
    private int age,strikeCooldown,beamCooldown,novaCooldown,spawnCooldown,healDelay=75;
    private float lost;
    private Vec3 targetStart=Vec3.ZERO,strikeAt;
    public WildUmvuthi(EntityType<? extends WildUmvuthi> t,Level l){super(t,l);xpReward=45;setPersistenceRequired();}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_umvuthi"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<WildUmvuthi>of(WildUmvuthi::new,MobCategory.MONSTER).sized(1.5F,3.2F).clientTrackingRange(16).updateInterval(1).build(key));FabricDefaultAttributeRegistry.register(TYPE,Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,150).add(Attributes.ATTACK_DAMAGE,2).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.FOLLOW_RANGE,40).add(Attributes.MOVEMENT_SPEED,0));}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTION,IDLE);b.define(START,0);b.define(COMBAT,false);}
    public int action(){return entityData.get(ACTION);}public float actionTime(float p){return level().getGameTime()-entityData.get(START)+p;}
    public boolean angry(){return entityData.get(COMBAT); }
    private void begin(int action){entityData.set(ACTION,action);entityData.set(START,(int)level().getGameTime());age=0;navigation.stop();if(action==STRIKE&&getTarget()!=null)targetStart=getTarget().position();if(action==NOVA)playSound(OwnerWilds.sound("supernova.start"),3,1);if(action==FLARE)playSound(OwnerWilds.sound("umvuthi.burst"),1.7F,1.5F);if(action==ROAR)playSound(OwnerWilds.sound("umvuthi.roar"),3,1);if(action==BLESS)playSound(OwnerWilds.sound("umvuthi.bless"),2,1);}
    private boolean enemy(LivingEntity e){return e instanceof Player p&&WildBluff.victim(p)&&level().getDifficulty()!=Difficulty.PEACEFUL&&(!WildSunEquipment.masked(p)||p.getUUID().equals(anger))||e instanceof WildElokosa&&e.isAlive();}
    private List<WildUmvuthana> followers(ServerLevel l){var list=new ArrayList<WildUmvuthana>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(WildUmvuthana.class),getBoundingBox().inflate(40),e->e.isAlive()&&e.ownerId()==null,list,32);return list;}
    @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel l)||!isAlive())return;setDeltaMovement(0,getDeltaMovement().y,0);bar.setProgress(getHealth()/getMaxHealth());bar.setVisible(getTarget()!=null);entityData.set(COMBAT,getTarget()!=null);if(strikeCooldown>0)strikeCooldown--;if(beamCooldown>0&&action()!=NOVA)beamCooldown--;if(novaCooldown>0&&action()!=BEAM)novaCooldown--;if(spawnCooldown>0)spawnCooldown--;
        if(tickCount%10==0&&action()!=BLESS){var current=getTarget();if(current!=null&&(!enemy(current)||distanceToSqr(current)>1600))setTarget(null);if(getTarget()==null){var players=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(32),e->enemy(e)&&hasLineOfSight(e),players,32);setTarget(players.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));}}
        var target=getTarget();if(target!=null){getLookControl().setLookAt(target,10,10);healDelay=75;}else if(--healDelay<=0){heal(.3F);if(getHealth()==getMaxHealth())lost=0;}
        if(action()!=IDLE){age++;
            if(action()==STRIKE&&age==7&&target!=null){var velocity=target.position().subtract(targetStart).scale(38.0/9);var prediction=target.position().add(velocity);if(prediction.distanceToSqr(position())<9)prediction=target.position();strikeAt=prediction;}
            if(action()==STRIKE&&age==9&&strikeAt!=null){var ground=ground(strikeAt);if(ground!=null)WildSolar.spawn(l,this,WildSolar.STRIKE,ground,43);playSound(OwnerWilds.sound("umvuthi.attack"),1.4F,1);}
            if(action()==FLARE&&age==12){var targets=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(4,8,4),e->WildSolar.canHarm(this,e),targets,64);for(var e:targets)if(distanceToSqr(e)<=16&&hasLineOfSight(e)&&e.hurtServer(l,damageSources().mobAttack(this),2)){var v=e.position().subtract(position()).multiply(1,0,1).normalize().scale(3);e.setDeltaMovement(v.x,.3,v.z);e.needsSync=true;}}
            if(action()==BEAM&&age==22)WildSolar.spawn(l,this,WildSolar.BEAM,getEyePosition(),55);
            if(action()==NOVA){if(age<44&&age>5){var targets=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(10),e->WildSolar.canHarm(this,e),targets,64);for(var e:targets)if(hasLineOfSight(e)){var v=position().add(0,1,0).subtract(e.position()).normalize().scale(.05);e.setDeltaMovement(e.getDeltaMovement().add(v));e.needsSync=true;}}if(age==44)WildSolar.spawn(l,this,WildSolar.NOVA,position().add(0,1.5,0),40);}
            if((action()==SPAWN||action()==HEALERS)&&(age==6||age==24||age==42))summon(l,action()==HEALERS);
            if(action()==BELLY&&(age==9||age==29))playSound(OwnerWilds.sound("umvuthi.belly"),3,1);
            if(action()==BLESS&&age==40){var p=customer==null?null:l.getServer().getPlayerList().getPlayer(customer);if(p!=null&&p.level()==l&&distanceToSqr(p)<256&&p.isAlive())p.addEffect(new MobEffectInstance(WildSunEquipment.BLESSING,-1,0,false,false),this);}
            int duration=switch(action()){case STRIKE->22;case FLARE->31;case BEAM,NOVA->100;case SPAWN,HEALERS->54;case BLESS->84;case BELLY->40;case ROAR->70;default->13;};if(age>=duration){customer=null;begin(IDLE);}return;
        }
        if(target==null){if(random.nextInt(200)==0)begin(BELLY);return;}
        var followers=followers(l);if(lost>=45&&followers.stream().filter(e->e.mask()==5).count()<3){lost=0;begin(HEALERS);return;}
        double distance=distanceToSqr(target);if(spawnCooldown==0&&distance>30.25&&followers.size()<4&&random.nextInt(80)==0){spawnCooldown=200;begin(SPAWN);}else if(getHealth()/getMaxHealth()<=.6&&beamCooldown==0&&distance<900&&hasLineOfSight(target)){beamCooldown=230;begin(BEAM);}else if(getHealth()/getMaxHealth()<=.6&&novaCooldown==0&&distance<=110.25&&!hasEffect(WildSunEquipment.SUNBLOCK)){novaCooldown=230;begin(NOVA);}else if(distance<16){begin(FLARE);}else if(strikeCooldown==0){strikeCooldown=(int)(50-Math.min(.6,1-getHealth()/getMaxHealth())/.6*20);begin(STRIKE);}
    }
    private Vec3 ground(Vec3 at){for(int y=3;y>=-6;y--){var p=BlockPos.containing(at).offset(0,y,0);if(!level().hasChunkAt(p))return null;if(level().getBlockState(p).isSolidRender()&&level().getBlockState(p.above()).isAir()&&level().canSeeSky(p.above()))return net.minecraft.world.phys.Vec3.atBottomCenterOf(p.above());}return null;}
    private void summon(ServerLevel l,boolean healer){if(followers(l).size()>=7)return;var e=(healer?WildUmvuthana.CRANE:WildUmvuthana.TYPE).create(l,EntitySpawnReason.MOB_SUMMONED);if(e==null)return;double angle=getYRot()*Math.PI/180+(age/18-1)*.7;var at=position().add(-Math.sin(angle)*3,1,Math.cos(angle)*3);if(!l.hasChunkAt(BlockPos.containing(at))||!l.getWorldBorder().isWithinBounds(BlockPos.containing(at)))return;e.bind(this,healer?5:1+random.nextInt(4));e.setPos(at);e.setYRot(getYRot());if(l.noCollision(e)){e.setDeltaMovement(-Math.sin(angle)*.5,.4,Math.cos(angle)*.5);l.addFreshEntity(e);}}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;if(!isAlive()||getTarget()!=null||action()!=IDLE||!WildSunEquipment.masked(p))return InteractionResult.PASS;
        boolean paid=traded.contains(p.getUUID());var held=p.getItemInHand(hand);if(!paid&&(!held.is(Items.GOLD_BLOCK)||held.getCount()<7)){p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Offer 7 gold blocks for the Sun's Blessing. Wear a tribal mask to approach peacefully. Future blessings from this guardian are free."));return InteractionResult.SUCCESS;}
        if(!paid&&traded.size()>=4096){p.sendSystemMessage(net.minecraft.network.chat.Component.literal("This guardian cannot accept another pact."));return InteractionResult.FAIL;}
        if(!paid){held.consume(7,p);traded.add(p.getUUID());}customer=p.getUUID();setTarget(null);begin(BLESS);return InteractionResult.SUCCESS;
    }
    @Override public boolean hurtServer(ServerLevel l,DamageSource s,float amount){if(hasEffect(WildSunEquipment.SUNBLOCK)&&!s.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return false;float before=getHealth();boolean hit=super.hurtServer(l,s,amount);if(hit){lost+=Math.max(0,before-getHealth());healDelay=75;if(s.getEntity() instanceof Player p&&!p.isCreative()){anger=p.getUUID();setTarget(p);}if(action()==IDLE)begin(HURT);}return hit;}
    @Override public void checkDespawn(){}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);bar.addPlayer(p);}@Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
    @Override protected void tickDeath(){bar.setVisible(false);if(++deathTime>=115&&!level().isClientSide())remove(RemovalReason.KILLED);}
    @Override protected SoundEvent getHurtSound(DamageSource s){return OwnerWilds.sound("umvuthi.hurt");}@Override protected SoundEvent getDeathSound(){return OwnerWilds.sound("umvuthi.die");}
    public String clip(){if(!isAlive())return "death";return switch(action()){case STRIKE->"sun_strike";case FLARE->"flare";case BEAM->"solar_beam";case NOVA->"supernova";case SPAWN,HEALERS->"spawn_strix";case BLESS->"bless";case BELLY->"belly_drum";case ROAR->"roar";case HURT->"hurt";default->"idle";};}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<WildUmvuthi>("body",2,s->s.setAndContinue(action()==IDLE?RawAnimation.begin().thenLoop(clip()):RawAnimation.begin().thenPlayAndHold(clip()))));for(String name:new String[]{"mask_twitch","blink"})c.add(new AnimationController<WildUmvuthi>(name,0,s->s.setAndContinue(RawAnimation.begin().thenLoop(name))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.store("traded",UUIDUtil.CODEC.listOf(0,4096),List.copyOf(traded));if(anger!=null)out.store("anger",UUIDUtil.CODEC,anger);out.putFloat("health_lost",lost);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);traded.clear();traded.addAll(in.read("traded",UUIDUtil.CODEC.listOf(0,4096)).orElse(List.of()));anger=in.read("anger",UUIDUtil.CODEC).orElse(null);lost=Math.clamp(in.getFloatOr("health_lost",0),0,150);}
}
