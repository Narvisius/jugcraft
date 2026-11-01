package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
/** Original portable ranged tools. No research gate; real ammo is paid only after entity insertion. */
public final class OwnerFieldTools {
 private OwnerFieldTools(){}
 static boolean allowed(ItemStack ammo){return !ammo.isEmpty()&&(ammo.getItem() instanceof BlockItem||ammo.getItem() instanceof ProjectileItem||ammo.getItem() instanceof BucketItem bucket&&bucket.getContent()!=net.minecraft.world.level.material.Fluids.EMPTY||ammo.is(Items.POWDER_SNOW_BUCKET)||ammo.is(Items.ENDER_PEARL)||ammo.is(Items.FIRE_CHARGE))&&!ammo.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,io.github.jimbozoomer.jugcraft.Jugcraft.id("supps_slingshot_blacklist")));}
 static ItemStack ammo(Player p){var off=p.getOffhandItem();if(allowed(off))return off;for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(allowed(s))return s;}return ItemStack.EMPTY;}
 public static final class Slingshot extends Item {
  public static int chargeTicks(ItemStack s,LivingEntity user){return Math.max(1,Math.round(net.minecraft.world.item.enchantment.EnchantmentHelper.modifyCrossbowChargingTime(s,user,1)*20));}
  private static boolean parrots(Player p){return p.getShoulderParrotLeft().isPresent()||p.getShoulderParrotRight().isPresent();}
  private static boolean launchParrots(ServerPlayer p,ItemStack weapon,float power){boolean launched=false;
   for(boolean left:new boolean[]{true,false}){var variant=left?p.getShoulderParrotLeft():p.getShoulderParrotRight();if(variant.isEmpty()||weapon.isEmpty())continue;
    var bird=new net.minecraft.world.entity.animal.parrot.Parrot(net.minecraft.world.entity.EntityTypes.PARROT,p.level());var skin=new ItemStack(Items.PARROT_SPAWN_EGG);skin.set(net.minecraft.core.component.DataComponents.PARROT_VARIANT,variant.get());bird.applyComponentsFromItemStack(skin);bird.tame(p);bird.setOrderedToSit(false);bird.setPos(p.getEyePosition().add(p.getLookAngle().scale(.6)));bird.setDeltaMovement(p.getLookAngle().scale(power*4.4));
    if(!p.level().hasChunkAt(bird.blockPosition())||!io.github.jimbozoomer.jugcraft.concordance.Authority.mayChange(p.level(),p,bird.blockPosition())||!p.level().getWorldBorder().isWithinBounds(bird.getBoundingBox())||!p.level().noCollision(bird)||!p.level().addFreshEntity(bird))continue;
    if(left)p.setShoulderParrotLeft(java.util.Optional.empty());else p.setShoulderParrotRight(java.util.Optional.empty());bird.needsSync=true;weapon.hurtAndBreak(1,p,p.getUsedItemHand().asEquipmentSlot());launched=true;
   }return launched;
  }
  public Slingshot(Properties p){super(p.durability(192).repairable(Items.STRING).enchantable(1));}
  @Override public int getUseDuration(ItemStack s,LivingEntity e){return 72000;}
  @Override public net.minecraft.world.item.ItemUseAnimation getUseAnimation(ItemStack s){return net.minecraft.world.item.ItemUseAnimation.BOW;}
  @Override public InteractionResult use(Level l,Player p,InteractionHand h){if(ammo(p).isEmpty()&&!parrots(p)||p.isSpectator())return InteractionResult.FAIL;p.startUsingItem(h);l.playSound(p,p.blockPosition(),OwnerSupplementaries.SOUNDS.get("slingshot_charge"),SoundSource.PLAYERS,1,1);return InteractionResult.CONSUME;}
  @Override public boolean releaseUsing(ItemStack weapon,Level l,LivingEntity entity,int remaining){
   if(!(entity instanceof ServerPlayer p)||p.isSpectator())return false;float f=(72000-remaining)/(float)chargeTicks(weapon,p);float power=Math.min(1,(f*f+2*f)/3);if(power<.085F)return false;
   if(parrots(p)&&launchParrots(p,weapon,power)){l.playSound(null,p.blockPosition(),OwnerSupplementaries.SOUNDS.get("slingshot_shoot"),SoundSource.PLAYERS,1,1);return true;}
   var ammo=ammo(p);if(ammo.isEmpty())return false;boolean stasis=OwnerBubbles.stasis(weapon);
   int shots=Math.clamp(net.minecraft.world.item.enchantment.EnchantmentHelper.processProjectileCount(p.level(),weapon,p,1),1,5);float spread=net.minecraft.world.item.enchantment.EnchantmentHelper.processProjectileSpread(p.level(),weapon,p,0);boolean launched=false;
   for(int i=0;i<shots&&!weapon.isEmpty()&&!ammo.isEmpty();i++){
    Projectile shot;if(ammo.getItem() instanceof BlockItem||ammo.getItem() instanceof DispensibleContainerItem||ammo.getItem() instanceof Bomb)shot=OwnerFieldProjectiles.make(l,ammo,p);
    else if(ammo.is(Items.ENDER_PEARL))shot=new net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl(l,p,ammo.copyWithCount(1));
    else if(ammo.is(Items.FIRE_CHARGE)){var charge=new OwnerAmendmentCharge(OwnerAmendmentCharge.TYPE,l);charge.setItem(ammo.copyWithCount(1));shot=charge;}
    else if(ammo.getItem() instanceof ProjectileItem item)shot=item.asProjectile(l,p.getEyePosition(),ammo.copyWithCount(1),p.getDirection());else break;
    shot.setOwner(p);shot.addTag("JugcraftHouseholdCannon");shot.setNoGravity(stasis);shot.setPos(p.getX(),p.getEyeY()-.1,p.getZ());float angle=i==0?0:((i%2==1)?1:-1)*((i+1)/2)*spread;shot.shootFromRotation(p,p.getXRot(),p.getYRot()+angle,0,power*(stasis?1.65F:1.1F),1);
    if(!p.level().addFreshEntity(shot))continue;ammo.consume(1,p);weapon.hurtAndBreak(1,p,p.getUsedItemHand().asEquipmentSlot());launched=true;
   }
   if(launched)l.playSound(null,p.blockPosition(),OwnerSupplementaries.SOUNDS.get("slingshot_shoot"),SoundSource.PLAYERS,1,1);return launched;
  }

  @Override public void appendHoverText(ItemStack s,TooltipContext c,TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){out.accept(Component.literal("Hold to draw; release to launch. Offhand selects ammunition."));out.accept(Component.literal("Blocks and filled buckets place on impact where you have permission. Each projectile uses real ammunition."));}
 }
 public static final class Bomb extends Item implements ProjectileItem {
  public Bomb(Properties p){super(p.stacksTo(16));}
  @Override public InteractionResult use(Level l,Player p,InteractionHand h){if(!(p instanceof ServerPlayer actor))return InteractionResult.SUCCESS;var s=p.getItemInHand(h);if(p.isSpectator()||p.getCooldowns().isOnCooldown(s))return InteractionResult.FAIL;var shot=OwnerFieldProjectiles.make(l,s,p);shot.setPos(p.getX(),p.getEyeY()-.1,p.getZ());shot.shootFromRotation(p,p.getXRot(),p.getYRot(),-10,1.25F,1);if(!actor.level().addFreshEntity(shot))return InteractionResult.FAIL;s.consume(1,p);p.getCooldowns().addCooldown(s,30);l.playSound(null,p.blockPosition(),SoundEvents.SNOWBALL_THROW,SoundSource.PLAYERS,.5F,.8F);return InteractionResult.SUCCESS;}
  @Override public Projectile asProjectile(Level l,Position p,ItemStack s,Direction d){var shot=new OwnerFieldProjectiles(OwnerFieldProjectiles.TYPE,l);shot.setItem(OwnerFieldProjectiles.display(s,false));shot.setPos(p.x(),p.y(),p.z());return shot;}
 }
}
