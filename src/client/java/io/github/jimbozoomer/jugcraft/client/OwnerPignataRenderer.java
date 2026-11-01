package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerPignata;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderTypes;
/** Original 64x64 pig-shaped party vessel and ears; damped hit swing at the attachment. */
final class OwnerPignataRenderer extends EntityRenderer<OwnerPignata,OwnerPignataRenderer.State> {
 private final ModelPart root;
 static final class State extends EntityRenderState {float yaw,swingX,swingZ,pivot;net.minecraft.world.phys.Vec3 offset=net.minecraft.world.phys.Vec3.ZERO;}
 OwnerPignataRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.4F;var mesh=new MeshDefinition();var r=mesh.getRoot();var body=r.addOrReplaceChild("body",CubeListBuilder.create().texOffs(1,1).addBox(-6,-16,-7,12,12,14).texOffs(16,35).addBox(-3,-10,-9,6,4,2).texOffs(16,27).addBox(1,-4,-6,4,4,4).texOffs(0,35).addBox(1,-4,2,4,4,4).mirror().texOffs(0,27).addBox(-5,-4,-6,4,4,4).texOffs(32,27).addBox(-5,-4,2,4,4,4),PartPose.offsetAndRotation(0,24,0,0,(float)Math.PI,0));
 body.addOrReplaceChild("left_ear",CubeListBuilder.create().texOffs(32,35).addBox(0,0,-2,0,5,4),PartPose.offsetAndRotation(6,-15,-4,0,0,-.2618F));body.addOrReplaceChild("right_ear",CubeListBuilder.create().texOffs(40,35).addBox(0,0,-2,0,5,4),PartPose.offsetAndRotation(-6,-15,-4,0,0,.2618F));root=LayerDefinition.create(mesh,64,64).bakeRoot();}
 @Override public State createRenderState(){return new State();}
 @Override public void extractRenderState(OwnerPignata e,State s,float partial){super.extractRenderState(e,s,partial);s.yaw=net.minecraft.util.Mth.rotLerp(partial,e.yRotO,e.getYRot());s.swingX=net.minecraft.util.Mth.lerp(partial,e.oldSwingX,e.swingX);s.swingZ=net.minecraft.util.Mth.lerp(partial,e.oldSwingZ,e.swingZ);s.pivot=net.minecraft.util.Mth.lerp(partial,e.oldPivotY,e.pivotY);s.offset=e.anchorOffset(partial);}
 @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){super.submit(s,pose,out,camera);pose.pushPose();pose.translate(s.offset.x,s.offset.y,s.offset.z);pose.translate(0,s.pivot,0);pose.rotate(Axis.ZP.rotation(s.swingX));pose.rotate(Axis.XP.rotation(-s.swingZ));pose.translate(0,-s.pivot,0);pose.rotateDegrees(Axis.YP,-s.yaw);pose.translate(0,1.5,0);pose.scale(-.998F,-.998F,.998F);out.submitModelPart(root,pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/supps/entity/pignata.png")),s.lightCoords,OverlayTexture.NO_OVERLAY,null);pose.popPose();}
}
