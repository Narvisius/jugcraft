package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Renders the supplied pitched fan blades around their actual axle. */
public final class HouseholdFanRenderer implements BlockEntityRenderer<HouseholdFan,HouseholdFanRenderer.State> {
    private final ItemModelResolver models;
    public static final class State extends BlockEntityRenderState {final ItemStackRenderState blade=new ItemStackRenderState();Direction facing=Direction.UP;float spin;}
    public HouseholdFanRenderer(BlockEntityRendererProvider.Context c){models=c.itemModelResolver();}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(HouseholdFan fan,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderState.extractBase(fan,s,overlay);var state=fan.getBlockState();s.facing=state.getValue(BlockStateProperties.FACING);
        s.spin=HouseholdBlock.flag(state,"powered")&&fan.getLevel()!=null?(fan.getLevel().getGameTime()%120+partial)*18:0;
        var stack=new ItemStack(Items.STICK);stack.set(DataComponents.ITEM_MODEL,Jugcraft.id("household/fan_blade_"+fan.design()));
        models.updateForTopItem(s.blade,stack,ItemDisplayContext.NONE,fan.getLevel(),null,0);
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        pose.pushPose();pose.translate(.5,.5,.5);
        if(s.facing==Direction.DOWN)pose.rotateDegrees(Axis.XP,180);
        else if(s.facing.getAxis().isHorizontal()){pose.rotateDegrees(Axis.YP,-s.facing.toYRot());pose.rotateDegrees(Axis.XP,90);}
        pose.translate(0,1/16F,0);pose.rotateDegrees(Axis.YP,s.spin);
        for(int i=0;i<4;i++){pose.pushPose();pose.rotateDegrees(Axis.YP,i*90);pose.rotateDegrees(Axis.ZP,8);pose.translate(0,-1/16F,0);s.blade.submit(pose,collector,s.lightCoords,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0);pose.popPose();}
        pose.popPose();
    }
}
