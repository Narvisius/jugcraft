package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.wilds.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.*;
final class WildGongRenderer implements BlockEntityRenderer<WildGong,WildGongRenderer.State> {
    private final ModelPart model=RecoveredWildlifeModel.bake(RecoveredWildlifeModel.read("recovered/wilds_gong")),chain=model.getChild("root").getChild("chain");
    WildGongRenderer(BlockEntityRendererProvider.Context c){}
    static void register(){BlockEntityRenderers.register(OwnerWilds.GONG,WildGongRenderer::new);}
    static final class State extends BlockEntityRenderState {float elapsed;Direction facing,hit;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(WildGong be,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){BlockEntityRenderState.extractBase(be,s,overlay);s.elapsed=(be.getLevel()==null?1000:be.getLevel().getGameTime()-be.struck)+partial;s.facing=be.getBlockState().getValue(WildGong.FACING);s.hit=be.strikeDirection;}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){model.getAllParts().forEach(ModelPart::resetPose);if(s.elapsed>=0&&s.elapsed<148)chain.xRot=(float)(Math.sin(s.elapsed/Math.PI)/(4+s.elapsed/2))*(s.hit==Direction.NORTH||s.hit==Direction.EAST?1:-1);pose.pushPose();pose.translate(.5,1.485,.5);pose.rotate(Axis.ZP.rotationDegrees(180));if(s.facing.getAxis()==Direction.Axis.X)pose.rotate(Axis.YP.rotationDegrees(90));out.order(0).submitModelPart(model,pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/wilds/block/gong.png")),s.lightCoords,OverlayTexture.NO_OVERLAY,null,-1,0);pose.popPose();}
    @Override public boolean shouldRenderOffScreen(){return true;}
}
