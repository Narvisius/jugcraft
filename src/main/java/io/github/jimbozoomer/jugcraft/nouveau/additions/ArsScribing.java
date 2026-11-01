package io.github.jimbozoomer.jugcraft.nouveau.additions;
import io.github.jimbozoomer.jugcraft.concordance.compose.Instrument;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.compose.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Automation copies spell instructions, never gear contents; consumable casts must be paid during inscription. */
public final class ArsScribing {
    public static Parchment PARCHMENT;
    public static DataComponentType<Inscription> PREPAID;
    private static final Instrument LIMITS=new Instrument("jugcraft:ars_parchment","jugcraft:ars_additions_imbued_spell_parchment",32,48,256,2,2400);
    private ArsScribing(){}
    public static void register(){
        PREPAID=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("ars_prepaid_spell"),DataComponentType.<Inscription>builder().persistent(Inscription.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(Inscription.CODEC)).build());
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("ars_additions_imbued_spell_parchment"));
        PARCHMENT=Registry.register(BuiltInRegistries.ITEM,key,new Parchment(new Item.Properties().setId(key).stacksTo(16)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->e.accept(PARCHMENT));
        for(var entry:Map.of("codex_entry",55,"lost_codex_entry",160,"ancient_codex_entry",480).entrySet()){
            var codexKey=ResourceKey.create(Registries.ITEM,Jugcraft.id("ars_additions_"+entry.getKey()));
            var item=Registry.register(BuiltInRegistries.ITEM,codexKey,new Item(new Item.Properties().setId(codexKey).stacksTo(1)){
                @Override public boolean isFoil(ItemStack stack){return true;}
                @Override public InteractionResult use(Level l,Player user,InteractionHand hand){
                    if(user instanceof ServerPlayer p&&JugNouveau.enabled()&&p.isAlive()&&!p.isSpectator()){
                        // Equipment and its glyph vocabulary are already accessible in Jugcraft.
                        // Preserve the supplied already-known fallback instead of reinstating a research gate.
                        p.giveExperiencePoints(entry.getValue());p.getItemInHand(hand).consume(1,p);
                        p.sendOverlayMessage(Component.literal("Your glyphs are already available. Recovered "+entry.getValue()+" experience."));
                    }return InteractionResult.SUCCESS;
                }
            });CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->e.accept(item));
        }
    }
    private static Plan plan(Inscription inscription){return inscription==null?null:io.github.jimbozoomer.jugcraft.concordance.compose.Compiler.compileAuthored(inscription.text(),ConcordanceData.rules().catalog(),LIMITS).plan();}
    public static final class Parchment extends Item {
        Parchment(Properties p){super(p);}
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand){
            var stack=p.getItemInHand(hand);var paid=stack.get(PREPAID);
            if(!JugNouveau.enabled()||paid==null||!paid.equals(stack.get(JugcraftConcordance.INSCRIPTION)))return InteractionResult.FAIL;
            p.startUsingItem(hand);return InteractionResult.SUCCESS;
        }
        @Override public int getUseDuration(ItemStack stack,LivingEntity entity){var plan=plan(stack.get(PREPAID));return plan==null?20:Math.clamp(plan.focus()*10,10,1200);}
        @Override public ItemStack finishUsingItem(ItemStack stack,Level l,LivingEntity user){
            if(user instanceof ServerPlayer p&&JugNouveau.enabled()&&p.isAlive()&&!p.isSpectator()){
                var paid=stack.get(PREPAID);var plan=plan(paid);
                if(plan!=null&&paid.equals(stack.get(JugcraftConcordance.INSCRIPTION))&&RateGate.allow(p,"ars_parchment",Math.max(10,plan.cooldown()))){
                    var outcome=ComposedSpells.perform(p.level(),p,plan,Cause.of(p.getUUID(),Cause.Origin.SPELL,"jugcraft:ars_imbued_parchment",ConcordanceEffects.nextSerial()),new Ledger(plan.limits()));
                    if(outcome.applied())stack.consume(1,p);
                }
            }return stack;
        }
    }
    /** Called once per second before ordinary imbuement recipes. A single visible pedestal supplies the master. */
    public static boolean tick(ServerLevel level,ServerPlayer p,NouveauWorkshop chamber){
        if(!chamber.kind().equals("imbuement_chamber")||!chamber.selected().isEmpty()||!chamber.permitted(p))return false;
        var input=chamber.getItem(0);boolean blank=input.is(NouveauCatalog.item("blank_parchment")),imbued=input.is(PARCHMENT);
        if(input.getItem() instanceof ArsAdditionCharms.Charm charm){
            int held=input.getOrDefault(ArsAdditionCharms.CHARGES,0),missing=charm.kind.capacity-held;
            if(missing<=0)return false;
            int credit=input.getOrDefault(ArsAdditionCharms.SOURCE_CREDIT,0);
            int price=Math.max(0,(missing*charm.kind.sourcePerCharge-credit+99)/100);
            var output=input.copyWithCount(1);output.set(ArsAdditionCharms.CHARGES,charm.kind.capacity);
            output.set(ArsAdditionCharms.SOURCE_CREDIT,price*100+credit-missing*charm.kind.sourcePerCharge);
            if(!chamber.canOutput(output)){chamber.status("Charm recharge output full");chamber.setActive(false);return true;}
            if(!NouveauSource.pay(level,p,chamber,price)){chamber.status("Charm recharge needs "+price+" Ley");chamber.setActive(false);return true;}
            input.shrink(1);chamber.output(output);chamber.setChanged();chamber.setActive(true);chamber.status("Charm recharged");return true;
        }
        // The original scroll recipe charges an already written ordinary parchment.
        if(input.is(NouveauCatalog.item("spell_parchment"))&&input.has(JugcraftConcordance.INSCRIPTION)){
            var inscription=input.get(JugcraftConcordance.INSCRIPTION);var scrollPlan=plan(inscription);
            if(scrollPlan==null){chamber.status("Scroll inscription is invalid");chamber.setActive(false);return true;}
            var output=new ItemStack(PARCHMENT);output.set(JugcraftConcordance.INSCRIPTION,inscription);output.set(PREPAID,inscription);
            int price=Math.max(1,scrollPlan.focus());
            if(!chamber.canOutput(output)){chamber.status("Imbued scroll output full");chamber.setActive(false);return true;}
            if(!NouveauSource.pay(level,p,chamber,price)){chamber.status("Imbuing needs "+price+" Ley");chamber.setActive(false);return true;}
            input.shrink(1);chamber.output(output);chamber.setChanged();chamber.setActive(true);chamber.status("Imbued a prepaid single-use spell");return true;
        }
        if(!blank&&!imbued&&!(input.getItem() instanceof NouveauInstruments.Visual)&&!(input.getItem() instanceof NouveauGear.Armor))return false;
        Inscription master=null;
        for(var pos:BlockPos.betweenClosed(chamber.getBlockPos().offset(-3,0,-3),chamber.getBlockPos().offset(3,0,3))){
            if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof NouveauWorkshop pedestal)||!pedestal.pedestal()||!pedestal.permitted(p))continue;
            var inscription=pedestal.getItem(0).get(JugcraftConcordance.INSCRIPTION);if(inscription==null)continue;
            if(master!=null&&!master.equals(inscription)){chamber.status("Use one distinct master spell on the pedestals");chamber.setActive(false);return true;}master=inscription;
        }
        if(master==null)return false;
        if(master.equals(input.get(JugcraftConcordance.INSCRIPTION))&&(!imbued||master.equals(input.get(PREPAID))))return false;
        var plan=plan(master);if(plan==null){chamber.status("Master spell is invalid");chamber.setActive(false);return true;}
        var output=blank?new ItemStack(NouveauCatalog.item("spell_parchment")):input.copyWithCount(1);
        output.set(JugcraftConcordance.INSCRIPTION,master);if(imbued)output.set(PREPAID,master);
        int price=Math.max(1,plan.focus());
        if(!chamber.canOutput(output)){chamber.status("Scribing output full");chamber.setActive(false);return true;}
        if(!NouveauSource.pay(level,p,chamber,price)){chamber.status("Scribing needs "+price+" Ley");chamber.setActive(false);return true;}
        input.shrink(1);chamber.output(output);chamber.setChanged();chamber.setActive(true);chamber.status(imbued?"Imbued a prepaid single-use spell":"Copied the pedestal spell");return true;
    }
}
