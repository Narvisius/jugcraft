package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/** Source-sized 24-by-16 cloth; bounded strips wave together with all banner layers. */
final class OwnerFlagRenderer {
    private OwnerFlagRenderer(){}
    static void draw(OwnerSupplementRenderer.State s,PoseStack pose,SubmitNodeCollector out){
        if(s.flagPatterns==null)return;
        pose.pushPose();pose.translate(.5,0,.5);pose.rotateDegrees(Axis.YP,s.yaw);pose.translate(0,0,.0625);
        layer(s,pose,out,Identifier.withDefaultNamespace("textures/entity/banner/base.png"),DyeColor.byName(s.name.substring(5),DyeColor.WHITE),0);
        int index=1;
        for(var pattern:s.flagPatterns.layers()){
            var id=pattern.pattern().value().assetId();layer(s,pose,out,Identifier.fromNamespaceAndPath(id.getNamespace(),"textures/entity/banner/"+id.getPath()+".png"),pattern.color(),index++);
            if(index>16)break;
        }
        pose.popPose();
    }
    private static void layer(OwnerSupplementRenderer.State s,PoseStack pose,SubmitNodeCollector out,Identifier texture,DyeColor color,int index){
        int tint=0xFF000000|color.getTextureDiffuseColor();float time=s.time;
        out.submitCustomGeometry(pose,RenderTypes.entityCutout(texture),(matrix,buffer)->{
            // Native banner cloth region, rotated into the supplied horizontal flag layout.
            for(int i=0;i<24;i++){
                float a=i/24F,b=(i+1)/24F;
                float xa=(float)Math.sin(i*.32-time*.08)*.10F*a,xb=(float)Math.sin((i+1)*.32-time*.08)*.10F*b;
                for(int side=0;side<2;side++){
                    float offset=(.03125F+index*.0003F)*(side==0?1:-1);
                    float[][] vertices={{xa+offset,0,a*1.5F,1/64F,(1+a*40)/64F},{xa+offset,1,a*1.5F,21/64F,(1+a*40)/64F},{xb+offset,1,b*1.5F,21/64F,(1+b*40)/64F},{xb+offset,0,b*1.5F,1/64F,(1+b*40)/64F}};
                    for(int j=0;j<4;j++){var v=vertices[side==0?j:3-j];buffer.addVertex(matrix,v[0],v[1],v[2]).setColor(tint).setUv(v[3],v[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(s.lightCoords).setNormal(matrix,side==0?1:-1,0,0);}
                }
            }
        });
    }
}
