package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.WoodType;

/** Native implementations keep the supplied botanical names separate from existing Jugcraft IDs. */
public final class Jugtania {
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    public static BlockEntityType<BotanicalWorkshop> WORKSHOP;
    public static net.minecraft.core.particles.SimpleParticleType LUMINIZER_WISP;
    public static final List<String> COLORS=List.of("white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black");
    private Jugtania(){}
    public static Block block(String name){return BLOCKS.getOrDefault(name,Blocks.AIR);}
    public static Item item(String name){return ITEMS.getOrDefault(name,Items.AIR);}
    public static void register(){
        BotanicalFlowerMemory.register();
        LUMINIZER_WISP=Registry.register(BuiltInRegistries.PARTICLE_TYPE,Jugcraft.id("botanical_luminizer_wisp"),net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple());
        BotanicalManaStorm.register();BotanicalTransport.register();BotanicalLaputa.register();BotanicalGaia.register();BotanicalRelics.register();BotanicalRoyalWeapon.register();BotanicalPinkWither.register();BotanicalHalos.register();BotanicalThrownTools.register();BotanicalIvy.register();BotanicalBuilding.register();BotanicalBags.register();BotanicalEquestrian.register();BotanicalEnderAir.register();BotanicalConsumables.register();BotanicalSpawnerMover.register();BotanicalBlackHole.register();BotanicalUtilityItems.register();BotanicalBlaster.register();BotanicalLenses.register();BotanicalCacophonium.register();BotanicalLightRide.register();BotanicalCosmetics.register();BotanicalInspection.register();BotanicalMovement.register();BotanicalAccessories.register();BotanicalDiva.register();CorporeaSpark.register();BotanicalTools.register();BotanicalAdvancedTools.register();BotanicalManaItems.register();BotanicalArmor.register();BotanicalRods.register();BotanicalTerrainRods.register();BotanicalCombatRods.register();BotanicalDiviner.register();BotanicalSounds.register();BotanicalBrewing.register();
        var descriptor=OwnerBackpacks.source("newest_jugtania.json");var workshops=new ArrayList<Block>();
        for(var entry:descriptor.getAsJsonObject("blocks").entrySet().stream().sorted(java.util.Comparator.comparing(e->e.getKey().startsWith("potted_"))).toList()){
            String name=entry.getKey(),kind=entry.getValue().getAsJsonObject().get("kind").getAsString();
            var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jugtania_"+name));
            var properties=BlockBehaviour.Properties.ofFullCopy(name.contains("wood")?Blocks.OAK_PLANKS:Blocks.STONE).setId(key);
            if(!kind.equals("block")&&!kind.equals("pillar"))properties.noOcclusion();
            if(kind.equals("floating"))properties=BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY).mapColor(net.minecraft.world.level.material.MapColor.PLANT).setId(key).noOcclusion();
            if(kind.contains("flower")||name.equals("pure_daisy")||name.equals("manastar")||BotanicalGenerators.TYPES.contains(name)||BotanicalFunctions.TYPES.contains(name))properties=BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY).mapColor(net.minecraft.world.level.material.MapColor.PLANT).setId(key);
            if(name.equals("fake_air"))properties=BlockBehaviour.Properties.ofFullCopy(Blocks.BARRIER).mapColor(net.minecraft.world.level.material.MapColor.NONE).setId(key).noCollision().noOcclusion();
            int light=entry.getValue().getAsJsonObject().get("light").getAsInt();if(light>0)properties.lightLevel(s->light);
            Block b;try {b=switch(kind){
                case "workshop"->name.equals("starfield")?new BotanicalOddities.Starfield(properties):name.equals("gaia_head")||name.equals("gaia_wall_head")?new BotanicalGaiaHead(name,properties):name.equals("light_launcher")?new BotanicalOddities.Launcher(properties):name.equals("pump")?new BotanicalTransport.Pump(properties):name.equals("mana_fluxfield")?new BotanicalTransport.FluxBlock(properties):name.equals("tiny_potato")?new BotanicalPotato.Block(properties):BotanicalPlatforms.TYPES.contains(name)?new BotanicalPlatforms.Platform(name,properties):BotanicalRedString.TYPES.contains(name)?new BotanicalRedString.Block(name,properties):BotanicalLuminizer.TYPES.contains(name)?new BotanicalLuminizer.Block(name,properties):name.equals("avatar")?new BotanicalAvatar.Block(properties):CorporeaDevices.TYPES.contains(name)?new CorporeaDevices.Device(name,properties):BotanicalDevices.TYPES.contains(name)?new BotanicalDevices.Device(name,properties):BotanicalCrates.handles(name)?new BotanicalCrates.Crate(name,properties):name.startsWith("drum_")?new BotanicalHorns.Drum(name,properties):name.equals("incense_plate")?new BotanicalIncense.Plate(name,properties):BotanicalMechanisms.handles(name)?new BotanicalMechanisms.Block(name,properties):new BotanicalBlock(name,properties);
                case "floating"->new FloatingBotanicalBlock(name,properties);
                case "ghost_rail"->new BotanicalGhostRail(BlockBehaviour.Properties.ofFullCopy(Blocks.RAIL).setId(key));
                case "buried_petal"->new BotanicalOddities.PetalBlock(name,BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).mapColor(net.minecraft.world.level.material.MapColor.PLANT).setId(key));
                case "fel_pumpkin"->new BotanicalOddities.FelPumpkin(properties);
                case "solid_vine"->new BotanicalThrownTools.SolidVines(properties.strength(.2F));
                case "force_relay"->new BotanicalForceRelay(properties);
                case "mana_flame"->new BotanicalFlame(properties);
                case "mana_detector"->new BotanicalRouting.Detector(properties);
                case "stairs"->new StairBlock(Blocks.STONE.defaultBlockState(),properties);
                case "slab"->new SlabBlock(properties);
                case "wall"->new WallBlock(properties);
                case "fence"->new FenceBlock(properties);
                case "fence_gate"->new FenceGateBlock(WoodType.OAK,properties);
                case "pillar"->Set.of("livingwood","livingwood_log","dreamwood","dreamwood_log").contains(name)
                        ?new io.github.jimbozoomer.jugcraft.agriculture.StrippableLogBlock(properties,"stripped_"+name){
                            @Override public Block stripped(){return block("stripped_"+name);}
                        }:new RotatedPillarBlock(properties);
                case "facing_decor"->new io.github.jimbozoomer.jugcraft.menagerie.OwnerDecorVariants.Facing(properties);
                case "bifrost"->new BotanicalTerrainRods.Bifrost(properties.noOcclusion());
                case "pot"->new FlowerPotBlock(block(name.substring(7)),BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWER_POT).setId(key));
                case "pane"->new IronBarsBlock(properties);
                case "glass"->new TransparentBlock(properties);
                case "flower"->new BotanicalFlowers.Flower(name,properties);
                case "double_flower"->new DoublePlantBlock(properties);
                default->new Block(properties);
            };
            } catch(RuntimeException failure) {throw new IllegalStateException("Failed to register botanical block "+name+" ("+kind+")",failure);}
            Registry.register(BuiltInRegistries.BLOCK,key.identifier(),b);BLOCKS.put(name,b);if(b instanceof BotanicalBlock)workshops.add(b);
            if(name.equals("gaia_head")||name.equals("gaia_wall_head")||name.equals("fake_air")||kind.equals("pot")||kind.equals("buried_petal")||kind.equals("bifrost")||kind.equals("mana_flame"))continue;
            var itemKey=ResourceKey.create(Registries.ITEM,key.identifier());
            Item i=Registry.register(BuiltInRegistries.ITEM,itemKey.identifier(),new BlockItem(b,new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));ITEMS.put(name,i);
        }
        var headKey=ResourceKey.create(Registries.ITEM,Jugcraft.id("jugtania_gaia_head"));
        ITEMS.put("gaia_head",Registry.register(BuiltInRegistries.ITEM,headKey.identifier(),new StandingAndWallBlockItem(block("gaia_head"),block("gaia_wall_head"),Direction.DOWN,new Item.Properties().setId(headKey).useBlockDescriptionPrefix())));
        WORKSHOP=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("botanical_workshop"),FabricBlockEntityTypeBuilder.create(BotanicalWorkshop::new,workshops.toArray(Block[]::new)).build());
        BotanicalTransport.registerEnergy();
        for(var element:descriptor.getAsJsonArray("items")){
            String name=element.getAsString();var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jugtania_"+name));var p=new Item.Properties().setId(key);if(name.startsWith("pattern_"))p.stacksTo(1);if(name.startsWith("record_gaia_"))p.stacksTo(1).jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG,Jugcraft.id("jugtania_gaia_"+name.substring(12))));
            Item i=name.endsWith("_petal")?new BlockItem(block(name.replace("_petal","_buried_petals")),p):BotanicalRelics.ITEMS.contains(name)?new BotanicalRelics(name,p):BotanicalUtilityItems.ITEMS.contains(name)?BotanicalUtilityItems.create(name,p):BotanicalLenses.handles(name)?BotanicalLenses.create(name,p):BotanicalCosmetics.cosmetic(name)?BotanicalCosmetics.create(name,p):BotanicalAccessories.ITEMS.contains(name)?BotanicalAccessories.create(name,p):BotanicalCombatRods.ITEMS.contains(name)?BotanicalCombatRods.create(name,p):BotanicalTerrainRods.ITEMS.contains(name)?BotanicalTerrainRods.create(name,p):BotanicalRods.ITEMS.contains(name)?BotanicalRods.create(name,p):BotanicalArmor.handles(name)?BotanicalArmor.create(name,p):BotanicalAdvancedTools.ITEMS.contains(name)?BotanicalAdvancedTools.create(name,p):BotanicalManaItems.ITEMS.contains(name)?BotanicalManaItems.create(name,p):BotanicalEquipment.TOOLS.contains(name)?BotanicalEquipment.create(name,p):BotanicalLandscaping.SEEDS.contains(name)?new BotanicalLandscaping.Seed(name,p):switch(name){case "water_bowl"->new BotanicalWaterBowl(p);case "pool_minecart"->new BotanicalTransport.CartItem(p);case "laputa_shard"->new BotanicalLaputa(p);case "pinkinator"->new BotanicalPinkWither.Pinkinator(p);case "crafting_halo","auto_crafting_halo"->new BotanicalHalos(name.equals("auto_crafting_halo"),p);case "vine_ball","thorn_chakram","flare_chakram"->new BotanicalThrownTools.Thrower(p);case "slingshot"->new BotanicalThrownTools.Slingshot(p);case "astrolabe"->new BotanicalBuilding.Astrolabe(p);case "sextant"->new BotanicalBuilding.Sextant(p);case "virus_necrodermal","virus_nullodermal"->new BotanicalEquestrian(name.equals("virus_necrodermal"),p);case "ender_air_bottle"->new BotanicalEnderAir.Bottle(p);case "mana_cookie","mana_bottle"->new BotanicalConsumables(name.equals("mana_bottle"),p);case "spawner_mover"->new BotanicalSpawnerMover(p);case "black_hole_talisman"->new BotanicalBlackHole(p);case "flower_bag","bauble_box"->new BotanicalBags(name.equals("flower_bag"),p);case "mana_gun"->new BotanicalBlaster(p);case "corporea_spark","corporea_spark_master","corporea_spark_creative"->new CorporeaSpark.SparkItem(name.endsWith("creative")?2:name.endsWith("master")?1:0,p);case "cacophonium"->new BotanicalCacophonium.Instrument(p);case "divining_rod"->new BotanicalDiviner(p);case "horn_grass","horn_leaves","horn_snow"->new BotanicalHorns.Horn(name.equals("horn_snow")?2:name.equals("horn_leaves")?1:0,p);case "incense_stick"->new BotanicalIncense.Stick(p);case "brew_vial","brew_flask"->new BotanicalBrewing.Drink(name.equals("brew_flask"),p);case "overgrowth_seed"->new BotanicalTreasures.Overgrowth(p);case "lexicon"->new BotanicalTreasures.Lexicon(p);case "twig_wand","dreamwood_wand"->new BotanicalTools.Wand(p.stacksTo(1));case "mana_tablet"->new BotanicalTools.Tablet(p.stacksTo(1));case "fertilizer"->new BotanicalFlowers.Fertilizer(p);default->new Item(p);};
            ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,key.identifier(),i));
        }
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(Jugcraft.id("botanical_recipes"),JugtaniaRecipes.INSTANCE);
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(Jugcraft.id("botanical_encounters"),BotanicalLoonium.INSTANCE);
        BotanicalTransport.registerDispenser();BotanicalGhostRail.register();BotanicalWaterBowl.register();BotanicalRedString.register();BotanicalFlowers.register();BotanicalEquipment.register();BotanicalLandscaping.register();BotanicalTreasures.register();
    }
}
