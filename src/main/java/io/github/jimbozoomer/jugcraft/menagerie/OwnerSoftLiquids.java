package io.github.jimbozoomer.jugcraft.menagerie;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;

/** Component-preserving drink portions. Empty containers are required to bottle them again. */
public final class OwnerSoftLiquids {
 private OwnerSoftLiquids(){}
 public static ItemStack empty(ItemStack filled){
  if(filled.isEmpty())return ItemStack.EMPTY;
  if(filled.is(Items.MILK_BUCKET))return new ItemStack(Items.BUCKET);
  if(filled.is(Items.POTION)||filled.is(Items.HONEY_BOTTLE))return new ItemStack(Items.GLASS_BOTTLE);
  var consume=filled.get(DataComponents.CONSUMABLE);var remainder=filled.get(DataComponents.USE_REMAINDER);
  if(consume==null||consume.animation()!=ItemUseAnimation.DRINK||remainder==null)return ItemStack.EMPTY;
  var container=remainder.convertInto().create();
  return container.getCount()==1&&(container.is(Items.GLASS_BOTTLE)||container.is(Items.BOWL)||container.is(Items.BUCKET))?container:ItemStack.EMPTY;
 }
 static void normalize(OwnerSupplementData d){
  // Earlier local goblets kept the full drink in their sole inventory slot.
  if(d.block().function.equals("goblet")&&d.softPortions==0&&!empty(d.getItem(0)).isEmpty()){
   d.softDrink=d.getItem(0).copyWithCount(1);d.softPortions=1;d.removeItemNoUpdate(0);
  }
  if(d.softPortions<=0){d.softDrink=ItemStack.EMPTY;d.softPortions=0;}
  else d.softDrink.setCount(1);
 }
 public static int signal(OwnerSupplementData d){return d.softPortions<=0?0:1+14*d.softPortions/Math.max(1,capacity(d,d.softDrink));}
 public static int units(ItemStack filled){return empty(filled).is(Items.BUCKET)?3:1;}
 public static int capacity(OwnerSupplementData d,ItemStack drink){return d.block().function.equals("jar")?9/units(drink):d.block().function.equals("goblet")?1:0;}
 public static boolean canFill(OwnerSupplementData d,ItemStack drink){return !empty(drink).isEmpty()&&d.captive==null&&d.fluid.amount==0&&d.isEmpty()&&d.softPortions<capacity(d,drink)&&(d.softPortions==0||ItemStack.isSameItemSameComponents(d.softDrink,drink));}
 public static InteractionResult use(OwnerSupplementData d,Player player,ItemStack held,InteractionHand hand){
  var container=empty(held);
  if(!container.isEmpty()){
   if(!canFill(d,held))return InteractionResult.FAIL;
   d.softDrink=held.copyWithCount(1);d.softPortions++;d.changed();
   player.setItemInHand(hand,ItemUtils.createFilledResult(held,player,container));
   d.getLevel().playSound(null,d.getBlockPos(),net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY,net.minecraft.sounds.SoundSource.BLOCKS,.7F,1);return InteractionResult.SUCCESS;
  }
  if(d.softPortions>0&&held.is(empty(d.softDrink).getItem())){
   var result=d.softDrink.copyWithCount(1);remove(d);player.setItemInHand(hand,ItemUtils.createFilledResult(held,player,result));
   d.getLevel().playSound(null,d.getBlockPos(),net.minecraft.sounds.SoundEvents.BOTTLE_FILL,net.minecraft.sounds.SoundSource.BLOCKS,.7F,1);return InteractionResult.SUCCESS;
  }
  return InteractionResult.PASS;
 }
 public static InteractionResult drink(OwnerSupplementData d,Player player){
  if(d.softPortions<=0||d.getLevel().getGameTime()<d.pulseUntil)return InteractionResult.PASS;
  var drink=d.softDrink.copyWithCount(1);var consumable=drink.get(DataComponents.CONSUMABLE);
  if(consumable==null||!consumable.canConsume(player,drink))return InteractionResult.FAIL;
  // Its bottle/bucket was returned when poured; drinking must not create another one.
  drink.remove(DataComponents.USE_REMAINDER);remove(d);d.pulseUntil=d.getLevel().getGameTime()+20;d.changed();drink.finishUsingItem(d.getLevel(),player);return InteractionResult.SUCCESS;
 }
 private static void remove(OwnerSupplementData d){if(--d.softPortions<=0){d.softPortions=0;d.softDrink=ItemStack.EMPTY;}d.changed();}
 static boolean move(OwnerSupplementData from,OwnerSupplementData to){if(from==to||from.softPortions<=0||!canFill(to,from.softDrink))return false;to.softDrink=from.softDrink.copyWithCount(1);to.softPortions++;remove(from);to.changed();return true;}
 public static int color(ItemStack drink){return drink.is(Items.MILK_BUCKET)?0xFFF5EEE0:drink.is(Items.HONEY_BOTTLE)?0xFFE9A327:0xFF000000|drink.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).getColor();}
}
