package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.*;
import net.minecraft.world.phys.*;

/** Readable source lore, bounded exploration maps and the source Cinderous Soulcaller ritual. */
public final class JimtaExploration {
    public static final Set<String> DESTINATIONS=Set.of("ancient_battleground","catacombs","citadel","evoker_fort","ice_spider_den","impaled_icebreaker","mangrove_hut","mountain_tower","pyromancer_tower");
    public static DataComponentType<String> TARGET;
    public static void register(){
        TARGET=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("jimtafacts_map_destination"),DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
        for(String name:List.of("furled_map","furled_map_ancient","furled_map_citadel","furled_map_ice_spider_den","wayward_compass","ruined_book","unchained_book","cinderous_soulcaller")){
            var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(JimtaSpellbooks.PREFIX+name));var p=new Item.Properties().setId(key).stacksTo(1);
            Item item=name.equals("cinderous_soulcaller")?new Soulcaller(p):name.endsWith("book")?new Lore(p,name):new Map(p,name);
            JimtaSpellbooks.ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,key,item));
        }
    }
    private static final class Map extends Item {
        final String kind;Map(Properties p,String kind){super(p);this.kind=kind;}
        @Override public InteractionResult use(Level level,Player actor,InteractionHand hand){if(!(actor instanceof ServerPlayer p))return InteractionResult.SUCCESS;if(!JimtaSpellbooks.enabled()||p.isSpectator()||!RateGate.allow(p,"jimtafacts_map_search",200))return InteractionResult.FAIL;
            var held=p.getItemInHand(hand);String dest=held.getOrDefault(TARGET,switch(kind){case "furled_map_citadel"->"citadel";case "furled_map_ice_spider_den"->"ice_spider_den";case "furled_map_ancient"->"ancient_battleground";case "wayward_compass"->"catacombs";default->"impaled_icebreaker";});
            if(!DESTINATIONS.contains(dest))return InteractionResult.FAIL;boolean nether=dest.equals("citadel")||dest.equals("ancient_battleground");if(nether!=level.dimension().equals(Level.NETHER)){p.sendOverlayMessage(Component.literal("Unfurl this chart in the "+(nether?"Nether":"Overworld")+"."));return InteractionResult.FAIL;}
            if(kind.equals("wayward_compass")&&held.has(DataComponents.LODESTONE_TRACKER))return InteractionResult.SUCCESS;
            var target=p.level().findNearestMapStructure(TagKey.create(Registries.STRUCTURE,Jugcraft.id("jimtafacts/spellbooks/locate/"+dest)),p.blockPosition(),50,false);
            if(target==null){p.sendOverlayMessage(Component.literal("No matching site was found nearby. Keep this chart and try farther away."));return InteractionResult.FAIL;}
            if(kind.equals("wayward_compass")){held.set(DataComponents.LODESTONE_TRACKER,new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(),target)),false));return InteractionResult.SUCCESS;}
            var map=MapItem.create(p.level(),target.getX(),target.getZ(),(byte)2,true,true);MapItem.renderBiomePreviewMap(p.level(),map);MapItemSavedData.addTargetDecoration(map,target,"+",MapDecorationTypes.RED_X);map.set(DataComponents.ITEM_NAME,Component.literal(dest.replace('_',' ')+" exploration chart"));held.consume(1,p);if(held.isEmpty())p.setItemInHand(hand,map);else if(!p.getInventory().add(map))p.drop(map,false,net.minecraft.util.Prediction.SERVER_ONLY);return InteractionResult.SUCCESS;
        }
    }
    private static final class Lore extends WrittenBookItem {
        final String name;Lore(Properties p,String name){super(p);this.name=name;}
        @Override public InteractionResult use(Level level,Player actor,InteractionHand hand){if(actor instanceof ServerPlayer p){List<Component> pages=new ArrayList<>();if(name.equals("unchained_book")){for(var line:JimtaSpellbooks.data("exploration").getAsJsonArray("unchained_pages"))pages.add(Component.literal(line.getAsString()));}else{
                pages.add(Component.literal("In the beginning, only darkness.\nWhy did she leave?\nDaughter of Darkness, Bringer of Light.\nOh how the Void must hate you,\nArathyll, Arathyll, Arathyll..."));for(int i=1;i<10;i++)pages.add(Component.literal("Darkness"));if(!p.hasEffect(JimtaWardSpells.EFFECTS.get("planar_sight")))pages.replaceAll(c->c.copy().withStyle(net.minecraft.ChatFormatting.OBFUSCATED));}
            p.getItemInHand(hand).set(DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(Filterable.passThrough(name.equals("ruined_book")?"Ruined Book":"Unchained Book"),"???",0,pages.stream().map(Filterable::passThrough).toList(),true));}
            return super.use(level,actor,hand);}
    }
    private static final class Soulcaller extends Item {
        Soulcaller(Properties p){super(p.fireResistant());}
        @Override public InteractionResult use(Level level,Player actor,InteractionHand hand){if(!(actor instanceof ServerPlayer p))return InteractionResult.SUCCESS;if(!JimtaSpellbooks.enabled()||p.isSpectator()||!RateGate.allow(p,"jimtafacts_soulcaller",80))return InteractionResult.FAIL;
            BlockPos found=null;double nearest=Double.MAX_VALUE;for(BlockPos at:BlockPos.betweenClosed(p.blockPosition().offset(-22,-22,-22),p.blockPosition().offset(22,2,22))){if(!level.hasChunkAt(at)||!level.getBlockState(at).is(JimtaWorkshops.BLOCKS.get("cinderous_soul_rune")))continue;double distance=p.distanceToSqr(Vec3.atCenterOf(at));if(distance<nearest){nearest=distance;found=at.immutable();}}
            if(found==null||!Authority.mayChange(p.level(),p,found)||!level.getEntitiesOfClass(JimtaWizard.class,new AABB(found).inflate(40),e->e.kind().equals("fire_boss")).isEmpty()){p.sendOverlayMessage(Component.literal("The Soulcaller needs an unoccupied Cinderous Soul Rune nearby."));return InteractionResult.FAIL;}
            var boss=JimtaWizard.TYPES.get("fire_boss").create(p.level(),EntitySpawnReason.MOB_SUMMONED);if(boss==null)return InteractionResult.FAIL;boss.setPos(Vec3.atBottomCenterOf(found.above()));boss.setTarget(p);boss.setPersistenceRequired();if(!level.noCollision(boss)||!p.level().addFreshEntity(boss))return InteractionResult.FAIL;p.getItemInHand(hand).consume(1,p);level.playSound(null,found,net.minecraft.sounds.SoundEvents.BELL_BLOCK,net.minecraft.sounds.SoundSource.BLOCKS,6,.5F);return InteractionResult.SUCCESS;
        }
    }
}
