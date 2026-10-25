package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaShot;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

/** Recovered 8px source orb cubes and 8x8x16 animated fire shells; source snow uses vanilla snow. */
public final class JimtaShotRenderer extends EntityRenderer<JimtaShot,JimtaShotRenderer.State> {
    private final ItemModelResolver models;
    private final net.minecraft.client.model.geom.ModelPart icicle;
    public static final class State extends EntityRenderState{final ItemStackRenderState item=new ItemStackRenderState();String kind;float time,yaw,pitch;}
    public JimtaShotRenderer(EntityRendererProvider.Context c){super(c);models=c.getItemModelResolver();var mesh=new net.minecraft.client.model.geom.builders.MeshDefinition();mesh.getRoot().addOrReplaceChild("model",net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,8).addBox(-1,-1,-6,2,2,6).texOffs(0,0).addBox(-1.5F,-1.5F,0,3,3,5),net.minecraft.client.model.geom.PartPose.ZERO);icicle=net.minecraft.client.model.geom.builders.LayerDefinition.create(mesh,16,16).bakeRoot();}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(JimtaShot e,State s,float partial){super.extractRenderState(e,s,partial);s.kind=e.kind();s.time=e.tickCount+partial;var d=e.getDeltaMovement();s.yaw=(float)(Math.toDegrees(Math.atan2(d.x,d.z)));s.pitch=(float)(-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));models.updateForTopItem(s.item,e.visualItem(),ItemDisplayContext.FIXED,e.level(),null,e.getId());}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        if(s.kind.endsWith("_field"))return;pose.pushPose();pose.rotateDegrees(Axis.YP,s.yaw);pose.rotateDegrees(Axis.XP,s.pitch);
        if(!s.item.isEmpty()){if(s.kind.equals("throw"))pose.rotateDegrees(Axis.XP,s.time*30);s.item.submit(pose,collector,s.lightCoords,OverlayTexture.NO_OVERLAY,0);}
        else if(s.kind.equals("ice_shard")){pose.scale(1,-1,1);collector.order(0).submitModelPart(icicle,pose,RenderTypes.entityCutout(tex("entity/icicle_projectile")),s.lightCoords,OverlayTexture.NO_OVERLAY,null);}
        else if(s.kind.equals("snowball")||s.kind.equals("magma_bomb")){
            pose.rotateDegrees(Axis.XP,s.time*8);pose.rotateDegrees(Axis.YP,s.time*5);pose.rotateDegrees(Axis.ZP,s.time*3);
            cube(pose,collector,s.kind.equals("snowball")?Identifier.withDefaultNamespace("textures/block/snow.png"):tex("entity/fireball/magma"),8,8,8,8,8,true,s.lightCoords);
            if(s.kind.equals("magma_bomb")){pose.scale(1.15F,1.15F,1.15F);cube(pose,collector,tex("entity/fireball/swirl_"+((int)s.time%11)),8,8,8,8,8,true,0xf000f0);}
        }else if((s.kind.equals("thunder_step")||s.kind.equals("ball_lightning"))){float base=s.kind.equals("ball_lightning")?1:.2F;pose.scale(base,base,base);for(int i=0;i<3;i++){pose.pushPose();float t=s.time+i*777;pose.rotateDegrees(Axis.XP,(float)Math.cos(.065*t)*180);pose.rotateDegrees(Axis.YP,(float)Math.sin(.065*t)*180);pose.rotateDegrees(Axis.ZP,(float)Math.cos(.065*t+5464)*180);float size=2-i*.2F;pose.scale(size,size,size);cube(pose,collector,tex("entity/ball_lightning/ball_lightning_"+(((int)s.time+i*i)%5)),8,8,8,8,8,true,0xf000f0);pose.popPose();}}
        else if(s.kind.equals("echo_arrow")){pose.scale(.4F,.4F,.4F);cube(pose,collector,tex("entity/magic_arrow"),2,2,8,16,16,true,0xf000f0);}
        else{boolean comet=s.kind.equals("starfall");pose.scale(.55F,.55F,.55F);cube(pose,collector,tex(comet?"entity/comet/comet":"entity/fireball/fireball_core"),8,8,8,48,24,false,0xf000f0);pose.scale(1.15F,1.15F,1.15F);cube(pose,collector,tex(comet?"entity/comet/fire_"+(1+(int)s.time/2%4):"entity/fireball/fire_"+((int)s.time/2%4)),8,8,16,48,24,false,0xf000f0);}
        pose.popPose();super.submit(s,pose,collector,camera);
    }
    private static Identifier tex(String path){return Jugcraft.id("textures/jimtafacts/spellbooks/"+path+".png");}
    private static void cube(PoseStack pose,SubmitNodeCollector collector,Identifier texture,float dx,float dy,float dz,float tw,float th,boolean repeat,int light){
        float x=dx/32,y=dy/32,z=dz/32;
        float[][][] faces={{{-x,-y,-z},{-x,-y,z},{-x,y,z},{-x,y,-z}},{{-x,-y,-z},{x,-y,-z},{x,y,-z},{-x,y,-z}},{{x,-y,z},{x,-y,-z},{x,y,-z},{x,y,z}},{{x,-y,z},{-x,-y,z},{-x,y,z},{x,y,z}},{{-x,y,-z},{x,y,-z},{x,y,z},{-x,y,z}},{{-x,-y,z},{x,-y,z},{x,-y,-z},{-x,-y,-z}}};
        float[][] rectangles={{0,dz,dz,dz+dy},{dz,dz,dz+dx,dz+dy},{dz+dx,dz,dz+dx+dz,dz+dy},{dz+dx+dz,dz,2*dz+2*dx,dz+dy},{dz,0,dz+dx,dz},{dz+dx,dz,dz+2*dx,0}};
        collector.submitCustomGeometry(pose,RenderTypes.entityCutout(texture),(matrix,buffer)->{for(int f=0;f<6;f++){float[] r=repeat?new float[]{0,0,tw,th}:rectangles[f];float[][] uv={{r[0]/tw,r[3]/th},{r[2]/tw,r[3]/th},{r[2]/tw,r[1]/th},{r[0]/tw,r[1]/th}};for(int v=0;v<4;v++)buffer.addVertex(matrix,faces[f][v][0],faces[f][v][1],faces[f][v][2]).setColor(-1).setUv(uv[v][0],uv[v][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix,0,1,0);}});
    }
}
