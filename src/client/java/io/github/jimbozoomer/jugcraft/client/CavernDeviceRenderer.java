package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import java.util.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** Display copies never enter the level, tick AI, or carry copied inventory/ownership data. */
public final class CavernDeviceRenderer implements BlockEntityRenderer<CavernDevice,CavernDeviceRenderer.State> {
    private record Cached(ItemStack card,Entity entity){}
    private final Map<CavernDevice,Cached> cache=new WeakHashMap<>();
    private final EntityRenderDispatcher entities;
    private static int displayId=-3000000;
    public static final class State extends BlockEntityRenderState {EntityRenderState image;float scale,angle;}
    public CavernDeviceRenderer(BlockEntityRendererProvider.Context c){entities=c.entityRenderer();}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(CavernDevice b,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderState.extractBase(b,s,overlay);s.image=null;var level=b.getLevel();var card=b.getItem(0);
        if(level==null||!b.projector()||!card.is(Menagerie.ITEMS.get("cavern_holocoder"))){cache.remove(b);return;}
        var data=card.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var id=Identifier.tryParse(data.getStringOr("DisplayType",""));if(id==null)return;
        var current=cache.get(b);
        if(current==null||current.entity.level()!=level||!ItemStack.isSameItemSameComponents(card,current.card)){
            var type=BuiltInRegistries.ENTITY_TYPE.getValue(id);if(type==null)return;var entity=type.create(level,EntitySpawnReason.MOB_SUMMONED);if(entity==null)return;
            entity.setId(displayId--);entity.setPos(b.getBlockPos().getX()+.5,b.getBlockPos().getY()+1,b.getBlockPos().getZ()+.5);
            if(entity instanceof MenagerieAnimal animal){animal.variant(data.getIntOr("DisplayVariant",0));animal.setBaby(data.getBooleanOr("DisplayBaby",false));}
            var profile=card.get(DataComponents.PROFILE);if(profile!=null)entity.setComponent(DataComponents.PROFILE,profile);
            current=new Cached(card.copy(),entity);cache.put(b,current);
        }
        s.scale=Math.min(.9F,1.4F/Math.max(.1F,Math.max(current.entity.getBbHeight(),current.entity.getBbWidth())));s.angle=ConcordanceClientOptions.reducedMotion()?0:(level.getGameTime()%720+partial)*.5F;
        s.image=entities.extractEntity(current.entity,partial);
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        if(s.image==null)return;pose.pushPose();pose.translate(.5,.9,.5);pose.rotateDegrees(Axis.YP,s.angle);pose.scale(s.scale,s.scale,s.scale);entities.getRenderer(s.image).submit(s.image,pose,collector,camera);pose.popPose();
    }
}
