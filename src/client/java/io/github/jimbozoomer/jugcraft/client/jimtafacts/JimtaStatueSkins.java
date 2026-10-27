package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.*;

/** Bounded render-thread stone skin cache. Never fetches a URL or changes the player's actual skin texture. */
final class JimtaStatueSkins {
    record Appearance(Identifier texture,String model){}
    private static final Appearance FALLBACK=new Appearance(Jugcraft.id("textures/jimtafacts/statue/sbeve.png"),"sbeve");
    private static final Map<Identifier,Appearance> CACHE=new HashMap<>();
    private static final int MAX_SKINS=256;
    static void register(){ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Jugcraft.id("jimta_statue_skins"),(ResourceManagerReloadListener)r->{
        var manager=Minecraft.getInstance().getTextureManager();CACHE.values().forEach(a->manager.release(a.texture));CACHE.clear();
    });}
    static Appearance resolve(PlayerSkin skin){
        var source=skin.body().texturePath();var previous=CACHE.get(source);if(previous!=null)return previous;
        if(CACHE.size()>=MAX_SKINS)return FALLBACK;
        var minecraft=Minecraft.getInstance();NativeImage image=null;
        try {
            var texture=minecraft.getTextureManager().getTexture(source);
            if(texture instanceof DynamicTexture dynamic&&dynamic.getPixels()!=null)image=dynamic.getPixels().mappedCopy(i->i);
            else {var resource=minecraft.getResourceManager().getResource(source);if(resource.isEmpty())return FALLBACK;try(var input=resource.get().open()){image=NativeImage.read(input);}}
            if(image.getWidth()!=64||(image.getHeight()!=32&&image.getHeight()!=64))return FALLBACK;
            // The source uses luminance, a cool-grey/warm-grey stone palette and a
            // stone overlay. Native bounded per-pixel conversion avoids the source's
            // unbounded color-clustering pass while keeping alpha and UVs untouched.
            int low=255,high=0;
            for(int color:image.getPixels())if((color>>>24)!=0){int l=luma(color);low=Math.min(low,l);high=Math.max(high,l);}
            for(int x=0;x<image.getWidth();x++)for(int y=0;y<image.getHeight();y++){
                int color=image.getPixel(x,y),alpha=color>>>24;if(alpha==0)continue;
                float t=high<=low?.5F:(float)(luma(color)-low)/(high-low);
                int r=Math.round(99+t*89),g=Math.round(99+t*89),b=Math.round(101+t*68);
                image.setPixel(x,y,(alpha<<24)|(r<<16)|(g<<8)|b);
            }
            var id=Jugcraft.id("dynamic/jimta_statue/"+UUID.nameUUIDFromBytes(source.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            var result=new Appearance(id,image.getHeight()==32?"player_legacy":skin.model()==PlayerModelType.SLIM?"player_slim":"player");
            minecraft.getTextureManager().register(id,new DynamicTexture(()->"Jugcraft stone statue",image));image=null;CACHE.put(source,result);return result;
        }catch(java.io.IOException failure){return FALLBACK;}finally{if(image!=null)image.close();}
    }
    private static int luma(int c){return (((c>>16)&255)*299+((c>>8)&255)*587+(c&255)*114)/1000;}
}
