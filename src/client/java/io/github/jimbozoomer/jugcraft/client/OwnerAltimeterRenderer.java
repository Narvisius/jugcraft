package io.github.jimbozoomer.jugcraft.client;
import java.lang.Math;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.*;

/** Moving original altitude strip; frame remains a native baked item model. */
final class OwnerAltimeterRenderer implements SpecialModelRenderer<OwnerAltimeterRenderer.Reading> {
    private static final com.google.gson.JsonObject METADATA=io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks.source("supps_altimeters.json");
    record Reading(Identifier texture,float v0,float v1,float top){}
    static void register(){SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("supps_altimeter"),Unbaked.CODEC);}
    record Unbaked() implements SpecialModelRenderer.Unbaked<Reading>{
        static final MapCodec<Unbaked> CODEC=MapCodec.unit(new Unbaked());
        @Override public MapCodec<Unbaked> type(){return CODEC;}
        @Override public SpecialModelRenderer<Reading> bake(SpecialModelRenderer.BakingContext c){return new OwnerAltimeterRenderer();}
    }
    @Override public Reading extractArgument(ItemStack stack){
        var mc=Minecraft.getInstance();var level=mc.level;
        String name=level==null?"minecraft_overworld":level.dimension().identifier().toString().replace(':','_');
        var metadata=METADATA;
        if(!metadata.has(name))name="minecraft_overworld";
        int height=metadata.get(name).getAsInt();
        int min=level==null?-64:level.getMinY(),max=level==null?320:level.getMaxY()+1;
        double y=mc.player==null?64:mc.player.getY();
        float depth=(float)Math.clamp((y-min)/(max-min),0,1)*(height-6);
        float inverse=height-depth-5;
        return new Reading(Jugcraft.id("textures/newest/supps/item/altimeter/"+name+".png"),inverse/height,(inverse+5)/height,(inverse-1)/height);
    }
    @Override public void submit(Reading reading,PoseStack pose,SubmitNodeCollector out,int light,int overlay,boolean foil,int outline){
        if(reading==null)return;
        out.submitCustomGeometry(pose,RenderTypes.entityCutout(reading.texture()),(matrix,buffer)->{
            for(int side=0;side<2;side++){
                float z=side==0?.4686F:.5314F,normal=side==0?-1:1;
                for(int strip=0;strip<2;strip++){
                    float bottom=strip==0?.375F:.6875F,top=strip==0?.6875F:.75F;
                    float u0=strip==0?0:.25F,u1=strip==0?.25F:.5F;
                    float v0=strip==0?reading.v0():reading.top(),v1=strip==0?reading.v1():reading.v0();
                    float[][] corners={{.375F,bottom,u0,v1},{.375F,top,u0,v0},{.625F,top,u1,v0},{.625F,bottom,u1,v1}};
                    for(int i=0;i<4;i++){var v=corners[side==0?i:3-i];buffer.addVertex(matrix,v[0],v[1],z).setColor(-1).setUv(side==0?v[2]:u0+u1-v[2],v[3]).setOverlay(overlay).setLight(light).setNormal(matrix,0,0,normal);}
                }
            }
        });
    }
    @Override public void getExtents(Consumer<Vector3fc> result){result.accept(new Vector3f(.375F,.375F,.4686F));result.accept(new Vector3f(.625F,.75F,.5314F));}
}
