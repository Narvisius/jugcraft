package local.peepo.client;

import local.peepo.*;
import io.github.jimbozoomer.jugcraft.client.DecorQuads;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class WheelRenderer implements BlockEntityRenderer<WheelBlockEntity,WheelRenderer.State> {
    public static final class State extends BlockEntityRenderState { Direction facing;float angle; }
    public WheelRenderer(BlockEntityRendererProvider.Context context){}
    public State createRenderState(){return new State();}
    public void extractRenderState(WheelBlockEntity wheel,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderState.extractBase(wheel,state,overlay);state.facing=wheel.facing();
        state.angle=wheel.angle+(wheel.getBlockState().getValue(WheelBlock.RUNNING)?partial*9:0);
    }
    public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        var base=DecorQuads.get("companion_wheel_base");var wheel=DecorQuads.get("companion_wheel_rotor");
        if(base==null || wheel==null)return;
        float yaw=switch(state.facing){case EAST->90;case SOUTH->180;case WEST->270;default->0;};
        pose.pushPose();pose.translate(.5,0,.5);pose.rotateDegrees(Axis.YP,-yaw);pose.translate(-.5,0,-.5);
        base.submit(pose,collector,state.lightCoords);
        pose.translate(1,17.725/16,.5);pose.rotateDegrees(Axis.ZP,-state.angle);pose.translate(-1,-17.725/16,-.5);
        wheel.submit(pose,collector,state.lightCoords);pose.popPose();
    }
    @Override public boolean shouldRenderOffScreen(){return true;}
}
