package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** A bounded sighting aid for the cannon under the player's wrench/spyglass. Never queries unloaded terrain. */
final class OwnerCannonPreview {
 private OwnerCannonPreview(){}
 static void register(){LevelRenderEvents.COLLECT_SUBMITS.register(context->{
  var mc=Minecraft.getInstance();var player=mc.player;var level=mc.level;
  if(player==null||level==null||mc.gui.screen()!=null||!(mc.hitResult instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK)return;
  boolean tool=player.getMainHandItem().getItem() instanceof OwnerSupplementWrench||player.getOffhandItem().getItem() instanceof OwnerSupplementWrench||player.isScoping();
  if(!tool||!(level.getBlockEntity(hit.getBlockPos()) instanceof OwnerSupplementData data)||!data.block().function.equals("cannon")||player.distanceToSqr(Vec3.atCenterOf(hit.getBlockPos()))>64)return;
  var ammo=data.getItem(1);double gravity,drag=.99;
  if(ammo.is(OwnerSupplementaries.ITEMS.get("cannonball")))gravity=.035;
  else if(ammo.getItem() instanceof ArrowItem||ammo.is(Items.TRIDENT))gravity=.05;
  else if(ammo.is(Items.SNOWBALL)||ammo.is(Items.EGG)||ammo.is(Items.ENDER_PEARL))gravity=.03;
  else if(ammo.is(Items.SPLASH_POTION)||ammo.is(Items.LINGERING_POTION))gravity=.05;
  else if(ammo.getItem() instanceof OwnerFieldTools.Bomb)gravity=.04;
  else if(ammo.getItem() instanceof ProjectileItem)return; // Unsupported trajectories are not misrepresented.
  else if(ammo.isEmpty()||ammo.getItem() instanceof SpawnEggItem||ammo.is(Items.ARMOR_STAND)||ammo.is(Items.FIREWORK_ROCKET)||ammo.is(Items.FIRE_CHARGE))return;
  else {gravity=.04;drag=.98;}
  Vec3 origin=OwnerCannon.mouth(data),pos=origin,velocity=OwnerCannon.direction(data.cannonYaw,data.cannonPitch).scale(OwnerCannon.velocity(data.cannonPower));
  var dots=new ArrayList<Vec3>();boolean collision=false;
  for(int tick=0;tick<100;tick++){
   Vec3 next=pos.add(velocity);boolean loaded=true;
   for(int part=0;part<=4;part++){var cell=BlockPos.containing(pos.lerp(next,part/4D));if(!level.hasChunkAt(cell)||!level.isInWorldBounds(cell)||!level.getWorldBorder().isWithinBounds(cell)){loaded=false;break;}}
   if(!loaded)break;
   var ray=level.clip(new ClipContext(pos,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,player));
   if(ray.getType()!=HitResult.Type.MISS){dots.add(ray.getLocation().subtract(origin));collision=true;break;}
   if(tick%2==0)dots.add(next.subtract(origin));pos=next;velocity=velocity.scale(drag).add(0,-gravity,0);
  }
  if(dots.isEmpty())return;
  boolean hitSurface=collision;var camera=context.levelState().cameraRenderState.pos;var pose=context.poseStack();pose.pushPose();pose.translate(origin.x-camera.x,origin.y-camera.y,origin.z-camera.z);
  context.submitNodeCollector().submitCustomGeometry(pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(Jugcraft.id("textures/misc/blueprint_ghost.png")),(matrix,consumer)->{
   for(int i=0;i<dots.size();i++){var dot=dots.get(i);boolean end=hitSurface&&i==dots.size()-1;float radius=end?.12F:.035F,x=(float)dot.x,y=(float)dot.y,z=(float)dot.z;LinePreview.cube(matrix,consumer,x-radius,y-radius,z-radius,x+radius,y+radius,z+radius,end?0xCCFFAA44:0xAA80DFFF);}
  });pose.popPose();
 });}
}
