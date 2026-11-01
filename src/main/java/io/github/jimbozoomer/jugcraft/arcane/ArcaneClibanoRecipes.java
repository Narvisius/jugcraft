package io.github.jimbozoomer.jugcraft.arcane;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauRecipes;
import java.util.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/** Source combustion recipes take precedence over ordinary furnace recipes. */
public final class ArcaneClibanoRecipes extends SimpleJsonResourceReloadListener<JsonElement> {
    public static final ArcaneClibanoRecipes INSTANCE=new ArcaneClibanoRecipes();
    private static volatile List<JsonObject> recipes=List.of();
    private ArcaneClibanoRecipes(){super(ExtraCodecs.JSON,FileToIdConverter.json("arcane_combustion"));}
    public record Work(String id,ItemStack output,int duration,float experience,boolean pair,String residue,double chance){}
    public static Work find(ServerLevel level,ItemStack first,ItemStack second,ItemStack enhancer,int fire,boolean pairs){
        if(first.isEmpty())return null;
        for(var d:recipes){
            var ingredients=d.getAsJsonObject("ingredients");boolean pair=ingredients.has("first");if(pair!=pairs)continue;
            int required=List.of("fire","soul_fire","enchanted_fire").indexOf(d.get("fire_type").getAsString());if(fire<required)continue;
            if(d.has("enhancer")&&!ArcaneEnhancers.name(enhancer).equals(d.get("enhancer").getAsString()))continue;
            if(pair?!(NouveauRecipes.ingredient(ingredients.get("first"),first)&&NouveauRecipes.ingredient(ingredients.get("second"),second)||NouveauRecipes.ingredient(ingredients.get("second"),first)&&NouveauRecipes.ingredient(ingredients.get("first"),second)):!NouveauRecipes.ingredient(ingredients,first))continue;
            var output=NouveauRecipes.stack(d.get("result"),level.registryAccess());if(output.isEmpty())continue;
            var residue=d.has("residue")?d.getAsJsonObject("residue"):null;
            return new Work(d.get("key").getAsString(),output,d.get("cooking_time").getAsInt(),d.get("experience").getAsFloat(),pair,residue==null?"":residue.get("type").getAsString(),residue==null?0:residue.get("chance").getAsDouble());
        }
        if(pairs)return null;
        var input=new SingleRecipeInput(first);var found=level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,level);
        if(found.isEmpty())return null;var holder=found.get();var recipe=holder.value();var output=recipe.assemble(input);
        return output.isEmpty()?null:new Work(holder.id().identifier().toString(),output,recipe.cookingTime(),recipe.experience(),false,"",0);
    }
    @Override protected void apply(Map<Identifier,JsonElement> entries,ResourceManager manager,ProfilerFiller profiler){
        var next=new ArrayList<JsonObject>();entries.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString))).forEach(e->{try{
            var d=e.getValue().getAsJsonObject().deepCopy();int duration=d.get("cooking_time").getAsInt();float xp=d.get("experience").getAsFloat();
            if(duration<1||duration>72000||!Float.isFinite(xp)||xp<0||xp>1000||!List.of("fire","soul_fire","enchanted_fire").contains(d.get("fire_type").getAsString()))throw new IllegalArgumentException("Invalid cooking limits");
            d.getAsJsonObject("ingredients");d.getAsJsonObject("result");d.addProperty("key",e.getKey().toString());next.add(d);
        }catch(RuntimeException ex){Jugcraft.LOGGER.error("Clibano recipe {} rejected: {}",e.getKey(),ex.getMessage());}});recipes=List.copyOf(next);
    }
}
