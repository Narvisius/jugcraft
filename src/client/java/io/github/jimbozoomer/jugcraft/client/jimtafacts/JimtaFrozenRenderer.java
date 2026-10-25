package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaApparition;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;

/** Like the source renderer, reuse the copied creature's native model under the supplied ice texture. */
@SuppressWarnings({"rawtypes","unchecked"})
public final class JimtaFrozenRenderer extends EntityRenderer<JimtaApparition,JimtaFrozenRenderer.State>{
    private final EntityRenderDispatcher dispatcher;
    private final HumanoidModel<HumanoidRenderState> fallback;
    private final java.util.Map<JimtaApparition,Entity> copies=new java.util.WeakHashMap<>();
    private final java.util.Map<Identifier,Boolean> rectangular=new java.util.HashMap<>();
    public static final class State extends EntityRenderState{EntityModel model;LivingEntityRenderState frozen;float yaw;boolean rectangular;}
    public JimtaFrozenRenderer(EntityRendererProvider.Context c){super(c);dispatcher=c.getEntityRenderDispatcher();fallback=new HumanoidModel<>(c.bakeLayer(ModelLayers.PLAYER));}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(JimtaApparition e,State s,float partial){
        super.extractRenderState(e,s,partial);s.yaw=e.getYRot();s.model=fallback;s.frozen=new HumanoidRenderState();s.rectangular=false;
        var key=Identifier.tryParse(e.copiedType());if(key==null||e.copiedType().equals("minecraft:player"))return;
        var type=BuiltInRegistries.ENTITY_TYPE.getValue(key);if(type==null||type==e.getType())return;
        var copy=copies.computeIfAbsent(e,k->type.create(e.level(),EntitySpawnReason.LOAD));if(!(copy instanceof LivingEntity living))return;
        var renderer=dispatcher.getRenderer(living);if(!(renderer instanceof LivingEntityRenderer nativeRenderer))return;
        living.snapTo(e.getX(),e.getY(),e.getZ(),e.getYRot(),0);living.setYBodyRot(e.getYRot());living.setYHeadRot(e.getYRot());living.setNoGravity(true);
        s.frozen=(LivingEntityRenderState)nativeRenderer.createRenderState();nativeRenderer.extractRenderState(living,s.frozen,0);s.model=nativeRenderer.getModel();s.frozen.ageInTicks=0;s.frozen.walkAnimationPos=0;s.frozen.walkAnimationSpeed=0;
        Identifier texture=nativeRenderer.getTextureLocation(s.frozen);s.rectangular=rectangular.computeIfAbsent(texture,k->{try(var stream=net.minecraft.client.Minecraft.getInstance().getResourceManager().open(k)){var image=javax.imageio.ImageIO.read(stream);return image!=null&&image.getWidth()!=image.getHeight();}catch(java.io.IOException ex){return false;}});
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){pose.pushPose();pose.rotateDegrees(Axis.YP,180-s.yaw);pose.scale(-1,-1,1);pose.translate(0,-1.501,0);collector.order(0).submitModel(s.model,s.frozen,pose,RenderTypes.entityCutout(Jugcraft.id("textures/jimtafacts/spellbooks/entity/frozen_humanoid"+(s.rectangular?"_alt":"")+".png")),s.lightCoords,OverlayTexture.NO_OVERLAY,-1,null,s.outlineColor);pose.popPose();super.submit(s,pose,collector,camera);}
}
