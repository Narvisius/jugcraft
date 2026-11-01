package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
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

final class ArcaneJarRenderer implements BlockEntityRenderer<ArcaneJar,ArcaneJarRenderer.State> {
    private final ModelPart root=ArcaneJarSoulModel.createBodyLayer().bakeRoot();
    private final KeyframeAnimation animation=ArcaneJarSoulModel.ROTATE.bake(root);
    ArcaneJarRenderer(BlockEntityRendererProvider.Context ignored){}
    static void register(){BlockEntityRenderers.register(ForbiddenArcane.JAR,ArcaneJarRenderer::new);net.minecraft.client.renderer.special.SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("arcane_jar_souls"),Unbaked.CODEC);}
    static final class State extends BlockEntityRenderState {boolean souls;long time;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(ArcaneJar be,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){BlockEntityRenderState.extractBase(be,s,overlay);var state=be.getBlockState();s.souls=state.getValue(ArcaneJar.JarBlock.KIND)==1&&state.getValue(ArcaneJar.JarBlock.FILL)>0;s.time=(long)(((be.getLevel()==null?0:be.getLevel().getGameTime())%192+partial)*50);}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){if(!s.souls)return;root.getAllParts().forEach(ModelPart::resetPose);animation.apply(s.time,1);pose.pushPose();pose.translate(.5,1.5,.5);pose.rotate(Axis.ZP.rotationDegrees(180));collector.order(0).submitModelPart(root,pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/forbidden/entity/lost_soul/lost_soul.png")),s.lightCoords,OverlayTexture.NO_OVERLAY,null,-1,0);pose.popPose();}
    record Unbaked() implements net.minecraft.client.renderer.special.SpecialModelRenderer.Unbaked<Long> {
        static final MapCodec<Unbaked> CODEC=MapCodec.unit(new Unbaked());
        public MapCodec<Unbaked> type(){return CODEC;}
        public net.minecraft.client.renderer.special.SpecialModelRenderer<Long> bake(net.minecraft.client.renderer.special.SpecialModelRenderer.BakingContext c){return new Held();}
    }
    private static final class Held implements net.minecraft.client.renderer.special.SpecialModelRenderer<Long> {
        private final ModelPart root=ArcaneJarSoulModel.createBodyLayer().bakeRoot();
        private final KeyframeAnimation animation=ArcaneJarSoulModel.ROTATE.bake(root);
        public Long extractArgument(net.minecraft.world.item.ItemStack stack){return net.minecraft.util.Util.getMillis()%9600;}
        public void submit(Long time,PoseStack pose,SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline){root.getAllParts().forEach(ModelPart::resetPose);animation.apply(time==null?0:time,1);pose.pushPose();pose.translate(.5,1.5,.5);pose.rotate(Axis.ZP.rotationDegrees(180));collector.order(0).submitModelPart(root,pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/forbidden/entity/lost_soul/lost_soul.png")),light,overlay,null,-1,outline);pose.popPose();}
        public void getExtents(java.util.function.Consumer<org.joml.Vector3fc> out){out.accept(new org.joml.Vector3f(.2F,0,.2F));out.accept(new org.joml.Vector3f(.8F,.85F,.8F));}
    }
}
