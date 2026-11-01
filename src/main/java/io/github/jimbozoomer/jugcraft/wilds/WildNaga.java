package io.github.jimbozoomer.jugcraft.wilds;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Territorial flying encounter: dodge, interruptible acid charge/swoop, fall, stun and recovery. */
public final class WildNaga extends Monster {
    public static EntityType<WildNaga> TYPE;
    public static final int IDLE=0,FLAP=1,SPIT=2,SWOOP=3,FALL=4,LAND=5,RISE=6,DODGE=7;
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(WildNaga.class,EntityDataSerializers.INT),START=SynchedEntityData.defineId(WildNaga.class,EntityDataSerializers.INT),PHASE=SynchedEntityData.defineId(WildNaga.class,EntityDataSerializers.INT);
    private BlockPos home;private Vec3 destination,swoopVector;
    private int actionAge,spitCooldown,swoopCooldown,groundTimer;
    private final Set<UUID> struck=new HashSet<>();
    public WildNaga(EntityType<? extends WildNaga> type,Level l){super(type,l);setNoGravity(true);xpReward=10;}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_naga"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<WildNaga>of(WildNaga::new,MobCategory.MONSTER).sized(3,1).clientTrackingRange(13).updateInterval(1).build(key));FabricDefaultAttributeRegistry.register(TYPE,Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ARMOR,12).add(Attributes.FOLLOW_RANGE,40).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.MOVEMENT_SPEED,.3));SpawnPlacements.register(TYPE,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(t,l,reason,p,r)->p.getY()>=68&&l.canSeeSky(p)&&Monster.isDarkEnoughToSpawn(l,p,r)&&WildSpawns.outsideSettlements(l,p,reason,false)&&(l.getBlockState(p.below()).is(BlockTags.BASE_STONE_OVERWORLD)||l.getBlockState(p.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)));BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.STONY_SHORE),MobCategory.MONSTER,TYPE,20,1,2);WildPoisonBall.register();}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTION,0);b.define(START,0);b.define(PHASE,0);}
    public int action(){return entityData.get(ACTION);}public int phase(){return entityData.get(PHASE);}public float actionTime(float partial){return level().getGameTime()-entityData.get(START)+partial;}
    private void begin(int action){entityData.set(ACTION,action);entityData.set(START,(int)level().getGameTime());actionAge=0;struck.clear();if(action==SPIT)playSound(OwnerWilds.sound("naga.acidcharge"),2,1);if(action==FLAP||action==SWOOP)playSound(OwnerWilds.sound("naga.flap1"),2,action==SWOOP?.7F:1);}
    private static int duration(int action){return switch(action){case FLAP->25;case SPIT->50;case SWOOP->54;case FALL->20;case LAND->8;case RISE->33;case DODGE->10;default->0;};}
    private boolean eligible(Player p){return p.isAlive()&&!p.isSpectator()&&!p.isCreative()&&p.blockPosition().distSqr(home)<=2500&&distanceToSqr(p)<=1600;}
    private void steer(Vec3 target,double speed){Vec3 diff=target.subtract(position());Vec3 wish=diff.lengthSqr()<1?Vec3.ZERO:diff.normalize().scale(speed);Vec3 next=getDeltaMovement().scale(.85).add(wish.scale(.15));if(!level().hasChunkAt(BlockPos.containing(position().add(next.scale(5))))||!level().getWorldBorder().isWithinBounds(BlockPos.containing(position().add(next.scale(5))))||!level().noCollision(this,getBoundingBox().move(next.scale(3)))){next=new Vec3(0,.12,0);destination=null;}setDeltaMovement(next);}
    @Override public void travel(Vec3 input){if(!isAlive()||phase()==1||phase()==2){super.travel(input);return;}move(MoverType.SELF,getDeltaMovement());setDeltaMovement(getDeltaMovement().scale(.98));calculateEntityAnimation(false);}
    @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel l)||!isAlive())return;if(home==null)home=blockPosition();if(spitCooldown>0)spitCooldown--;if(swoopCooldown>0)swoopCooldown--;if(groundTimer>0)groundTimer--;removeEffect(MobEffects.POISON);
        if(action()!=IDLE){actionAge++;if(actionAge>=duration(action())){entityData.set(ACTION,IDLE);actionAge=0;}}
        if(phase()==1){setNoGravity(false);if(onGround()||isInWater()||isInLava()){entityData.set(PHASE,2);groundTimer=60;begin(LAND);setDeltaMovement(0,getDeltaMovement().y,0);playSound(OwnerWilds.sound("misc.groundhit2"),1.5F,1);}return;}
        if(phase()==2){if(groundTimer==0&&action()==IDLE)begin(RISE);if(action()==RISE&&actionAge==15){setNoGravity(true);setDeltaMovement(0,1.6,0);playSound(OwnerWilds.sound("naga.flap1"),2,1);}if(action()==RISE&&actionAge==26)entityData.set(PHASE,0);return;}
        setNoGravity(true);
        if(tickCount%10==0){var targets=new ArrayList<ServerPlayer>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ServerPlayer.class),getBoundingBox().inflate(40),this::eligible,targets,32);setTarget(targets.stream().filter(this::hasLineOfSight).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));}
        var target=getTarget();if(target instanceof Player p&&!eligible(p)){setTarget(null);target=null;}
        if(target!=null){getLookControl().setLookAt(target,30,30);if(action()!=SWOOP||actionAge<15)setYRot((float)Math.toDegrees(Math.atan2(target.getZ()-getZ(),target.getX()-getX()))-90);}
        else if(getDeltaMovement().horizontalDistanceSqr()>.001)setYRot((float)Math.toDegrees(Math.atan2(getDeltaMovement().z,getDeltaMovement().x))-90);yBodyRot=getYRot();
        if(action()==FALL&&actionAge>=17){entityData.set(PHASE,1);setNoGravity(false);return;}
        if(action()==SPIT){setDeltaMovement(getDeltaMovement().scale(.8));if(actionAge<9)setDeltaMovement(getDeltaMovement().add(0,.015,0));if(actionAge==30&&target!=null&&hasLineOfSight(target))WildPoisonBall.fire(this,target);return;}
        if(action()==SWOOP){if(actionAge<16){double f=(actionAge-1)/15D;setDeltaMovement(new Vec3(1.9*Math.sin(f*Math.PI*2)*f,-1.9*Math.sin(f*Math.PI*2)*f,.8*Math.sin(f*Math.PI*2)).yRot((float)Math.toRadians(-getYRot()-90)));}else if(actionAge==23&&target!=null){Vec3 d=target.position().subtract(position());swoopVector=d.normalize().scale(Math.min(1.8,Math.max(.5,d.length()*.1)));}else if(actionAge>=23&&actionAge<44&&swoopVector!=null){double phase=(actionAge-23)/21D;setDeltaMovement(new Vec3(swoopVector.x*(1-Math.exp(2*(phase-1))),swoopVector.y*Math.cos(phase*Math.PI),swoopVector.z*(1-Math.exp(2*(phase-1)))));if(target!=null&&distanceToSqr(target)<=16&&hasLineOfSight(target)&&struck.add(target.getUUID()))target.hurtServer(l,damageSources().mobAttack(this),4);}else setDeltaMovement(getDeltaMovement().scale(.9));if(actionAge==22)playSound(OwnerWilds.sound("naga.roar1"),3,1);return;}
        if(action()==FLAP&&actionAge>=4&&actionAge<=9)setDeltaMovement(getDeltaMovement().add(0,.1,0));
        if(action()==IDLE&&target!=null){if(swoopCooldown==0&&getY()>target.getY()&&random.nextInt(80)==0){begin(SWOOP);swoopCooldown=90;return;}if(spitCooldown==0&&random.nextInt(80)==0){begin(SPIT);spitCooldown=120;return;}if(tickCount%5==0){var arrows=new ArrayList<Projectile>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(Projectile.class),getBoundingBox().inflate(30),e->e.tickCount>1&&e.getOwner()!=this,arrows,24);for(var arrow:arrows){var motion=arrow.getDeltaMovement();if(motion.lengthSqr()>.01&&motion.normalize().dot(position().subtract(arrow.position()).normalize())>.96){Vec3 dodge=motion.cross(new Vec3(0,1,0)).normalize().scale(random.nextBoolean()?1.2:-1.2);if(l.noCollision(this,getBoundingBox().move(dodge.scale(2)))){setDeltaMovement(getDeltaMovement().add(dodge));begin(DODGE);break;}}}}}
        if(action()==DODGE)return;
        if(target!=null){double angle=tickCount*.025;steer(target.position().add(Math.cos(angle)*12,7,Math.sin(angle)*12),.45);}else{if(destination==null||distanceToSqr(destination)<4||tickCount%100==0){destination=Vec3.atCenterOf(home).add(random.nextInt(41)-20,8+random.nextInt(12),random.nextInt(41)-20);}steer(destination,.45);}
        if(onGround()&&action()==IDLE)begin(FLAP);
    }
    @Override public boolean hurtServer(ServerLevel l,DamageSource source,float damage){boolean hit=super.hurtServer(l,source,damage);if(hit&&isAlive()&&phase()==0&&(action()==SPIT&&actionAge<30||action()==SWOOP&&actionAge<25))begin(FALL);return hit;}
    @Override public boolean causeFallDamage(double distance,float multiplier,DamageSource source){return phase()==1&&super.causeFallDamage(distance,multiplier,source);}
    @Override protected void tickDeath(){deathTime++;setNoGravity(false);if(deathTime>=70&&!level().isClientSide())remove(RemovalReason.KILLED);}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(home!=null)out.store("home",BlockPos.CODEC,home);out.putInt("phase",phase());out.putInt("ground_timer",groundTimer);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);home=in.read("home",BlockPos.CODEC).orElse(blockPosition());entityData.set(PHASE,Math.clamp(in.getIntOr("phase",0),0,2));groundTimer=Math.clamp(in.getIntOr("ground_timer",0),0,60);setNoGravity(phase()==0);}
}
