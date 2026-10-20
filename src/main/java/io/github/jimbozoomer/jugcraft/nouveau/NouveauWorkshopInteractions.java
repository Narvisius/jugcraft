package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.concordance.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;

/** Physical workbench actions complement the picker: place, inscribe, socket and retrieve. */
public final class NouveauWorkshopInteractions {
    private NouveauWorkshopInteractions(){}
    public static InteractionResult use(ServerPlayer p,NouveauWorkshop b,InteractionHand hand){
        if(!b.mayUse(p))return InteractionResult.FAIL;var held=p.getItemInHand(hand);String kind=b.kind();
        if(held.is(JugNouveau.WAND))return InteractionResult.PASS;
        if(kind.equals("ritual_brazier")&&!b.ritual().isEmpty()&&!b.ritualRunning()){
            if(held.isEmpty()){if(!NouveauSource.pay(p.level(),p,b,5))return InteractionResult.FAIL;b.startRitual();return InteractionResult.SUCCESS;}
            for(int i=1;i<9;i++)if(b.getItem(i).isEmpty()){b.setItem(i,held.split(1));return InteractionResult.SUCCESS;}return InteractionResult.FAIL;
        }
        if(kind.equals("item_detector")){
            if(!held.isEmpty()){b.filter(held.copyWithCount(1));p.sendOverlayMessage(Component.literal("Tracking "+held.getHoverName().getString()));}
            else b.threshold(b.threshold()+(p.isShiftKeyDown()?16:1));return InteractionResult.SUCCESS;
        }
        if(kind.equals("spell_sensor")&&held.has(JugcraftConcordance.INSCRIPTION)){b.filter(held.copyWithCount(1));return InteractionResult.SUCCESS;}
        if(b.storage()&&(held.is(NouveauCatalog.item("allow_scroll"))||held.is(NouveauCatalog.item("deny_scroll")))){b.filter(held.copyWithCount(1));return InteractionResult.SUCCESS;}
        if(kind.equals("wixie_cauldron")&&!held.isEmpty()){b.filter(held.copyWithCount(1));p.sendOverlayMessage(Component.literal("Wixie target: "+held.getHoverName().getString()));return InteractionResult.SUCCESS;}
        if(kind.equals("scribes_table")){
            var target=b.getItem(0);
            if(target.is(NouveauCatalog.item("void_jar"))&&!held.isEmpty()){NouveauVoiding.toggle(target,held);b.setChanged();return InteractionResult.SUCCESS;}
            if(held.is(NouveauCatalog.item("abjuration_essence"))&&target.getOrDefault(NouveauGear.GLAMOUR,false)){target.remove(NouveauGear.GLAMOUR);held.consume(1,p);b.setChanged();return InteractionResult.SUCCESS;}
            if(p.isShiftKeyDown()&&(target.is(NouveauCatalog.item("allow_scroll"))||target.is(NouveauCatalog.item("deny_scroll")))&&!held.isEmpty()){
                var items=new ArrayList<>(target.getOrDefault(NouveauUtilities.FILTER,List.of()));String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()).toString();if(!items.remove(id)&&items.size()<16)items.add(id);target.set(NouveauUtilities.FILTER,List.copyOf(items));b.setChanged();return InteractionResult.SUCCESS;
            }
            if(p.isShiftKeyDown()&&held.has(JugcraftConcordance.INSCRIPTION)&&!target.isEmpty()){
                boolean blank=target.is(NouveauCatalog.item("blank_parchment"));
                if(!blank&&!(target.getItem() instanceof NouveauInstruments.Visual)&&!(target.getItem() instanceof NouveauGear.Armor)&&!target.is(NouveauCatalog.item("spell_parchment")))return InteractionResult.FAIL;
                if(target.getCount()!=1)return InteractionResult.FAIL;
                var result=blank?new ItemStack(NouveauCatalog.item("spell_parchment")):target.copy();result.set(JugcraftConcordance.INSCRIPTION,held.get(JugcraftConcordance.INSCRIPTION));b.setItem(0,result);p.sendOverlayMessage(Component.literal("Spell inscribed"));return InteractionResult.SUCCESS;
            }
        }
        if(kind.equals("alteration_table")&&b.getItem(0).getItem() instanceof NouveauGear.Armor){
            var armor=b.getItem(0);var threads=new ArrayList<>(armor.getOrDefault(NouveauGear.THREADS,List.of()));
            if(held.getItem() instanceof NouveauGear.Gear thread&&thread.entry.kind().equals("thread")){
                String name=thread.entry.source().substring(7);int slot=threads.size();if(slot>=1+armor.getOrDefault(NouveauGear.ARMOR_TIER,0)||threads.contains(name)||Set.of("gliding","undying").contains(name)&&NouveauThreads.socket(armor,slot)<3)return InteractionResult.FAIL;
                threads.add(name);armor.set(NouveauGear.THREADS,List.copyOf(threads));held.consume(1,p);b.setChanged();return InteractionResult.SUCCESS;
            }
            if(held.isEmpty()&&p.isShiftKeyDown()&&!threads.isEmpty()){String removed=threads.removeLast();armor.set(NouveauGear.THREADS,List.copyOf(threads));var thread=new ItemStack(NouveauCatalog.item("thread_"+removed));if(!p.getInventory().add(thread))p.drop(thread,false,net.minecraft.util.Prediction.SERVER_ONLY);b.setChanged();return InteractionResult.SUCCESS;}
        }
        if(Set.of("arcane_pedestal","arcane_platform","scribes_table","alteration_table","spell_prism","basic_spell_turret","spell_turret","rotating_spell_turret","timer_spell_turret").contains(kind)){
            if(held.isEmpty()&&!b.getItem(0).isEmpty()){p.setItemInHand(hand,b.removeItem(0,b.getItem(0).getCount()));return InteractionResult.SUCCESS;}
            if(!held.isEmpty()&&b.getItem(0).isEmpty()){b.setItem(0,held.split(1));return InteractionResult.SUCCESS;}
        }
        return InteractionResult.PASS;
    }
}
