package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.PersonalWorkshops;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Six recovered ModelPersonalChest boxes, mirrored source UVs and cubic eased hinged door. */
public final class PersonalSafeRenderer implements BlockEntityRenderer<PersonalWorkshops.Node,PersonalSafeRenderer.State> {
    private final ModelPart model;
    public PersonalSafeRenderer(BlockEntityRendererProvider.Context c){model=layer().bakeRoot();}
    public static void register(){BlockEntityRenderers.register(PersonalWorkshops.TYPE,PersonalSafeRenderer::new);}
    public static final class State extends BlockEntityRenderState {boolean safe;Direction facing;float door;}
    public State createRenderState(){return new State();}
    public void extractRenderState(PersonalWorkshops.Node n,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){BlockEntityRenderState.extractBase(n,s,overlay);s.safe=n.safe();s.facing=n.getBlockState().getValue(PersonalWorkshops.FACING);float a=n.previousDoor+(n.door-n.previousDoor)*partial;s.door=(1-(1-a)*(1-a)*(1-a))*(float)Math.PI/2;}
    public void submit(State s,PoseStack pose,SubmitNodeCollector collector,net.minecraft.client.renderer.state.level.CameraRenderState camera){if(!s.safe)return;pose.pushPose();pose.translate(0,1,1);pose.scale(1,-1,-1);pose.translate(.5,.5,.5);
        pose.rotateDegrees(Axis.YP,s.facing==Direction.NORTH?180:s.facing==Direction.WEST?90:s.facing==Direction.EAST?-90:0);pose.translate(-.5,-.5,-.5);model.getChild("door").yRot=s.door;
        collector.order(0).submitModelPart(model,pose,RenderTypes.entityCutout(Jugcraft.id("textures/block/jimtafacts/personal/newsafe.png")),s.lightCoords,OverlayTexture.NO_OVERLAY,null);pose.popPose();}
    public static LayerDefinition layer(){var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("right",CubeListBuilder.create().texOffs(0,0).mirror().addBox(0,0,0,14,16,1),PartPose.offsetAndRotation(1,0,15,0,1.570796F,0));
        root.addOrReplaceChild("left",CubeListBuilder.create().texOffs(0,0).mirror().addBox(0,0,0,14,16,1),PartPose.offsetAndRotation(15,0,1,0,-1.570796F,0));
        root.addOrReplaceChild("back",CubeListBuilder.create().texOffs(1,1).mirror().addBox(1,1,0,12,14,1),PartPose.offsetAndRotation(15,0,15,0,3.141593F,0));
        root.addOrReplaceChild("top",CubeListBuilder.create().texOffs(1,17).mirror().addBox(1,0,0,12,14,1),PartPose.offsetAndRotation(1,0,15,-1.570796F,0,0));
        root.addOrReplaceChild("bottom",CubeListBuilder.create().texOffs(1,17).mirror().addBox(1,0,0,12,14,1),PartPose.offsetAndRotation(15,15,1,-1.570796F,3.141593F,0));
        root.addOrReplaceChild("door",CubeListBuilder.create().texOffs(30,0).mirror().addBox(0,0,0,12,14,1),PartPose.offset(2,1,2));return LayerDefinition.create(mesh,64,64);}
}
