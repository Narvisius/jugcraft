package io.github.jimbozoomer.jugcraft.menagerie;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.dispenser.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.entity.vehicle.minecart.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
/** Nine-slot upward dispenser on vanilla rails, firing once at each powered-rail edge. */
public final class OwnerDispenserCart extends AbstractMinecartContainer {
 public static EntityType<OwnerDispenserCart> TYPE;private UUID owner;private boolean powered,onActivator;private long lastFire=-20;
 private final NonNullList<ItemStack> stacks=NonNullList.withSize(9,ItemStack.EMPTY);
 public OwnerDispenserCart(EntityType<? extends OwnerDispenserCart> t,Level l){super(t,l);}
 static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("supps_dispenser_minecart"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<OwnerDispenserCart>of(OwnerDispenserCart::new,MobCategory.MISC).sized(.98F,.7F).clientTrackingRange(8).build(key));}
 @Override public int getContainerSize(){return 9;}@Override public NonNullList<ItemStack> getItemStacks(){return stacks;}@Override public void clearItemStacks(){stacks.clear();}
 @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return new DispenserMenu(id,inv,this);}
 @Override public boolean stillValid(Player p){return super.stillValid(p)&&Menagerie.mayChange(level(),p,blockPosition());}
 @Override public InteractionResult interact(Player p,InteractionHand h,Vec3 hit){if(p.isSpectator()||!Menagerie.mayChange(level(),p,blockPosition()))return InteractionResult.FAIL;if(owner==null&&!level().isClientSide())owner=p.getUUID();return super.interact(p,h,hit);}
 @Override protected Item getDropItem(){return OwnerSupplementaries.ITEMS.get("dispenser_minecart");}@Override public ItemStack getPickResult(){return new ItemStack(getDropItem());}
 @Override public BlockState getDefaultDisplayBlockState(){return Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.UP);}
 @Override public void activateMinecart(ServerLevel l,int x,int y,int z,boolean signal){onActivator=true;if(signal&&!powered&&l.getGameTime()-lastFire>=4){lastFire=l.getGameTime();dispense(l);}powered=signal;}
 @Override public void tick(){onActivator=false;super.tick();if(!level().isClientSide()&&!onActivator)powered=false;}
 private void dispense(ServerLevel l){var actor=Authority.present(l,owner);var pos=blockPosition();if(actor==null)return;
  for(var at:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,2,1)))if(!l.hasChunkAt(at)||!l.isInWorldBounds(at)||!Authority.mayChange(l,actor,at)||!ClaimTransfers.allowed(l,pos,at))return;
  int slot=-1,seen=0;for(int i=0;i<9;i++)if(!getItem(i).isEmpty()&&random.nextInt(++seen)==0)slot=i;if(slot<0){l.levelEvent(1001,pos,0);return;}var ammo=getItem(slot);var shotOrigin=position().add(0,1.15,0);
  if(ammo.getItem() instanceof ProjectileItem||ammo.is(Items.FIRE_CHARGE)||ammo.is(OwnerAmendmentCharge.DRAGON)||ammo.is(Items.TNT)||ammo.is(Items.ENDER_PEARL)){
   Vec3 direction=new Vec3(0,1,0);var rail=l.getBlockState(pos);if(!(rail.getBlock() instanceof BaseRailBlock))rail=l.getBlockState(pos.below());if(rail.getBlock() instanceof BaseRailBlock base){var shape=rail.getValue(base.getShapeProperty());direction=switch(shape){case ASCENDING_EAST->new Vec3(.707,.707,0);case ASCENDING_WEST->new Vec3(-.707,.707,0);case ASCENDING_NORTH->new Vec3(0,.707,-.707);case ASCENDING_SOUTH->new Vec3(0,.707,.707);default->direction;};}
   var entity=OwnerCannon.projectile(l,actor,ammo.copyWithCount(1),shotOrigin,direction,1.1F);if(entity!=null&&l.addFreshEntity(entity)){ammo.shrink(1);setChanged();l.levelEvent(1002,pos,0);}return;
  }
  // Native dispenser methods receive a private nine-slot proxy; no world block is replaced.
  var state=getDefaultDisplayBlockState();var proxy=new DispenserBlockEntity(pos,state);proxy.setLevel(l);for(int i=0;i<9;i++)proxy.setItem(i,getItem(i).copy());var source=new BlockSource(l,pos,state,proxy);var behavior=DispenserBlock.DISPENSER_REGISTRY.get(ammo.getItem());if(behavior==null)behavior=new DefaultDispenseItemBehavior();
  if(behavior==DispenseItemBehavior.NOOP)return;
  var result=behavior.dispense(source,proxy.getItem(slot));proxy.setItem(slot,result);for(int i=0;i<9;i++)setItem(i,proxy.getItem(i));setChanged();
 }
 @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(owner!=null)out.store("Owner",UUIDUtil.CODEC,owner);out.putBoolean("Powered",powered);}
 @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);owner=in.read("Owner",UUIDUtil.CODEC).orElse(null);powered=in.getBooleanOr("Powered",false);}
 public static final class Place extends Item {
  public Place(Properties p){super(p.stacksTo(1));}
  @Override public InteractionResult useOn(UseOnContext c){var p=c.getPlayer();var pos=c.getClickedPos();var state=c.getLevel().getBlockState(pos);if(p==null||p.isSpectator()||!(state.getBlock() instanceof BaseRailBlock rail)||!Menagerie.mayChange(c.getLevel(),p,pos))return InteractionResult.FAIL;if(!(c.getLevel() instanceof ServerLevel l))return InteractionResult.SUCCESS;
   var cart=AbstractMinecart.createMinecart(l,pos.getX()+.5,pos.getY()+.0625+(state.getValue(rail.getShapeProperty()).isSlope()?.5:0),pos.getZ()+.5,TYPE,EntitySpawnReason.SPAWN_ITEM_USE,c.getItemInHand(),p);if(cart==null)return InteractionResult.FAIL;cart.owner=p.getUUID();if(!l.addFreshEntity(cart))return InteractionResult.FAIL;c.getItemInHand().consume(1,p);return InteractionResult.SUCCESS;
  }
 }
}
