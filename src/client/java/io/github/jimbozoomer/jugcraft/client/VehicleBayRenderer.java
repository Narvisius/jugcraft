package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.vehiclebay.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import java.util.*;

/** Bounded carriage travel and shoulder motion, attached to real rails/posts; smooth start/stop without random jitter. */
public class VehicleBayRenderer implements BlockEntityRenderer<VehicleBayBlockEntity, VehicleBayRenderer.State> {
    private final Map<Boolean, QuadModel[]> models = new HashMap<>();
    private final WeakHashMap<VehicleBayBlockEntity,double[]> phases = new WeakHashMap<>();
    public static class State extends BlockEntityRenderState {
        QuadModel[] models; float facing, slide, shoulder; int height;
    }
    public VehicleBayRenderer(BlockEntityRendererProvider.Context context) { }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(VehicleBayBlockEntity bay, State out, float partial, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderState.extractBase(bay,out,overlay); out.models=models(bay.tank()); out.height=bay.tank()?3:6;
        out.lightCoords=net.minecraft.util.LightCoordsUtil.getLightCoords(bay.getLevel(),bay.getBlockPos().above(out.height));
        Direction facing=bay.getBlockState().getValue(MachineBlock.FACING);
        out.facing=switch(facing){case EAST->90;case SOUTH->180;case WEST->270;default->0;};
        double now=bay.getLevel().getGameTime()+partial;
        double[] phase=phases.computeIfAbsent(bay,k->new double[]{now,0,0});
        double dt=Math.clamp(now-phase[0],0,2);phase[0]=now;
        double target=bay.getBlockState().getValue(MachineBlock.LIT)?1:0;
        phase[2]+=Math.clamp(target-phase[2],-dt/12,dt/12);phase[1]+=dt*phase[2];
        out.slide=(float)Math.sin(phase[1]*.035)*1.4F;
        out.shoulder=(float)Math.sin(phase[1]*.06)*8;
    }
    private QuadModel[] models(boolean tank) {
        return models.computeIfAbsent(tank,key->{
            try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(Jugcraft.id("vehicle_bay_quads.json"))){
                var data=JsonParser.parseReader(reader).getAsJsonObject();
                String prefix=key?"tank_workbench":"mecha_workbench";
                return new QuadModel[]{QuadModel.parse(data.getAsJsonArray(prefix+"_carriage")),QuadModel.parse(data.getAsJsonArray(prefix+"_arm"))};
            }catch(Exception e){throw new IllegalStateException("Vehicle bay animation assets",e);}
        });
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if(state.models==null)return;
        pose.pushPose();pose.translate(.5,0,.5);pose.rotateDegrees(Axis.YP,-state.facing);pose.translate(-.5,0,-.5);
        pose.pushPose();pose.translate(0,0,state.slide);state.models[0].submit(pose,collector,state.lightCoords);pose.popPose();
        pose.translate(-4.5,state.height-1,5.5);pose.rotateDegrees(Axis.YP,state.shoulder);pose.translate(4.5,1-state.height,-5.5);
        state.models[1].submit(pose,collector,state.lightCoords);pose.popPose();
    }
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public int getViewDistance(){return 96;}
}
