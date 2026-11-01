package io.github.jimbozoomer.jugcraft.menagerie;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Native boats carrying the original cannon, finite supplies and a saved player-owned firing fuse. */
public final class OwnerCannonBoat extends Boat implements Container,MenuProvider {
 public static final Map<String,EntityType<OwnerCannonBoat>> TYPES=new LinkedHashMap<>();
 private static final EntityDataAccessor<Integer> FUSE=SynchedEntityData.defineId(OwnerCannonBoat.class,EntityDataSerializers.INT),COOL=SynchedEntityData.defineId(OwnerCannonBoat.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Float> PITCH=SynchedEntityData.defineId(OwnerCannonBoat.class,EntityDataSerializers.FLOAT),AIM=SynchedEntityData.defineId(OwnerCannonBoat.class,EntityDataSerializers.FLOAT);
 private static final EntityDataAccessor<Boolean> LOADED=SynchedEntityData.defineId(OwnerCannonBoat.class,EntityDataSerializers.BOOLEAN);
 private final NonNullList<ItemStack> cargo=NonNullList.withSize(9,ItemStack.EMPTY);private UUID owner,firingActor;
 private float oldAim,oldPitch;
 public final String wood;
 OwnerCannonBoat(EntityType<? extends OwnerCannonBoat> t,Level l,String wood){super(t,l,()->OwnerSupplementaries.ITEMS.get("cannon_boat_"+wood));this.wood=wood;}
 static void register(){for(String wood:List.of("oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","bamboo")){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("supps_cannon_boat_"+wood));var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<OwnerCannonBoat>of((t,l)->new OwnerCannonBoat(t,l,wood),MobCategory.MISC).sized(1.375F,.5625F).clientTrackingRange(10).updateInterval(1).build(key));TYPES.put(wood,type);var ik=ResourceKey.create(Registries.ITEM,key.identifier());var item=Registry.register(BuiltInRegistries.ITEM,ik,new BoatItem(type,new Item.Properties().setId(ik).stacksTo(1)));OwnerSupplementaries.ITEMS.put("cannon_boat_"+wood,item);net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->e.accept(item));}
  net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((p,l,h)->{if(!(p.getVehicle() instanceof OwnerCannonBoat boat))return InteractionResult.PASS;var held=p.getItemInHand(h);if(!held.is(Items.FLINT_AND_STEEL)&&!held.is(Items.FIRE_CHARGE))return InteractionResult.PASS;if(l.isClientSide())return InteractionResult.SUCCESS;if(!(p instanceof ServerPlayer actor)||!boat.ignite(actor))return InteractionResult.FAIL;if(held.is(Items.FLINT_AND_STEEL))held.hurtAndBreak(1,p,h.asEquipmentSlot());else held.consume(1,p);return InteractionResult.SUCCESS;});
 }
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(FUSE,0);b.define(COOL,0);b.define(PITCH,0F);b.define(AIM,0F);b.define(LOADED,false);}
 public int fuse(){return entityData.get(FUSE);}public int cooldown(){return entityData.get(COOL);}public float pitch(){return entityData.get(PITCH);}public float aim(){return entityData.get(AIM);}public boolean loaded(){return entityData.get(LOADED);}public boolean unowned(){return owner==null;}
 public float aim(float partial){return net.minecraft.util.Mth.rotLerp(partial,oldAim,aim());}
 public float pitch(float partial){return net.minecraft.util.Mth.lerp(partial,oldPitch,pitch());}
 private int powder(){for(int i=0;i<9;i++)if(cargo.get(i).is(Items.GUNPOWDER))return i;return -1;}
 private int ammo(){for(int i=0;i<9;i++)if(!cargo.get(i).is(Items.GUNPOWDER)&&OwnerCannon.ammunition(cargo.get(i)))return i;return -1;}
 boolean pearlAllowed(ServerPlayer p){return allowed(p)&&(owner==null||owner.equals(p.getUUID()))&&!isRemoved();}
 private boolean allowed(Player p){return !p.isSpectator()&&Menagerie.mayChange(level(),p,blockPosition());}
 @Override public InteractionResult interact(Player p,InteractionHand h,Vec3 hit){if(!allowed(p))return InteractionResult.FAIL;if(!level().isClientSide()&&owner==null&&!(getFirstPassenger() instanceof OwnerPlunderer))owner=p.getUUID();if(p.isSecondaryUseActive()){if(!level().isClientSide())p.openMenu(this);return InteractionResult.SUCCESS;}var held=p.getItemInHand(h);if(held.is(Items.FLINT_AND_STEEL)||held.is(Items.FIRE_CHARGE)){if(level().isClientSide())return InteractionResult.SUCCESS;if(!(p instanceof ServerPlayer actor)||!ignite(actor))return InteractionResult.FAIL;if(held.is(Items.FLINT_AND_STEEL))held.hurtAndBreak(1,p,h.asEquipmentSlot());else held.consume(1,p);return InteractionResult.SUCCESS;}return super.interact(p,h,hit);}
 private void aimAt(float yaw,float pitch){entityData.set(AIM,Math.clamp(net.minecraft.util.Mth.wrapDegrees(yaw-getYRot()),-70,70));entityData.set(PITCH,Math.clamp(pitch,-70,25));}
 private Vec3 direction(){return OwnerCannon.direction(getYRot()+aim(),pitch());}private Vec3 muzzle(){return position().add(0,.9,0).add(direction().scale(.95));}
 private boolean ignite(ServerPlayer actor){if(!allowed(actor)||actor.distanceToSqr(this)>36||fuse()>0||cooldown()>0||powder()<0||ammo()<0||isUnderWater())return false;aimAt(actor.getYRot(),actor.getXRot());firingActor=actor.getUUID();entityData.set(FUSE,40);playSound(net.minecraft.sounds.SoundEvents.TNT_PRIMED,.7F,1.2F);return true;}
 @Override public void tick(){oldAim=aim();oldPitch=pitch();super.tick();if(!(level() instanceof ServerLevel l))return;if(cooldown()>0)entityData.set(COOL,cooldown()-1);entityData.set(LOADED,powder()>=0);if(fuse()==0){if(getControllingPassenger() instanceof Player p)aimAt(p.getYRot(),p.getXRot());return;}if(isUnderWater()){entityData.set(FUSE,0);return;}entityData.set(FUSE,fuse()-1);if(fuse()>0){if(fuse()%3==0)l.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,getX(),getY()+1,getZ(),1,.03,.03,.03,.01);return;}
  var actor=Authority.present(l,firingActor);if(actor==null||!allowed(actor)||actor.distanceToSqr(this)>256||powder()<0||ammo()<0)return;var start=muzzle();if(!l.hasChunkAt(BlockPos.containing(start))||!Authority.mayChange(l,actor,BlockPos.containing(start)))return;int powder=powder(),ammo=ammo();var shot=OwnerCannon.projectile(l,actor,cargo.get(ammo).copyWithCount(1),start,direction(),1.8F);if(shot instanceof OwnerCannonPearl pearl)pearl.boat(this);if(shot!=null&&l.addFreshEntity(shot)){cargo.get(powder).shrink(1);cargo.get(ammo).shrink(1);fired(l);}
 }
 private void fired(ServerLevel l){entityData.set(COOL,60);setDeltaMovement(getDeltaMovement().subtract(direction().scale(.08)));playSound(OwnerSupplementaries.SOUNDS.get("cannon_fire"),2.5F,1);var p=muzzle();l.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,p.x,p.y,p.z,8,.1,.1,.1,.05);}
 boolean fireCrew(OwnerPlunderer crew,LivingEntity target){if(owner!=null||getControllingPassenger()!=crew||cooldown()>0||powder()<0||ammo()<0||!cargo.get(ammo()).is(OwnerSupplementaries.ITEMS.get("cannonball"))||!(level() instanceof ServerLevel l)||isUnderWater())return false;var delta=target.getEyePosition().subtract(position().add(0,.9,0));if(delta.lengthSqr()<25||delta.lengthSqr()>1600)return false;aimAt((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),(float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));if(direction().dot(delta.normalize())<.95)return false;var ball=new OwnerCannonball(OwnerCannon.BALL,l);ball.setOwner(crew);ball.setPos(muzzle());ball.setDeltaMovement(direction().scale(1.8));if(!l.noCollision(ball)||!l.addFreshEntity(ball))return false;cargo.get(powder()).shrink(1);cargo.get(ammo()).shrink(1);fired(l);return true;}
 @Override protected int getMaxPassengers(){return 1;}
 @Override public boolean hurtServer(ServerLevel l,DamageSource d,float amount){if(d.getEntity() instanceof Player p&&!allowed(p))return false;return super.hurtServer(l,d,amount);}
 @Override public void remove(RemovalReason reason){if(reason.shouldDestroy()&&level() instanceof ServerLevel l){Containers.dropContents(l,this,this);clearContent();}super.remove(reason);}
 @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);ContainerHelper.saveAllItems(out,cargo);if(owner!=null)out.store("Owner",UUIDUtil.CODEC,owner);if(firingActor!=null)out.store("FiringActor",UUIDUtil.CODEC,firingActor);out.putInt("Fuse",fuse());out.putInt("Cooldown",cooldown());out.putFloat("CannonPitch",pitch());out.putFloat("CannonYaw",aim());}
 @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);ContainerHelper.loadAllItems(in,cargo);owner=in.read("Owner",UUIDUtil.CODEC).orElse(null);firingActor=in.read("FiringActor",UUIDUtil.CODEC).orElse(null);entityData.set(FUSE,Math.clamp(in.getIntOr("Fuse",0),0,40));entityData.set(COOL,Math.clamp(in.getIntOr("Cooldown",0),0,80));entityData.set(PITCH,finite(in.getFloatOr("CannonPitch",0),-70,25));entityData.set(AIM,finite(in.getFloatOr("CannonYaw",0),-70,70));}
 private static float finite(float v,float min,float max){return Float.isFinite(v)?Math.clamp(v,min,max):0;}
 @Override public Component getDisplayName(){return Component.literal("Cannon Boat — gunpowder + ammunition");}
 @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new DispenserMenu(id,inv,this);}
 @Override public int getContainerSize(){return 9;}@Override public boolean isEmpty(){return cargo.stream().allMatch(ItemStack::isEmpty);}@Override public ItemStack getItem(int slot){return cargo.get(slot);}@Override public ItemStack removeItem(int s,int n){return ContainerHelper.removeItem(cargo,s,n);}@Override public ItemStack removeItemNoUpdate(int s){return ContainerHelper.takeItem(cargo,s);}@Override public void setItem(int slot,ItemStack s){cargo.set(slot,s.copyWithCount(Math.min(s.getCount(),s.getMaxStackSize())));}@Override public void setChanged(){}@Override public boolean stillValid(Player p){return !isRemoved()&&p.distanceToSqr(this)<=64&&allowed(p);}@Override public void clearContent(){cargo.clear();}
}
