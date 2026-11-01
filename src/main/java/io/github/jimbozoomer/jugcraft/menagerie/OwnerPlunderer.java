package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Galleon crew with finite ammunition, original sword/spyglass behavior and boat navigation. */
public final class OwnerPlunderer extends AbstractIllager implements InventoryCarrier {
 public static EntityType<OwnerPlunderer> TYPE;
 private static final EntityDataAccessor<Boolean> SPY=SynchedEntityData.defineId(OwnerPlunderer.class,EntityDataSerializers.BOOLEAN);
 private final SimpleContainer supplies=new SimpleContainer(5);
 private int gunCooldown,spyTicks;private BlockPos cannon;
 public OwnerPlunderer(EntityType<? extends OwnerPlunderer> t,Level l){super(t,l);setCanJoinRaid(false);xpReward=5;}
 static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("supps_plunderer"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<OwnerPlunderer>of(OwnerPlunderer::new,MobCategory.MONSTER).sized(.6F,1.95F).clientTrackingRange(8).build(key));net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(TYPE,Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED,.35).add(Attributes.MAX_HEALTH,24).add(Attributes.ATTACK_DAMAGE,6).add(Attributes.FOLLOW_RANGE,24));
  var eggKey=ResourceKey.create(Registries.ITEM,Jugcraft.id("supps_plunderer_spawn_egg"));var egg=Registry.register(BuiltInRegistries.ITEM,eggKey,new SpawnEggItem(new Item.Properties().setId(eggKey).spawnEgg(TYPE)));OwnerSupplementaries.ITEMS.put("plunderer_spawn_egg",egg);net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(e->e.accept(egg));
 }
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(SPY,false);}
 public boolean usingSpyglass(){return entityData.get(SPY);}
 @Override protected void registerGoals(){
  goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new CrewGoal());goalSelector.addGoal(3,new MeleeAttackGoal(this,1,false));goalSelector.addGoal(7,new RandomStrollGoal(this,.6));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,16));goalSelector.addGoal(9,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
 }
 // The owner's town rule permits pillagers alone to attack villagers. This crew is not a raid wave.
 @Override public boolean canAttack(LivingEntity e){return !(e instanceof AbstractVillager)&&super.canAttack(e);}
 @Override public boolean canJoinRaid(){return false;}@Override public boolean canJoinPatrol(){return false;}
 @Override public void applyRaidBuffs(ServerLevel l,int wave,boolean unused){}
 @Override public IllagerArmPose getArmPose(){return isAggressive()||usingSpyglass()?IllagerArmPose.ATTACKING:IllagerArmPose.CROSSED;}
 @Override public SimpleContainer getInventory(){return supplies;}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l,DifficultyInstance d,EntitySpawnReason reason,SpawnGroupData group){var result=super.finalizeSpawn(l,d,reason,group);setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.GOLDEN_SWORD));setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SPYGLASS));return result;}
 @Override protected SoundEvent getAmbientSound(){return OwnerSupplementaries.SOUNDS.get("plunderer_idle");}@Override protected SoundEvent getHurtSound(DamageSource d){return OwnerSupplementaries.SOUNDS.get("plunderer_hurt");}@Override protected SoundEvent getDeathSound(){return OwnerSupplementaries.SOUNDS.get("plunderer_death");}@Override public SoundEvent getCelebrateSound(){return OwnerSupplementaries.SOUNDS.get("plunderer_celebrate");}
 @Override protected void customServerAiStep(ServerLevel l){super.customServerAiStep(l);if(gunCooldown>0)gunCooldown--;if(spyTicks>0)spyTicks--;var target=getTarget();if(target!=null&&distanceToSqr(target)>144&&hasLineOfSight(target)&&tickCount%100==0)spyTicks=30;entityData.set(SPY,spyTicks>0);}
 @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);writeInventoryToTag(out);out.putInt("GunCooldown",gunCooldown);}
 @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);readInventoryFromTag(in);gunCooldown=Math.clamp(in.getIntOr("GunCooldown",0),0,100);setCanJoinRaid(false);}
 @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource source,boolean recentlyHit){super.dropCustomDeathLoot(l,source,recentlyHit);Containers.dropContents(l,this,supplies);}
 private static boolean usable(OwnerSupplementData d){return d.owner==null&&d.block().function.equals("cannon")&&d.getItem(0).is(Items.GUNPOWDER)&&d.getItem(1).is(OwnerSupplementaries.ITEMS.get("cannonball"))&&d.cannonFuse==0&&d.cannonCooldown==0;}
 private boolean fire(ServerLevel l,OwnerSupplementData d,LivingEntity target){if(!usable(d)||!hasLineOfSight(target))return false;var start=OwnerCannon.mouth(d);var aim=target.getEyePosition().subtract(start);if(aim.lengthSqr()>1600||aim.lengthSqr()<16)return false;d.cannonYaw=(float)Math.toDegrees(Math.atan2(-aim.x,aim.z));d.cannonPitch=(float)-Math.toDegrees(Math.atan2(aim.y,aim.horizontalDistance()));start=OwnerCannon.mouth(d);var ball=new OwnerCannonball(OwnerCannon.BALL,l);ball.setOwner(this);ball.setPos(start);ball.setDeltaMovement(aim.normalize().scale(1.8));if(!l.noCollision(ball)||!l.addFreshEntity(ball))return false;d.getItem(0).shrink(1);d.getItem(1).shrink(1);d.cannonCooldown=80;d.changed();l.playSound(null,d.getBlockPos(),OwnerSupplementaries.SOUNDS.get("cannon_fire"),net.minecraft.sounds.SoundSource.HOSTILE,2,1);return true;}
 private final class CrewGoal extends Goal {
  private AbstractBoat boat;private int search;
  CrewGoal(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
  @Override public boolean canUse(){var target=getTarget();if(target==null||!target.isAlive())return false;if(getVehicle() instanceof AbstractBoat b){boat=b;return true;}if(++search%20!=0)return false;
   if(distanceToSqr(target)<36)return false;
   // Search a bounded 13x5x13 area for unowned encounter cannons only.
   var p=blockPosition();for(var at:BlockPos.betweenClosed(p.offset(-6,-2,-6),p.offset(6,2,6)))if(level().hasChunkAt(at)&&level().getBlockEntity(at) instanceof OwnerSupplementData d&&usable(d)){cannon=at.immutable();return true;}
   if(isInWater()||target.isInWater())for(var b:level().getEntitiesOfClass(AbstractBoat.class,getBoundingBox().inflate(8),e->e.getPassengers().isEmpty()&&e.isInWater()&&(e.entityTags().contains("JugcraftGalleon")||e instanceof OwnerCannonBoat armed&&armed.unowned()))){boat=b;return true;}return false;
  }
  @Override public boolean canContinueToUse(){return getTarget()!=null&&getTarget().isAlive()&&(boat!=null&&!boat.isRemoved()||cannon!=null&&level().hasChunkAt(cannon)&&level().getBlockEntity(cannon) instanceof OwnerSupplementData d&&usable(d));}
  @Override public void tick(){var target=getTarget();if(target==null||!(level() instanceof ServerLevel l))return;getLookControl().setLookAt(target,30,30);
   if(boat!=null){if(getVehicle()!=boat){getNavigation().moveTo(boat,1);if(distanceToSqr(boat)<4)startRiding(boat,true,true);return;}
    if(!boat.isInWater()&&tickCount%40==0){stopRiding();boat=null;return;}var delta=target.position().subtract(boat.position());if(delta.lengthSqr()<16&&!target.isInWater()){stopRiding();boat=null;return;}
    float desired=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));boat.setYRot(net.minecraft.util.Mth.approachDegrees(boat.getYRot(),desired,4));var forward=Vec3.directionFromRotation(0,boat.getYRot()).scale(.035);var next=boat.position().add(forward.scale(6));if(l.hasChunkAt(BlockPos.containing(next))&&l.getWorldBorder().isWithinBounds(BlockPos.containing(next))&&l.noCollision(boat,boat.getBoundingBox().move(forward.scale(6)))){var v=boat.getDeltaMovement().add(forward);double speed=v.horizontalDistance();if(speed>.22)v=new Vec3(v.x*.22/speed,v.y,v.z*.22/speed);boat.setDeltaMovement(v);boat.setPaddleState(true,true);}else boat.setPaddleState(false,false);
    if(boat instanceof OwnerCannonBoat armed&&gunCooldown==0&&hasLineOfSight(target)&&armed.fireCrew(OwnerPlunderer.this,target))gunCooldown=80;
   }else if(cannon!=null){if(distanceToSqr(Vec3.atCenterOf(cannon))>6)getNavigation().moveTo(cannon.getX()+.5,cannon.getY(),cannon.getZ()+.5,1);else{getNavigation().stop();if(gunCooldown==0&&l.getBlockEntity(cannon) instanceof OwnerSupplementData d&&fire(l,d,target))gunCooldown=80;}}
  }
  @Override public void stop(){if(boat!=null)boat.setPaddleState(false,false);boat=null;cannon=null;getNavigation().stop();}
 }
}
