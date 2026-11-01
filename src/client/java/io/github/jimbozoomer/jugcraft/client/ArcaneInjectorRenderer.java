package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.arcane.*;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

final class ArcaneInjectorRenderer implements BlockEntityRenderer<ArcaneInjector,ArcaneInjectorRenderer.State> {
    private final ModelPart root=ArcaneInjectorModel.createBodyLayer().bakeRoot();
    private final KeyframeAnimation transform=ArcaneInjectorModel.TRANSFORM.bake(root),rotate=ArcaneInjectorModel.ROTATE.bake(root);
    ArcaneInjectorRenderer(BlockEntityRendererProvider.Context c){}
    static void register(){BlockEntityRenderers.register(ForbiddenArcane.INJECTOR,ArcaneInjectorRenderer::new);ArcaneLostSoulRenderer.register();ArcaneDarkTraderRenderer.register();ArcaneBlackHoleRenderer.register();ArcaneArrowRenderer.register();}
    static final class State extends BlockEntityRenderState {boolean enabled;long elapsed;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(ArcaneInjector be,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){BlockEntityRenderState.extractBase(be,s,overlay);s.enabled=be.getBlockState().getValue(ArcaneInjector.InjectorBlock.ENABLED);s.elapsed=(long)(Math.max(0,(be.getLevel()==null?0:be.getLevel().getGameTime())-be.activated+partial)*50);}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){if(!s.enabled)return;root.getAllParts().forEach(ModelPart::resetPose);if(s.elapsed<3000)transform.apply(s.elapsed,1);else rotate.apply((s.elapsed-3000)%12000,1);pose.pushPose();pose.translate(.5,1.5,.5);pose.rotate(Axis.ZP.rotationDegrees(180));out.order(0).submitModelPart(root,pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/forbidden/entity/quantum_injector.png")),s.lightCoords,OverlayTexture.NO_OVERLAY,null,-1,0);out.order(1).submitModelPart(root,pose,RenderTypes.entityTranslucentEmissive(Jugcraft.id("textures/newest/forbidden/entity/quantum_injector_layer.png")),net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,null,-1,0);pose.popPose();}
}
