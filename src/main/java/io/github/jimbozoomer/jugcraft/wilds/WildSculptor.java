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
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Peaceful challenge host with a separate provoked fight; progress and reward payment survive saves. */
public final class WildSculptor extends Monster implements GeoEntity {
    public static EntityType<WildSculptor> TYPE;
    public static final int IDLE=0,SCAN=1,START=2,TRIAL=3,PASS=4,FAIL=5,DISAPPEAR=6,ATTACK=7,GUARD=8,HURT=9,TALK=10;
    public static final int TEST_HEIGHT=60,TEST_TIME=8000;
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(WildSculptor.class,EntityDataSerializers.INT),AGE=SynchedEntityData.defineId(WildSculptor.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DESCENDING=SynchedEntityData.defineId(WildSculptor.class,EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar=new ServerBossEvent(UUID.randomUUID(),Component.literal("Sculptor's Trial"),BossEvent.BossBarColor.GREEN,BossEvent.BossBarOverlay.PROGRESS);
    private WildTrialCourse plannedCourse;
    private UUID playerId,trialId,pillarId,anger,combatPillar;private BlockPos origin;private int age,elapsed,scanY=1,healDelay=75,combatCooldown;private boolean rewarded,descending;private Vec3 lastPlayer;
    public WildSculptor(EntityType<? extends WildSculptor> t,Level l){super(t,l);xpReward=40;setPersistenceRequired();}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_sculptor"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<WildSculptor>of(WildSculptor::new,MobCategory.MONSTER).sized(1.2F,2.4F).clientTrackingRange(16).updateInterval(1).build(key));FabricDefaultAttributeRegistry.register(TYPE,Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,140).add(Attributes.ATTACK_DAMAGE,10).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.FOLLOW_RANGE,60).add(Attributes.MOVEMENT_SPEED,0));}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTION,IDLE);b.define(AGE,0);b.define(DESCENDING,false);}
    public int action(){return entityData.get(ACTION);}public int actionAge(){return entityData.get(AGE);}public BlockPos origin(){return origin==null?blockPosition():origin;}
    public boolean trialAlive(UUID id){return isAlive()&&id.equals(trialId)&&action()!=IDLE&&action()!=DISAPPEAR;}
    private void begin(int action){entityData.set(ACTION,action);age=0;entityData.set(AGE,0);navigation.stop();}
    private ServerPlayer player(ServerLevel l){return playerId==null?null:l.getServer().getPlayerList().getPlayer(playerId);}
    private WildBoulder pillar(ServerLevel l){return pillarId!=null&&l.getEntity(pillarId) instanceof WildBoulder b?b:null;}
    private List<WildBoulder> rocks(ServerLevel l){var result=new ArrayList<WildBoulder>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(WildBoulder.class),new AABB(origin()).inflate(18,65,18),e->trialId!=null&&e.belongsTo(trialId),result,160);return result;}
    @Override protected InteractionResult mobInteract(Player p,InteractionHand hand){
        if(!(p instanceof ServerPlayer player))return InteractionResult.SUCCESS;
        if(action()==TRIAL&&player.getUUID().equals(playerId)&&distanceToSqr(player)<16&&player.getY()>=origin().getY()+TEST_HEIGHT-1){pass(player);return InteractionResult.SUCCESS;}
        if(action()!=IDLE||getTarget()!=null||combatPillar!=null||!isAlive())return InteractionResult.PASS;
        if(!p.getItemInHand(hand).is(OwnerWilds.item("bluff_rod"))){player.sendSystemMessage(Component.literal("Offer one Bluff Rod to begin a 400-second climbing trial. Reach me at the summit and interact to earn the Earthrend Gauntlet. Flying or leaving the course ends the attempt."));begin(TALK);return InteractionResult.SUCCESS;}
        origin=blockPosition();if(origin.getY()+TEST_HEIGHT+4>=level().getMaxY()){player.sendSystemMessage(Component.literal("There is not enough height for the trial here."));return InteractionResult.FAIL;}
        playerId=p.getUUID();plannedCourse=null;scanY=1;begin(SCAN);player.sendSystemMessage(Component.literal("Checking the space above the trial. Your Bluff Rod is only taken when the course is ready."));return InteractionResult.SUCCESS;
    }
    private void scan(ServerLevel l,ServerPlayer p){
        if(p==null||p.level()!=l||!p.isAlive()||distanceToSqr(p)>64){playerId=null;plannedCourse=null;begin(IDLE);return;}
        if(scanY<=TEST_HEIGHT+3){
            double radius=WildTrialCourse.radius(scanY);
            for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){
                if(x*x+z*z>radius*radius)continue;var pos=origin.offset(x,scanY,z);
                if(!l.hasChunkAt(pos)||!l.getWorldBorder().isWithinBounds(pos)||!io.github.jimbozoomer.jugcraft.concordance.Authority.mayChange(l,p,pos)||!l.getBlockState(pos).isAir()&&!l.getBlockState(pos).is(Blocks.LIGHT)){
                    p.sendSystemMessage(Component.literal("The trial is obstructed or protected at "+pos.toShortString()+". Clear permitted space above the Sculptor first."));playerId=null;plannedCourse=null;begin(IDLE);return;
                }
            }
            scanY++;return;
        }
        if(plannedCourse==null)plannedCourse=new WildTrialCourse(net.minecraft.world.phys.Vec3.atBottomCenterOf(origin));
        plannedCourse.tick(l,this,p);
        if(!plannedCourse.done()&&!plannedCourse.failed())return;
        if(plannedCourse.failed()){p.sendSystemMessage(Component.literal("A safe route could not be formed. No rod was taken; offer it again to try a new layout."));plannedCourse=null;playerId=null;begin(IDLE);return;}
        int slot=-1;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(OwnerWilds.item("bluff_rod"))){slot=i;break;}
        if(slot<0){playerId=null;plannedCourse=null;begin(IDLE);return;}
        if(!plannedCourse.validate(l,p)||!makeCourse(l)){p.sendSystemMessage(Component.literal("The course could not be created safely. No rod was taken."));playerId=null;trialId=null;plannedCourse=null;begin(IDLE);return;}
        p.getInventory().getItem(slot).consume(1,p);plannedCourse=null;rewarded=false;elapsed=0;lastPlayer=null;begin(START);playSound(OwnerWilds.sound("sculptor.test_start"),1,1);
    }
    private boolean makeCourse(ServerLevel l){
        if(plannedCourse==null||!plannedCourse.done())return false;
        trialId=UUID.randomUUID();var made=new ArrayList<WildBoulder>();var center=net.minecraft.world.phys.Vec3.atBottomCenterOf(origin);
        var column=WildBoulder.create(l,this,center,WildBoulder.PILLAR,1,9000);if(column==null)return false;
        column.trial(trialId,false);column.height(.1F);made.add(column);
        for(var step:plannedCourse.steps()){
            var rock=WildBoulder.create(l,this,step.at(),WildBoulder.PLATFORM,1,9000);if(rock==null)return false;
            rock.trial(trialId,step.fragile());rock.awaitPillar();made.add(rock);
        }
        for(var rock:made)if(!l.addFreshEntity(rock)){for(var inserted:made)inserted.discard();return false;}
        pillarId=column.getUUID();return true;
    }
    boolean platformReached(ServerLevel level,double y){var column=pillar(level);return column!=null&&origin().getY()+column.height()>=y;}
    private WildBoulder combatColumn(ServerLevel level){return combatPillar!=null&&level.getEntity(combatPillar) instanceof WildBoulder b&&b.getOwner()==this&&b.mode()==WildBoulder.PILLAR?b:null;}
    private void combatHeight(ServerLevel level,boolean fighting){
        var column=combatColumn(level);
        if(column==null&&fighting){
            if(!onGround()||tickCount%20!=0)return;
            // A fight can start away from the monastery. Use the current ground,
            // never teleport back through a player's later building.
            origin=blockPosition();column=WildBoulder.create(level,this,net.minecraft.world.phys.Vec3.atBottomCenterOf(origin),WildBoulder.PILLAR,1,9000);
            if(column==null)return;column.height(.1F);column.combatPillar();
            if(!level.addFreshEntity(column))return;combatPillar=column.getUUID();
            playSound(OwnerWilds.sound("sculptor.test_start"),1,1);
        }
        if(column==null){if(combatPillar!=null){combatPillar=null;setNoGravity(false);}return;}
        float next=fighting?Math.min(TEST_HEIGHT,column.height()+.25F):Math.max(.05F,column.height()-.25F);
        var base=column.position();var top=new AABB(base.x-1,base.y+next,base.z-1,base.x+1,base.y+next+getBbHeight(),base.z+1);
        boolean clear=top.maxY<level.getMaxY()&&level.getWorldBorder().isWithinBounds(top);
        for(var at:BlockPos.betweenClosed(BlockPos.containing(top.minX,top.minY,top.minZ),BlockPos.containing(top.maxX,top.maxY,top.maxZ)))
            if(!level.hasChunkAt(at)||!level.getBlockState(at).getCollisionShape(level,at).isEmpty())clear=false;
        if(clear){column.moveRiders(level,next-column.height());column.height(next);setPos(base.add(0,next,0));}
        setNoGravity(true);setDeltaMovement(Vec3.ZERO);fallDistance=0;
        if(!fighting&&next<=.05F&&clear){column.discard();combatPillar=null;setNoGravity(false);}
    }
    private void pass(ServerPlayer p){begin(PASS);descending=true;entityData.set(DESCENDING,true);p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,280,0,false,false));for(var rock:rocks(p.level()))if(rock.mode()==WildBoulder.PLATFORM)rock.descend();playSound(OwnerWilds.sound("sculptor.congrats"),1,1);}
    private void fail(ServerLevel l,String reason){if(action()==FAIL||action()==PASS)return;var p=player(l);if(p!=null&&p.level()==l){p.sendSystemMessage(Component.literal(reason));p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,280,0,false,false));}descending=true;entityData.set(DESCENDING,true);begin(FAIL);for(var rock:rocks(l))if(rock.mode()==WildBoulder.PLATFORM)rock.descend();playSound(OwnerWilds.sound("sculptor.disappoint"),1,1);}
    @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel l)||!isAlive())return;if(origin==null)origin=blockPosition();setDeltaMovement(0,getDeltaMovement().y,0);entityData.set(AGE,++age);if(combatCooldown>0)combatCooldown--;var p=player(l);
        if(action()==SCAN){scan(l,p);return;}if(action()==TALK||action()==HURT){if(age>=27)begin(IDLE);return;}
        if(action()==DISAPPEAR){if(age>=80)discard();return;}
        if(action()==START||action()==TRIAL){bar.setVisible(true);bar.setName(Component.literal("Sculptor's Trial — "+Math.max(0,(TEST_TIME-elapsed)/20)+"s"));bar.setProgress(p==null?0:(float)Math.clamp((p.getY()-origin.getY())/60,0,1));
            if(p==null||p.level()!=l||!p.isAlive()||p.isSpectator()){fail(l,"The trial ended because its challenger left.");return;}
            if(++elapsed>TEST_TIME){fail(l,"Time ran out. You can try again with another Bluff Rod.");return;}
            if(!p.isCreative()&&(p.getAbilities().flying||p.isFallFlying()||p.isPassenger()||p.isInWater()||p.position().subtract(net.minecraft.world.phys.Vec3.atBottomCenterOf(origin)).horizontalDistanceSqr()>256||p.getY()<origin.getY()-10||lastPlayer!=null&&p.position().distanceToSqr(lastPlayer)>36)){fail(l,"The trial ended: stay on the course and climb without flight or teleportation.");return;}lastPlayer=p.position();
            var pillar=pillar(l);if(pillar==null){fail(l,"The trial's pillar was interrupted. Please try again.");return;}if(age>18||action()==TRIAL){float h=Math.min(TEST_HEIGHT,pillar.height()+.25F);pillar.height(h);setPos(origin.getX()+.5,origin.getY()+h,origin.getZ()+.5);setNoGravity(true);}getLookControl().setLookAt(p,30,30);if(action()==START&&age>=52)begin(TRIAL);return;
        }
        if(action()==PASS||action()==FAIL){var column=pillar(l);if(column!=null&&column.height()>.1){column.height(column.height()-.25F);setPos(origin.getX()+.5,origin.getY()+column.height(),origin.getZ()+.5);age=0;return;}descending=false;entityData.set(DESCENDING,false);setNoGravity(false);setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(origin));
            if(action()==PASS&&age==40)playSound(OwnerWilds.sound("sculptor.make_gauntlet"),1,1);
            if(action()==PASS&&age>=134&&!rewarded){rewarded=true;var stack=new ItemStack(OwnerWilds.item("earthrend_gauntlet"));if(p!=null&&p.level()==l&&!p.isRemoved()){if(!p.getInventory().add(stack))p.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);}else spawnAtLocation(l,stack);}
            if(age>=(action()==PASS?150:30)){boolean won=action()==PASS;for(var rock:rocks(l))rock.discard();trialId=null;pillarId=null;playerId=null;descending=false;bar.setVisible(false);begin(won?DISAPPEAR:IDLE);}return;
        }
        if(anger!=null&&getTarget()==null){var e=l.getEntity(anger);if(e instanceof LivingEntity living&&living.isAlive()&&living.position().subtract(position()).horizontalDistanceSqr()<3600&&Math.abs(living.getY()-getY())<80)setTarget(living);else anger=null;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||target.level()!=l||target.position().subtract(position()).horizontalDistanceSqr()>3600||Math.abs(target.getY()-getY())>80||target instanceof Player player&&!WildBluff.victim(player))){setTarget(null);anger=null;target=null;}
        bar.setVisible(target!=null);bar.setName(getDisplayName());bar.setProgress(getHealth()/getMaxHealth());if(target==null){combatHeight(l,false);if(--healDelay<=0)heal(.3F);if(action()!=IDLE)begin(IDLE);return;}combatHeight(l,true);healDelay=75;getLookControl().setLookAt(target,30,30);
        if(action()==ATTACK||action()==GUARD){if(age==12){var stone=WildBoulder.create(l,this,getEyePosition().add(getLookAngle().scale(2)),WildBoulder.SHOT,1,200);if(stone!=null){stone.shootAt(target.getEyePosition().subtract(stone.position()).add(target.getDeltaMovement().scale(8)),1.2F,10,0);l.addFreshEntity(stone);playSound(OwnerWilds.sound("sculptor.attack"),2,1);}}if(age>=28){begin(IDLE);combatCooldown=12;}return;}
        if(tickCount%5==0){var incoming=new ArrayList<Projectile>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(Projectile.class),getBoundingBox().inflate(8),e->e.getOwner()!=this&&e.getDeltaMovement().lengthSqr()>.01&&e.getDeltaMovement().normalize().dot(position().subtract(e.position()).normalize())>.8,incoming,8);if(!incoming.isEmpty()){incoming.get(0).discard();begin(GUARD);return;}}
        if(combatCooldown==0&&hasLineOfSight(target))begin(ATTACK);
    }
    @Override public boolean hurtServer(ServerLevel l,DamageSource s,float amount){if(action()==PASS||action()==DISAPPEAR)return false;boolean hit=super.hurtServer(l,s,amount);if(hit&&s.getEntity() instanceof LivingEntity e&&WildSolar.canHarm(this,e)){healDelay=75;if(trialId!=null)fail(l,"The challenge was interrupted by combat.");anger=e.getUUID();setTarget(e);if(action()==IDLE)begin(HURT);}return hit;}
    @Override public void checkDespawn(){}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);bar.addPlayer(p);}@Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
    @Override protected void tickDeath(){bar.setVisible(false);if(++deathTime>=94&&!level().isClientSide())remove(RemovalReason.KILLED);}
    @Override protected SoundEvent getHurtSound(DamageSource s){return OwnerWilds.sound("sculptor.hurt");}@Override protected SoundEvent getDeathSound(){return OwnerWilds.sound("sculptor.death");}
    public String clip(){if(!isAlive())return deathTime<9?"death_start":"death_end";return switch(action()){case START->"testStart";case PASS->entityData.get(DESCENDING)?"test_pass_start":"test_pass_end";case FAIL->entityData.get(DESCENDING)?"test_fail_start":"test_fail_end";case DISAPPEAR->"disappear";case ATTACK->"attack_right";case GUARD->"guard";case HURT->"hurt";case TALK->"talk";default->"idle";};}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<WildSculptor>("body",3,s->s.setAndContinue(action()==IDLE||action()==TRIAL||action()==SCAN?RawAnimation.begin().thenLoop(clip()):RawAnimation.begin().thenPlayAndHold(clip()))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("action",action());out.putInt("action_age",age);out.putInt("elapsed",elapsed);out.putBoolean("rewarded",rewarded);out.putBoolean("descending",descending);out.store("origin",BlockPos.CODEC,origin());if(playerId!=null)out.store("challenger",UUIDUtil.CODEC,playerId);if(trialId!=null)out.store("trial",UUIDUtil.CODEC,trialId);if(pillarId!=null)out.store("pillar",UUIDUtil.CODEC,pillarId);if(anger!=null)out.store("anger",UUIDUtil.CODEC,anger);if(combatPillar!=null)out.store("combat_pillar",UUIDUtil.CODEC,combatPillar);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);origin=in.read("origin",BlockPos.CODEC).orElse(blockPosition());playerId=in.read("challenger",UUIDUtil.CODEC).orElse(null);trialId=in.read("trial",UUIDUtil.CODEC).orElse(null);pillarId=in.read("pillar",UUIDUtil.CODEC).orElse(null);anger=in.read("anger",UUIDUtil.CODEC).orElse(null);combatPillar=in.read("combat_pillar",UUIDUtil.CODEC).orElse(null);begin(Math.clamp(in.getIntOr("action",0),0,10));if(action()==SCAN){scanY=1;plannedCourse=null;}age=Math.clamp(in.getIntOr("action_age",0),0,9000);entityData.set(AGE,age);elapsed=Math.clamp(in.getIntOr("elapsed",0),0,9000);rewarded=in.getBooleanOr("rewarded",false);descending=in.getBooleanOr("descending",false);entityData.set(DESCENDING,descending);}
}
