package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** The original ferrouslime is orbiting heads inside gel, not a solitary head mesh. */
final class RecoveredSlimeVisual {
    static void submit(RecoveredWildlifeModel model,RecoveredWildlifeRenderer.State s,PoseStack pose,SubmitNodeCollector collector){
        pose.pushPose();pose.translate(0,.2+s.sourceHeight*.5,0);
        float ageScale=s.isBaby?.5F:1, size=s.slimeSize*ageScale,orbit=size*.1F;
        for(int i=1;i<=s.slimeHeads;i++){
            var random=new java.util.Random(s.visualSeed+i*31337L);
            double x=i==1?0:(random.nextFloat()-.5)*size*.5,y=i==1?0:(random.nextFloat()-.5)*size*.5,z=i==1?0:(random.nextFloat()-.5)*size*.5;
            pose.pushPose();pose.translate(x+orbit*Math.sin(i+s.ageInTicks*.05),y+orbit*Math.sin(2+i+s.ageInTicks*.1),z+orbit*Math.cos(i+s.ageInTicks*.035));
            pose.rotateDegrees(Axis.YP,180-s.sourceYaw);pose.rotateDegrees(Axis.XP,180);
            pose.scale(ageScale,ageScale,ageScale);
            collector.order(0).submitModel(model,s,pose,RenderTypes.entityCutout(Jugcraft.id("textures/menagerie/cavern/entity/ferrouslime.png")),s.lightCoords,OverlayTexture.NO_OVERLAY,-1,null,s.outlineColor);
            collector.order(1).submitModel(model,s,pose,RenderTypes.entityTranslucentEmissive(Jugcraft.id("textures/menagerie/cavern/entity/ferrouslime_eyes.png")),15728880,OverlayTexture.NO_OVERLAY,-1,null,s.outlineColor);
            pose.popPose();
        }
        float gel=size-.2F;
        var layer=RenderTypes.entityTranslucent(Jugcraft.id("textures/menagerie/cavern/entity/ferrouslime_gel.png"));
        collector.submitCustomGeometry(pose,layer,(matrix,buffer)->{
            float a=-gel*.5F,b=gel*.5F;
            face(matrix,buffer,s.lightCoords,a,b,a,b,b,b,b,b,gel,s.ageInTicks);
            face(matrix,buffer,s.lightCoords,a,b,b,a,a,a,a,a,gel,s.ageInTicks);
            face(matrix,buffer,s.lightCoords,b,b,b,a,a,b,b,a,gel,s.ageInTicks);
            face(matrix,buffer,s.lightCoords,a,a,a,b,a,b,b,a,gel,s.ageInTicks);
            face(matrix,buffer,s.lightCoords,a,b,a,a,a,a,b,b,gel,s.ageInTicks);
            face(matrix,buffer,s.lightCoords,a,b,b,b,b,b,a,a,gel,s.ageInTicks);
        });
        float length=(gel+1)*s.combat;
        if(length>0)for(int i=0;i<8;i++){
            pose.pushPose();pose.rotateDegrees(Axis.YP,i*45);
            float bend=20*(float)Math.sin(i*2.3F+s.ageInTicks*.4F);
            pose.rotateDegrees(Axis.ZP,bend);pose.rotateDegrees(Axis.XP,bend);
            collector.submitCustomGeometry(pose,layer,(matrix,buffer)->{
                float width=gel*.25F;
                vertex(matrix,buffer,s.lightCoords,0,0,width+length,0,0,s.ageInTicks);
                vertex(matrix,buffer,s.lightCoords,0,width,width,width,length,s.ageInTicks);
                vertex(matrix,buffer,s.lightCoords,0,-width,width,0,length,s.ageInTicks);
                // Native entity pipelines submit quads; a repeated corner retains the source triangle.
                vertex(matrix,buffer,s.lightCoords,0,-width,width,0,length,s.ageInTicks);
            });
            pose.popPose();
        }
        pose.popPose();
    }
    private static void face(PoseStack.Pose matrix,com.mojang.blaze3d.vertex.VertexConsumer out,int light,
                             float f1,float f2,float f3,float f4,float f5,float f6,float f7,float f8,float uv,float age){
        vertex(matrix,out,light,f1,f3,f5,0,uv,age);vertex(matrix,out,light,f2,f3,f6,uv,uv,age);
        vertex(matrix,out,light,f2,f4,f7,uv,0,age);vertex(matrix,out,light,f1,f4,f8,0,0,age);
    }
    private static void vertex(PoseStack.Pose matrix,com.mojang.blaze3d.vertex.VertexConsumer out,int light,float x,float y,float z,float u,float v,float age){
        // Supplied gel vertex shader: 0.12-block waves at GameTime * 3000.
        float phase=age*.125F;
        x+=(float)Math.cos(x+phase)*.12F;y+=Math.abs((float)Math.sin(y+phase+1.4F))*.12F;z-=(float)Math.sin(z+phase)*.12F;
        out.addVertex(matrix,x,y,z).setColor(-1).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix,0,-1,0);
    }
    private RecoveredSlimeVisual(){}
}
