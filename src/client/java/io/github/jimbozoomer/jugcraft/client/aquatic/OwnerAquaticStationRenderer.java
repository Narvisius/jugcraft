package io.github.jimbozoomer.jugcraft.client.aquatic;
import java.lang.Math;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.aquatic.OwnerAquaticStations;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

/** Original supplied station geometry/UVs for both placed blocks and inventory items. */
public final class OwnerAquaticStationRenderer implements BlockEntityRenderer<OwnerAquaticStations.Storage,OwnerAquaticStationRenderer.State> {
    private final ModelPart tackle=OwnerAquaticStationMeshes.tackle().bakeRoot(),bounty=OwnerAquaticStationMeshes.bounty().bakeRoot();
    public static void register(){BlockEntityRenderers.register(OwnerAquaticStations.STORAGE,c->new OwnerAquaticStationRenderer());SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("aquatic_station"),Unbaked.CODEC);MenuScreens.register(OwnerAquaticStations.MENU,OwnerTackleStationScreen::new);}
    public static class State extends BlockEntityRenderState {boolean tackle;float openness,angle;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(OwnerAquaticStations.Storage box,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){BlockEntityRenderState.extractBase(box,s,overlay);s.tackle=box.tackle();s.angle=box.getBlockState().getValue(OwnerAquaticStations.Station.FACING).toYRot();s.openness=net.minecraft.util.Mth.lerp(partial,box.previousLid,box.lid);}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){draw(s,pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);}
    private void draw(State s,PoseStack pose,SubmitNodeCollector out,int light,int overlay,int outline){
        var model=s.tackle?tackle:bounty;model.getAllParts().forEach(ModelPart::resetPose);float open=1-(float)Math.pow(1-s.openness,3);model.getChild("lid").xRot=-open*(float)Math.PI/2;
        pose.pushPose();pose.translate(.5,.5,.5);pose.rotateDegrees(Axis.YP,-s.angle);pose.translate(-.5,-.5,-.5);
        if(s.tackle){pose.translate(.0625,1.125,.5);pose.rotateDegrees(Axis.XP,180);}
        var texture=Jugcraft.id("textures/newest/aquatic/"+(s.tackle?"block/tackle_box":"entity/chest/neptunes_bounty")+".png");
        out.order(0).submitModelPart(model,pose,RenderTypes.entityCutout(texture),light,overlay,null,-1,outline);pose.popPose();
    }
    public record Unbaked(boolean tackle) implements SpecialModelRenderer.Unbaked<State>{
        static final MapCodec<Unbaked> CODEC=Codec.BOOL.fieldOf("tackle").xmap(Unbaked::new,Unbaked::tackle);
        @Override public MapCodec<Unbaked> type(){return CODEC;}
        @Override public SpecialModelRenderer<State> bake(SpecialModelRenderer.BakingContext c){return new Held(tackle);}
    }
    private static final class Held implements SpecialModelRenderer<State>{
        private final boolean tackle;private final OwnerAquaticStationRenderer renderer=new OwnerAquaticStationRenderer();Held(boolean t){tackle=t;}
        @Override public State extractArgument(ItemStack stack){var s=new State();s.tackle=tackle;return s;}
        @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,int light,int overlay,boolean foil,int outline){renderer.draw(s,pose,out,light,overlay,outline);}
        @Override public void getExtents(Consumer<Vector3fc> out){out.accept(new Vector3f(0,0,0));out.accept(new Vector3f(1,1,1));}
    }
}
