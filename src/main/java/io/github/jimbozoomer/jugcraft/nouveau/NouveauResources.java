package io.github.jimbozoomer.jugcraft.nouveau;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;

/** Runtime registries for the supplied audio/particle assets and native potion recipes. */
public final class NouveauResources {
    public static final Map<String,SoundEvent> SOUNDS=new LinkedHashMap<>();
    public static final Map<String,SimpleParticleType> PARTICLES=new LinkedHashMap<>();
    private NouveauResources(){}
    public static JsonArray list(String file){
        try(var stream=NouveauResources.class.getResourceAsStream("/jugcraft/nouveau/"+file+".json")){
            if(stream==null)throw new IllegalStateException("Missing Nouveau resource list: "+file);
            return JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonArray();
        }catch(IOException e){throw new IllegalStateException("Reading Nouveau "+file,e);}
    }
    public static void register(){
        for(var entry:list("sounds")){String name=entry.getAsString();var id=Jugcraft.id("nouveau."+name);SOUNDS.put(name,Registry.register(BuiltInRegistries.SOUND_EVENT,id,SoundEvent.createVariableRangeEvent(id)));}
        for(var entry:list("particles")){String name=entry.getAsString();PARTICLES.put(name,Registry.register(BuiltInRegistries.PARTICLE_TYPE,Jugcraft.id("nouveau/"+name),FabricParticleTypes.simple()));}
        for(String name:List.of("shielding","recovery","freezing","blasting","mana_regen"))for(int tier=0;tier<3;tier++){
            String suffix=tier==1?"_long":tier==2?"_strong":"";int duration=name.equals("blasting")?100:tier==1?9600:tier==2?1800:3600;
            Registry.register(BuiltInRegistries.POTION,Jugcraft.id("nouveau_"+name+suffix),new Potion("nouveau_"+name+suffix,new MobEffectInstance(NouveauStatuses.EFFECTS.get(name),duration,tier==2?1:0)));
        }
        var injections=list("loot_injections");
        LootTableEvents.MODIFY.register((key,table,source,registries)->{
            if(!source.isBuiltin()||!JugNouveau.enabled())return;
            for(var entry:injections){var row=entry.getAsJsonObject();boolean applies=false;for(var id:row.getAsJsonArray("tables"))applies|=id.getAsString().equals(key.identifier().toString());if(!applies)continue;
                addPool(table,row,"common",List.of("source_gem","magebloom_fiber","sourceberry_bush","experience_gem"));
                addPool(table,row,"uncommon",List.of("wilden_horn","wilden_wing","wilden_spike","abjuration_essence","conjuration_essence","manipulation_essence"));
                addPool(table,row,"rare",List.of("greater_experience_gem","wilden_tribute","thread_magic_capacity","thread_spellpower"));
            }
        });
    }
    private static void addPool(net.minecraft.world.level.storage.loot.LootTable.Builder table,JsonObject row,String rarity,List<String> candidates){
        var pool=LootPool.lootPool().setRolls(net.minecraft.core.Holder.direct(new ConstantValue(row.get(rarity+"_rolls").getAsInt()))).when(LootItemRandomChanceCondition.randomChance(row.get(rarity+"_chance").getAsFloat()));
        for(String name:candidates){var item=NouveauCatalog.ITEMS.get(name);if(item!=null)pool.add(LootItem.lootTableItem(item));}table.withPool(pool);
    }
    public static void sound(ServerLevel level,BlockPos at,String name,float volume,float pitch){var sound=SOUNDS.get(name);if(sound!=null)level.playSound(null,at,sound,SoundSource.BLOCKS,volume,pitch);}
}
