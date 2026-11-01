package io.github.jimbozoomer.jugcraft.arcane;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Source enhancer effects, applied once per installed enhancer with source time conditions. */
public final class ArcaneEnhancers {
    private static final Map<String,JsonObject> DATA=new LinkedHashMap<>();
    static void load(){for(var e:OwnerBackpacks.source("newest_forbidden.json").getAsJsonObject("enhancers").entrySet())DATA.put("jugcraft:forbidden_"+e.getKey(),e.getValue().getAsJsonObject());}
    public static String name(ItemStack stack){String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();return DATA.containsKey(id)?id:"";}
    public static boolean accepts(ItemStack stack){return !name(stack).isEmpty();}
    static int soulDuration(ItemStack enhancer){double duration=2700;var d=DATA.get(name(enhancer));if(d!=null)for(var entry:d.getAsJsonArray("effects")){var e=entry.getAsJsonObject();if(e.get("type").getAsString().equals("jugcraft:forbidden_multiply_soul_duration")){double m=e.get("multiplier").getAsDouble();if(Double.isFinite(m)&&m>0)duration*=m;}}return (int)Math.clamp(duration,1,72000);}
    static int[] costs(ArcaneRituals.Ritual ritual,List<ItemStack> enhancers,ServerLevel level){
        var values=new double[4];for(int i=0;i<4;i++)values[i]=ritual.essence()[i];
        var applied=new HashSet<String>();
        for(var stack:enhancers){String id=name(stack);if(id.isEmpty()||!applied.add(id))continue;
            for(var entry:DATA.get(id).getAsJsonArray("effects")){
                var effect=entry.getAsJsonObject();if(!effect.get("type").getAsString().equals("jugcraft:forbidden_multiply_required_essence"))continue;
                boolean allowed=true;
                if(effect.has("conditions"))for(var raw:effect.getAsJsonArray("conditions")){var c=raw.getAsJsonObject();long t=Math.floorMod(level.getDefaultClockTime(),24000);if(!c.get("type").getAsString().equals("jugcraft:forbidden_time")||t<c.get("start").getAsInt()||t>c.get("end").getAsInt())allowed=false;}
                int type=List.of("aureal","souls","blood","experience").indexOf(effect.get("essence_type").getAsString());double multiplier=effect.get("multiplier").getAsDouble();
                if(allowed&&type>=0&&Double.isFinite(multiplier)&&multiplier>=0)values[type]*=multiplier;
            }
        }
        var result=new int[4];for(int i=0;i<4;i++)result[i]=(int)Math.clamp(Math.ceil(values[i]),0,100000);return result;
    }
    static boolean satisfies(JsonElement needs,List<ItemStack> enhancers){if(needs==null)return true;if(needs.isJsonArray()){for(var n:needs.getAsJsonArray())if(!satisfies(n,enhancers))return false;return true;}if(!needs.isJsonPrimitive())return false;return enhancers.stream().anyMatch(s->name(s).equals(needs.getAsString()));}
}
