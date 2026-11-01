package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.arcane.*;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
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

/** Recovered source skull cube/overlay geometry and original 32x16 / 64x16 textures. */
final class ArcaneSkullRenderer implements BlockEntityRenderer<ArcaneSkulls.Head,ArcaneSkullRenderer.State> {
    private final ModelPart ordinary=model(false),detailed=model(true);
    ArcaneSkullRenderer(BlockEntityRendererProvider.Context c){}
    static void register(){BlockEntityRenderers.register(ForbiddenArcane.SKULL,ArcaneSkullRenderer::new);SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("arcane_skull"),Unbaked.CODEC);}
    static ModelPart model(boolean detailed){var mesh=new MeshDefinition();var head=mesh.getRoot().addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4,8,8,8),PartPose.ZERO);if(detailed)head.addOrReplaceChild("layer",CubeListBuilder.create().texOffs(32,0).addBox(-4,-8,-4,8,8,8,new CubeDeformation(.25F)),PartPose.ZERO);return LayerDefinition.create(mesh,detailed?64:32,16).bakeRoot();}
    static boolean detailed(String kind){return kind.startsWith("aurealic")||kind.startsWith("eternal");}
    static void draw(ModelPart part,String kind,PoseStack pose,SubmitNodeCollector out,int light,int overlay,int outline){pose.pushPose();pose.scale(-1,-1,1);out.order(0).submitModelPart(part,pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/forbidden/block/obsidian_skull/"+kind+"_skull.png")),light,overlay,null,-1,outline);pose.popPose();}
    static final class State extends BlockEntityRenderState {String kind="obsidian";float yaw;double x=.5,y=0,z=.5;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(ArcaneSkulls.Head be,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){BlockEntityRenderState.extractBase(be,s,overlay);s.x=s.z=.5;s.y=0;var state=be.getBlockState();if(state.getBlock() instanceof ArcaneSkulls.Skull skull){s.kind=skull.kind;if(skull.wall){var d=state.getValue(ArcaneSkulls.Skull.FACING);s.x-=d.getStepX()*.25;s.z-=d.getStepZ()*.25;s.y=.25;s.yaw=d.toYRot();}else s.yaw=state.getValue(ArcaneSkulls.Skull.ROTATION)*22.5F;}}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){pose.pushPose();pose.translate(s.x,s.y,s.z);pose.rotate(Axis.YP.rotationDegrees(-s.yaw));draw(detailed(s.kind)?detailed:ordinary,s.kind,pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
    record Unbaked(String kind) implements SpecialModelRenderer.Unbaked<Boolean> {
        static final MapCodec<Unbaked> CODEC=Codec.STRING.fieldOf("kind").xmap(Unbaked::new,Unbaked::kind);
        public MapCodec<Unbaked> type(){return CODEC;}
        public SpecialModelRenderer<Boolean> bake(SpecialModelRenderer.BakingContext c){return new Held(kind);}
    }
    private static final class Held implements SpecialModelRenderer<Boolean> {
        private final String kind;private final ModelPart model;
        Held(String kind){this.kind=kind;model=model(detailed(kind));}
        public Boolean extractArgument(ItemStack stack){return true;}
        public void submit(Boolean data,PoseStack pose,SubmitNodeCollector out,int light,int overlay,boolean foil,int outline){pose.pushPose();pose.translate(.5,0,.5);draw(model,kind,pose,out,light,overlay,outline);pose.popPose();}
        public void getExtents(Consumer<Vector3fc> out){out.accept(new Vector3f(.23F,0,.23F));out.accept(new Vector3f(.77F,.52F,.77F));}
    }
}
