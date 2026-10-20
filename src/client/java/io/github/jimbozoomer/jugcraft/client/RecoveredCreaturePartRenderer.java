package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.RecoveredCreaturePart;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Renders each recovered segment with its own original atlas and pivot. */
public final class RecoveredCreaturePartRenderer extends EntityRenderer<RecoveredCreaturePart,RecoveredWildlifeRenderer.State> {
    private final Map<String,RecoveredWildlifeModel> models=new HashMap<>();
    public static void register(){EntityRendererRegistry.register(RecoveredCreaturePart.TYPE,RecoveredCreaturePartRenderer::new);}
    private RecoveredCreaturePartRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public RecoveredWildlifeRenderer.State createRenderState(){return new RecoveredWildlifeRenderer.State();}
    @Override public void extractRenderState(RecoveredCreaturePart entity,RecoveredWildlifeRenderer.State state,float partial){
        super.extractRenderState(entity,state,partial);
        state.model=entity.model();state.skin=entity.skin();state.sourceYaw=entity.getYRot();state.yRot=0;
        state.segmentIndex=entity.segmentIndex();
        state.action="";state.actionTime=-1;state.isBaby=false;
        var parent=entity.parent();
        if(parent!=null){state.isBaby=parent.isBaby();state.isInWater=parent.isInWater();state.flying=parent.species().flying();state.aquatic=parent.species().aquatic();state.walkAnimationPos=parent.walkAnimation.position(partial);state.walkAnimationSpeed=parent.walkAnimation.speed(partial);}
    }
    @Override public void submit(RecoveredWildlifeRenderer.State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        if(state.model.isEmpty()||state.skin.isEmpty())return;
        var model=models.computeIfAbsent(state.model,k->new RecoveredWildlifeModel("recovered/"+k));
        pose.pushPose();pose.rotateDegrees(Axis.YP,180-state.sourceYaw);pose.scale(-1,-1,1);
        if(state.isBaby)pose.scale(.5F,.5F,.5F);
        pose.translate(0,-1.501,0);
        collector.order(0).submitModel(model,state,pose,RenderTypes.entityCutout(Jugcraft.id("textures/"+state.skin+".png")),state.lightCoords,OverlayTexture.NO_OVERLAY,-1,null,state.outlineColor);
        pose.popPose();super.submit(state,pose,collector,camera);
    }
}
