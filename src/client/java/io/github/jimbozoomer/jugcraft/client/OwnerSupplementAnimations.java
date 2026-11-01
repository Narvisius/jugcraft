package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Original declarative clock/wind-vane meshes from the supplied SUPPS renderer classes. */
final class OwnerSupplementAnimations {
    private final ModelPart hour,minute,vane,sand,stream,bellowsCenter,bellowsTop,bellowsLeather,statue,statueSlim,statueCape;
    OwnerSupplementAnimations(){
        statue=net.minecraft.client.Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER);
        statueSlim=net.minecraft.client.Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM);
        var capeMesh=new MeshDefinition();capeMesh.getRoot().addOrReplaceChild("cape",CubeListBuilder.create().texOffs(0,0).addBox(-5,0,-1,10,16,1),PartPose.ZERO);
        statueCape=LayerDefinition.create(capeMesh,64,32).bakeRoot().getChild("cape");
        var bellows=new MeshDefinition();var b=bellows.getRoot();
        b.addOrReplaceChild("center",CubeListBuilder.create().texOffs(0,0).addBox(-2,-2,-8,4,1,1).texOffs(0,2).addBox(-2,1,-8,4,1,1).texOffs(0,19).addBox(-8,-1,-8,16,2,16),PartPose.ZERO);
        b.addOrReplaceChild("top",CubeListBuilder.create().texOffs(0,0).addBox(-8,5,-8,16,3,16),PartPose.ZERO);
        b.addOrReplaceChild("leather",CubeListBuilder.create().texOffs(0,37).addBox(-7,-5,-7,14,10,14),PartPose.ZERO);
        var bellowsRoot=LayerDefinition.create(bellows,64,64).bakeRoot();bellowsCenter=bellowsRoot.getChild("center");bellowsTop=bellowsRoot.getChild("top");bellowsLeather=bellowsRoot.getChild("leather");
        var clock=new MeshDefinition();var root=clock.getRoot();
        root.addOrReplaceChild("hour",CubeListBuilder.create().texOffs(0,0).addBox(-.5F,0,0,1,5,0),PartPose.ZERO);
        root.addOrReplaceChild("minute",CubeListBuilder.create().texOffs(2,0).addBox(-.5F,0,0,1,6,0),PartPose.ZERO);
        var hands=LayerDefinition.create(clock,16,16).bakeRoot();hour=hands.getChild("hour");minute=hands.getChild("minute");
        var wind=new MeshDefinition();wind.getRoot().addOrReplaceChild("chicken",CubeListBuilder.create().texOffs(0,-11).addBox(0,-8,-5.5F,0,11,11),PartPose.ZERO);
        vane=LayerDefinition.create(wind,32,32).bakeRoot();
        var dust=new MeshDefinition();dust.getRoot().addOrReplaceChild("sand",CubeListBuilder.create().texOffs(0,0).addBox(-2,0,-2,4,4,4),PartPose.ZERO);
        sand=LayerDefinition.create(dust,16,16).bakeRoot();
        var falling=new MeshDefinition();falling.getRoot().addOrReplaceChild("stream",CubeListBuilder.create().texOffs(0,0).addBox(-.5F,0,-.5F,1,8,1),PartPose.ZERO);
        stream=LayerDefinition.create(falling,16,16).bakeRoot();
    }
    private void part(ModelPart model,Identifier tex,PoseStack p,SubmitNodeCollector out,int light){out.order(0).submitModelPart(model,p,RenderTypes.entityCutout(tex),light,OverlayTexture.NO_OVERLAY,null,-1,0);}
    void draw(OwnerSupplementRenderer.State s,PoseStack p,SubmitNodeCollector out){
        if(s.name.equals("goblet")&&s.liquidFill>0){
            out.submitCustomGeometry(p,RenderTypes.entityTranslucentCull(Jugcraft.id("textures/block/glass_tank_fluid.png")),(matrix,buffer)->{
                float[][] corners={{.375F,.375F,0,0},{.375F,.625F,0,1},{.625F,.625F,1,1},{.625F,.375F,1,0}};
                for(var v:corners)buffer.addVertex(matrix,v[0],.5F,v[1]).setColor(s.liquidColor).setUv(v[2],v[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(s.lightCoords).setNormal(matrix,0,1,0);
            });
        }else if(s.liquidFill>0){p.pushPose();p.translate(.5,.08,.5);p.scale(1.9F,s.liquidFill*2.8F,1.9F);out.order(0).submitModelPart(sand,p,RenderTypes.entityTranslucentCull(Jugcraft.id("textures/block/glass_tank_fluid.png")),s.lightCoords,OverlayTexture.NO_OVERLAY,null,s.liquidColor,0);p.popPose();}
        if(s.name.equals("statue")){
            var statue=s.statueSlim?this.statueSlim:this.statue;
            statue.getAllParts().forEach(ModelPart::resetPose);var left=statue.getChild("left_arm");var right=statue.getChild("right_arm");var leftLeg=statue.getChild("left_leg");var rightLeg=statue.getChild("right_leg");
            switch(s.statuePose){
                case "sword"->{left.xRot=right.xRot=-1.2566371F;left.yRot=.4712389F;right.yRot=-.4712389F;}
                case "tool"->{left.xRot=right.xRot=-.9424778F;left.yRot=.4712389F;right.yRot=-.4712389F;}
                case "holding"->left.xRot=right.xRot=-.7853982F;
                case "candle"->left.xRot=right.xRot=-.3926991F;
                default->{int direction=s.direction.getAxisDirection()==net.minecraft.core.Direction.AxisDirection.NEGATIVE?-1:1;leftLeg.xRot=.3926991F*direction;rightLeg.xRot=-leftLeg.xRot;left.xRot=-leftLeg.xRot;right.xRot=leftLeg.xRot;}
            }
            if(s.waving){right.yRot=0;right.xRot=(float)Math.PI;right.zRot=-.5F-.5F*(float)Math.sin(s.time*Math.PI*2/15);}
            // 26.3 clothing layers are children of their body parts and inherit
            // their transforms. Copying a parent's pose applies it twice.
            statue.getChild("head").visible=!s.statueHeadless;
            if(!s.statueHat.isEmpty()){p.pushPose();p.translate(.5,1,.5);p.rotateDegrees(Axis.YP,s.yaw);p.scale(.5F,.5F,.5F);s.statueHat.submit(p,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);p.popPose();}
            p.pushPose();p.translate(.5,.8125,.5);p.rotateDegrees(Axis.YP,s.yaw);p.scale(-.5F,-.499F,.5F);
            part(statue,s.statueSkin==null?Jugcraft.id("textures/newest/supps/entity/statue.png"):s.statueSkin,p,out,s.lightCoords);
            if(s.statueCape!=null){statueCape.resetPose();statueCape.z=2;statueCape.yRot=(float)Math.PI;statueCape.xRot=(float)((-.04+.01*Math.cos(s.time*Math.PI*2/100))*Math.PI);part(statueCape,s.statueCape,p,out,s.lightCoords);}
            p.popPose();
        }else if(s.name.equals("bellows")){
            var texture=Jugcraft.id("textures/newest/supps/block/bellows.png");p.pushPose();p.translate(.5,.5,.5);
            if(s.direction==net.minecraft.core.Direction.UP)p.rotateDegrees(Axis.XP,90);else if(s.direction==net.minecraft.core.Direction.DOWN)p.rotateDegrees(Axis.XP,-90);else p.rotateDegrees(Axis.YP,s.yaw);
            p.scale(-1,-1,1);part(bellowsCenter,texture,p,out,s.lightCoords);
            p.pushPose();p.translate(0,-.8125-s.bellowsHeight,0);part(bellowsTop,texture,p,out,s.lightCoords);p.popPose();
            p.pushPose();p.translate(0,s.bellowsHeight,0);part(bellowsTop,texture,p,out,s.lightCoords);p.popPose();
            p.scale(1,1+3.2F*s.bellowsHeight,1);part(bellowsLeather,texture,p,out,s.lightCoords);p.popPose();
        }else if(s.name.equals("clock_block")){
            p.pushPose();p.translate(.5,.5,.5);p.rotateDegrees(Axis.YP,s.yaw);p.scale(-1,-1,1);
            for(int side=0;side<(s.twoFaces?2:1);side++){
                p.pushPose();p.rotateDegrees(Axis.YP,side*180);
                p.pushPose();p.translate(0,0,-.47916667);p.rotateDegrees(Axis.ZP,(s.day+6000)*360/12000);part(hour,Jugcraft.id("textures/newest/supps/block/clock_hand.png"),p,out,s.lightCoords);p.popPose();
                p.pushPose();p.translate(0,0,-.45833333);p.rotateDegrees(Axis.ZP,s.day*360/1000);part(minute,Jugcraft.id("textures/newest/supps/block/clock_hand.png"),p,out,s.lightCoords);p.popPose();p.popPose();
            }p.popPose();
        }else if(s.name.equals("wind_vane")){
            p.pushPose();p.translate(.5,.5,.5);p.scale(1,-1,-1);p.rotateDegrees(Axis.YP,(float)(Math.sin(s.time*(1+s.power*.4)/90)*55+Math.sin(s.time/173)*25)+(s.power==3?s.time*25:0));part(vane,Jugcraft.id("textures/newest/supps/block/wind_vane.png"),p,out,s.lightCoords);p.popPose();
        }else if(s.name.equals("hourglass")&&s.sandTexture!=null){
            p.pushPose();p.translate(.5,.5,.5);
            if(s.direction.getAxis()!=net.minecraft.core.Direction.Axis.Y)p.rotateDegrees(s.direction.getAxis()==net.minecraft.core.Direction.Axis.X?Axis.ZP:Axis.XP,90);
            float lower=s.progress,upper=1-s.progress;
            p.pushPose();p.translate(0,-.34375,0);p.scale(1,lower,1);part(sand,s.sandTexture,p,out,s.lightCoords);p.popPose();
            p.pushPose();p.translate(0,.09375,0);p.scale(1,upper,1);part(sand,s.sandTexture,p,out,s.lightCoords);p.popPose();
            if(s.direction.getAxis()==net.minecraft.core.Direction.Axis.Y&&lower>.001&&upper>.001){p.pushPose();p.translate(0,-.25,0);part(stream,s.sandTexture,p,out,s.lightCoords);p.popPose();}p.popPose();
        }
    }
}
