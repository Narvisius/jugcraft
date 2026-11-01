package io.github.jimbozoomer.jugcraft.client.aquatic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.aquatic.*;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Original supplied plaque JSON and original fish rigs; display copies never enter or tick in the world. */
public final class OwnerFishMountRenderer extends EntityRenderer<OwnerFishMount,OwnerFishMountRenderer.State> {
    private final ItemModelResolver items;
    private final WeakHashMap<OwnerFishMount,Entity> fish=new WeakHashMap<>();
    public OwnerFishMountRenderer(EntityRendererProvider.Context context){super(context);items=context.getItemModelResolver();}
    public static final class State extends EntityRenderState {
        final ItemStackRenderState plaque=new ItemStackRenderState();
        Direction direction=Direction.NORTH;
        EntityRenderState fish;
        OwnerAquaculture.Shape shape;
        boolean puffer;
    }
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(OwnerFishMount mount,State state,float partial){
        super.extractRenderState(mount,state,partial);state.direction=mount.getDirection();state.fish=null;state.shape=null;state.puffer=false;
        items.updateForTopItem(state.plaque,new ItemStack(OwnerFishMount.ITEM),ItemDisplayContext.NONE,mount.level(),null,mount.getId());
        var type=OwnerFishMount.fishType(mount.getItem());if(type==null){fish.remove(mount);return;}
        var display=fish.get(mount);
        if(display==null||display.getType()!=type){display=type.create(mount.level(),EntitySpawnReason.LOAD);if(display==null)return;if(display instanceof Mob mob)mob.setNoAi(true);fish.put(mount,display);}
        state.fish=entityRenderDispatcher.extractEntity(display,0);state.fish.lightCoords=state.lightCoords;
        if(display instanceof OwnerAquaculture.Fish f)state.shape=f.species.shape();state.puffer=type==net.minecraft.world.entity.EntityTypes.PUFFERFISH;
    }
    @Override public Vec3 getRenderOffset(State s){return new Vec3(s.direction.getStepX()*.3,-.25,s.direction.getStepZ()*.3);}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
        super.submit(s,pose,out,camera);pose.pushPose();var offset=getRenderOffset(s);pose.translate(-offset.x,-offset.y,-offset.z);
        pose.translate(s.direction.getStepX()*.46875,s.direction.getStepY()*.46875,s.direction.getStepZ()*.46875);
        pose.rotateDegrees(Axis.XP,s.direction.getAxis().isHorizontal()?0:-90*s.direction.getAxisDirection().getStep());
        pose.rotateDegrees(Axis.YP,s.direction.getAxis().isHorizontal()?180-s.direction.toYRot():180);
        if(!s.isInvisible)s.plaque.submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,s.outlineColor);
        if(s.fish!=null){
            double x=-.1,y=0,depth=s.puffer?.64:.55;
            if(s.shape==OwnerAquaculture.Shape.HALIBUT)depth=.56;
            else if(s.shape==OwnerAquaculture.Shape.LONGNOSE){pose.scale(.75f,.75f,.75f);depth=.67;}
            else if(s.shape==OwnerAquaculture.Shape.JELLYFISH){pose.scale(.75f,.75f,.75f);x+=.025;depth+=.15;}
            else if(s.shape==OwnerAquaculture.Shape.SMALL)y=.1;else pose.scale(.95f,.95f,.95f);
            pose.translate(x,y,depth);pose.rotateDegrees(Axis.XP,-90);pose.rotateDegrees(Axis.YP,-90);
            entityRenderDispatcher.submit(s.fish,camera,0,0,0,pose,out);
        }
        pose.popPose();
    }
}
