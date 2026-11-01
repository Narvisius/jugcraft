package io.github.jimbozoomer.jugcraft.arcane;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauRecipes;
import java.util.*;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;

/** Reloadable original ritual requirements. Recipe matching is bounded and side-effect free. */
public final class ArcaneRituals extends SimpleJsonResourceReloadListener<JsonElement> {
    public static final ArcaneRituals INSTANCE=new ArcaneRituals();
    private static volatile List<Ritual> recipes=List.of();
    private ArcaneRituals(){super(ExtraCodecs.JSON,FileToIdConverter.json("arcane_ritual"));}
    public record Ritual(String id,JsonObject data,List<JsonElement> inputs,int tier,boolean exact,int duration,int[] essence){
        public int upgrade(){return data.getAsJsonObject("result").has("result_tier")?data.getAsJsonObject("result").get("result_tier").getAsInt():0;}
        public ItemStack output(net.minecraft.core.HolderLookup.Provider registries,ItemStack main){var result=NouveauRecipes.stack(data.getAsJsonObject("result").get("result_item"),registries);return !result.isEmpty()&&data.getAsJsonObject("result").get("type").getAsString().endsWith("transmute_input")?main.transmuteCopy(result.getItem(),1):result;}
        public boolean matches(ItemStack main,List<ItemStack> slots,int currentTier,int[] stored,int[] cost,List<ItemStack> enhancers){
            if(exact?currentTier!=tier:currentTier<tier)return false;
            if(!NouveauRecipes.ingredient(data.get("main_ingredient"),main))return false;
            for(int i=0;i<4;i++)if(stored[i]<cost[i])return false;
            if(!ArcaneEnhancers.satisfies(data.get("enhancers"),enhancers))return false;
            if(slots.stream().filter(s->!s.isEmpty()).count()!=inputs.size())return false;
            return assign(0,slots,new boolean[8],new int[]{4096});
        }
        private boolean assign(int index,List<ItemStack> slots,boolean[] used,int[] budget){
            if(--budget[0]<0)return false;if(index==inputs.size())return true;
            for(int i=0;i<slots.size();i++)if(!used[i]&&!slots.get(i).isEmpty()&&NouveauRecipes.ingredient(inputs.get(index),slots.get(i))){used[i]=true;if(assign(index+1,slots,used,budget))return true;used[i]=false;}
            return false;
        }
    }
    public static List<Ritual> all(){return recipes;}
    @Override protected void apply(Map<Identifier,JsonElement> entries,ResourceManager manager,ProfilerFiller profiler){
        var found=new ArrayList<Ritual>();
        entries.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString))).forEach(entry->{try{
            var d=entry.getValue().getAsJsonObject();var inputs=new ArrayList<JsonElement>();
            for(var v:d.getAsJsonArray("inputs")){var part=v.getAsJsonObject();int count=part.get("amount").getAsInt();if(count<1||count>8||inputs.size()+count>8)throw new IllegalArgumentException("Invalid pedestal count");for(int i=0;i<count;i++)inputs.add(part.get("ingredient"));}
            int tier=d.has("forge_tier")?d.get("forge_tier").getAsInt():1,duration=d.has("duration")?d.get("duration").getAsInt():500;
            if(tier<1||tier>5||duration<1||duration>72000)throw new IllegalArgumentException("Invalid tier/duration");
            String result=d.getAsJsonObject("result").get("type").getAsString();if(!Set.of("jugcraft:forbidden_create_item","jugcraft:forbidden_upgrade_tier","jugcraft:forbidden_transmute_input").contains(result))throw new IllegalArgumentException("Unsupported ritual result "+result);
            var costs=new int[4];String[] names={"aureal","souls","blood","experience"};var ess=d.getAsJsonObject("essences");for(int i=0;i<4;i++){costs[i]=ess!=null&&ess.has(names[i])?ess.get(names[i]).getAsInt():0;if(costs[i]<0||costs[i]>100000)throw new IllegalArgumentException("Invalid essence cost");}
            found.add(new Ritual(entry.getKey().toString(),d,List.copyOf(inputs),tier,d.has("match_tier_exact")&&d.get("match_tier_exact").getAsBoolean(),duration,costs));
        }catch(RuntimeException error){Jugcraft.LOGGER.error("Arcane ritual {} rejected: {}",entry.getKey(),error.getMessage());}});
        recipes=List.copyOf(found);
    }
}
