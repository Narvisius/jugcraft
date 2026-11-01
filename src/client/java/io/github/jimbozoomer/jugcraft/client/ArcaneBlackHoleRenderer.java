package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.arcane.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** Supplied black-hole cube, aura geometry and all three aura frames. */
final class ArcaneBlackHoleRenderer implements BlockEntityRenderer<ArcaneBlackHole,ArcaneBlackHoleRenderer.State> {
    private final ModelPart cube=model(false),aura=model(true);
    ArcaneBlackHoleRenderer(BlockEntityRendererProvider.Context c){}
    static void register(){BlockEntityRenderers.register(ForbiddenArcane.BLACK_HOLE,ArcaneBlackHoleRenderer::new);}
    private static ModelPart model(boolean aura){var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("part",aura?CubeListBuilder.create().texOffs(0,0).addBox(-10,0,-10,20,.1F,20):CubeListBuilder.create().texOffs(0,0).addBox(-3,-3,-3,6,6,6),PartPose.ZERO);return LayerDefinition.create(mesh,aura?20:16,aura?20:16).bakeRoot();}
    static final class State extends BlockEntityRenderState {float age;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(ArcaneBlackHole be,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){BlockEntityRenderState.extractBase(be,s,overlay);s.age=(be.getLevel()==null?0:be.getLevel().getGameTime()%3600)+partial;}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){pose.pushPose();pose.translate(.5,.5,.5);pose.rotate(Axis.YP.rotationDegrees(s.age*3));out.order(0).submitModelPart(aura,pose,RenderTypes.entityTranslucentEmissive(Jugcraft.id("textures/newest/forbidden/block/black_hole_aura_"+((int)s.age/5%3)+".png")),net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,null,-1,0);pose.rotate(new org.joml.Quaternionf().rotationAxis((float)Math.PI/3,.7071068F,0,.7071068F));out.order(1).submitModelPart(cube,pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/forbidden/block/black_hole.png")),s.lightCoords,OverlayTexture.NO_OVERLAY,null,-1,0);pose.popPose();}
}
