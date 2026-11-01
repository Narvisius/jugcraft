package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.botany.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

/** The supplied chakram sprite spins flat in world space; vine balls keep billboard presentation. */
final class BotanicalThrownRenderer extends EntityRenderer<BotanicalThrownTools,BotanicalThrownRenderer.State>{
    private final ItemModelResolver items;
    static final class State extends EntityRenderState{final ItemStackRenderState item=new ItemStackRenderState();boolean chakram;float yaw;}
    BotanicalThrownRenderer(EntityRendererProvider.Context context){super(context);items=context.getItemModelResolver();shadowRadius=.1F;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(BotanicalThrownTools entity,State s,float partial){super.extractRenderState(entity,s,partial);var item=entity.getItem();s.chakram=item.is(Jugtania.item("thorn_chakram"))||item.is(Jugtania.item("flare_chakram"));s.yaw=entity.getYRot(partial);items.updateForTopItem(s.item,item,ItemDisplayContext.NONE,entity.level(),null,entity.getId());}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
        pose.pushPose();if(s.chakram){pose.rotateDegrees(Axis.YP,s.yaw+s.ageInTicks*48);pose.rotateDegrees(Axis.XP,90);pose.scale(.8F,.8F,.8F);}else{pose.rotate(camera.orientation);pose.rotateDegrees(Axis.YP,180);pose.scale(.5F,.5F,.5F);}
        s.item.submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();super.submit(s,pose,out,camera);
    }
}
