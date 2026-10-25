package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Shared Focus potions, original readable lore, playable discs and the two source tinctures. */
public final class JimtaSpellbookLore {
    public static Holder<MobEffect> INSTANT_MANA;
    public static void register(){
        INSTANT_MANA=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Jugcraft.id("jimtafacts_instant_mana"),new Mana());
        JimtaSpellbooks.data("music_sounds").keySet().forEach(name->Registry.register(BuiltInRegistries.SOUND_EVENT,Jugcraft.id(name),SoundEvent.createVariableRangeEvent(Jugcraft.id(name))));
        var data=JimtaSpellbooks.data("lore");int amp=0;
        for(var name:data.getAsJsonArray("potions")){String n=name.getAsString();Registry.registerForHolder(BuiltInRegistries.POTION,Jugcraft.id(JimtaSpellbooks.PREFIX+n),new Potion("jimta_spellbooks_"+n,new MobEffectInstance(INSTANT_MANA,1,amp++)));}
        for(var e:data.getAsJsonArray("materials"))registerItem(e.getAsString(),false);
        for(var e:data.getAsJsonArray("special"))registerItem(e.getAsString(),true);
    }
    private static void registerItem(String name,boolean special){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(JimtaSpellbooks.PREFIX+name));var p=new Item.Properties().setId(key);Item item;
        if(name.startsWith("music_disc_")){String song=name.substring(11);p.stacksTo(1).jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG,Jugcraft.id("jimtafacts/"+song)));item=new Item(p);}
        else if(name.startsWith("archevoker_logbook")){var pages=new ArrayList<Filterable<Component>>();int[] days={2,14,31,73};for(int i=1;i<=4;i++){for(int part=1;part<=2;part++){var text=Component.empty();if(part==1)text.append(Component.translatable("item.jugcraft.jimta_spellbooks_archevoker_log.header")).append(days[i-1]+":\n\n");text.append(Component.translatable("item.jugcraft.jimta_spellbooks_archevoker_log.entry_"+i+"."+part));if(name.endsWith("untranslated"))text.withStyle(net.minecraft.ChatFormatting.OBFUSCATED);pages.add(Filterable.passThrough(text));}}item=new WrittenBookItem(p.stacksTo(1).component(DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(Filterable.passThrough("Archevoker's Logbook"),"Archevoker",0,pages,true)));}
        else if(name.equals("netherward_tincture"))item=new Netherward(p.stacksTo(16));
        else if(name.equals("tincture_of_forgetfulness"))item=new Forgetfulness(p.stacksTo(16));
        else item=new Item(p);
        JimtaSpellbooks.ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,key,item));
    }
    private static final class Mana extends MobEffect {
        Mana(){super(MobEffectCategory.BENEFICIAL,0x6fbce8);}
        @Override public boolean isInstantaneous(){return true;}
        @Override public void applyInstantaneousEffect(ServerLevel level,Entity source,Entity indirect,LivingEntity target,int amplifier,double scale){if(JimtaSpellbooks.enabled()&&target instanceof ServerPlayer player){var pool=ConcordanceProgress.focus(player);int restored=(int)Math.ceil((amplifier+1)*(5+pool.capacity()*.05)*Math.clamp(scale,0,1));player.setAttached(JugcraftConcordance.FOCUS,pool.gain(ConcordanceProgress.now(level),restored));}}
        @Override public boolean shouldApplyEffectTickThisTick(int duration,int amplifier){return true;}
        @Override public boolean applyEffectTick(ServerLevel level,LivingEntity target,int amplifier){applyInstantaneousEffect(level,null,null,target,amplifier,1);return false;}
    }
    private static final class Netherward extends Item {
        Netherward(Properties p){super(p);}
        @Override public InteractionResult interactLivingEntity(ItemStack stack,Player actor,LivingEntity target,InteractionHand hand){
            if(!(actor instanceof ServerPlayer player))return InteractionResult.SUCCESS;if(!JimtaSpellbooks.enabled()||!Authority.mayChange(player.level(),player,target.blockPosition()))return InteractionResult.FAIL;
            if(target instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin piglin)piglin.setImmuneToZombification(true);else if(target instanceof net.minecraft.world.entity.monster.hoglin.Hoglin hoglin)hoglin.setImmuneToZombification(true);else return InteractionResult.PASS;
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA,200));target.playSound(SoundEvents.INK_SAC_USE);stack.consume(1,player);return InteractionResult.SUCCESS;
        }
    }
    private static final class Forgetfulness extends Item {
        Forgetfulness(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext c){if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;if(!JimtaSpellbooks.enabled()||!Authority.mayChange(p.level(),p,c.getClickedPos()))return InteractionResult.FAIL;
            if(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof JimtaWorkshops.Node vault&&vault.forget(p.getUUID())){c.getItemInHand().consume(1,p);c.getLevel().playSound(null,c.getClickedPos(),SoundEvents.HONEY_DRINK.value(),SoundSource.BLOCKS,1,1);return InteractionResult.SUCCESS;}return InteractionResult.PASS;}
    }
}
