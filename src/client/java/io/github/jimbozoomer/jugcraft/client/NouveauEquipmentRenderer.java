package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.*;
import com.geckolib.renderer.base.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

/** Dye variants and native weapon action pose the owner's equipment geometry. */
public final class NouveauEquipmentRenderer {
    private static final DataTicket<String> TEXTURE=DataTicket.create("nouveau_equipment_texture",String.class);
    private static final DataTicket<Float> DRAW=DataTicket.create("nouveau_equipment_draw",Float.class),TIME=DataTicket.create("nouveau_equipment_time",Float.class);
    private NouveauEquipmentRenderer(){}
    public static String dye(ItemStack stack){var color=stack.get(DataComponents.DYED_COLOR);if(color==null)return "purple";int rgb=color.rgb(),best=Integer.MAX_VALUE;DyeColor picked=DyeColor.PURPLE;
        for(var candidate:DyeColor.values()){int c=candidate.getTextureDiffuseColor(),r=(rgb>>16&255)-(c>>16&255),g=(rgb>>8&255)-(c>>8&255),b=(rgb&255)-(c&255),distance=r*r+g*g+b*b;if(distance<best){best=distance;picked=candidate;}}return picked.getName();}
    public static <T extends Item & NouveauInstruments.Visual> GeoItemRenderer<T> item(NouveauCatalog.Entry entry){
        return new GeoItemRenderer<>(new GeoModel<T>(){
            public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("item/nouveau/"+entry.geo()+"");}
            public Identifier getTextureResource(GeoRenderState s){String texture=s.getGeckolibData(TEXTURE);return Jugcraft.id("textures/nouveau/item/"+(texture==null?entry.geo():texture)+".png");}
            public Identifier getAnimationResource(T item){return Jugcraft.id("item/nouveau/"+entry.geo()+"");}
        }){
            @Override public void addRenderData(T item,GeoItemRenderer.RenderData data,GeoRenderState state,float partial){
                var owner=data.itemOwner() instanceof net.minecraft.world.entity.LivingEntity living?living:null;var stack=data.itemStack();String texture=switch(entry.geo()){case "sword"->"enchanters_sword";case "spellbook_closed"->"spellbook_"+dye(stack);case "enchanters_rod"->owner instanceof Player p&&p.fishing!=null?"enchanters_rod_cast":"enchanters_rod_stowed";default->entry.geo();};
                state.addGeckolibData(TEXTURE,texture);float draw=owner!=null&&owner.getUseItem()==stack?Math.min(1,(owner.getTicksUsingItem()+partial)/20F):0;
                if(entry.source().equals("spell_crossbow")&&CrossbowItem.isCharged(stack))draw=1;state.addGeckolibData(DRAW,draw);state.addGeckolibData(TIME,Minecraft.getInstance().level==null?0F:Minecraft.getInstance().level.getGameTime()+partial);
            }
            @Override public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> info,BoneSnapshots bones){super.adjustModelBonesForRender(info,bones);Float draw=info.getGeckolibData(DRAW),time=info.getGeckolibData(TIME);float amount=draw==null?0:draw;
                if(entry.source().equals("spell_bow")){bones.ifPresent("bow_top",b->b.setRotX(amount*.38F));bones.ifPresent("bow_bot",b->b.setRotX(-amount*.38F));}
                if(entry.source().equals("spell_crossbow")){bones.ifPresent("bow_right",b->b.setRotY(amount*.4F));bones.ifPresent("bow_left",b->b.setRotY(-amount*.4F));}
                if(entry.source().equals("wand")&&time!=null)bones.ifPresent("gem",b->b.setRotY(time*.035F));
            }
        };
    }
    public static GeoArmorRenderer<NouveauGear.Armor,HumanoidRenderState> armor(String family){String model=family.equals("sorcerer")?"light_armor":family.equals("arcanist")?"medium_armor":"heavy_armor";
        return new GeoArmorRenderer<>(new GeoModel<NouveauGear.Armor>(){
            public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("armor/nouveau/"+model+"");}
            public Identifier getTextureResource(GeoRenderState s){var dye=s.getGeckolibData(TEXTURE);return Jugcraft.id("textures/nouveau/item/"+model+"/"+model+"_"+(dye==null?"purple":dye)+".png");}
            public Identifier getAnimationResource(NouveauGear.Armor a){return Jugcraft.id("armor/nouveau/empty");}
        }){
            @Override public void addRenderData(NouveauGear.Armor armor,RenderData data,HumanoidRenderState state,float partial){state.addGeckolibData(TEXTURE,dye(data.itemStack()));}
            @Override public void adjustModelBonesForRender(RenderPassInfo<HumanoidRenderState> info,BoneSnapshots bones){super.adjustModelBonesForRender(info,bones);for(String bone:new String[]{"armorRightArmSlim","armorLeftArmSlim","ArmorLeftArmSlim"})bones.ifPresent(bone,b->b.skipRender(true));}
        };
    }
}
