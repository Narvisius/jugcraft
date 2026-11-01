package io.github.jimbozoomer.jugcraft.client;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.FloweryMooblooms;
import net.minecraft.client.model.animal.cow.*;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Source adult/baby skins with native cow UVs and the variant's actual flower block geometry. */
public final class FloweryMoobloomRenderer extends AgeableMobRenderer<FloweryMooblooms.Bloom,FloweryMoobloomRenderer.State,CowModel> {
    private static final BlockDisplayContext DISPLAY=BlockDisplayContext.create();
    private final BlockModelResolver resolver;
    public FloweryMoobloomRenderer(EntityRendererProvider.Context context){
        super(context,new CowModel(context.bakeLayer(ModelLayers.COW)),new BabyCowModel(context.bakeLayer(ModelLayers.COW_BABY)),.7f);
        resolver=context.getBlockModelResolver();addLayer(new Flowers(this));
    }
    public static final class State extends LivingEntityRenderState {
        String flower="dandelion";boolean sheared;
        final BlockModelRenderState bottom=new BlockModelRenderState(),top=new BlockModelRenderState();
    }
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State state){return Jugcraft.id("textures/newest/flowery/moobloom_"+state.flower+(state.isBaby?"_baby":"")+".png");}
    @Override public void extractRenderState(FloweryMooblooms.Bloom entity,State state,float partial){
        super.extractRenderState(entity,state,partial);state.flower=entity.flowers().name();state.sheared=entity.sheared();
        var block=entity.flowers().flower().defaultBlockState();resolver.update(state.bottom,block,DISPLAY);
        resolver.update(state.top,block.hasProperty(DoublePlantBlock.HALF)?block.setValue(DoublePlantBlock.HALF,DoubleBlockHalf.UPPER):Blocks.AIR.defaultBlockState(),DISPLAY);
    }
    private static final class Flowers extends RenderLayer<State,CowModel> {
        Flowers(FloweryMoobloomRenderer renderer){super(renderer);}
        @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,State state,float yaw,float pitch){
            if(state.isBaby||state.sheared||state.isInvisible)return;
            for(int i=0;i<3;i++){
                pose.pushPose();
                if(i==2){getParentModel().getHead().translateAndRotate(pose);pose.translate(0,-.7,-.2);}
                else pose.translate(i==0?.2:-.2,-.35,i==0?.5:-.25);
                pose.rotateDegrees(Axis.YP,i==0?-48:42);pose.scale(-.65f,-.65f,.65f);pose.translate(-.5,-.5,-.5);
                int overlay=LivingEntityRenderer.getOverlayCoords(state,0);
                state.bottom.submit(pose,collector,light,overlay,state.outlineColor);
                if(!state.top.isEmpty()){pose.translate(0,1,0);state.top.submit(pose,collector,light,overlay,state.outlineColor);}
                pose.popPose();
            }
        }
    }
}
