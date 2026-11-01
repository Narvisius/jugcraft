package io.github.jimbozoomer.jugcraft.client;
import java.lang.Math;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.*;
import net.minecraft.world.item.ItemStack;
import org.joml.*;

/** Supplied machine cuboids and UVs with their authored compression/turnover transforms. */
final class BotanicalMechanismModels {
    private final Map<String,ModelPart> models=new HashMap<>();private int seen=-1;private static int generation;
    static void register(){SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("botanical_mechanism"),Unbaked.CODEC);
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES).registerReloadListener(Jugcraft.id("botanical_mechanism_models"),(net.minecraft.server.packs.resources.ResourceManagerReloadListener)r->generation++);
    }
    private ModelPart model(String kind){if(seen!=generation){models.clear();seen=generation;}return models.computeIfAbsent(kind,k->{
        String name=k.equals("avatar")?"AvatarModel":k.equals("corporea_index")?"CorporeaIndexBlockEntityRenderer":k.equals("teru_teru_bozu")?"TeruTeruBozuModel":k.equals("brewery")?"BotanicalBreweryModel":k.equals("bellows")?"BellowsModel":"HourglassModel";
        try(var reader=Minecraft.getInstance().getResourceManager().getResourceOrThrow(Jugcraft.id("newest_jugtania_models/"+name+".json")).openAsReader()){return RecoveredWildlifeModel.bake(JsonParser.parseReader(reader).getAsJsonObject());}
        catch(java.io.IOException e){throw new IllegalStateException("Missing supplied botanical model "+name,e);}
    });}
    void draw(String kind,float compression,float fraction,boolean filled,boolean flip,int pulse,int color,float ticks,float yaw,PoseStack pose,SubmitNodeCollector collector,int light,int overlay,int outline){
        if(kind.equals("avatar")){avatar(pose,collector,light,overlay,outline,yaw,ticks,"");return;}
        if(kind.equals("corporea_index")){index(pose,collector,light,overlay,outline,ticks);return;}
        if(kind.equals("teru_teru_bozu")){doll(pose,collector,light,overlay,outline,ticks,filled,false);return;}
        if(kind.equals("brewery")){brewery(pose,collector,light,overlay,outline,ticks);return;}
        var root=model(kind);var type=kind.equals("hourglass")?RenderTypes.entityTranslucent(Jugcraft.id("textures/newest/jugtania/model/hourglass.png")):RenderTypes.entityCutout(Jugcraft.id("textures/newest/jugtania/model/bellows.png"));
        root.getAllParts().forEach(p->{p.resetPose();p.visible=true;});
        pose.pushPose();
        if(kind.equals("bellows")){
            pose.translate(.5,1.5,.5);pose.scale(1,-1,-1);pose.rotateDegrees(Axis.YP,yaw);
            for(String part:List.of("base","pipe"))collector.order(0).submitModelPart(root.getChild(part),pose,type,light,overlay,null,-1,outline);
            float stretch=Math.max(.1F,.9F-compression);pose.pushPose();pose.translate(0,(1-stretch)*.5,0);
            collector.order(0).submitModelPart(root.getChild("top"),pose,type,light,overlay,null,-1,outline);pose.popPose();
            pose.rotateDegrees(Axis.XP,180);pose.translate(-.19,-1.375,-.19);pose.scale(1,stretch,1);
            collector.order(0).submitModelPart(root.getChild("funnel"),pose,type,light,overlay,null,-1,outline);
        }else{
            pose.translate(.5+Math.cos(ticks*.05)*.025,.60+Math.sin(ticks*.04)*.05,.5+Math.sin(ticks*.05)*.025);
            pose.rotateDegrees(Axis.ZP,(flip?180:0)+pulse*45);pose.scale(1,-1,-1);
            for(String part:List.of("top","bottom","ring"))collector.order(0).submitModelPart(root.getChild(part),pose,type,light,overlay,null,-1,outline);
            if(filled){
                for(boolean top:new boolean[]{true,false}){
                    float height=top?1-fraction:fraction;if(height<=0)continue;
                    pose.pushPose();if(flip){pose.rotateDegrees(Axis.ZP,180);}pose.translate(-.125,top?-.3125:.3125-height*.25,-.125);pose.scale(1,height,1);
                    collector.order(0).submitModelPart(root.getChild(top?"sand_top":"sand_bottom"),pose,type,light,overlay,null,color,outline);pose.popPose();
                }
            }
            for(String part:List.of("glass_top","glass_bottom"))collector.order(1).submitModelPart(root.getChild(part),pose,type,light,overlay,null,-1,outline);
        }pose.popPose();
    }
    void avatar(PoseStack pose,SubmitNodeCollector collector,int light,int overlay,int outline,float yaw,float ticks,String rod){
        var root=model("avatar");root.getAllParts().forEach(ModelPart::resetPose);pose.pushPose();pose.translate(.5,1.6,.5);pose.scale(1,-1,-1);pose.rotateDegrees(Axis.YP,yaw);
        collector.order(0).submitModelPart(root,pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/jugtania/model/avatar.png")),light,overlay,null,-1,outline);
        if(io.github.jimbozoomer.jugcraft.botany.BotanicalAvatar.RODS.contains(rod)){
            pose.scale(1.01F,1.01F,1.01F);pose.translate(0,-.01,0);int alpha=(int)((Math.sin(ticks/20)*.5+.5)*255);
            collector.order(1).submitModelPart(root,pose,RenderTypes.entityTranslucentEmissive(Jugcraft.id("textures/newest/jugtania/model/avatar_"+rod.substring(0,rod.length()-4)+".png")),net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,overlay,null,alpha<<24|0xFFFFFF,outline);
        }pose.popPose();
    }
    void index(PoseStack pose,SubmitNodeCollector collector,int light,int overlay,int outline,float ticks){
        var root=model("corporea_index");var type=RenderTypes.entityCutout(Jugcraft.id("textures/newest/jugtania/model/corporea_index.png"));
        pose.pushPose();pose.translate(.5,.5+(Math.cos(ticks/10)*.5+.5)*.125,.5);pose.rotateDegrees(Axis.YP,ticks*2);
        for(int i=0;i<3;i++){
            pose.mulPose(new Matrix4f().rotation((float)Math.PI/3,(float)Math.sqrt(.5),0,(float)Math.sqrt(.5)));
            if(i>0){pose.scale(.875F,.875F,.875F);pose.rotateDegrees(Axis.YP,ticks*2);}
            collector.order(0).submitModelPart(root.getChild(i==2?"cube":"ring"),pose,type,light,overlay,null,-1,outline);
        }pose.popPose();
    }
    void brewery(PoseStack pose,SubmitNodeCollector collector,int light,int overlay,int outline,float ticks){
        var root=model("brewery");var type=RenderTypes.entityCutout(Jugcraft.id("textures/newest/jugtania/model/brewery.png"));
        root.getAllParts().forEach(ModelPart::resetPose);pose.pushPose();pose.translate(.5,1.5,.5);pose.scale(1,-1,-1);pose.translate(0,Math.sin(ticks/40)*.1+.05,0);
        pose.pushPose();pose.rotateDegrees(Axis.YP,-ticks*25/16);
        for(String part:List.of("top","pole","bottom"))collector.order(0).submitModelPart(root.getChild(part),pose,type,light,overlay,null,-1,outline);pose.popPose();
        for(int i=0;i<6;i++){pose.pushPose();pose.translate(0,Math.sin(ticks/20+i*40)*.2-.2,0);pose.rotateDegrees(Axis.YP,(float)Math.toDegrees(ticks/16)+i*60);collector.order(0).submitModelPart(root.getChild("plate"),pose,type,light,overlay,null,-1,outline);pose.popPose();}pose.popPose();
    }
    void doll(PoseStack pose,SubmitNodeCollector collector,int light,int overlay,int outline,float ticks,boolean raining,boolean halloween){
        var root=model("teru_teru_bozu");root.getAllParts().forEach(ModelPart::resetPose);
        var type=RenderTypes.entityCutout(Jugcraft.id("textures/newest/jugtania/model/teru_teru_bozu"+(halloween?"_halloween":"")+".png"));
        pose.pushPose();pose.rotateDegrees(Axis.XP,180);pose.translate(.5,-1.25+Math.sin(ticks*.01)*.05,-.5);pose.rotateDegrees(Axis.YP,ticks*.3F);pose.rotateDegrees(Axis.ZP,4*(float)Math.sin(ticks*.05));pose.scale(.75F,.75F,.75F);
        for(String part:List.of(raining?"sad_face":"happy_face","thread","cloth"))collector.order(0).submitModelPart(root.getChild(part),pose,type,light,overlay,null,-1,outline);pose.popPose();
    }
    record Unbaked(String kind) implements SpecialModelRenderer.Unbaked<String> {
        static final MapCodec<Unbaked> CODEC=Codec.STRING.fieldOf("kind").xmap(Unbaked::new,Unbaked::kind);
        @Override public MapCodec<Unbaked> type(){return CODEC;}
        @Override public SpecialModelRenderer<String> bake(SpecialModelRenderer.BakingContext context){return new Held(kind);}
    }
    private static final class Held implements SpecialModelRenderer<String> {
        private final String kind;private final BotanicalMechanismModels renderer=new BotanicalMechanismModels();
        Held(String kind){this.kind=kind;}
        @Override public String extractArgument(ItemStack stack){return kind;}
        @Override public void submit(String value,PoseStack pose,SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline){renderer.draw(kind,0,0,false,false,0,-1,0,180,pose,collector,light,overlay,outline);}
        @Override public void getExtents(Consumer<Vector3fc> out){out.accept(new Vector3f(0,0,0));out.accept(new Vector3f(1,1,1));}
    }
}
