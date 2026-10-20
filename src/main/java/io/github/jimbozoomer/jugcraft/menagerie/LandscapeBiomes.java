package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.*;

/** Additional seeded regional choices, keeping the existing climate table and designed-world biome source. */
public final class LandscapeBiomes {
    public record Entry(ResourceKey<Biome> key,String source,float temperature,float downfall){
        boolean cave(){return source.startsWith("cavern_")||source.startsWith("cave/")||source.equals("glowing_grotto")||source.equals("spider_nest");}
        boolean end(){return source.startsWith("end_");}
        boolean nether(){return Set.of("erupting_inferno","visceral_heap","withered_abyss","undergrowth","crystalline_chasm").contains(source);}
        boolean water(){return source.contains("ocean")||source.contains("river");}
        boolean shore(){return source.contains("beach");}
    }
    public static final List<Entry> ENTRIES=load();
    private LandscapeBiomes(){}
    private static List<Entry> load(){
        var entries=new ArrayList<Entry>();
        try(var in=LandscapeBiomes.class.getResourceAsStream("/jugcraft/menagerie/landscape.json")){
            if(in==null)return List.of();var root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var raw:root.getAsJsonArray("biomes")){var d=raw.getAsJsonObject();entries.add(new Entry(ResourceKey.create(Registries.BIOME,Identifier.parse(d.get("id").getAsString())),d.get("source").getAsString(),d.get("temperature").getAsFloat(),d.get("downfall").getAsFloat()));}
        }catch(IOException e){throw new IllegalStateException(e);}for(String name:CaveWorkshop.CAVES)entries.add(new Entry(ResourceKey.create(Registries.BIOME,io.github.jimbozoomer.jugcraft.Jugcraft.id("cavern_"+name)),"cavern_"+name,.5F,.5F));return List.copyOf(entries);
    }
    public static Set<ResourceKey<Biome>> overworld(){var out=new LinkedHashSet<ResourceKey<Biome>>();for(var e:ENTRIES)if(!e.end()&&!e.nether())out.add(e.key());return out;}
    /** Cache per biome source: no mutable current-world singleton or post-generation rewriting. */
    public static final class Choices {
        private final Map<ResourceKey<Biome>,Holder<Biome>> holders=new HashMap<>();
        public Choices(Set<Holder<Biome>> possible){possible.forEach(b->b.unwrapKey().ifPresent(k->holders.put(k,b)));}
        public Holder<Biome> pick(Holder<Biome> fallback,long fingerprint,int quartX,int quartZ,Climate.TargetPoint target){
            long cell=mix(fingerprint^((long)Math.floorDiv(quartX,192)*0x9E3779B97F4A7C15L)^((long)Math.floorDiv(quartZ,192)*0xC2B2AE3D27D4EB4FL));
            if(target.depth()>Climate.quantizeCoord(.5F)&&(cell&3)==0){
                var key=ResourceKey.create(Registries.BIOME,io.github.jimbozoomer.jugcraft.Jugcraft.id("cavern_"+CaveWorkshop.CAVES.get(Math.floorMod((int)(cell>>>32),6))));
                if(holders.containsKey(key))return holders.get(key);
            }
            if((cell&3)!=0)return fallback; // Existing regions remain the majority of the landscape.
            // These rare ocean islands have their own climate/terrain contract.
            // Regional land choices must not consume their only nearby occurrence.
            if(fallback.is(net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS)
                    ||fallback.is(ResourceKey.create(Registries.BIOME,io.github.jimbozoomer.jugcraft.Jugcraft.id("flower_isle"))))return fallback;
            boolean cave=target.depth()>Climate.quantizeCoord(.35F),water=fallback.is(BiomeTags.IS_OCEAN)||fallback.is(BiomeTags.IS_RIVER),shore=fallback.is(BiomeTags.IS_BEACH);
            float temperature=fallback.value().getBaseTemperature();float rain=(Climate.unquantizeCoord(target.humidity())+1)/2;
            var candidates=new ArrayList<Entry>();double closest=Double.POSITIVE_INFINITY;
            for(var e:ENTRIES){
                if(e.source().startsWith("cavern_")||e.end()||e.nether()||e.cave()!=cave||e.water()!=water||!cave&&e.shore()!=shore||!holders.containsKey(e.key()))continue;
                double difference=Math.abs(e.temperature()-temperature)+Math.abs(e.downfall()-rain)*.4;
                if(difference<closest){closest=difference;}
            }
            for(var e:ENTRIES)if(!e.source().startsWith("cavern_")&&!e.end()&&!e.nether()&&e.cave()==cave&&e.water()==water&&(cave||e.shore()==shore)&&holders.containsKey(e.key())&&Math.abs(e.temperature()-temperature)+Math.abs(e.downfall()-rain)*.4<=closest+.18)candidates.add(e);
            if(candidates.isEmpty())return fallback;
            return holders.get(candidates.get(Math.floorMod((int)(cell>>>32),candidates.size())).key());
        }
    }
    private static long mix(long x){x^=x>>>33;x*=0xff51afd7ed558ccdL;x^=x>>>33;x*=0xc4ceb9fe1a85ec53L;return x^(x>>>33);}
}
