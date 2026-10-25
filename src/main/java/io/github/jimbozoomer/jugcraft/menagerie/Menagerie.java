package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

/** One native registry and saved identity for the owner's wildlife and mount collection. */
public final class Menagerie {
    /** Client prediction has no effects; authoritative interactions always consult shared protection. */
    static boolean mayChange(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,BlockPos pos){return player!=null&&(level.isClientSide()||level instanceof net.minecraft.server.level.ServerLevel server&&io.github.jimbozoomer.jugcraft.concordance.Authority.mayChange(server,player,pos));}

    public record Variant(String name,String texture,String babyTexture,int weight,String model,String babyModel) {}
    public record Species(String source,String id,String name,String shape,String movement,String habitat,String mood,
                          float width,float height,int health,String feed,boolean mount,String model,List<Variant> variants,List<String> clips,Map<String,String> armorTextures) {
        public boolean aquatic(){return movement.equals("water");}
        public boolean flying(){return movement.equals("air");}
        public boolean amphibious(){return movement.equals("amphibious");}
        public boolean predator(){return mood.equals("predator");}
        public boolean small(){return width<=.65F&&!mount;}
    }
    public static final Map<String,Species> SPECIES=new LinkedHashMap<>();
    public static final Map<String,EntityType<MenagerieAnimal>> TYPES=new LinkedHashMap<>();
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static final Map<String,SoundEvent> SOUNDS=new LinkedHashMap<>();
    private Menagerie(){}
    public static void register(){
        RecoveredCreaturePart.register();
        JsonObject root;
        try(var in=Menagerie.class.getResourceAsStream("/jugcraft/menagerie/catalog.json")){
            if(in==null)throw new IllegalStateException("Missing menagerie catalog");
            root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException e){throw new IllegalStateException(e);}
        for(String collection:List.of("grove","fable","companion","ecology","mutant","beast","landscape","cavern","rotten"))try(var in=Menagerie.class.getResourceAsStream("/jugcraft/menagerie/"+collection+".json")){
            if(in!=null){var extra=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
                root.getAsJsonArray("species").addAll(extra.getAsJsonArray("species"));root.getAsJsonArray("sounds").addAll(extra.getAsJsonArray("sounds"));}
        }catch(IOException e){throw new IllegalStateException(e);}
        var gson=new Gson();
        for(var value:root.getAsJsonArray("species")){
            var s=gson.fromJson(value,Species.class);SPECIES.put(s.id(),s);
            var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id(s.id()));
            MobCategory category=s.mood().equals("hostile")?MobCategory.MONSTER:s.aquatic()?MobCategory.WATER_CREATURE:s.flying()&&!s.mount()?MobCategory.AMBIENT:MobCategory.CREATURE;
            var builder=EntityType.Builder.<MenagerieAnimal>of((t,l)->s.id().startsWith("rotten_")?new RottenCreatures.Undead(t,l,s):s.id().startsWith("cavern_")?Caverns.create(t,l,s):s.id().startsWith("beast_")?Beasts.create(t,l,s):s.id().startsWith("mutant_")?Mutations.create(t,l,s):s.mood().equals("hostile")?new FableCreatures.Hostile(t,l,s):new MenagerieAnimal(t,l,s),category).sized(s.width(),s.height()).eyeHeight(s.height()*.8F).clientTrackingRange(10);
            if(s.id().equals("grove_cindershell")||s.id().equals("mount_firebird")||s.id().equals("rotten_burned")||s.id().equals("rotten_immortal"))builder.fireImmune();
            var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,builder.build(key));
            TYPES.put(s.id(),type);FabricDefaultAttributeRegistry.register(type,s.id().startsWith("rotten_")?RottenCreatures.attributes(s):MenagerieAnimal.attributes(s));
            SpawnPlacements.register(type,LavaSwimming.species(s)?SpawnPlacementTypes.NO_RESTRICTIONS:s.aquatic()?SpawnPlacementTypes.IN_WATER:SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t,l,reason,p,r)->(LavaSwimming.species(s)?LavaSwimming.fluid(s,l.getFluidState(p))&&LavaSwimming.fluid(s,l.getFluidState(p.below())):s.aquatic()?l.getFluidState(p).is(Fluids.WATER)&&l.getFluidState(p.below()).is(Fluids.WATER):Mob.checkMobSpawnRules(t,l,reason,p,r)&&l.getBlockState(p.below()).isSolidRender())&&(!Set.of("deep_ocean","cave").contains(s.habitat())||p.getY()<45)&&(!s.mood().equals("hostile")||l.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL&&l.getMaxLocalRawBrightness(p)<=7)&&(!s.habitat().equals("storm")||l.getLevel().isThundering()&&l.canSeeSky(p))&&(!s.id().equals("fable_scarecrow")||!FableCreatures.spawnWarded(l,p)));
            BiomeModifications.addSpawn(c->habitat(c,s),category,type,s.habitat().equals("storm")?1:s.mount()?1:s.width()>1.5?2:5,1,s.habitat().equals("storm")?1:s.mount()?1:s.small()?3:2);
        }
        for(var sound:root.getAsJsonArray("sounds")){
            String name=sound.getAsString();SOUNDS.put(name,Registry.register(BuiltInRegistries.SOUND_EVENT,Jugcraft.id(name),SoundEvent.createVariableRangeEvent(Jugcraft.id(name))));
        }
        MenagerieBlocks.register(root.getAsJsonObject("blocks"));
        root.getAsJsonObject("items").entrySet().forEach(e->{
            var v=e.getValue().getAsJsonObject();String id=e.getKey();
            if(v.get("kind").getAsString().equals("spawn")){item(id,p->new MenagerieItems.Spawn(v.get("species").getAsString(),p));return;}
            item(id,p->MenagerieItems.create(id,p));
        });
        PocketPets.register();
        for(String block:List.of("pocket_cloud_block","pocket_sand_block","pocket_stone_block","mount_firebird_feather","mount_firebird_feather2","mount_firebird_feather3","mount_firebird_feather4"))MenagerieBlocks.decorative(block);
        MenagerieSeasonal.register();
        MenagerieEcology.register();
        MenagerieHabitats.register();
        GroveCreatures.register();
        FableCreatures.register();
        CompanionCreatures.register();
        Household.register();
        CraftedFurniture.register();
        Ecology.register();
        Mutations.register();
        Beasts.register();
        NaturalCollections.register();
        Caverns.register();
        RottenCreatures.register();
        item("mount_summoning_staff",p->new MenagerieItems.Recall(p.stacksTo(1)));
        item("menagerie_field_guide",p->new MenagerieItems.Guide(p.stacksTo(1)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries->ITEMS.forEach((id,item)->{if((id.endsWith("spawn_egg")||id.contains("_spawn_egg_")))entries.accept(item);}));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries->ITEMS.forEach((id,item)->{if(!(id.endsWith("spawn_egg")||id.contains("_spawn_egg_"))&&!Ecology.legacyWalnut(id))entries.accept(item);}));
    }
    private static boolean habitat(BiomeSelectionContext c,Species s){
        if(!io.github.jimbozoomer.jugcraft.config.JugcraftConfig.isFeatureEnabled("agriculture"))return false;
        String biome=c.getBiomeKey().identifier().getPath();
        if(s.id().equals("grove_sporeling")&&c.hasTag(BiomeTags.IS_NETHER))return true;
        if(s.id().equals("grove_lizard")&&(c.hasTag(BiomeTags.IS_JUNGLE)||biome.equals("mushroom_fields")))return true;
        if(s.habitat().equals("nether"))return c.hasTag(BiomeTags.IS_NETHER);
        if(s.habitat().equals("end"))return c.hasTag(BiomeTags.IS_END);
        if(!c.hasTag(BiomeTags.IS_OVERWORLD))return false;
        return switch(s.habitat()){
            case "summoned"->false;
            case "storm"->!c.hasTag(BiomeTags.IS_OCEAN)&&!biome.contains("mushroom");
            case "mushroom"->biome.equals("mushroom_fields");
            case "ocean"->c.hasTag(BiomeTags.IS_OCEAN);case "deep_ocean"->c.hasTag(BiomeTags.IS_DEEP_OCEAN);
            case "river"->c.hasTag(BiomeTags.IS_RIVER);case "swamp"->biome.contains("swamp");
            case "beach"->c.hasTag(BiomeTags.IS_BEACH)||s.aquatic()&&c.hasTag(BiomeTags.IS_OCEAN);
            case "jungle"->c.hasTag(BiomeTags.IS_JUNGLE);case "forest"->c.hasTag(BiomeTags.IS_FOREST)||c.hasTag(BiomeTags.IS_TAIGA);
            case "savanna"->c.hasTag(BiomeTags.IS_SAVANNA);case "desert"->biome.contains("desert");
            case "snowy"->c.getBiome().getBaseTemperature()<.15F&&!c.hasTag(BiomeTags.IS_OCEAN);
            case "mountain"->c.hasTag(BiomeTags.IS_MOUNTAIN);case "badlands"->c.hasTag(BiomeTags.IS_BADLANDS);case "cave"->!c.hasTag(BiomeTags.IS_OCEAN);
            case "meadow"->biome.contains("meadow")||biome.contains("flower_forest");default->biome.contains("plains");
        };
    }
    static Item item(String id,Function<Item.Properties,Item> factory){
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(id));var item=Registry.register(BuiltInRegistries.ITEM,key,factory.apply(new Item.Properties().setId(key)));ITEMS.put(id,item);return item;
    }
    public static Item item(String id){return Objects.requireNonNull(ITEMS.get(id),id);}
    public static Item feed(Species s){return BuiltInRegistries.ITEM.getValue(Identifier.parse(s.feed()));}
    public static SoundEvent sound(String id){return SOUNDS.get(id);}
}
