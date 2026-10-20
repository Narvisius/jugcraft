package io.github.jimbozoomer.jugcraft.nouveau;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Generated from the supplied names and source files, with explicit behavior families. */
public final class NouveauCatalog {
    public record Entry(String source,String id,String name,String kind,String geo) {}
    public static final Map<String,Entry> BLOCK_ENTRIES=new LinkedHashMap<>(), ITEM_ENTRIES=new LinkedHashMap<>();
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    private NouveauCatalog() {}
    public static void load(){
        try(var stream=NouveauCatalog.class.getResourceAsStream("/jugcraft/nouveau/catalog.json")){
            if(stream==null)throw new IllegalStateException("Missing generated Jug Nouveau catalog");
            var root=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            read(root.getAsJsonObject("blocks"),BLOCK_ENTRIES);read(root.getAsJsonObject("items"),ITEM_ENTRIES);
        }catch(IOException error){throw new IllegalStateException("Cannot read Jug Nouveau catalog",error);}
        BLOCKS.put("bookwyrm_lectern",JugNouveau.LECTERN);BLOCKS.put("storage_lectern",JugNouveau.LECTERN);
        ITEMS.put("bookwyrm_lectern",JugNouveau.LECTERN_ITEM);ITEMS.put("storage_lectern",JugNouveau.LECTERN_ITEM);
        ITEMS.put("bookwyrm_charm",JugNouveau.CHARM);ITEMS.put("dominion_wand",JugNouveau.WAND);ITEMS.put("starbuncle_charm",JugNouveau.STARBUNCLE_CHARM);
    }
    private static void read(JsonObject json,Map<String,Entry> into){
        json.entrySet().forEach(e->{var v=e.getValue().getAsJsonObject();into.put(e.getKey(),new Entry(e.getKey(),v.get("id").getAsString(),
            v.get("name").getAsString(),v.get("kind").getAsString(),v.has("geo")?v.get("geo").getAsString():""));});
    }
    public static Block block(String source){return Objects.requireNonNull(BLOCKS.get(source),source);}
    public static Item item(String source){return Objects.requireNonNull(ITEMS.get(source),source);}
    public static String id(String source){return BLOCK_ENTRIES.containsKey(source)?BLOCK_ENTRIES.get(source).id():ITEM_ENTRIES.get(source).id();}
    public static Block registerBlock(Entry e,BlockBehaviour.Properties p,Function<BlockBehaviour.Properties,Block> make){
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(e.id()));
        var block=Registry.register(BuiltInRegistries.BLOCK,key,make.apply(p.setId(key)));BLOCKS.put(e.source(),block);return block;
    }
    public static Item registerItem(Entry e,Function<Item.Properties,Item> make){
        if(ITEMS.containsKey(e.source()))return ITEMS.get(e.source());
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(e.id()));
        var item=Registry.register(BuiltInRegistries.ITEM,key,make.apply(new Item.Properties().setId(key)));ITEMS.put(e.source(),item);return item;
    }
    public static Item registerBlockItem(Entry e){return registerItem(e,p->new BlockItem(block(e.source()),p.useBlockDescriptionPrefix()));}
}
