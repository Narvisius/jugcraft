package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.*;
/** Source antique palette on a stack-local map view; ordinary copies retain their original colors and map data. */
public final class OwnerAntiqueMaps {
 private record Entry(byte[] colors,Identifier id){}
 private static final Map<MapId,Entry> CACHE=new LinkedHashMap<>();private static int next;
 public static void register(){net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->clear());net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES).registerReloadListener(Jugcraft.id("antique_maps"),(net.minecraft.server.packs.resources.ResourceManagerReloadListener)r->clear());}
 private static void clear(){for(var e:CACHE.values())Minecraft.getInstance().getTextureManager().release(e.id);CACHE.clear();}
 public static void apply(ItemStack stack,MapRenderState state){
  var mc=Minecraft.getInstance();if(mc.level==null||!stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("JugcraftAntiqueMap",false))return;
  var id=stack.get(DataComponents.MAP_ID);var data=MapItem.getSavedData(stack,mc.level);if(id==null||data==null)return;var cached=CACHE.get(id);
  if(cached==null||!Arrays.equals(cached.colors,data.colors)){
   var image=new NativeImage(128,128,true);for(int y=0;y<128;y++)for(int x=0;x<128;x++)image.setPixel(x,y,color(Byte.toUnsignedInt(data.colors[x+y*128])));
   var textureId=cached==null?Jugcraft.id("dynamic/antique_map_"+next++):cached.id;var texture=new DynamicTexture(()->"Owner antique map",image);texture.upload();mc.getTextureManager().register(textureId,texture);
   cached=new Entry(data.colors.clone(),textureId);CACHE.put(id,cached);
   if(CACHE.size()>32){var iterator=CACHE.keySet().iterator();var first=iterator.next();var removed=CACHE.get(first);iterator.remove();mc.getTextureManager().release(removed.id);}
  }state.texture=cached.id;
 }
 private static int color(int packed){
  var original=MapColor.byId(packed/4);if(original==MapColor.NONE)return 0;
  int rgb=10976850;
  if(original==MapColor.STONE||original==MapColor.DEEPSLATE)rgb=MapColor.DIRT.col;
  else if(original==MapColor.PLANT||original==MapColor.COLOR_GRAY)rgb=MapColor.COLOR_BROWN.col;
  else if(original==MapColor.DIRT||original==MapColor.SAND||original==MapColor.QUARTZ||original==MapColor.SNOW||original==MapColor.METAL||original==MapColor.WOOL)rgb=13870193;
  else if(original==MapColor.WOOD)rgb=MapColor.WOOD.col;
  else if(original==MapColor.TERRACOTTA_BLACK||original==MapColor.COLOR_BLACK)rgb=MapColor.TERRACOTTA_BLACK.col;
  else if(original==MapColor.COLOR_BROWN)rgb=MapColor.TERRACOTTA_BROWN.col;
  int brightness=switch(packed&3){case 0->180;case 1->220;case 2->255;default->135;};
  return 0xFF000000|(((rgb>>16&255)*brightness/255)<<16)|(((rgb>>8&255)*brightness/255)<<8)|((rgb&255)*brightness/255);
 }
}
