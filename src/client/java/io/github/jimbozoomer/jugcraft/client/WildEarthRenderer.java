package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.*;
import com.geckolib.renderer.base.*;
import com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.wilds.*;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;

/** Original Sculptor, armor, gauntlet and geomancy meshes; no substitute creature geometry. */
final class WildEarthRenderer {
    private static final DataTicket<Integer> MODE=DataTicket.create("wilds_earth_mode",Integer.class),TIER=DataTicket.create("wilds_earth_tier",Integer.class),CRUMBLE=DataTicket.create("wilds_earth_crumble",Integer.class);
    private static final DataTicket<Float> HEIGHT=DataTicket.create("wilds_earth_height",Float.class),AGE=DataTicket.create("wilds_earth_age",Float.class),YAW=DataTicket.create("wilds_earth_yaw",Float.class),PITCH=DataTicket.create("wilds_earth_pitch",Float.class);
    private static final DataTicket<Identifier> TEXTURE=DataTicket.create("wilds_earth_texture",Identifier.class);
    private static final DataTicket<WildSpringChain.Frame> BEARD=DataTicket.create("wilds_sculptor_beard",WildSpringChain.Frame.class);
    private static final DataTicket<Boolean> ALIVE=DataTicket.create("wilds_earth_alive",Boolean.class);
    static void register(){EntityRendererRegistry.register(WildSculptor.TYPE,Sculptor::new);EntityRendererRegistry.register(WildBoulder.TYPE,Boulder::new);
        var cache=new IdentityHashMap<Item,GeoRenderProvider>();WildEarthEquipment.renderer=item->cache.computeIfAbsent(item,key->{
            String model=key instanceof WildEarthEquipment.Gauntlet?"earthrend_gauntlet":key instanceof WildEarthEquipment.Staff?"sculptor_staff":"geomancer_armor";
            return new GeoRenderProvider(){
                final GeoModel geo=new EquipmentModel(model);final GeoItemRenderer held=new GeoItemRenderer(geo);final GeoArmorRenderer armor=key instanceof WildEarthEquipment.Armor equipment?new EarthArmor(new EquipmentModel<>(model),equipment.slot):new GeoArmorRenderer(geo);
                @Override public GeoItemRenderer<?> getGeoItemRenderer(){return held;}@Override public GeoArmorRenderer<?,?> getGeoArmorRenderer(ItemStack stack,EquipmentSlot slot){return armor;}
            };
        });
    }
    private static final class EarthArmor extends GeoArmorRenderer<WildEarthEquipment.Armor,net.minecraft.client.renderer.entity.state.HumanoidRenderState>{
        final net.minecraft.world.item.equipment.ArmorType type;
        EarthArmor(GeoModel<WildEarthEquipment.Armor> model,net.minecraft.world.item.equipment.ArmorType type){super(model);this.type=type;}
        @Override public List<ArmorSegment> getSegmentsForSlot(net.minecraft.client.renderer.entity.state.HumanoidRenderState state,EquipmentSlot slot){
            // The source beads occupy the head slot but sit on the chest; the belt also follows the torso.
            return switch(type){case HELMET->List.of(ArmorSegment.CHEST);case LEGGINGS->List.of(ArmorSegment.CHEST,ArmorSegment.LEFT_LEG,ArmorSegment.RIGHT_LEG);default->super.getSegmentsForSlot(state,slot);};
        }
        @Override public void adjustModelBonesForRender(RenderPassInfo<net.minecraft.client.renderer.entity.state.HumanoidRenderState> info,BoneSnapshots bones){super.adjustModelBonesForRender(info,bones);hide(bones,"prayer_beads",type!=net.minecraft.world.item.equipment.ArmorType.HELMET);hide(bones,"robes",type!=net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);hide(bones,"belt",type!=net.minecraft.world.item.equipment.ArmorType.LEGGINGS);}
    }
    private static final class EquipmentModel<T extends GeoAnimatable> extends GeoModel<T>{
        final String name;EquipmentModel(String name){this.name=name;}
        public Identifier getModelResource(GeoRenderState state){return Jugcraft.id("equipment/wilds/"+name);}
        public Identifier getTextureResource(GeoRenderState state){return Jugcraft.id("textures/newest/wilds/item/"+name+".png");}
        public Identifier getAnimationResource(T e){return Jugcraft.id("equipment/wilds/"+name);}
    }
    private static class Sculptor extends GeoEntityRenderer<WildSculptor,EntityRenderState>{
        Sculptor(EntityRendererProvider.Context c){super(c,new GeoModel<WildSculptor>(){
            public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("entity/wilds/sculptor");}
            public Identifier getTextureResource(GeoRenderState s){return Jugcraft.id("textures/newest/wilds/entity/sculptor.png");}
            public Identifier getAnimationResource(WildSculptor e){return Jugcraft.id("entity/wilds/sculptor");}
        });shadowRadius=.7F;
            withRenderLayer(new BlockAndItemGeoLayer<WildSculptor,Void,EntityRenderState>(c,this){
                @Override protected List<RenderData> getRelevantBones(WildSculptor e,Void v,EntityRenderState s,float partial){var staff=new ItemStackRenderState();itemModelResolver.updateForLiving(staff,new ItemStack(OwnerWilds.item("sculptor_staff")),ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,e);return List.of(RenderData.item("itemHandLeft",ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,staff),RenderData.item("itemHandRight",ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,staff),RenderData.item("backItem",ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,staff));}
                @Override public void addRenderData(WildSculptor e,Void v,EntityRenderState s,float partial){s.addGeckolibData(CONTENTS,getRelevantBones(e,v,s,partial));}
            });
        }
        @Override public void captureDefaultRenderState(WildSculptor e,Void v,EntityRenderState s,float partial){super.captureDefaultRenderState(e,v,s,partial);s.addGeckolibData(BEARD,WildSpringChain.capture(e,partial));s.addGeckolibData(AGE,e.tickCount+partial);s.addGeckolibData(ALIVE,e.isAlive());s.addGeckolibData(YAW,Mth.rotLerp(partial,e.yHeadRotO-e.yBodyRotO,e.yHeadRot-e.yBodyRot)*Mth.DEG_TO_RAD);s.addGeckolibData(PITCH,Mth.lerp(partial,e.xRotO,e.getXRot())*Mth.DEG_TO_RAD);}
        @Override public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info,BoneSnapshots bones){super.adjustModelBonesForRender(info,bones);float torso=controller(bones,"torsoAimController"),progress=controller(bones,"gauntletProgressController");for(String name:List.of("head","chestJoint")){float amount=name.equals("head")?1-torso:torso;bones.ifPresent(name,b->{b.setRotX(b.getRotX()+info.getOrDefaultGeckolibData(PITCH,0F)*amount);b.setRotY(b.getRotY()+info.getOrDefaultGeckolibData(YAW,0F)*amount);});}
            for(String side:List.of("Left","Right")){boolean open=controller(bones,"handController"+side)==0;hide(bones,"handClosed"+side,open);hide(bones,"handOpen"+side,!open);}
            int staff=(int)controller(bones,"staffController");hide(bones,"itemHandLeft",staff!=1);hide(bones,"itemHandRight",staff!=-1);hide(bones,"backItem",staff!=0);for(String n:List.of("itemHandLeft","itemHandRight","backItem"))bones.ifPresent(n,b->b.setScale(1.2F,1.2F,1.2F));
            boolean absent=progress<=0||progress>1.15F;hide(bones,"gauntlet",absent);hide(bones,"gauntletUnparented",absent);String[] sequence={"centerRock","sleeve","rightRock1","wrist","pinky","index","thumb","leftRock1","leftRock2","rightRock2"};for(int i=0;i<sequence.length;i++){float wait=i/(float)sequence.length+.2F;hide(bones,sequence[i],progress<wait);hide(bones,sequence[i]+"Unparented",progress>=wait||progress<wait-.1F);}
            if(info.getOrDefaultGeckolibData(ALIVE,true))WildSculptorPose.apply(bones,info.getOrDefaultGeckolibData(AGE,0F));
            WildSculptorPose.correct(bones);
            var beard=info.getGeckolibData(BEARD);if(beard!=null)beard.chain().apply(beard,bones);
        }
    }
    static float controller(BoneSnapshots bones,String name){return -bones.get(name).map(b->b.getTranslateX()).orElse(0F);}
    static void hide(BoneSnapshots bones,String name,boolean hide){bones.ifPresent(name,b->b.skipRender(hide).skipChildrenRender(hide));}
    private static class Boulder extends GeoEntityRenderer<WildBoulder,EntityRenderState>{
        Boulder(EntityRendererProvider.Context c){super(c,new GeoModel<WildBoulder>(){
            public Identifier getModelResource(GeoRenderState s){int mode=s.getOrDefaultGeckolibData(MODE,0);return Jugcraft.id("entity/wilds/"+(mode==WildBoulder.PILLAR?"geomancy_pillar":mode==WildBoulder.SLING?"rock_sling":"boulder_"+s.getOrDefaultGeckolibData(TIER,1)));}
            public Identifier getTextureResource(GeoRenderState s){return s.getOrDefaultGeckolibData(TEXTURE,Identifier.withDefaultNamespace("textures/block/stone.png"));}
            public Identifier getAnimationResource(WildBoulder e){return Jugcraft.id("entity/wilds/rock_sling");}
        });shadowRadius=.7F;}
        @Override public void captureDefaultRenderState(WildBoulder e,Void v,EntityRenderState s,float partial){super.captureDefaultRenderState(e,v,s,partial);s.addGeckolibData(MODE,e.mode());s.addGeckolibData(TIER,e.tier());s.addGeckolibData(HEIGHT,e.height());s.addGeckolibData(AGE,e.tickCount+partial);s.addGeckolibData(CRUMBLE,e.active()?e.crumble():-1);var sprite=net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(e.material()).sprite().contents().name();s.addGeckolibData(TEXTURE,Identifier.fromNamespaceAndPath(sprite.getNamespace(),"textures/"+sprite.getPath()+".png"));}
        @Override public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info,BoneSnapshots bones){super.adjustModelBonesForRender(info,bones);if(info.getOrDefaultGeckolibData(MODE,0)==WildBoulder.PILLAR){for(int i=0;i<64;i++){float fraction=Math.clamp(info.getOrDefaultGeckolibData(HEIGHT,1F)-i,0,1);hide(bones,"tier2_"+i,fraction<=0);if(fraction>0&&fraction<1)bones.ifPresent("tier2_"+i,b->b.setScaleY(fraction));}}else{boolean absent=info.getOrDefaultGeckolibData(CRUMBLE,0)==-1;hide(bones,"root",absent);if(info.getOrDefaultGeckolibData(CRUMBLE,0)>0)bones.ifPresent("root",b->b.setTranslateX(b.getTranslateX()+(float)Math.sin(info.getOrDefaultGeckolibData(AGE,0F)*3)*.3F));}}
    }
}
