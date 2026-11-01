package io.github.jimbozoomer.jugcraft.wilds;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;

/** Sleeping chamber guardian; only the exposed back during an embedded axe attack is vulnerable. */
public final class WildWroughtnaut extends Monster {
    public static EntityType<WildWroughtnaut> TYPE;
    public static final int IDLE=0,SWING=1,TWICE=2,THRICE=3,VERTICAL=4,STOMP=5,ACTIVATE=6,DEACTIVATE=7,HURT=8;
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(WildWroughtnaut.class,EntityDataSerializers.INT),START=SynchedEntityData.defineId(WildWroughtnaut.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ACTIVE=SynchedEntityData.defineId(WildWroughtnaut.class,EntityDataSerializers.BOOLEAN);
    private final ServerBossEvent bar=new ServerBossEvent(UUID.randomUUID(),net.minecraft.network.chat.Component.translatable("entity.jugcraft.wilds_ferrous_wroughtnaut"),net.minecraft.world.BossEvent.BossBarColor.RED,net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);
    private BlockPos rest,ceiling;private int actionAge,attackWait,ceilingRadius,ceilingDelay,ceilingTicks,ceilingDuration;
    private final Set<UUID> stompHit=new HashSet<>();
    public WildWroughtnaut(EntityType<? extends WildWroughtnaut> type,Level l){super(type,l);setPersistenceRequired();xpReward=30;}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_ferrous_wroughtnaut"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<WildWroughtnaut>of(WildWroughtnaut::new,MobCategory.MONSTER).sized(2.5F,3.5F).clientTrackingRange(12).updateInterval(1).build(key));FabricDefaultAttributeRegistry.register(TYPE,Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,40).add(Attributes.ATTACK_DAMAGE,30).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.MOVEMENT_SPEED,.2).add(Attributes.FOLLOW_RANGE,24));
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,player,alive)->{
            if(alive)return;var guardians=new ArrayList<WildWroughtnaut>();
            player.level().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(WildWroughtnaut.class),player.getBoundingBox().inflate(40),e->e.isAlive()&&e.distanceToSqr(player)<=1600,guardians,16);
            for(var guardian:guardians)guardian.setHealth(guardian.getMaxHealth());
        });
    }
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTION,0);b.define(START,0);b.define(ACTIVE,false);}
    public int action(){return entityData.get(ACTION);}public boolean active(){return entityData.get(ACTIVE);}public float actionTime(float partial){return level().getGameTime()-entityData.get(START)+partial;}
    private void begin(int action){entityData.set(ACTION,action);entityData.set(START,(int)level().getGameTime());actionAge=0;getNavigation().stop();stompHit.clear();if(action==ACTIVATE)entityData.set(ACTIVE,true);if(action==DEACTIVATE)entityData.set(ACTIVE,false);if(action==SWING||action==VERTICAL)playSound(OwnerWilds.sound(action==SWING?"wroughtnaut.pre_swing1":"wroughtnaut.pre_swing2"),1.5F,1);}
    private static int duration(int n){return switch(n){case SWING->50;case TWICE->36;case THRICE->59;case VERTICAL->105;case STOMP->40;case ACTIVATE->45;case DEACTIVATE,HURT->15;default->0;};}
    private void face(LivingEntity e){setYRot((float)Math.toDegrees(Math.atan2(e.getZ()-getZ(),e.getX()-getX()))-90);yBodyRot=getYRot();yHeadRot=getYRot();}
    private float angle(Entity e){return Math.abs(Mth.wrapDegrees((float)Math.toDegrees(Math.atan2(e.getZ()-getZ(),e.getX()-getX()))-90-getYRot()));}
    private void strike(ServerLevel l,double range,float arc,float damage){var victims=new ArrayList<ServerPlayer>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ServerPlayer.class),getBoundingBox().inflate(range,1,range),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator(),victims,32);for(var p:victims){if(distanceToSqr(p)>range*range||angle(p)>arc/2||!hasLineOfSight(p))continue;p.hurtServer(l,damageSources().mobAttack(this),damage);if(p.isBlocking())p.getUseItem().hurtAndBreak(400,p,p.getUsedItemHand().asEquipmentSlot());}playSound(OwnerWilds.sound("wroughtnaut.axe_hit"),1,.5F);}
    @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel l)||!isAlive())return;tickCeiling(l);bar.setProgress(getHealth()/getMaxHealth());bar.setVisible(active());if(l.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){getNavigation().stop();setTarget(null);if(active())begin(DEACTIVATE);heal(.3F);return;}if(rest==null)rest=blockPosition();if(attackWait>0)attackWait--;if(action()!=IDLE&&++actionAge>=duration(action())){entityData.set(ACTION,IDLE);actionAge=0;attackWait=20;}
        if(tickCount%10==0){var players=new ArrayList<ServerPlayer>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ServerPlayer.class),getBoundingBox().inflate(24,8,24),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&p.blockPosition().distSqr(rest)<1024,players,32);setTarget(players.stream().filter(this::hasLineOfSight).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));}
        var target=getTarget();if(!active()){setDeltaMovement(0,getDeltaMovement().y,0);if(target!=null&&distanceToSqr(target)<=20.25&&action()==IDLE)begin(ACTIVATE);else heal(.3F);return;}
        if(action()!=IDLE){setDeltaMovement(0,getDeltaMovement().y,0);if(target!=null&&(action()==VERTICAL&&actionAge<21||action()==SWING&&actionAge<21||action()==TWICE&&actionAge<10||action()==THRICE&&actionAge<22))face(target);
            if((action()==SWING||action()==VERTICAL)&&actionAge==27)strike(l,5,action()==VERTICAL?40:100,action()==VERTICAL?45:30);
            if(action()==TWICE&&actionAge==12)strike(l,5,100,30);if(action()==THRICE&&actionAge==24)strike(l,5.2,360,30);
            if(action()==SWING&&actionAge==37&&target!=null&&distanceToSqr(target)<56.25&&getHealth()<=36&&random.nextFloat()<.6)begin(TWICE);else if(action()==TWICE&&actionAge==23&&target!=null&&distanceToSqr(target)<72.25&&getHealth()<=24&&random.nextFloat()<.6)begin(THRICE);
            if(action()==ACTIVATE&&actionAge==1)playSound(OwnerWilds.sound("wroughtnaut.creak"),1,1);if(action()==STOMP&&actionAge==10)playSound(OwnerWilds.sound("wroughtnaut.step"),2,.5F);
            if(action()==VERTICAL&&actionAge==28){startCeiling(l);playSound(OwnerWilds.sound("wroughtnaut.axe_land"),1,.5F);l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,l.getBlockState(blockPosition().below())),getX(),getY()+.2,getZ(),24,2,.1,2,.1);}
            if(action()==STOMP&&actionAge>=10&&actionAge<=16&&actionAge%2==0){int radius=actionAge/2-2;var players=new ArrayList<ServerPlayer>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ServerPlayer.class),getBoundingBox().inflate(radius,1,radius),p->p.onGround()&&!p.isCreative()&&!p.isSpectator(),players,32);for(var p:players){double distance=distanceTo(p);if(distance>radius+1||distance<radius-1.5||!hasLineOfSight(p)||!stompHit.add(p.getUUID()))continue;float factor=1-(float)distance/6;p.hurtServer(l,damageSources().mobAttack(this),Math.max(1,factor*5+1));var push=p.position().subtract(position()).normalize().scale(.1+Math.max(0,factor)*.15);p.setDeltaMovement(p.getDeltaMovement().add(push.x,.1+Math.max(0,factor)*.15,push.z));p.needsSync=true;}l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,l.getBlockState(blockPosition().below())),getX(),getY()+.1,getZ(),16,radius,.1,radius,.05);}
            return;
        }
        if(target==null){if(blockPosition().distSqr(rest)>4)getNavigation().moveTo(rest.getX()+.5,rest.getY(),rest.getZ()+.5,1);else begin(DEACTIVATE);return;}
        face(target);double distance=distanceToSqr(target);if(distance>20.25)getNavigation().moveTo(target,1);else{getNavigation().stop();if(attackWait==0)begin(random.nextInt(4)==0?STOMP:random.nextBoolean()?VERTICAL:SWING);}if(tickCount%20==1&&getDeltaMovement().horizontalDistanceSqr()>.001)playSound(OwnerWilds.sound("wroughtnaut.step"),.5F,.5F);
    }
    private void startCeiling(ServerLevel level){
        double facing=Math.toRadians(getYRot());var impact=position().add(-Math.sin(facing)*4.2,0,Math.cos(facing)*4.2);
        var at=BlockPos.containing(impact.x,getBoundingBox().maxY,impact.z);
        for(int i=0;i<5;i++,at=at.above()){
            if(!level.hasChunkAt(at))return;
            if(level.getBlockState(at).isSolid()){
                float strength=(4-i)/5F;ceiling=at;ceilingRadius=(int)Math.ceil(Math.sqrt(1-strength*strength)*5);ceilingDelay=random.nextInt(5)+3;ceilingTicks=ceilingDuration=random.nextInt(60)+20;return;
            }
        }
    }
    private void tickCeiling(ServerLevel level){
        if(ceiling==null||--ceilingDelay>0)return;
        float t=ceilingTicks/(float)ceilingDuration;int count=(int)Math.ceil((1-Math.sqrt(1-t*t))*ceilingRadius*ceilingRadius*.15);boolean sound=true;
        for(int i=0;i<count;i++){
            double a=random.nextDouble()*Math.PI*2,d=random.nextDouble()*ceilingRadius,x=ceiling.getX()+Math.cos(a)*d,z=ceiling.getZ()+Math.sin(a)*d;
            var at=BlockPos.containing(x,ceiling.getY(),z);if(!level.hasChunkAt(at))continue;var state=level.getBlockState(at);
            if(state.getRenderShape()==net.minecraft.world.level.block.RenderShape.INVISIBLE)continue;
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST,state),x,ceiling.getY()-.1-random.nextDouble()*.3,z,1,0,0,0,0);
            if(sound&&random.nextFloat()<.075F){var sounds=state.getSoundType();level.playSound(null,blockPosition(),sounds.getBreakSound(),net.minecraft.sounds.SoundSource.BLOCKS,sounds.getVolume()*2,sounds.getPitch()*.6F);sound=false;}
        }
        if(--ceilingTicks<=0)ceiling=null;
    }
    @Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount){if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return super.hurtServer(l,source,amount);var attacker=source.getEntity();if(attacker instanceof LivingEntity living&&!(living instanceof Player p&&(p.isCreative()||p.isSpectator())))setTarget(living);if(attacker!=null&&action()==VERTICAL&&actionAge>26&&actionAge<85&&angle(attacker)>110){boolean hurt=super.hurtServer(l,source,amount);if(hurt&&isAlive())begin(HURT);return hurt;}if(tickCount%5==0)playSound(OwnerWilds.sound("wroughtnaut.undamaged"),.4F,2);return false;}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);bar.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
    @Override public void remove(RemovalReason reason){bar.removeAllPlayers();super.remove(reason);}
    @Override protected void tickDeath(){bar.setVisible(false);deathTime++;if(deathTime>=130&&!level().isClientSide())remove(RemovalReason.KILLED);}
    @Override public void checkDespawn(){if(level().getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL)super.checkDespawn();}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(rest!=null)out.store("rest",BlockPos.CODEC,rest);out.putBoolean("active",active());}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);rest=in.read("rest",BlockPos.CODEC).orElse(blockPosition());entityData.set(ACTIVE,in.getBooleanOr("active",false));}
}
