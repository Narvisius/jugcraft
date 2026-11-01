package io.github.jimbozoomer.jugcraft.client;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.event.entity.*;
import com.geckolib.renderer.base.*;
import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.wilds.WildCold;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/** Ice uses the actual posed mesh, including owner-supplied Gecko and recovered rigs. */
public final class WildFrozenVisuals {
    static final Identifier TEXTURE=Jugcraft.id("textures/newest/wilds/entity/frozen.png");
    private static final DataTicket<Boolean> FROZEN=DataTicket.create("jugcraft_wilds_frozen",Boolean.class);
    private static final java.util.Map<LivingEntity,FrozenPose> POSES=new java.util.WeakHashMap<>();
    private static final DataTicket<FrozenPose> POSE=DataTicket.create("jugcraft_wilds_frozen_pose",FrozenPose.class);
    private record BonePose(float x,float y,float z,float rx,float ry,float rz,float sx,float sy,float sz,boolean hidden,boolean children) {
        static BonePose capture(com.geckolib.animation.state.BoneSnapshot b){return new BonePose(b.getTranslateX(),b.getTranslateY(),b.getTranslateZ(),b.getRotX(),b.getRotY(),b.getRotZ(),b.getScaleX(),b.getScaleY(),b.getScaleZ(),b.isHidden(),b.areChildrenHidden());}
        void apply(com.geckolib.animation.state.BoneSnapshot b){b.setTranslation(x,y,z).setRotation(rx,ry,rz).setScale(sx,sy,sz).skipRender(hidden).skipChildrenRender(children);}
    }
    private static final class FrozenPose {
        com.geckolib.cache.model.BakedGeoModel model;
        final java.util.Map<String,BonePose> bones=new java.util.HashMap<>();
        void apply(RenderPassInfo<?> info,BoneSnapshots snapshots){
            if(model!=info.model()){model=info.model();bones.clear();}
            for(var bone:model.boneLookup().get().values()){
                var snapshot=snapshots.get(bone);BonePose pose=bones.get(bone.name());
                if(pose==null)bones.put(bone.name(),BonePose.capture(snapshot));else pose.apply(snapshot);
            }
        }
    }
    private WildFrozenVisuals(){}
    @SuppressWarnings({"rawtypes","unchecked"})
    static void register(){
        CompileEntityRenderLayersEvent.EVENT.register(event->event.addLayer(new IceLayer(event.getRenderer())));
        CompileEntityRenderStateEvent.EVENT.register(event->{
            boolean frozen=event.getAnimatable() instanceof LivingEntity e&&WildCold.frozen(e);
            event.addData(FROZEN,frozen&&event.getAnimatable() instanceof LivingEntity e&&!e.isInvisible());
            if(event.getAnimatable() instanceof LivingEntity entity){
                if(frozen)event.addData(POSE,POSES.computeIfAbsent(entity,e->new FrozenPose()));else POSES.remove(entity);
            }
        });
        GeoEntityPreRenderEvent.EVENT.register(event->{
            var info=event.getRenderPassInfo();FrozenPose pose=(FrozenPose)info.getGeckolibData(POSE);
            if(pose!=null)info.addBoneUpdater((pass,bones)->pose.apply(pass,bones));
            return true;
        });
    }
    private static final class IceLayer<T extends GeoAnimatable,O,R extends GeoRenderState> extends TextureLayerGeoLayer<T,O,R>{
        IceLayer(GeoRenderer<T,O,R> renderer){super(renderer,TEXTURE,RenderTypes::entityTranslucent);}
        @Override protected net.minecraft.client.renderer.rendertype.RenderType getRenderType(R state){return state.getOrDefaultGeckolibData(FROZEN,false)?super.getRenderType(state):null;}
    }
    static void submit(ModelPart mesh,EntityRenderState state,PoseStack pose,SubmitNodeCollector out){
        if(!((WildFrozenState)state).jugcraft$frozen())return;
        pose.pushPose();pose.scale(1.002F,1.002F,1.002F);
        out.order(2).submitModelPart(mesh,pose,RenderTypes.entityTranslucent(TEXTURE),state.lightCoords,OverlayTexture.NO_OVERLAY,null,0xDDFFFFFF,0);
        pose.popPose();
    }
}
