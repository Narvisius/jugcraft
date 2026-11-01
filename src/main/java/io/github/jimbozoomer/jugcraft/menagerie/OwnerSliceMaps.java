package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
/** Native filled maps recording the chosen underground elevation instead of the surface. */
public final class OwnerSliceMaps extends Item {
 public OwnerSliceMaps(Properties p){super(p);}
 @Override public InteractionResult use(Level l,Player p,InteractionHand h){if(!(p instanceof ServerPlayer player))return InteractionResult.SUCCESS;if(p.isSpectator())return InteractionResult.FAIL;var used=p.getItemInHand(h);int y=p.blockPosition().getY()+1;var map=MapItem.create(player.level(),p.getBlockX(),p.getBlockZ(),(byte)0,true,false);var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("JugcraftSliceY",y);map.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));map.set(DataComponents.CUSTOM_NAME,Component.literal("Cave Map • Y "+y));used.consume(1,p);if(used.isEmpty())p.setItemInHand(h,map);else if(!p.getInventory().add(map))p.drop(map,false,net.minecraft.util.Prediction.SERVER_ONLY);return InteractionResult.SUCCESS;}
 public static boolean tick(ItemStack stack,ServerLevel level,Entity carrier,EquipmentSlot slot){
  var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(!tag.contains("JugcraftSliceY"))return false;var map=MapItem.getSavedData(stack,level);if(map==null)return true;
  if(carrier instanceof Player player)map.tickCarriedBy(player,stack,null);
  if(map.locked||slot==null||slot.getType()!=EquipmentSlot.Type.HAND||!map.dimension.equals(level.dimension()))return true;
  int y=Math.clamp(tag.getIntOr("JugcraftSliceY",carrier.getBlockY()),level.getMinY(),level.getMaxY());int scale=1<<map.scale;
  // One stripe per tick, capped at 128 samples and nine blocks of vertical inspection each.
  int stripe=Math.floorMod((int)level.getGameTime(),128);for(int z=0;z<128;z++){
   int wx=map.centerX+(stripe-64)*scale,wz=map.centerZ+(z-64)*scale;var pos=new BlockPos(wx,y,wz);
   if(carrier.distanceToSqr(wx,y,wz)>128D*128||!level.hasChunkAt(pos))continue;
   var state=level.getBlockState(pos);MapColor color=state.getMapColor(level,pos);var brightness=MapColor.Brightness.LOW;
   if(state.isAir()){for(int dy=1;dy<=8&&y-dy>=level.getMinY();dy++){var below=pos.below(dy);var floor=level.getBlockState(below);if(floor.isAir())continue;color=floor.getMapColor(level,below);brightness=dy<=3?MapColor.Brightness.HIGH:MapColor.Brightness.NORMAL;break;}}
   map.updateColor(stripe,z,color.getPackedId(brightness));
  }return true;
 }
}
