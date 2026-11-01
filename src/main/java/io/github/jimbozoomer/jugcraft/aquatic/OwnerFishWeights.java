package io.github.jimbozoomer.jugcraft.aquatic;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/** Optional competition weights, source bounds and fillet thresholds; disabled by default as supplied. */
public final class OwnerFishWeights {
    private OwnerFishWeights(){}
    public static DataComponentType<Float> WEIGHT;
    private static final Map<String,float[]> BOUNDS=new HashMap<>();
    public static void register(){
        WEIGHT=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("aquatic_fish_weight"),DataComponentType.<Float>builder().persistent(Codec.floatRange(.001f,100000)).networkSynchronized(ByteBufCodecs.FLOAT).build());
        for(var v:OwnerBackpacks.source("newest_aquatic.json").getAsJsonArray("fish")){var d=v.getAsJsonObject();if(d.has("min")&&d.has("max"))BOUNDS.put("jugcraft:aquatic_"+d.get("name").getAsString(),new float[]{d.get("min").getAsFloat(),d.get("max").getAsFloat()});}
        BOUNDS.put("minecraft:cod",new float[]{.1f,100});BOUNDS.put("minecraft:salmon",new float[]{.1f,100});
    }
    public static void assign(Iterable<ItemStack> results,RandomSource random){
        if(!JugcraftConfig.option("aquatic.fish_weights"))return;
        for(var stack:results)if(stack.is(ItemTags.FISHES)&&!stack.has(WEIGHT)){
            var bound=BOUNDS.getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),new float[]{.1f,100});
            stack.set(WEIGHT,Math.max(.001f,bound[0]+random.nextFloat()*(bound[1]-bound[0])));
        }
    }
    public static int fillets(ItemStack stack,int fallback){
        var weight=stack.get(WEIGHT);if(weight==null||!Float.isFinite(weight)||weight<=0)return fallback;
        float[] thresholds={10,50,100,150,200,250,300,350,400,450,500,600,700,800,1000};
        for(int i=0;i<thresholds.length;i++)if(weight<thresholds[i])return i+1;return 16;
    }
    public static String description(ItemStack stack){
        var weight=stack.get(WEIGHT);if(weight==null||!Float.isFinite(weight)||weight<=0)return "";
        var bounds=BOUNDS.getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),new float[]{.1f,100});float ratio=weight/bounds[1];
        String size=ratio<=.1f?"Juvenile":ratio<=.2f?"Small":ratio>=.9f?"Massive":ratio>=.8f?"Large":"";
        return (size.isEmpty()?"":size+" — ")+String.format(Locale.ROOT,"%.2f lb",weight);
    }
}
