package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.aircraft.*;
import java.io.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** Draws the supplied Blockbench hierarchy, including moving rotors and control surfaces. */
public final class AircraftRenderer extends EntityRenderer<Aircraft,AircraftRenderer.State> {
    private record Node(String name,float[] offset,float[] rotation,JsonObject animation,QuadModel plain,QuadModel tinted,List<Node> children) {}
    private static final Map<String,List<Node>> MODELS=new HashMap<>();
    public static final class State extends EntityRenderState {
        AircraftSpec spec;float yaw,pitch,spin,bank;int paint,weapons;float forward,turn,vertical;
        float aimYaw,aimPitch,recoil;net.minecraft.world.item.ItemStack banner=net.minecraft.world.item.ItemStack.EMPTY;
    }
    public AircraftRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=1.5F;}
    public static void clear(){MODELS.clear();}
    private static float[] vec(JsonArray a){return new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()};}
    private static List<Node> nodes(JsonArray a){
        var nodes=new ArrayList<Node>();
        for(var v:a){var d=v.getAsJsonObject();var plain=new JsonArray();var tinted=new JsonArray();
            for(var q:d.getAsJsonArray("quads"))(q.getAsJsonObject().has("tinted")&&q.getAsJsonObject().get("tinted").getAsBoolean()?tinted:plain).add(q);
            nodes.add(new Node(d.get("name").getAsString(),vec(d.getAsJsonArray("offset")),vec(d.getAsJsonArray("rotation")),d.has("animation")?d.getAsJsonObject("animation"):new JsonObject(),QuadModel.parse(plain),QuadModel.parse(tinted),nodes(d.getAsJsonArray("children"))));
        }return nodes;
    }
    private static void load(){
        if(!MODELS.isEmpty())return;
        try(var r=Minecraft.getInstance().getResourceManager().openAsReader(Jugcraft.id("aircraft_models.json"))){
            for(var e:JsonParser.parseReader(r).getAsJsonObject().entrySet())MODELS.put(e.getKey(),nodes(e.getValue().getAsJsonArray()));
        }catch(IOException e){throw new IllegalStateException("Owner aircraft geometry",e);}
    }
    @Override public State createRenderState(){return new State();}
    @Override protected AABB getBoundingBoxForCulling(Aircraft a,float delta){return a.getBoundingBox().inflate(a.spec().name().equals("warship")?12:6);}
    @Override public void extractRenderState(Aircraft a,State s,float delta){
        super.extractRenderState(a,s,delta);s.spec=a.spec();s.yaw=a.getYRot(delta);s.pitch=a.getXRot(delta);
        s.spin=Mth.lerp(delta,a.previousRotation,a.rotation);s.paint=a.paint();s.weapons=a.weapons();
        s.forward=a.animatedControl(0,delta);s.turn=a.animatedControl(1,delta);s.vertical=a.animatedControl(2,delta);s.bank=a.bank(delta);
        s.aimYaw=a.aimYaw();s.aimPitch=a.aimPitch();s.recoil=a.recoil();s.banner=a.banner().copy();
    }
    /** Only the arithmetic forms present in these owner files; never execute model-provided code. */
    private static float expression(String raw,State s){
        String e=raw.replace("variable.","").replaceAll("\\s+","");if(e.isEmpty())return 0;
        int sign=1;if(e.startsWith("-")){sign=-1;e=e.substring(1);}
        String[] parts=e.split("\\*");float base=switch(parts[0]){
            case "engine_rotation"->s.spin;
            case "pressing_interpolated_x"->s.turn;
            case "pressing_interpolated_y"->s.vertical;
            case "pressing_interpolated_z"->s.forward;
            case "balloon_pitch"->s.pitch*.2F;
            case "balloon_roll"->s.turn*2;
            case "turret_cooldown"->1-s.recoil;
            case "(1-turret_cooldown)"->s.recoil;
            case "pitch","turret_pitch"->s.aimPitch;
            case "yaw","turret_yaw"->-s.aimYaw;
            case "roll"->s.bank;
            case "chest"->0;
            default->{try{yield Float.parseFloat(parts[0]);}catch(NumberFormatException ex){yield 0;}}
        };
        if(parts.length>1)base*=Float.parseFloat(parts[1]);return sign*base;
    }
    private static float[] animated(Node n,String key,State s){
        var a=n.animation().getAsJsonObject(key);float[] out={0,0,0};if(a!=null)for(int i=0;i<3;i++){String k=List.of("x","y","z").get(i);if(a.has(k))out[i]=expression(a.get(k).getAsString(),s);}return out;
    }
    private static void draw(List<Node> nodes,State s,PoseStack pose,SubmitNodeCollector collector){
        for(var n:nodes){pose.pushPose();var p=animated(n,"position",s);var r=animated(n,"rotation",s);
            pose.translate((n.offset()[0]+p[0])/16,(n.offset()[1]+p[1])/16,(n.offset()[2]+p[2])/16);
            pose.rotateDegrees(Axis.ZP,n.rotation()[2]+r[2]);pose.rotateDegrees(Axis.YP,n.rotation()[1]+r[1]);pose.rotateDegrees(Axis.XP,n.rotation()[0]+r[0]);
            if(n.name().startsWith("banner_")){
                if(s.banner.getItem() instanceof net.minecraft.world.item.BannerItem banner){
                    n.plain().submitBanner(pose,collector,s.lightCoords,net.minecraft.client.renderer.Sheets.BANNER_PATTERN_BASE,0xFF000000|banner.getColor().getTextureDiffuseColor(),0);
                    int layer=1;
                    for(var pattern:s.banner.getOrDefault(net.minecraft.core.component.DataComponents.BANNER_PATTERNS,net.minecraft.world.level.block.entity.BannerPatternLayers.EMPTY).layers()){
                        if(layer>17)break;
                        n.plain().submitBanner(pose,collector,s.lightCoords,net.minecraft.client.renderer.Sheets.getBannerSprite(pattern.pattern()),0xFF000000|pattern.color().getTextureDiffuseColor(),layer++);
                    }
                }
            }else{n.plain().submit(pose,collector,s.lightCoords);n.tinted().submit(pose,collector,s.lightCoords,s.paint);}
            draw(n.children(),s,pose,collector);pose.popPose();
        }
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        load();pose.pushPose();pose.translate(0,s.spec.groundOffset(),0);pose.rotateDegrees(Axis.YP,-s.yaw);pose.rotateDegrees(Axis.XP,s.pitch);pose.rotateDegrees(Axis.ZP,s.bank);
        draw(MODELS.getOrDefault(s.spec.name(),List.of()),s,pose,collector);
        var mounts=s.spec.definition().getAsJsonArray("weaponMounts");
        for(int i=0;i<mounts.size();i++){
            int weapon=(s.weapons>>(i*3))&7;if(weapon==0)continue;
            String model=switch(weapon){case 1->"rotary_cannon";case 2->"heavy_crossbow";case 3->"bomb_bay";default->"telescope";};
            var types=mounts.get(i).getAsJsonObject();String key=weapon==3?"DROP":weapon==1||weapon==4?"ROTATING":"FRONT";
            if(!types.has(key))continue;
            for(var v:types.getAsJsonArray(key)){var p=v.getAsJsonObject();pose.pushPose();pose.translate(p.get("x").getAsDouble(),p.get("y").getAsDouble(),p.get("z").getAsDouble());draw(MODELS.getOrDefault(model,List.of()),s,pose,collector);pose.popPose();}
        }
        pose.popPose();super.submit(s,pose,collector,camera);
    }
}
