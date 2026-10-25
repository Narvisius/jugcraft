package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopRobot;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopRobots;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Original supplied robot geometry/UVs and board-specific skins; native gentle hover motion. */
public final class WorkshopRobotRenderer extends EntityRenderer<WorkshopRobot,WorkshopRobotRenderer.State> {
    private final ItemModelResolver items;
    public static final class State extends EntityRenderState {final ItemStackRenderState body=new ItemStackRenderState();float yaw,lean;}
    public static void register(){EntityRendererRegistry.register(WorkshopRobots.TYPE,WorkshopRobotRenderer::new);}
    public WorkshopRobotRenderer(EntityRendererProvider.Context context){super(context);items=context.getItemModelResolver();shadowRadius=.23F;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(WorkshopRobot robot,State state,float partialTick){
        super.extractRenderState(robot,state,partialTick);state.yaw=robot.getYRot(partialTick);state.lean=(float)Math.min(8,robot.getDeltaMovement().horizontalDistance()*35);
        items.updateForTopItem(state.body,new ItemStack(WorkshopRobots.SHELLS.get(robot.program())),ItemDisplayContext.NONE,robot.level(),null,robot.getId());
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        pose.pushPose();pose.translate(0,.25+Math.sin(state.ageInTicks*.12)*.025,0);pose.rotateDegrees(Axis.YP,180-state.yaw);pose.rotateDegrees(Axis.XP,state.lean);
        state.body.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();super.submit(state,pose,collector,camera);
    }
}
