package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.arcane.ArcaneMissile;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

final class ArcaneMissileRenderer extends EntityRenderer<ArcaneMissile,ArcaneMissileRenderer.State> {
    static final class State extends EntityRenderState {boolean energy;float yaw,pitch,age;}
    ArcaneMissileRenderer(EntityRendererProvider.Context c){super(c);}
    static void register(){net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ArcaneMissile.AUREAL,ArcaneMissileRenderer::new);net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ArcaneMissile.ENERGY,ArcaneMissileRenderer::new);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(ArcaneMissile e,State s,float partial){super.extractRenderState(e,s,partial);s.energy=e.energy();s.age=e.tickCount+partial;var v=e.getDeltaMovement();s.yaw=(float)-Math.toDegrees(Math.atan2(v.x,v.z));s.pitch=(float)Math.toDegrees(Math.atan2(v.y,v.horizontalDistance()));}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){pose.pushPose();pose.rotate(Axis.YP.rotationDegrees(s.yaw));pose.rotate(Axis.XP.rotationDegrees(s.pitch));pose.rotate(Axis.ZP.rotationDegrees(s.age*12));float half=s.energy?.4F:.2F;var texture=Jugcraft.id("textures/newest/forbidden/"+(s.energy?"effect/energy_ball.png":"entity/projectiles/aureal_missile.png"));for(int i=0;i<2;i++){collector.submitCustomGeometry(pose,RenderTypes.entityCutout(texture),(matrix,buffer)->{float[][] points={{-half,-half,0,1},{half,-half,1,1},{half,half,1,0},{-half,half,0,0}};for(var p:points)buffer.addVertex(matrix,p[0],p[1],0).setColor(-1).setUv(p[2],p[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT).setNormal(matrix,0,0,1);});pose.rotate(Axis.YP.rotationDegrees(90));}pose.popPose();super.submit(s,pose,collector,camera);}
}
