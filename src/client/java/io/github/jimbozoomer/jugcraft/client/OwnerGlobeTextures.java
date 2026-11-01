package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
/** Bounded dynamic textures using supplied globe palettes and server-generated 32x16 artwork. */
final class OwnerGlobeTextures {
 private record Key(String pixels,boolean sepia){}
 private static final Map<Key,Identifier> CACHE=new LinkedHashMap<>();private static final Map<Boolean,int[]> PALETTES=new HashMap<>();private static int next;
 static void register(){
  net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES).registerReloadListener(Jugcraft.id("supps_globes"),(net.minecraft.server.packs.resources.ResourceManagerReloadListener)r->clear());
  net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->clear());
 }
 private static void clear(){var textures=Minecraft.getInstance().getTextureManager();for(var id:CACHE.values())textures.release(id);CACHE.clear();PALETTES.clear();}
 static Identifier texture(byte[] pixels,boolean sepia){
  if(pixels.length!=512)return Jugcraft.id("textures/newest/supps/entity/globes/globe_the_world"+(sepia?"_sepia":"")+".png");
  var key=new Key(Base64.getEncoder().encodeToString(pixels),sepia);var old=CACHE.get(key);if(old!=null)return old;
  int[] palette=PALETTES.computeIfAbsent(sepia,OwnerGlobeTextures::palette);var image=new NativeImage(32,16,true);
  for(int x=0;x<32;x++)for(int y=0;y<16;y++)image.setPixel(x,y,palette[Math.clamp(Byte.toUnsignedInt(pixels[x*16+y]),0,palette.length-1)]);
  var id=Jugcraft.id("dynamic/supps_globe_"+next++);var texture=new DynamicTexture(()->"Owner globe",image);texture.upload();Minecraft.getInstance().getTextureManager().register(id,texture);
  CACHE.put(key,id);if(CACHE.size()>64){var iterator=CACHE.keySet().iterator();var first=iterator.next();var removed=CACHE.get(first);iterator.remove();Minecraft.getInstance().getTextureManager().release(removed);}return id;
 }
 private static int[] palette(boolean sepia){
  var id=Jugcraft.id("textures/newest/supps/entity/globes/palettes/"+(sepia?"sepia":"overworld")+".png");
  try(var input=Minecraft.getInstance().getResourceManager().open(id);var image=NativeImage.read(input)){
   int[] colors=new int[image.getWidth()*image.getHeight()];for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)colors[y*image.getWidth()+x]=image.getPixel(x,y);return colors;
  }catch(java.io.IOException e){throw new IllegalStateException("Missing supplied globe palette "+id,e);}
 }
}
