package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import io.github.jimbozoomer.jugcraft.town.Town;
import java.util.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Source Ice Spider and Citadel Keeper combat translated onto native authority and GeckoLib 5. */
public final class JimtaEncounterCreatures {
    public static EntityType<IceSpider> ICE_SPIDER;
    public static EntityType<Keeper> KEEPER;
    public static final Map<String,Item> EGGS=new LinkedHashMap<>();
    public static final Map<String,SoundEvent> SOUNDS=new LinkedHashMap<>();
    private JimtaEncounterCreatures(){}
    public static void register(){
        var spiderKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_ice_spider"));
        ICE_SPIDER=Registry.register(BuiltInRegistries.ENTITY_TYPE,spiderKey,EntityType.Builder.of(IceSpider::new,MobCategory.MONSTER).sized(1.75F,1.9F).clientTrackingRange(16).build(spiderKey));
        FabricDefaultAttributeRegistry.register(ICE_SPIDER,Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,50).add(Attributes.ATTACK_DAMAGE,8).add(Attributes.ARMOR,20).add(Attributes.KNOCKBACK_RESISTANCE,.6).add(Attributes.ATTACK_KNOCKBACK,1).add(Attributes.FOLLOW_RANGE,32).add(Attributes.STEP_HEIGHT,1.5).add(Attributes.ENTITY_INTERACTION_RANGE,4).add(Attributes.MOVEMENT_SPEED,.35));
        var keeperKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_citadel_keeper"));
        KEEPER=Registry.register(BuiltInRegistries.ENTITY_TYPE,keeperKey,EntityType.Builder.of(Keeper::new,MobCategory.MONSTER).sized(.85F,2.3F).eyeHeight(2.3F).fireImmune().clientTrackingRange(16).build(keeperKey));
        FabricDefaultAttributeRegistry.register(KEEPER,Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,60).add(Attributes.ATTACK_DAMAGE,10).add(Attributes.FOLLOW_RANGE,25).add(Attributes.KNOCKBACK_RESISTANCE,.8).add(Attributes.ATTACK_KNOCKBACK,2).add(Attributes.STEP_HEIGHT,1).add(Attributes.ENTITY_INTERACTION_RANGE,3.5).add(Attributes.MOVEMENT_SPEED,.19));
        egg("ice_spider",ICE_SPIDER);egg("keeper",KEEPER);
        for(var name:List.of("ice_spider_howl","ice_spider_ambient","ice_spider_hurt","ice_spider_death","ice_spider_swing","ice_spider_grapple_latch","ice_spider_grapple_spit","keeper_idle","keeper_hurt","keeper_death")){var id=Jugcraft.id("jimtafacts."+name);SOUNDS.put(name,Registry.register(BuiltInRegistries.SOUND_EVENT,id,SoundEvent.createVariableRangeEvent(id)));}
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(out->EGGS.values().forEach(out::accept));
        JimtaSpiderPatrols.register();
        SpawnPlacements.register(ICE_SPIDER,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,JimtaEncounterCreatures::canSpawn);
        SpawnPlacements.register(KEEPER,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,JimtaEncounterCreatures::canSpawn);
        JimtaSpiderTomb.register();
    }
    private static void egg(String name,EntityType<?> type){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_spellbooks_"+name+"_spawn_egg"));EGGS.put(name,Registry.register(BuiltInRegistries.ITEM,key,new SpawnEggItem(new Item.Properties().setId(key).spawnEgg(type))));}
    public static boolean canSpawn(EntityType<? extends Monster> type,ServerLevelAccessor world,EntitySpawnReason reason,BlockPos pos,RandomSource random){
        if(!JimtaSpellbooks.enabled()||Town.isInside(world.getLevel(),pos)||LandRegistry.isClaimed(world.getLevel(),pos))return false;
        if(reason==EntitySpawnReason.NATURAL&&world.getLevel().getEntitiesOfClass(Mob.class,new AABB(pos).inflate(64),e->e.getType()==type).size()>=2)return false;
        return Monster.checkMonsterSpawnRules(type,world,reason,pos,random);
    }
    static boolean prey(Mob hunter,LivingEntity victim){
        if(!JimtaSpellbooks.enabled()||!victim.isAlive()||victim instanceof AbstractVillager||victim instanceof Mob mob&&mob.getType()==hunter.getType()||victim instanceof Player p&&(p.isCreative()||p.isSpectator()))return false;
        if(victim instanceof Animal&&(Town.isInside(victim.level(),victim.blockPosition())||LandRegistry.isClaimed(victim.level(),victim.blockPosition())))return false;
        return !JimtaWardSpells.concealed(victim);
    }
    public abstract static class Encounter extends Monster implements GeoEntity {
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        protected static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(Encounter.class,EntityDataSerializers.INT);
        protected int actionAge,cooldown;protected String action="";
        Encounter(EntityType<? extends Monster> type,Level l){super(type,l);}
        @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTION,0);}
        public boolean animating(){return entityData.get(ACTION)>0;}
        protected abstract String[] animations();
        protected void start(String name,int ticks){action=name;actionAge=0;entityData.set(ACTION,ticks);triggerAnim("source",name);getNavigation().stop();}
        protected void stopAction(){action="";actionAge=0;entityData.set(ACTION,0);}
        @Override public void tick(){super.tick();if(!level().isClientSide()){if(cooldown>0)cooldown--;if(animating()){actionAge++;entityData.set(ACTION,Math.max(0,entityData.get(ACTION)-1));}if(getTarget()!=null&&!prey(this,getTarget()))setTarget(null);}}
        protected void face(LivingEntity target){getLookControl().setLookAt(target,90,90);float yaw=(float)(Math.atan2(target.getZ()-getZ(),target.getX()-getX())*180/Math.PI)-90;setYRot(yaw);yBodyRot=yaw;yHeadRot=yaw;}
        @Override public boolean canAttack(LivingEntity target){return prey(this,target)&&super.canAttack(target);}
        @Override public boolean doHurtTarget(ServerLevel l,Entity target){return target instanceof LivingEntity living&&prey(this,living)&&super.doHurtTarget(l,target);}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){var controller=new AnimationController<Encounter>("source",0,s->getPose()==Pose.EMERGING?s.setAndContinue(RawAnimation.begin().thenPlay("emerge_from_ground")):PlayState.STOP);for(var clip:animations())controller.triggerableAnim(clip,RawAnimation.begin().thenPlay(clip));c.add(controller);}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("attack_cooldown",cooldown);}
        @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);cooldown=Math.clamp(in.getIntOr("attack_cooldown",20),0,200);stopAction();}
    }
    public static final class Keeper extends Encounter {
        private static final EntityDataAccessor<Boolean> RESTORED=SynchedEntityData.defineId(Keeper.class,EntityDataSerializers.BOOLEAN),SUMMONED=SynchedEntityData.defineId(Keeper.class,EntityDataSerializers.BOOLEAN);
        public static final String[] CLIPS={"sword_double_slash","sword_single_upward","sword_single_horizontal","sword_single_horizontal_fast","sword_stab","sword_lunge","keeper_kneeling_rise"};
        static final int[] LENGTHS={43,26,28,24,21,76};
        static final int[][] HITS={{13,29},{13},{12},{12},{11},{56,57,58,59,60,61,62,63,64}};
        private int attackIndex,rise;private boolean lunged,lungeHit,combo;private Vec3 lungeOrigin=Vec3.ZERO;
        public Keeper(EntityType<? extends Monster> t,Level l){super(t,l);xpReward=25;equip();}
        @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(RESTORED,false);b.define(SUMMONED,false);}
        public boolean restored(){return entityData.get(RESTORED);}public boolean summoned(){return entityData.get(SUMMONED);}
        public void setRestored(boolean b){entityData.set(RESTORED,b);equip();}public void setSummoned(boolean b){entityData.set(SUMMONED,b);if(b)skipDropExperience();equip();}
        public int riseTicks(){return rise;}
        public void rise(){rise=25;start("keeper_kneeling_rise",25);}
        private void equip(){var item=JimtaSpellbooks.ITEMS.get(restored()?"legionnaire_flamberge":"keeper_flamberge");if(item!=null){setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(item));setDropChance(EquipmentSlot.MAINHAND,summoned()?0:.085F);}}
        @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l,net.minecraft.world.DifficultyInstance d,EntitySpawnReason reason,SpawnGroupData group){var result=super.finalizeSpawn(l,d,reason,group);equip();return result;}
        @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new Fight(this));goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,1));goalSelector.addGoal(8,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));}
        @Override protected String[] animations(){return CLIPS;}
        @Override public void tick(){super.tick();if(level().isClientSide())return;if(rise>0){rise--;getNavigation().stop();if(level() instanceof ServerLevel l)l.sendParticles(ParticleTypes.ASH,getX(),getY()+1,getZ(),5,.2,.2,.2,.05);}}
        private void attack(LivingEntity target){
            if(!(level() instanceof ServerLevel l)||rise>0)return;double distance=distanceTo(target);if(!lunged)face(target);
            if(animating()){
                if(attackIndex<0||attackIndex>=LENGTHS.length)return;
                for(int hit:HITS[attackIndex])if(actionAge==hit){
                    if(attackIndex==5){if(!lunged){var force=target.position().subtract(position()).normalize().multiply(2.4,.5,2.4).add(0,.15,0);push(force);lungeOrigin=position();lunged=true;getNavigation().stop();playSound(SoundEvents.PLAYER_ATTACK_SWEEP,1,1);}if(!lungeHit&&distance<3.5*.45&&hasLineOfSight(target)&&doHurtTarget(l,target)){lungeHit=true;var delta=lungeOrigin.subtract(target.position());target.knockback(1,delta.x,delta.z,damageSources().mobAttack(this),1);}}
                    else {var force=target.position().subtract(position()).normalize().scale(.55);push(force);playSound(SoundEvents.PLAYER_ATTACK_SWEEP,1,.9F+random.nextFloat()*.4F);if(distance<=3.5&&hasLineOfSight(target)&&doHurtTarget(l,target)){target.damageCooldownTime=0;combo=HITS[attackIndex].length==1&&(random.nextFloat()<.75||target.isBlocking());}}
                }
                return;
            }
            if(!action.isEmpty()){stopAction();cooldown=combo?0:10+random.nextInt(21);combo=false;}
            if(cooldown==0&&distance<=10.5&&hasLineOfSight(target)){attackIndex=distance<5.25?random.nextInt(5):random.nextInt(6);if(distance>8.75&&random.nextFloat()<.25)attackIndex=5;if(attackIndex!=5&&distance>3.5){getNavigation().moveTo(target,1.3);return;}lunged=false;lungeHit=false;start(CLIPS[attackIndex],LENGTHS[attackIndex]);}
            else if(distance>3.5)getNavigation().moveTo(target,1.3);
        }
        @Override public boolean hurtServer(ServerLevel l,DamageSource s,float amount){if(s.is(DamageTypeTags.IS_FALL))return false;return super.hurtServer(l,s,s.getDirectEntity() instanceof Projectile?amount*.75F:amount);}
        @Override protected void dropAllDeathLoot(ServerLevel l,DamageSource s){if(!summoned())super.dropAllDeathLoot(l,s);}
        @Override protected int getBaseExperienceReward(ServerLevel l){return summoned()?0:super.getBaseExperienceReward(l);}
        @Override protected SoundEvent getAmbientSound(){return SOUNDS.get("keeper_idle");}
        @Override protected SoundEvent getHurtSound(DamageSource s){return SOUNDS.get("keeper_hurt");}
        @Override protected SoundEvent getDeathSound(){return SOUNDS.get("keeper_death");}
        @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putBoolean("restored",restored());out.putBoolean("summoned",summoned());out.putInt("rise",rise);}
        @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);entityData.set(RESTORED,in.getBooleanOr("restored",false));entityData.set(SUMMONED,in.getBooleanOr("summoned",false));rise=Math.clamp(in.getIntOr("rise",0),0,25);}
    }
    private static final class Fight extends Goal {
        private final Encounter mob;Fight(Encounter mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return mob.getTarget()!=null&&prey(mob,mob.getTarget());}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void tick(){var target=mob.getTarget();if(target==null)return;if(mob instanceof Keeper k)k.attack(target);else if(mob instanceof IceSpider s)s.attack(target);}
        @Override public void stop(){mob.getNavigation().stop();if(mob instanceof IceSpider s)s.releaseGrapple(false);}
    }
    public static final class IceSpider extends Encounter {
        private static final EntityDataAccessor<Boolean> CROUCH=SynchedEntityData.defineId(IceSpider.class,EntityDataSerializers.BOOLEAN),CLIMB=SynchedEntityData.defineId(IceSpider.class,EntityDataSerializers.BOOLEAN);
        private static final EntityDataAccessor<Integer> GRAPPLE=SynchedEntityData.defineId(IceSpider.class,EntityDataSerializers.INT);
        private static final AttributeModifier CROUCH_SPEED=new AttributeModifier(Jugcraft.id("jimta_spider_crouch"),-.3,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        public static final String[] CLIPS={"attack_bite","attack_fang_basic","attack_right_swipe","attack_grapple_pounce","leap_back","long_cast","emerge_from_ground"};
        private int satiation,grappleAge,pounceCooldown,emerge,crouchChanged,attackIndex;private boolean hungry=true,retreat,releasing;private UUID grapple;
        public Vec3 normal=new Vec3(0,1,0),lastNormal=normal;
        public IceSpider(EntityType<? extends Monster> t,Level l){super(t,l);xpReward=15;}
        @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(CROUCH,false);b.define(CLIMB,false);b.define(GRAPPLE,-1);}
        @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new Fight(this));goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,.7));goalSelector.addGoal(8,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Animal.class,true,(animal,world)->hungry&&prey(this,animal)));}
        @Override protected PathNavigation createNavigation(Level l){return new WallClimberNavigation(this,l);}
        @Override protected String[] animations(){return CLIPS;}
        public int satiation(){return satiation;}public void addSatiation(int value){satiation=Math.clamp(satiation+value,0,200);if(satiation<10)hungry=true;else if(satiation>=18)hungry=false;}
        @Override public boolean isCrouching(){return entityData.get(CROUCH);}public boolean climbing(){return entityData.get(CLIMB);}
        public float crouch(float partial){float f=Math.clamp((tickCount+partial-crouchChanged)/10,0,1);return isCrouching()?1-f:f;}
        public float crouchHeight(float partial){return .5F+.5F*crouch(partial);}
        public boolean restraining(){return !releasing&&grapple!=null&&isAlive()&&JimtaSpellbooks.enabled();}
        public void emerge(){emerge=45;start("emerge_from_ground",45);setPose(Pose.EMERGING);}
        private void crouch(boolean b){if(isCrouching()==b)return;entityData.set(CROUCH,b);setPose(b?Pose.CROUCHING:Pose.STANDING);if(b)getAttribute(Attributes.MOVEMENT_SPEED).addOrUpdateTransientModifier(CROUCH_SPEED);else getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(CROUCH_SPEED);refreshDimensions();}
        @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(key==CROUCH){crouchChanged=tickCount;refreshDimensions();}}
        @Override protected EntityDimensions getDefaultDimensions(Pose pose){var d=super.getDefaultDimensions(pose);return pose==Pose.CROUCHING?d.scale(1,.5F):d;}
        @Override public boolean onClimbable(){return climbing();}
        @Override public boolean canFreeze(){return false;}
        @Override public void makeStuckInBlock(BlockState state,Vec3 slow){if(!state.is(Blocks.COBWEB))super.makeStuckInBlock(state,slow);}
        @Override public void tick(){
            super.tick();sampleGround();if(!(level() instanceof ServerLevel l))return;
            if(pounceCooldown>0)pounceCooldown--;
            if(emerge>0){emerge--;getNavigation().stop();setDeltaMovement(Vec3.ZERO);if(emerge==0)setPose(Pose.STANDING);return;}
            if(!JimtaSpellbooks.enabled()){releaseGrapple(false);return;}
            if(isCrouching()){if(l.noCollision(this,super.getDefaultDimensions(Pose.STANDING).makeBoundingBox(position()).deflate(1e-7)))crouch(false);}
            else if(horizontalCollision&&l.noCollision(this,super.getDefaultDimensions(Pose.STANDING).scale(1,.5F).makeBoundingBox(position().add(getForward().scale(.15))).deflate(1e-7)))crouch(true);
            entityData.set(CLIMB,horizontalCollision&&!isCrouching());if(climbing())setDeltaMovement(getDeltaMovement().multiply(1,0,1).add(0,.275,0));
            if(grapple!=null){var victim=l.getEntity(grapple);if(!(victim instanceof LivingEntity living)||!prey(this,living)||!hasPassenger(victim)){releaseGrapple(false);return;}if(grappleAge%20==0)heal(getMaxHealth()*.08F);victim.setTicksFrozen(Math.min(victim.getTicksRequiredToFreeze()*3,victim.getTicksFrozen()+10));if(++grappleAge>40)releaseGrapple(true);}
            if(tickCount%900==0&&satiation>0)addSatiation(-1);
            if(tickCount%40==0&&satiation>0&&getHealth()<getMaxHealth()&&getTarget()==null&&grapple==null&&tickCount-getLastHurtByMobTimestamp()>=300){heal(1);addSatiation(-1);}
        }
        private void sampleGround(){
            Vec3[] pins=new Vec3[4];for(int x=0;x<2;x++)for(int z=0;z<2;z++){var offset=new Vec3((x-.5)*4,0,(z-.5)*4).yRot(-yBodyRot*Mth.DEG_TO_RAD);var at=position().add(offset);if(!level().hasChunkAt(BlockPos.containing(at))){pins[x*2+z]=offset;continue;}var hit=level().clip(new ClipContext(at.add(0,2,0),at.add(0,-2,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));pins[x*2+z]=hit.getLocation().subtract(position());}
            Vec3 target=pins[1].subtract(pins[0]).cross(pins[2].subtract(pins[0])).add(pins[3].subtract(pins[1]).cross(pins[0].subtract(pins[1]))).add(pins[0].subtract(pins[2]).cross(pins[3].subtract(pins[2]))).add(pins[2].subtract(pins[3]).cross(pins[1].subtract(pins[3]))).normalize();if(target.y<0)target=target.scale(-1);if(target.lengthSqr()<.1)target=new Vec3(0,1,0);lastNormal=normal;normal=normal.lerp(target,.2);
        }
        private void attack(LivingEntity target){
            if(!(level() instanceof ServerLevel l)||emerge>0||grapple!=null)return;face(target);double distance=distanceTo(target);
            if(animating()){
                if(action.equals("attack_grapple_pounce")){
                    if(actionAge==20){double f=Math.clamp(distance/18,0,1);double forward=.125+2.875*f,up=.25+.95*f;push(target.position().subtract(position()).multiply(1,0,1).normalize().scale(forward*1.5).add(0,up*.5,0));playSound(SOUNDS.get("ice_spider_swing"),3,1.5F);}
                    if(actionAge>=30&&actionAge<=35&&distance<=4&&hasLineOfSight(target)&&doHurtTarget(l,target)){if(!target.isBlocking())grab(target);stopAction();cooldown=20;}
                }else if(action.equals("leap_back")){if(actionAge==1)push(getForward().scale(-2.2).add(0,.5,0));}
                else if(action.equals("long_cast")){if(actionAge==12){if(random.nextBoolean())JimtaWizardBolt.spawn(this,target,"ice",4);else JimtaSpiderTomb.spikes(this,target);}}
                else {int[] hits={14,12,10};if(attackIndex>=0&&attackIndex<3&&actionAge==hits[attackIndex]&&distance<=4&&hasLineOfSight(target)){playSound(SOUNDS.get("ice_spider_swing"),1,1);doHurtTarget(l,target);}}
                return;
            }
            if(action.equals("leap_back")){start("long_cast",30);return;}if(!action.isEmpty()){stopAction();cooldown=10+random.nextInt(31);}
            if(retreat&&!isCrouching()){retreat=false;start("leap_back",10);return;}
            if(cooldown==0&&pounceCooldown==0&&!isCrouching()&&hasLineOfSight(target)&&(distance>5||random.nextFloat()<.05)){start("attack_grapple_pounce",40);pounceCooldown=60+random.nextInt(101);return;}
            if(distance<=4&&cooldown==0&&hasLineOfSight(target)){attackIndex=random.nextInt(3);start(CLIPS[attackIndex],new int[]{22,20,14}[attackIndex]);}
            else if(distance>3.5)getNavigation().moveTo(target,animating()?.8:1.1);
        }
        private void grab(LivingEntity victim){if(grapple!=null||victim.isPassenger()||!prey(this,victim))return;if(victim.startRiding(this,true,false)){grapple=victim.getUUID();grappleAge=0;entityData.set(GRAPPLE,victim.getId());playSound(SOUNDS.get("ice_spider_grapple_latch"),1,1);}}
        void releaseGrapple(boolean entomb){var victim=getFirstPassenger();releasing=true;ejectPassengers();grapple=null;entityData.set(GRAPPLE,-1);releasing=false;if(entomb&&victim instanceof LivingEntity living&&prey(this,living)){JimtaSpiderTomb.entomb(this,living);playSound(SOUNDS.get("ice_spider_grapple_spit"),1,1);}}
        @Override public Vec3 getPassengerRidingPosition(Entity passenger){return position().add(getForward().scale(2));}
        @Override public LivingEntity getControllingPassenger(){return null;}
        @Override protected boolean canAddPassenger(Entity p){return getPassengers().isEmpty()&&p instanceof LivingEntity living&&prey(this,living);}
        @Override protected void removePassenger(Entity p){super.removePassenger(p);if(p.getUUID().equals(grapple)){grapple=null;entityData.set(GRAPPLE,-1);}}
        @Override public boolean doHurtTarget(ServerLevel l,Entity e){boolean hit=super.doHurtTarget(l,e);if(hit&&e instanceof Animal animal&&!animal.isAlive())addSatiation(Mth.ceil(animal.getMaxHealth()));return hit;}
        @Override public boolean hurtServer(ServerLevel l,DamageSource s,float amount){if(emerge>0||s.is(DamageTypeTags.IS_FALL))return false;if(s.getEntity()!=null&&s.getEntity().getUUID().equals(grapple))amount*=.2F;if(!isCrouching()&&grapple==null&&s.getDirectEntity()==s.getEntity()&&random.nextFloat()<Mth.lerp(Math.clamp(amount/12,0,1),.02F,.7F))retreat=true;return super.hurtServer(l,s,amount);}
        @Override public void die(DamageSource s){releaseGrapple(false);super.die(s);}
        @Override protected SoundEvent getAmbientSound(){return SOUNDS.get("ice_spider_ambient");}
        @Override protected SoundEvent getHurtSound(DamageSource s){return SOUNDS.get("ice_spider_hurt");}
        @Override protected SoundEvent getDeathSound(){return SOUNDS.get("ice_spider_death");}
        @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("satiation",satiation);out.putBoolean("hungry",hungry);out.putBoolean("crouching",isCrouching());if(grapple!=null)out.store("grapple",UUIDUtil.CODEC,grapple);out.putInt("grapple_age",grappleAge);}
        @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);hungry=in.getBooleanOr("hungry",true);addSatiation(in.getIntOr("satiation",0));crouch(in.getBooleanOr("crouching",false));grapple=in.read("grapple",UUIDUtil.CODEC).orElse(null);grappleAge=Math.clamp(in.getIntOr("grapple_age",0),0,41);}
    }
}
