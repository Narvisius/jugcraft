package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.*;
import com.geckolib.renderer.base.*;
import com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.wilds.*;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;

/** Authored sun-tribe rigs, layered masks, equipment sockets and directional gait equations. */
final class WildSunRenderer {
    private static final DataTicket<Boolean> ELITE=DataTicket.create("wilds_sun_elite",Boolean.class),RAPTOR=DataTicket.create("wilds_sun_raptor",Boolean.class),ALIVE=DataTicket.create("wilds_sun_alive",Boolean.class),WALKING=DataTicket.create("wilds_sun_walk_active",Boolean.class);
    private static final DataTicket<Float> AGE=DataTicket.create("wilds_sun_age",Float.class),WALK=DataTicket.create("wilds_sun_walk",Float.class),SPEED=DataTicket.create("wilds_sun_speed",Float.class),FORWARD=DataTicket.create("wilds_sun_forward",Float.class),SIDE=DataTicket.create("wilds_sun_side",Float.class),YAW=DataTicket.create("wilds_sun_yaw",Float.class),PITCH=DataTicket.create("wilds_sun_pitch",Float.class);
    private static final class Model<T extends GeoAnimatable> extends GeoModel<T>{
        final String name;Model(String name){this.name=name;}
        public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("entity/wilds/"+name);}
        public Identifier getTextureResource(GeoRenderState s){return Jugcraft.id("textures/newest/wilds/entity/"+name+(name.equals("umvuthana")&&s.getOrDefaultGeckolibData(ELITE,false)?"_elite":"")+".png");}
        public Identifier getAnimationResource(T e){return Jugcraft.id("entity/wilds/"+name);}
    }
    static void register(){
        WildSunControls.register();
        for(var type:List.of(WildUmvuthana.TYPE,WildUmvuthana.RAPTOR,WildUmvuthana.CRANE,WildUmvuthana.FOLLOWER))EntityRendererRegistry.register(type,Warrior::new);
        EntityRendererRegistry.register(WildUmvuthi.TYPE,Guardian::new);EntityRendererRegistry.register(WildSolar.TYPE,WildSolarRenderer::new);
        var providers=new HashMap<String,GeoRenderProvider>();WildSunEquipment.renderer=item->providers.computeIfAbsent(item.model(),name->new GeoRenderProvider(){
            final GeoModel<WildSunEquipment.Mask> model=new GeoModel<>(){
                public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("armor/wilds/"+name);}
                public Identifier getTextureResource(GeoRenderState s){return Jugcraft.id("textures/newest/wilds/item/"+item.texture()+".png");}
                public Identifier getAnimationResource(WildSunEquipment.Mask e){return Jugcraft.id("armor/wilds/"+name);}
            };
            final GeoArmorRenderer<WildSunEquipment.Mask,HumanoidRenderState> armor=new GeoArmorRenderer<>(model);
            final GeoItemRenderer<WildSunEquipment.Mask> held=new GeoItemRenderer<>(model);
            public GeoArmorRenderer<?,?> getGeoArmorRenderer(ItemStack stack,EquipmentSlot slot){return armor;}
            public GeoItemRenderer<?> getGeoItemRenderer(){return held;}
        });
    }
    private static class Warrior extends GeoEntityRenderer<WildUmvuthana,EntityRenderState>{
        Warrior(EntityRendererProvider.Context c){super(c,new Model<WildUmvuthana>("umvuthana"));shadowRadius=.6F;
            withRenderLayer(new BlockAndItemGeoLayer<WildUmvuthana,Void,EntityRenderState>(c,this){
                protected List<RenderData> getRelevantBones(WildUmvuthana e,Void unused,EntityRenderState s,float partial){var mask=new ItemStackRenderState();itemModelResolver.updateForLiving(mask,new ItemStack(OwnerWilds.item("umvuthana_mask_"+WildUmvuthana.MASKS[e.mask()])),ItemDisplayContext.NONE,e);var weapon=new ItemStackRenderState();itemModelResolver.updateForLiving(weapon,new ItemStack(OwnerWilds.item(e.weapon()==2?"sunblock_staff":e.weapon()==1?"blowgun":"spear")),ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,e);return List.of(RenderData.item("maskTwitcher",ItemDisplayContext.NONE,mask),RenderData.item("maskHand",ItemDisplayContext.NONE,mask),RenderData.item("rightHand",ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,weapon));}
                public void addRenderData(WildUmvuthana e,Void unused,EntityRenderState s,float partial){s.addGeckolibData(CONTENTS,getRelevantBones(e,unused,s,partial));}
                @Override protected void submitItemStackRender(PoseStack pose,com.geckolib.cache.model.GeoBone bone,ItemStackRenderState item,ItemDisplayContext context,EntityRenderState state,SubmitNodeCollector collector,int light){pose.pushPose();if(bone.name().startsWith("mask")){pose.scale(1.8132F,1.8132F,1.8132F);pose.translate(0,-1.75,.25);}super.submitItemStackRender(pose,bone,item,context,state,collector,light);pose.popPose();}
            });
        }
        @Override public void captureDefaultRenderState(WildUmvuthana e,Void v,EntityRenderState s,float partial){super.captureDefaultRenderState(e,v,s,partial);s.addGeckolibData(ELITE,e.elite());s.addGeckolibData(RAPTOR,e.mask()==0);s.addGeckolibData(ALIVE,e.isAlive());s.addGeckolibData(WALKING,e.isAlive()&&e.action()==WildUmvuthana.IDLE);s.addGeckolibData(WALK,e.walkAnimation.position(partial));s.addGeckolibData(SPEED,e.walkAnimation.speed(partial));s.addGeckolibData(YAW,Mth.rotLerp(partial,e.yHeadRotO-e.yBodyRotO,e.yHeadRot-e.yBodyRot)*Mth.DEG_TO_RAD);s.addGeckolibData(PITCH,Mth.lerp(partial,e.xRotO,e.getXRot())*Mth.DEG_TO_RAD);var look=e.getLookAngle().multiply(1,0,1).normalize();var motion=e.getDeltaMovement().multiply(1,0,1).normalize();s.addGeckolibData(FORWARD,(float)look.dot(motion));s.addGeckolibData(SIDE,(float)(look.x*motion.z-look.z*motion.x));}
        @Override public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info,BoneSnapshots bones){super.adjustModelBonesForRender(info,bones);boolean elite=info.getOrDefaultGeckolibData(ELITE,false),raptor=info.getOrDefaultGeckolibData(RAPTOR,false);for(String name:List.of("crestRight","crestLeft","crest1"))bones.ifPresent(name,b->b.skipRender(!elite).skipChildrenRender(!elite));for(String side:List.of("left","right")){bones.ifPresent(side+"IndexTalon",b->b.skipRender(!raptor).skipChildrenRender(!raptor));bones.ifPresent(side+"IndexClaw",b->b.skipRender(raptor).skipChildrenRender(raptor));}
            bones.ifPresent("root",b->{float scale=elite?.93F:.83F;b.setScale(b.getScaleX()*scale,b.getScaleY()*scale,b.getScaleZ()*scale);});
            boolean hand=-bones.get("maskPlacementSwitchController").map(b->b.getTranslateX()).orElse(0F)>.5F;bones.ifPresent("maskTwitcher",b->b.skipRender(hand).skipChildrenRender(hand));bones.ifPresent("maskHand",b->b.skipRender(!hand).skipChildrenRender(!hand));
            if(info.getOrDefaultGeckolibData(ALIVE,true))for(String name:List.of("head","neck"))bones.ifPresent(name,b->{b.setRotY(b.getRotY()+info.getOrDefaultGeckolibData(YAW,0F)/2);b.setRotX(b.getRotX()+info.getOrDefaultGeckolibData(PITCH,0F)/2);});
            if(info.getOrDefaultGeckolibData(WALKING,false)){float f=info.getOrDefaultGeckolibData(FORWARD,0F),side=info.getOrDefaultGeckolibData(SIDE,0F),speed=info.getOrDefaultGeckolibData(SPEED,0F);new WildUmvuthanaPose(bones).apply(info.getOrDefaultGeckolibData(WALK,0F),speed,Math.max(0,f),Math.max(0,-f),Math.max(0,side),Math.max(0,-side),Math.clamp((speed-.15)/.25,0,1),1);}
        }
    }
    private static class Guardian extends GeoEntityRenderer<WildUmvuthi,EntityRenderState>{
        Guardian(EntityRendererProvider.Context c){super(c,new Model<WildUmvuthi>("umvuthi"));shadowRadius=1;}
        @Override public void captureDefaultRenderState(WildUmvuthi e,Void v,EntityRenderState s,float partial){super.captureDefaultRenderState(e,v,s,partial);s.addGeckolibData(AGE,e.tickCount+partial);s.addGeckolibData(ALIVE,e.isAlive());s.addGeckolibData(YAW,Mth.rotLerp(partial,e.yHeadRotO-e.yBodyRotO,e.yHeadRot-e.yBodyRot)*Mth.DEG_TO_RAD);s.addGeckolibData(PITCH,Mth.lerp(partial,e.xRotO,e.getXRot())*Mth.DEG_TO_RAD);}
        @Override public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info,BoneSnapshots bones){super.adjustModelBonesForRender(info,bones);new WildUmvuthiPose(bones).apply(info.getOrDefaultGeckolibData(AGE,0F),info.getOrDefaultGeckolibData(YAW,0F),info.getOrDefaultGeckolibData(PITCH,0F),info.getOrDefaultGeckolibData(ALIVE,true));}
    }
}
