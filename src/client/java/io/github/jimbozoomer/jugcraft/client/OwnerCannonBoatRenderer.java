package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerCannonBoat;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.model.object.boat.RaftModel;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
/** Vanilla hull and rowing with the supplied cannon mesh at the bow. */
final class OwnerCannonBoatRenderer extends AbstractBoatRenderer {
 private final EntityModel<BoatRenderState> hull;private final OwnerCannonModel cannon=new OwnerCannonModel();
 OwnerCannonBoatRenderer(EntityRendererProvider.Context c,String wood){super(c,Identifier.withDefaultNamespace("textures/entity/boat/"+wood+".png"));hull=wood.equals("bamboo")?new RaftModel(c.bakeLayer(ModelLayers.BAMBOO_RAFT)):new BoatModel(c.bakeLayer(ModelLayers.OAK_BOAT));}
 @Override protected EntityModel<BoatRenderState> model(){return hull;}
 static final class State extends BoatRenderState {final OwnerSupplementRenderer.State cannon=new OwnerSupplementRenderer.State();}
 @Override public State createRenderState(){return new State();}
 @Override public void extractRenderState(AbstractBoat b,BoatRenderState state,float partial){super.extractRenderState(b,state,partial);if(b instanceof OwnerCannonBoat boat&&state instanceof State s){s.cannon.cannonYaw=boat.getYRot(partial)+boat.aim(partial);s.cannon.cannonPitch=boat.pitch(partial);s.cannon.cannonLoaded=boat.loaded();s.cannon.cannonFuse=Math.max(0,boat.fuse()-partial);s.cannon.cannonCooldown=Math.max(0,boat.cooldown()-partial);s.cannon.time=boat.tickCount+partial;s.cannon.lightCoords=s.lightCoords;}}
 @Override public void submit(BoatRenderState state,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){super.submit(state,pose,out,camera);if(!(state instanceof State s))return;pose.pushPose();
  // Share the hull's damage/bubble-column pivot before placing the gun in world-facing space.
  pose.translate(0,.375,0);pose.rotateDegrees(Axis.YP,180-state.yRot);
  if(state.hurtTime>0)pose.rotateDegrees(Axis.XP,Mth.sin(state.hurtTime)*state.hurtTime*state.damageTime/10*state.hurtDir);
  if(!state.isUnderWater&&!Mth.equal(state.bubbleAngle,0))pose.rotate(new org.joml.Quaternionf().setAngleAxis(state.bubbleAngle*Mth.DEG_TO_RAD,1,0,1));
  pose.rotateDegrees(Axis.YP,state.yRot-180);
  var forward=net.minecraft.world.phys.Vec3.directionFromRotation(0,state.yRot);pose.translate(forward.x*.35-.5,.12-.375,forward.z*.35-.5);cannon.draw(s.cannon,pose,out);pose.popPose();}
}
