package io.github.jimbozoomer.jugcraft.concordance.assay;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.*;
import io.github.jimbozoomer.jugcraft.concordance.resource.PrimaValue;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;

/** One price source for every exchange device. Numeric reference facts, recipe costs, then explicit fallback. */
public final class AlchemyValues {
    private static EquivalenceCatalog catalogue;
    private static final Map<String,String> sources=new HashMap<>();
    private AlchemyValues(){}
    public static synchronized EquivalenceCatalog catalog(){
        if(catalogue!=null)return catalogue;
        var materials=new LinkedHashMap<String,Material>();var errors=new ArrayList<String>();
        JsonObject values=new JsonObject();
        try(var stream=AlchemyValues.class.getResourceAsStream("/data/jugcraft/alchemy/values.json")){
            if(stream==null)throw new IOException("Missing generated essence values");
            values=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("items");
        }catch(Exception e){errors.add("Cannot load essence values: "+e.getMessage());}
        for(var item:BuiltInRegistries.ITEM){
            if(item==Items.AIR)continue;
            String id=BuiltInRegistries.ITEM.getKey(item).toString();long n=1,d=1;String source="fallback (balance pending)";
            try{
                if(values.has(id)){var v=values.getAsJsonObject(id);n=v.get("numerator").getAsLong();d=v.get("denominator").getAsLong();source=v.get("source").getAsString();}
                if(n<=0||d<=0||n/d>1_000_000_000L)throw new IllegalArgumentException("Invalid price for "+id);
                materials.put(id,new Material(id,new PrimaValue(n,d),true,true));sources.put(id,source);
            }catch(Exception e){errors.add(e.getMessage());}
        }
        catalogue=new EquivalenceCatalog(materials,Map.of(),errors);return catalogue;
    }
    public static String source(String id){catalog();return sources.getOrDefault(id,"fallback (balance pending)");}
}
