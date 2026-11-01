package io.github.jimbozoomer.jugcraft.menagerie;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
/** Native equipment persistence and original short wooden mannequin, with head-only interaction. */
public final class OwnerHatStand extends ArmorStand {
 public static EntityType<OwnerHatStand> TYPE;
 private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DANCE=net.minecraft.network.syncher.SynchedEntityData.defineId(OwnerHatStand.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
 private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> WATCH=net.minecraft.network.syncher.SynchedEntityData.defineId(OwnerHatStand.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
 private int danceRemaining,animationTicks;private float restYaw;
 @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(DANCE,0);b.define(WATCH,-1);}
 public int dance(){return entityData.get(DANCE);}
 public float animationTime(float partial){return animationTicks+partial;}
 private boolean animate(Player target){
  var base=level().getBlockState(getOnPos()).getBlock();int mode=base instanceof net.minecraft.world.level.block.AbstractCauldronBlock||base instanceof net.minecraft.world.level.block.ComposterBlock?2:base instanceof net.minecraft.world.level.block.HopperBlock||base instanceof net.minecraft.world.level.block.DecoratedPotBlock?1:0;
  if(mode==0||dance()!=0)return false;restYaw=getYRot();danceRemaining=160;entityData.set(DANCE,mode);entityData.set(WATCH,target==null?-1:target.getId());return true;
 }
 @Override public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> data){super.onSyncedDataUpdated(data);if(data.equals(DANCE))animationTicks=0;}
 @Override public void tick(){super.tick();if(dance()!=0)animationTicks++;
  if(level() instanceof ServerLevel){if(danceRemaining>0){if(--danceRemaining==0){entityData.set(DANCE,0);entityData.set(WATCH,-1);setYRot(restYaw);}}
   else if(tickCount%100==Math.floorMod(getId(),100)&&random.nextFloat()<.2F)animate(null);
   var target=level().getEntity(entityData.get(WATCH));if(dance()!=0&&target!=null&&target.isAlive()&&distanceToSqr(target)<100){var delta=target.position().subtract(position());float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));setYRot(net.minecraft.util.Mth.rotLerp(.5F,getYRot(),yaw));}
  }
 }

 public OwnerHatStand(EntityType<? extends OwnerHatStand> type,Level l){super(type,l);lastHit=-100;}
 static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("supps_hat_stand"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<OwnerHatStand>of(OwnerHatStand::new,MobCategory.MISC).sized(.5F,.75F).clientTrackingRange(8).build(key));net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(TYPE,ArmorStand.createAttributes());}
 @Override public boolean canUseSlot(EquipmentSlot slot){return slot==EquipmentSlot.HEAD;}
 @Override public InteractionResult interact(Player p,InteractionHand h,Vec3 hit){
  if(!Menagerie.mayChange(level(),p,blockPosition())||p.isSpectator())return InteractionResult.FAIL;var held=p.getItemInHand(h);if(held.getItem() instanceof OwnerSupplementWrench){if(!level().isClientSide()){setYRot(getYRot()+(p.isShiftKeyDown()?-22.5F:22.5F));held.hurtAndBreak(1,p,h.asEquipmentSlot());}return InteractionResult.SUCCESS;}
  if(p.isShiftKeyDown()&&held.isEmpty()){if(!level().isClientSide()&&!animate(p))setNoBasePlate(showBasePlate());return InteractionResult.SUCCESS;}
  if(!held.isEmpty()&&getEquipmentSlotForItem(held)!=EquipmentSlot.HEAD)return InteractionResult.PASS;
  if(!level().isClientSide()){var old=getItemBySlot(EquipmentSlot.HEAD).copy();setItemSlot(EquipmentSlot.HEAD,held.isEmpty()?ItemStack.EMPTY:held.copyWithCount(1));if(!held.isEmpty())held.consume(1,p);if(!old.isEmpty())p.getInventory().placeItemBackInInventory(old,net.minecraft.util.Prediction.SERVER_ONLY);}return InteractionResult.SUCCESS;
 }
 @Override public boolean hurtServer(ServerLevel l,net.minecraft.world.damagesource.DamageSource source,float amount){
  if(isRemoved()||!(source.getEntity() instanceof ServerPlayer p)||!Menagerie.mayChange(l,p,blockPosition()))return false;
  long now=l.getGameTime();if(!p.hasInfiniteMaterials()&&now-lastHit>5){lastHit=now;l.broadcastEntityEvent(this,(byte)32);return true;}
  var hat=getItemBySlot(EquipmentSlot.HEAD).copy();setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);if(!hat.isEmpty())spawnAtLocation(l,hat);if(!p.hasInfiniteMaterials())spawnAtLocation(l,new ItemStack(OwnerSupplementaries.ITEMS.get("hat_stand")));discard();return true;
 }
 @Override public ItemStack getPickResult(){return new ItemStack(OwnerSupplementaries.ITEMS.get("hat_stand"));}
 public static final class Place extends Item {
  public Place(Properties p){super(p);}
  @Override public InteractionResult useOn(UseOnContext c){var p=c.getPlayer();if(p==null||c.getClickedFace()==Direction.DOWN)return InteractionResult.FAIL;var context=new BlockPlaceContext(c);var at=context.getClickedPos();if(!context.canPlace()||!Menagerie.mayChange(c.getLevel(),p,at))return InteractionResult.FAIL;if(!(c.getLevel() instanceof ServerLevel l))return InteractionResult.SUCCESS;
   var entity=new OwnerHatStand(TYPE,l);entity.setPos(Vec3.atBottomCenterOf(at));entity.setYRot(Math.round((p.getYRot()-180)/22.5F)*22.5F);EntityType.createDefaultStackConfig(l,c.getItemInHand(),p).apply(entity);if(!l.getWorldBorder().isWithinBounds(entity.getBoundingBox())||!l.noCollision(entity)||!l.getEntities(entity,entity.getBoundingBox()).isEmpty()||!l.addFreshEntity(entity))return InteractionResult.FAIL;c.getItemInHand().consume(1,p);l.playSound(null,at,net.minecraft.sounds.SoundEvents.ARMOR_STAND_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,.75F,.8F);return InteractionResult.SUCCESS;
  }
 }
}
