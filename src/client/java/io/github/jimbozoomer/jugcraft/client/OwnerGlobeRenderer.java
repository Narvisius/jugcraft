package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerSupplementData;
import java.util.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Original stand-top meshes, named textures, tilt and spin. */
final class OwnerGlobeRenderer {
    record Appearance(String shape,Identifier texture) {}
    private final Map<String,ModelPart> meshes=new HashMap<>();
    private static final Set<String> TEXTURES=new HashSet<>();
    static {for(var name:OwnerBackpacks.source("newest_globes.json").getAsJsonArray("textures"))TEXTURES.add(name.getAsString());}
    OwnerGlobeRenderer(){
        var basic=new MeshDefinition();basic.getRoot().addOrReplaceChild("globe",CubeListBuilder.create().texOffs(0,0).addBox(-4,-28,-4,8,8,8),PartPose.offset(0,24,0));
        meshes.put("globe",LayerDefinition.create(basic,32,16).bakeRoot().getChild("globe"));
        var special=new MeshDefinition();var root=special.getRoot();
        root.addOrReplaceChild("flat",CubeListBuilder.create().texOffs(0,0).addBox(-4,-28,-4,8,4,8).texOffs(0,13).addBox(-4,-24,-4,8,2,8).texOffs(4,23).addBox(-3,-22,-3,6,1,6).texOffs(8,24).addBox(-2,-21,-2,4,1,4),PartPose.offset(0,24,0));
        root.addOrReplaceChild("sheared",CubeListBuilder.create().texOffs(0,0).addBox(-4,-28,-4,8,8,4).texOffs(0,12).addBox(0,-28,0,4,8,4),PartPose.offset(0,24,0));
        var snow=root.addOrReplaceChild("snow",CubeListBuilder.create().texOffs(0,0).addBox(-4,-28,-4,8,5,8).texOffs(0,14).addBox(-4,-23,-4,8,1,8).texOffs(4,16).addBox(-3,-22,-3,6,1,6).texOffs(0,17).addBox(-2,-24,-2,4,1,4).texOffs(0,28).addBox(-1,-25.975F,-1,2,2,2).texOffs(12,20).addBox(-1,-21,-1,2,1,2),PartPose.offset(0,24,0));
        snow.addOrReplaceChild("roof_l",CubeListBuilder.create().texOffs(11,27).addBox(0,-1,-1,2,1,2),PartPose.offsetAndRotation(0,-25.9F,0,0,0,.7854F));
        snow.addOrReplaceChild("roof_r",CubeListBuilder.create().texOffs(0,27).addBox(-2,-1,-1,3,1,2),PartPose.offsetAndRotation(0,-25.9F,0,0,0,-.7854F));
        var baked=LayerDefinition.create(special,32,32).bakeRoot();for(String name:List.of("flat","sheared","snow"))meshes.put(name,baked.getChild(name));
    }
    static Appearance appearance(OwnerSupplementData data){
        String name=data.hasCustomName()?data.getCustomName().getString().toLowerCase(Locale.ROOT).trim().replace(' ','_'):"the_world";
        name=switch(name){case "earth","terra","gaia","gaea","tierra","tellus","terre"->"the_world";case "flat_earth"->"flat";case "luna","selene","cynthia"->"moon";case "sol","helios"->"sun";default->name;};
        if(data.globeSheared)name="sheared";
        boolean round=Set.of("round","sphere","spherical").contains(name);
        if((!data.hasCustomName()||round)&&!data.globeSheared)return new Appearance(round?"round":"globe",OwnerGlobeTextures.texture(data.globePixels,data.block().name.equals("globe_sepia")));
        if(!TEXTURES.contains(name))name="the_world";
        String shape=name.equals("wais")?"snow":name.startsWith("flat")?"flat":name.startsWith("sheared")?"sheared":"globe";
        if(data.block().name.equals("globe_sepia")&&TEXTURES.contains(name+"_sepia"))name+="_sepia";
        return new Appearance(shape,Jugcraft.id("textures/newest/supps/entity/globes/globe_"+name+".png"));
    }
    void draw(OwnerSupplementRenderer.State s,PoseStack pose,SubmitNodeCollector out){
        if(s.globe==null)return;
        pose.pushPose();pose.translate(.5,.5625,.5);pose.rotateDegrees(Axis.YP,s.yaw);pose.rotateDegrees(Axis.XP,22);pose.rotateDegrees(Axis.YP,90-s.globeRotation);pose.rotateDegrees(Axis.XP,180);
        if(s.globe.shape().equals("round"))OwnerRoundGlobe.draw(s.globe.texture(),s.lightCoords,pose,out);
        else out.submitModelPart(meshes.get(s.globe.shape()),pose,RenderTypes.entityCutout(s.globe.texture()),s.lightCoords,OverlayTexture.NO_OVERLAY,null);pose.popPose();
    }
}
