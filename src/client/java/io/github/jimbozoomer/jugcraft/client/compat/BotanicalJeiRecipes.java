package io.github.jimbozoomer.jugcraft.client.compat;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.botany.*;
import java.util.*;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Blocks;

/** Optional JEI view generated from the same source formulas as the native botanical stations. */
final class BotanicalJeiRecipes {
    private static final Map<String,String> STATIONS=Map.ofEntries(
        Map.entry("petal_apothecary","apothecary_default"), Map.entry("pure_daisy","pure_daisy"),
        Map.entry("mana_infusion","mana_pool"), Map.entry("runic_altar","runic_altar"),
        Map.entry("terra_plate","terra_plate"), Map.entry("elven_trade","alfheim_portal"),
        Map.entry("brew","brewery"), Map.entry("orechid","orechid"),
        Map.entry("orechid_ignem","orechid_ignem"), Map.entry("marimorphosis","marimorphosis"));
    private BotanicalJeiRecipes(){}
    static void append(List<JugcraftJeiStations.Station> stations){
        var resource=Minecraft.getInstance().getResourceManager().getResource(Jugcraft.id("botanical_recipe_view.json"));
        if(resource.isEmpty())return;
        var groups=new TreeMap<String,List<JugcraftJeiStations.Recipe>>();
        try(var reader=resource.get().openAsReader()){
            for(var entry:JsonParser.parseReader(reader).getAsJsonObject().entrySet())try{
                var data=entry.getValue().getAsJsonObject();String type=data.get("type").getAsString();
                if(type.equals("state_copying_pure_daisy"))type="pure_daisy";
                if(!STATIONS.containsKey(type))continue;
                groups.computeIfAbsent(type,k->new ArrayList<>()).addAll(rows(type,data));
            }catch(RuntimeException failure){Jugcraft.LOGGER.warn("Botanical JEI recipe {} unavailable: {}",entry.getKey(),failure.getMessage());}
            for(var entry:groups.entrySet())if(!entry.getValue().isEmpty())stations.add(new JugcraftJeiStations.Station(
                IRecipeType.create(Jugcraft.id("station/botanical_"+entry.getKey()),JugcraftJeiStations.Recipe.class),
                List.of(Jugtania.item(STATIONS.get(entry.getKey()))),List.copyOf(entry.getValue()),
                "Bundled botanical recipes. Server datapacks may change these formulas; the encyclopedia shows server recipes."));
        }catch(Exception failure){Jugcraft.LOGGER.warn("Could not load botanical JEI recipes",failure);}
    }
    private static List<JugcraftJeiStations.Recipe> rows(String type,JsonObject d){
        var inputs=new ArrayList<List<ItemStack>>();var tools=new ArrayList<List<ItemStack>>();var outputs=new ArrayList<ItemStack>();var notes=new ArrayList<String>();
        boolean block=Set.of("pure_daisy","orechid","orechid_ignem","marimorphosis").contains(type);
        if(d.has("input"))inputs.add(ingredient(d.get("input"),block));
        if(d.has("ingredients"))for(var input:d.getAsJsonArray("ingredients"))inputs.add(ingredient(input,false));
        if(type.equals("brew"))return brews(d,inputs);
        JsonElement out=d.has("output")?d.get("output"):d.get("result");
        if(out!=null){if(out.isJsonArray())for(var value:out.getAsJsonArray())outputs.add(output(value,block));else outputs.add(output(out,block));}
        if(outputs.isEmpty())throw new IllegalArgumentException("No displayable output");
        int ticks=0;
        if(d.has("mana"))notes.add(d.get("mana").getAsInt()+" mana.");
        if(d.has("catalyst")){tools.add(ingredient(d.get("catalyst"),true));notes.add("Place the catalyst directly below the pool; it is not consumed.");}
        switch(type){
            case "petal_apothecary"->{inputs.add(ingredient(d.get("reagent"),false));tools.add(List.of(new ItemStack(Items.WATER_BUCKET)));notes.add("Fill with water, add the exact ingredients, then a seed reagent. Water and reagent are consumed; retain the bucket.");}
            case "pure_daisy"->{ticks=d.has("time")?Math.clamp(d.get("time").getAsInt()*8,20,24000):1200;notes.add("Place the input block beside the flower and leave it there. A water bucket icon means placed water; the bucket is not consumed.");}
            case "runic_altar"->{inputs.add(List.of(new ItemStack(Jugtania.item("livingrock"))));notes.add("Supply mana, then one Livingrock to finish. Ingredient runes are retained for reuse.");}
            case "terra_plate"->notes.add("Build the 3x3 Livingrock/Lapis checkerboard foundation. Drop exact ingredients on the plate and supply mana through sparks.");
            case "elven_trade"->notes.add("Open a valid Alfheim gateway with its two fueled mana pools. Offer exact ingredients; collect the returned items.");
            case "orechid","orechid_ignem","marimorphosis"->{int mana=type.equals("orechid")?17500:type.equals("orechid_ignem")?20000:12;notes.add(mana+" mana per conversion. Weighted random result: weight "+d.get("weight").getAsInt()+" among recipes matching this input.");if(type.equals("orechid_ignem"))notes.add("Requires a Nether biome.");if(d.has("biome_bonus"))notes.add("Matching biome tag adds "+d.get("biome_bonus").getAsInt()+" weight: "+d.get("biome_bonus_tag").getAsString()+".");}
            default->{}
        }
        return List.of(new JugcraftJeiStations.Recipe(List.copyOf(inputs),List.copyOf(outputs),List.copyOf(tools),ticks,List.copyOf(notes)));
    }
    private static List<JugcraftJeiStations.Recipe> brews(JsonObject d,List<List<ItemStack>> ingredients){
        String id=d.get("brew").getAsString();id=id.substring(id.indexOf(':')+1).replaceFirst("^jugtania_","");
        var brew=BotanicalBrewing.BREWS.get(id);if(brew==null)throw new IllegalArgumentException("Unknown brew "+id);
        var result=new ArrayList<JugcraftJeiStations.Recipe>();
        for(String vessel:List.of("vial","flask","incense_stick","blood_pendant")){
            boolean equipment=vessel.equals("incense_stick")||vessel.equals("blood_pendant"),flask=vessel.equals("flask");
            if(equipment&&!BotanicalBrewing.canInfuseEquipment(id))continue;
            var inputs=new ArrayList<>(ingredients);inputs.add(List.of(new ItemStack(Jugtania.item(vessel))));
            ItemStack output;
            if(equipment){output=new ItemStack(Jugtania.item(vessel));output.set(BotanicalBrewing.BREW,id);output.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of(),List.of(),List.of(),List.of(brew.color())));}
            else output=BotanicalBrewing.filled(id,flask);
            var notes=new ArrayList<String>();notes.add((brew.mana()*(equipment?10:flask?2:1))+" mana. Supply the exact ingredients and one empty vessel.");
            if(!equipment)notes.add((flask?6:4)+" doses; consuming the final dose returns the empty vessel.");
            else notes.add(vessel.equals("blood_pendant")?"Equip the pendant to maintain its effect using carried mana.":"Burn on an Incense Plate for a long-lasting area effect.");
            result.add(new JugcraftJeiStations.Recipe(List.copyOf(inputs),List.of(output),List.of(),0,List.copyOf(notes)));
        }return result;
    }
    private static List<ItemStack> ingredient(JsonElement element,boolean block){
        if(element==null)throw new IllegalArgumentException("Missing ingredient");
        if(element.isJsonArray()){var all=new ArrayList<ItemStack>();for(var e:element.getAsJsonArray())all.addAll(ingredient(e,block));if(all.isEmpty())throw new IllegalArgumentException("Empty alternatives");return List.copyOf(all);}
        if(element.isJsonObject()){
            var obj=element.getAsJsonObject();
            if(obj.has("tag")){
                String id=obj.get("tag").getAsString();var stacks=new ArrayList<ItemStack>();
                if(block)BuiltInRegistries.BLOCK.get(TagKey.create(Registries.BLOCK,Identifier.parse(id))).ifPresent(set->set.forEach(b->{if(b.value().asItem()!=Items.AIR)stacks.add(new ItemStack(b.value()));}));
                else BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM,Identifier.parse(id))).ifPresent(set->set.forEach(i->stacks.add(new ItemStack(i))));
                if(stacks.isEmpty())throw new IllegalArgumentException("Empty ingredient tag "+id);return List.copyOf(stacks);
            }
        }
        return List.of(output(element,block));
    }
    private static ItemStack output(JsonElement e,boolean block){
        if(block){var value=JugtaniaRecipes.block(e);if(value==Blocks.WATER)return new ItemStack(Items.WATER_BUCKET);if(value.asItem()==Items.AIR)throw new IllegalArgumentException("Block has no item");return new ItemStack(value);}
        var item=io.github.jimbozoomer.jugcraft.nouveau.NouveauRecipes.stack(e);if(item.isEmpty())throw new IllegalArgumentException("Unknown output "+e);return item;
    }
}
