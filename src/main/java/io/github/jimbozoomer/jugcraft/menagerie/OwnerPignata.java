package io.github.jimbozoomer.jugcraft.menagerie;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
/** Stuffable, hanging or free-standing party vessel. Only inserted contents are spilled. */
public final class OwnerPignata extends Entity implements Container,Leashable {
 public static EntityType<OwnerPignata> TYPE;
 private static final EntityDataAccessor<Optional<BlockPos>> HOOK=SynchedEntityData.defineId(OwnerPignata.class,EntityDataSerializers.OPTIONAL_BLOCK_POS);
 private final NonNullList<ItemStack> contents=NonNullList.withSize(9,ItemStack.EMPTY);
 private net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> lootTable;private long lootSeed;
 private final List<ItemStack> rewards=new ArrayList<>();
 public float swingX,swingZ,oldSwingX,oldSwingZ,pivotY=1,oldPivotY=1;private float speedX,speedZ;private Vec3 lastMovement=Vec3.ZERO;
 private LeashData leash;private float damage;private UUID owner;private boolean bursting;public int lastWobble=-100;
 public OwnerPignata(EntityType<? extends OwnerPignata> t,Level l){super(t,l);blocksBuilding=true;}
 static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("supps_pignata"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<OwnerPignata>of(OwnerPignata::new,MobCategory.MISC).sized(.8F,1F).clientTrackingRange(8).updateInterval(1).build(key));}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(HOOK,Optional.empty());}
 public BlockPos hook(){return entityData.get(HOOK).orElse(null);}
 private net.minecraft.world.level.block.piston.PistonMovingBlockEntity movingAnchor(BlockPos hook){
  for(Direction dir:Direction.values()){var pos=hook.relative(dir);if(!level().hasChunkAt(pos))continue;var node=level().getBlockEntity(pos);if(node instanceof net.minecraft.world.level.block.piston.PistonMovingBlockEntity moving&&moving.getMovedState().is(OwnerSupplementaries.BLOCKS.get("pignata_anchor"))&&moving.getMovementDirection()==dir)return moving;}
  return level().getBlockEntity(hook) instanceof net.minecraft.world.level.block.piston.PistonMovingBlockEntity m&&m.getMovedState().is(OwnerSupplementaries.BLOCKS.get("pignata_anchor"))?m:null;
 }
 public Vec3 anchorOffset(float partial){var hook=hook();if(hook==null||!level().hasChunkAt(hook))return Vec3.ZERO;var moving=movingAnchor(hook);if(moving==null)return Vec3.ZERO;var at=moving.getBlockPos();return new Vec3(at.getX()+.5+moving.getXOff(partial),at.getY()+moving.getYOff(partial),at.getZ()+.5+moving.getZOff(partial)).subtract(getPosition(partial));}
 private void swing(){oldSwingX=swingX;oldSwingZ=swingZ;oldPivotY=pivotY;var movement=position().subtract(xo,yo,zo);var acceleration=movement.subtract(lastMovement);lastMovement=movement;boolean feet=hook()==null&&onGround();pivotY+=((feet?0:getBbHeight())-pivotY)*.3F;
  float stiffness=feet?.3F:.1F,damping=feet?.25F:.06F,lag=feet?-1.5F:1.5F;float tx=0,tz=0;var holder=getLeashHolder();if(holder!=null&&hook()==null&&!onGround()){var rope=holder.getRopeHoldPosition(1).subtract(position().add(0,getBbHeight(),0));double horizontal=rope.horizontalDistance();if(horizontal>.0001&&rope.lengthSqr()>4){float tilt=(float)Math.min(Math.atan2(horizontal,rope.y),1.2);tx=(float)(-rope.x/horizontal*tilt);tz=(float)(-rope.z/horizontal*tilt);}}
  speedX+=(tx-swingX)*stiffness-speedX*damping-(float)acceleration.x*lag;speedZ+=(tz-swingZ)*stiffness-speedZ*damping-(float)acceleration.z*lag;swingX=Math.clamp(swingX+speedX,-1.3F,1.3F);swingZ=Math.clamp(swingZ+speedZ,-1.3F,1.3F);
 }
 @Override public void tick(){super.tick();if(level().isClientSide()){swing();return;}if(level() instanceof ServerLevel l){
  Leashable.tickLeash(l,this);var hook=hook();if(hook!=null){if(!l.hasChunkAt(hook))return;if(!l.getBlockState(hook).is(OwnerSupplementaries.BLOCKS.get("pignata_anchor"))){var moving=movingAnchor(hook);if(moving!=null){var at=moving.getBlockPos();entityData.set(HOOK,Optional.of(at));setDeltaMovement(Vec3.ZERO);setPos(at.getX()+.5+moving.getXOff(1),at.getY()+moving.getYOff(1),at.getZ()+.5+moving.getZOff(1));return;}entityData.set(HOOK,Optional.empty());}else{setDeltaMovement(Vec3.ZERO);setPos(hook.getX()+.5,hook.getY(),hook.getZ()+.5);return;}}
  var velocity=getDeltaMovement().add(0,isInWater()?.015:-.04,0);var target=BlockPos.containing(position().add(velocity));if(!l.hasChunkAt(target)){setDeltaMovement(Vec3.ZERO);return;}move(MoverType.SELF,velocity);setDeltaMovement(getDeltaMovement().multiply(onGround()?.6:.98,.98,onGround()?.6:.98));
 }}
 @Override public boolean isPickable(){return true;}
 @Override public boolean isPushable(){return hook()==null;}
 @Override public boolean canBeCollidedWith(Entity other){return !isRemoved();}
 @Override public InteractionResult interact(Player p,InteractionHand h,net.minecraft.world.phys.Vec3 hit){if(p.isSpectator()||!Menagerie.mayChange(level(),p,blockPosition()))return InteractionResult.FAIL;var held=p.getItemInHand(h);
  if(held.is(Items.LEAD)&&!isLeashed()){if(!level().isClientSide()){setLeashedTo(p,true);held.consume(1,p);}return InteractionResult.SUCCESS;}
  if(held.isEmpty()&&p.isShiftKeyDown()&&isLeashed()){if(!level().isClientSide())dropLeash();return InteractionResult.SUCCESS;}
  if(held.isEmpty()||!canPlaceItem(0,held))return InteractionResult.PASS;if(level().isClientSide())return InteractionResult.SUCCESS;var offered=p.hasInfiniteMaterials()?held.copy():held;int before=offered.getCount();
  for(int i=0;i<contents.size()&&!offered.isEmpty();i++){var current=contents.get(i);if(current.isEmpty())contents.set(i,offered.split(Math.min(offered.getCount(),offered.getMaxStackSize())));else if(ItemStack.isSameItemSameComponents(current,offered)){int n=Math.min(offered.getCount(),current.getMaxStackSize()-current.getCount());current.grow(n);offered.shrink(n);}}
  if(offered.getCount()==before){p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("The piñata is full."));return InteractionResult.FAIL;}level().broadcastEntityEvent(this,(byte)61);playSound(SoundEvents.BUNDLE_INSERT,1,.9F);return InteractionResult.SUCCESS;
 }
 @Override public boolean hurtServer(ServerLevel l,net.minecraft.world.damagesource.DamageSource source,float amount){if(isRemoved()||!(source.getEntity() instanceof ServerPlayer p)||!Menagerie.mayChange(l,p,blockPosition()))return false;damage+=Math.clamp(amount,0,100);playSound(OwnerSupplementaries.SOUNDS.get("pignata_hit"),1,.9F+random.nextFloat()*.2F);l.broadcastEntityEvent(this,(byte)61);if(hook()==null)setDeltaMovement(getDeltaMovement().add(position().subtract(p.position()).normalize().scale(Math.min(.5,.08+amount*.04))));if(damage>=10)burst(l,p);return true;}
 private void burst(ServerLevel l,Player p){if(bursting)return;bursting=true;try{
  if(lootTable!=null){var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(l).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,position()).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,p).withLuck(p.getLuck());var generated=l.getServer().reloadableRegistries().getLootTable(lootTable).getRandomItems(params.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST),lootSeed);rewards.addAll(generated);lootTable=null;}
  var all=new ArrayList<ItemStack>(contents);all.addAll(rewards);int singles=0;for(var stack:all){while(!stack.isEmpty()){int n=singles<32?1:stack.getCount();var loot=stack.copyWithCount(n);var entity=new ItemEntity(l,getX(),getY()+.5,getZ(),loot);entity.setDeltaMovement(new Vec3(random.nextGaussian(),Math.abs(random.nextGaussian()),random.nextGaussian()).normalize().scale(.4));entity.setDefaultPickUpDelay();if(!l.addFreshEntity(entity))return;stack.shrink(n);singles++;}}
  var at=hook();if(at!=null&&l.hasChunkAt(at)&&Menagerie.mayChange(l,p,at)&&l.getBlockState(at).is(OwnerSupplementaries.BLOCKS.get("pignata_anchor")))l.setBlockAndUpdate(at,l.getFluidState(at).createLegacyBlock());entityData.set(HOOK,Optional.empty());dropLeash();OwnerConfetti.burst(l,new OwnerConfetti.Burst(position().add(0,.5,0),Vec3.ZERO,List.of(),80,true));l.broadcastEntityEvent(this,(byte)60);playSound(OwnerSupplementaries.SOUNDS.get("pignata_burst"),1.2F,1);discard();
 }finally{bursting=false;}}
 @Override public void handleEntityEvent(byte id){if(id==60||id==61){lastWobble=tickCount;speedX+=(random.nextBoolean()?1:-1)*.2F;speedZ+=(random.nextBoolean()?1:-1)*.2F;}else super.handleEntityEvent(id);}
 @Override protected void addAdditionalSaveData(ValueOutput out){ContainerHelper.saveAllItems(out,contents);out.putFloat("Damage",damage);if(owner!=null)out.store("Owner",UUIDUtil.CODEC,owner);if(hook()!=null)out.store("Hook",BlockPos.CODEC,hook());if(lootTable!=null)out.store("LootTable",net.minecraft.resources.ResourceKey.codec(Registries.LOOT_TABLE),lootTable);out.putLong("LootTableSeed",lootSeed);out.store("PendingRewards",ItemStack.CODEC.listOf(),rewards);writeLeashData(out,leash);}
 @Override protected void readAdditionalSaveData(ValueInput in){ContainerHelper.loadAllItems(in,contents);damage=Math.clamp(in.getFloatOr("Damage",0),0,100);owner=in.read("Owner",UUIDUtil.CODEC).orElse(null);entityData.set(HOOK,in.read("Hook",BlockPos.CODEC));lootTable=in.read("LootTable",net.minecraft.resources.ResourceKey.codec(Registries.LOOT_TABLE)).orElse(null);lootSeed=in.getLongOr("LootTableSeed",0);rewards.clear();rewards.addAll(in.read("PendingRewards",ItemStack.CODEC.listOf()).orElse(List.of()));readLeashData(in);}
 @Override public LeashData getLeashData(){return leash;}@Override public void setLeashData(LeashData d){leash=d;}
 @Override public Vec3 getLeashOffset(float partial){return new Vec3(0,.85,0);}
 @Override public boolean canBeLeashed(){return hook()==null;}
 @Override public int getContainerSize(){return contents.size();}@Override public boolean isEmpty(){return contents.stream().allMatch(ItemStack::isEmpty);}
 @Override public ItemStack getItem(int slot){return contents.get(slot);}@Override public ItemStack removeItem(int slot,int count){return ContainerHelper.removeItem(contents,slot,count);}@Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(contents,slot);}
 @Override public void setItem(int slot,ItemStack stack){contents.set(slot,stack.copyWithCount(Math.min(stack.getCount(),stack.getMaxStackSize())));}@Override public void setChanged(){}@Override public boolean stillValid(Player p){return !isRemoved()&&p.distanceToSqr(this)<=64&&Menagerie.mayChange(level(),p,blockPosition());}@Override public void clearContent(){contents.clear();}
 @Override public boolean canPlaceItem(int slot,ItemStack s){return s.getItem().canFitInsideContainerItems()&&!s.has(net.minecraft.core.component.DataComponents.CONTAINER);}
 @Override public boolean canTakeItem(Container destination,int slot,ItemStack stack){return false;}
 @Override public ItemStack getPickResult(){return new ItemStack(OwnerSupplementaries.ITEMS.get("pignata"));}
 public static final class Place extends Item {
  public Place(Properties p){super(p);}
  @Override public InteractionResult useOn(UseOnContext c){var p=c.getPlayer();var at=c.getClickedPos().relative(c.getClickedFace());if(p==null||p.isSpectator()||!c.getLevel().getBlockState(at).isAir()||!Menagerie.mayChange(c.getLevel(),p,at))return InteractionResult.FAIL;if(!(c.getLevel() instanceof ServerLevel l))return InteractionResult.SUCCESS;
   var entity=new OwnerPignata(TYPE,l);entity.owner=p.getUUID();entity.setPos(Vec3.atBottomCenterOf(at));entity.setYRot(p.getYRot()+180);EntityType.createDefaultStackConfig(l,c.getItemInHand(),p).apply(entity);if(!l.noCollision(entity)||!l.getWorldBorder().isWithinBounds(entity.getBoundingBox())||!l.getEntities(entity,entity.getBoundingBox()).isEmpty())return InteractionResult.FAIL;
   boolean hang=c.getClickedFace()!=Direction.UP&&l.getBlockState(at.above()).isFaceSturdy(l,at.above(),Direction.DOWN);if(hang){if(!l.setBlockAndUpdate(at,OwnerSupplementaries.BLOCKS.get("pignata_anchor").defaultBlockState()))return InteractionResult.FAIL;entity.entityData.set(HOOK,Optional.of(at));}
   if(!l.addFreshEntity(entity)){if(hang)l.removeBlock(at,false);return InteractionResult.FAIL;}c.getItemInHand().consume(1,p);l.playSound(null,at,SoundEvents.WOOL_PLACE,SoundSource.BLOCKS,1,1);return InteractionResult.SUCCESS;
  }
 }
}
