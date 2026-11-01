package io.github.jimbozoomer.jugcraft.botany;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauRecipes;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Original botanical recipes, server reloaded; matching never spends an ingredient. */
public final class JugtaniaRecipes extends SimpleJsonResourceReloadListener<JsonElement> {
    public static final JugtaniaRecipes INSTANCE=new JugtaniaRecipes();
    private static volatile List<Recipe> recipes=List.of();
    private JugtaniaRecipes(){super(ExtraCodecs.JSON,FileToIdConverter.json("jugtania_recipe"));}
    public record Recipe(String id,String type,JsonObject data,List<JsonElement> ingredients,int mana) {
        public ItemStack output(){var value=data.has("output")?data.get("output"):data.get("result");if(value!=null&&value.isJsonArray()){var a=value.getAsJsonArray();if(a.size()!=1)return ItemStack.EMPTY;value=a.get(0);}return NouveauRecipes.stack(value);}
        public int[] match(List<ItemStack> slots){int[] used=new int[slots.size()];return assign(ingredients,0,slots,used,new int[]{4096})?used:null;}
        public boolean blockInput(BlockState state){
            var input=data.getAsJsonObject("input");
            if(input.has("block"))return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(input.get("block").getAsString());
            return input.has("tag")&&state.is(TagKey.create(Registries.BLOCK,Identifier.parse(input.get("tag").getAsString())));
        }
        public BlockState blockOutput(BlockState original){
            var value=data.get("output");var out=value.isJsonObject()?value.getAsJsonObject():new JsonObject();var block=block(value);
            var state=block.defaultBlockState();
            if(type.equals("state_copying_pure_daisy"))for(var property:original.getProperties())if(state.hasProperty(property))state=copy(state,original,property);
            if(out.has("properties"))for(var entry:out.getAsJsonObject("properties").entrySet()){
                var property=block.getStateDefinition().getProperty(entry.getKey());if(property!=null)state=set(state,property,entry.getValue().getAsString());
            }return state;
        }
        public boolean catalyst(BlockState state){
            if(!data.has("catalyst"))return true;
            return state.is(block(data.get("catalyst")));
        }
    }
    /** Supplied recipes use both block predicates and serialized block states. */
    public static net.minecraft.world.level.block.Block block(JsonElement value){
        String name;
        if(value.isJsonPrimitive())name=value.getAsString();
        else {var obj=value.getAsJsonObject();var ref=obj.has("block")?obj.get("block"):obj.get("name");if(ref==null)throw new IllegalArgumentException("Missing botanical block reference");name=ref.getAsString();}
        var id=Identifier.parse(name);if(!BuiltInRegistries.BLOCK.containsKey(id))throw new IllegalArgumentException("Unknown botanical block "+name);
        return BuiltInRegistries.BLOCK.getValue(id);
    }
    private static <T extends Comparable<T>> BlockState copy(BlockState target,BlockState source,net.minecraft.world.level.block.state.properties.Property<T> p){return target.setValue(p,source.getValue(p));}
    private static <T extends Comparable<T>> BlockState set(BlockState state,net.minecraft.world.level.block.state.properties.Property<T> p,String value){return p.getValue(value).map(v->state.setValue(p,v)).orElse(state);}
    public static List<Recipe> of(String type){return recipes.stream().filter(r->r.type.equals(type)).toList();}
    @Override protected void apply(Map<Identifier,JsonElement> entries,ResourceManager manager,ProfilerFiller profiler){
        var loaded=new ArrayList<Recipe>();
        entries.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString))).forEach(entry->{
            try{
                var d=entry.getValue().getAsJsonObject();String type=d.get("type").getAsString();var needs=new ArrayList<JsonElement>();
                if(type.equals("mana_infusion"))needs.add(d.get("input"));
                if(d.has("ingredients"))d.getAsJsonArray("ingredients").forEach(needs::add);
                if(needs.size()>16)throw new IllegalArgumentException("More than 16 ingredients");
                int mana=d.has("mana")?d.get("mana").getAsInt():0;if(mana<0||mana>1000000)throw new IllegalArgumentException("Invalid mana cost");
                var recipe=new Recipe(entry.getKey().toString(),type,d,List.copyOf(needs),mana);
                if(d.has("catalyst"))block(d.get("catalyst"));
                if(type.equals("pure_daisy")||type.equals("state_copying_pure_daisy"))recipe.blockOutput(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                loaded.add(recipe);
            }catch(RuntimeException e){Jugcraft.LOGGER.error("Botanical recipe {} rejected: {}",entry.getKey(),e.getMessage());}
        });recipes=List.copyOf(loaded);
    }
    private static boolean assign(List<JsonElement> needs,int index,List<ItemStack> slots,int[] used,int[] budget){
        if(--budget[0]<0)return false;if(index==needs.size())return true;
        for(int i=0;i<slots.size();i++)if(slots.get(i).getCount()>used[i]&&NouveauRecipes.ingredient(needs.get(index),slots.get(i))){used[i]++;if(assign(needs,index+1,slots,used,budget))return true;used[i]--;}
        return false;
    }
}
