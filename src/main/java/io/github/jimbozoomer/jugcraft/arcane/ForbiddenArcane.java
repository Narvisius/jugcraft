package io.github.jimbozoomer.jugcraft.arcane;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** Owner-supplied arcane building materials and equipment, with their native behavior. */
public final class ForbiddenArcane {
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    private static final Map<String,net.minecraft.sounds.SoundEvent> SOUNDS=new HashMap<>();
    public static net.minecraft.sounds.SoundEvent sound(String name){return SOUNDS.getOrDefault(name,SoundEvents.AMETHYST_BLOCK_CHIME);}
    public static BlockEntityType<ArcaneWorkshop> WORKSHOP;
    public static BlockEntityType<ArcaneJar> JAR;
    public static BlockEntityType<ArcaneSkulls.Head> SKULL;
    public static BlockEntityType<ArcaneClibano> CLIBANO;
    public static BlockEntityType<ArcaneInjector> INJECTOR;
    public static BlockEntityType<ArcaneWind> WIND;
    public static BlockEntityType<ArcaneBlackHole> BLACK_HOLE;
    public static net.minecraft.world.inventory.MenuType<ArcaneClibanoMenu> CLIBANO_MENU;
    public static net.minecraft.world.inventory.MenuType<ArcaneForgeMenu> FORGE_MENU;
    private ForbiddenArcane(){}
    public static Block block(String name){return BLOCKS.getOrDefault(name,Blocks.AIR);}
    public static Item item(String name){return ITEMS.getOrDefault(name,Items.AIR);}
    public static void register(){
        ArcaneEssences.register();
        ArcaneJar.register();
        ArcaneGrowth.register();
        ArcaneCapture.register();
        ArcaneMissile.register();
        ArcaneLostSoul.register();
        ArcaneDarkTrader.register();
        ArcaneArrow.register();
        ArcaneSplash.register();
        ArcaneSkulls.register();
        ArcaneBuckets.registerComponent();
        Registry.register(BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("arcane_growth"),ArcaneGrowth.GrowthFeature.CODEC);
        ArcaneModifiers.register();
        ArcaneEnhancers.load();
        FORGE_MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("arcane_forge"),new net.minecraft.world.inventory.MenuType<>(ArcaneForgeMenu::new,net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
        CLIBANO_MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("arcane_clibano"),new net.minecraft.world.inventory.MenuType<>(ArcaneClibanoMenu::new,net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
        var data=OwnerBackpacks.source("newest_forbidden.json");
        for(var entry:data.getAsJsonArray("sounds")){String name=entry.getAsString();var id=Jugcraft.id("forbidden_"+name);SOUNDS.put(name,Registry.register(BuiltInRegistries.SOUND_EVENT,id,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id)));}
        for(var entry:data.getAsJsonObject("blocks").entrySet().stream().sorted(Comparator.comparing(e->e.getKey().startsWith("potted_"))).toList()){
            String name=entry.getKey();var d=entry.getValue().getAsJsonObject();String kind=d.get("kind").getAsString();boolean wood=d.get("wood").getAsBoolean();
            var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("forbidden_"+name));
            Block base=switch(kind){case "farmland"->Blocks.FARMLAND;case "soulless_sand"->Blocks.SOUL_SAND;case "ladder"->Blocks.LADDER;case "chain"->Blocks.IRON_CHAIN;case "glass","pane","jar"->Blocks.GLASS;case "heavy_core"->Blocks.HEAVY_CORE;case "lantern"->Blocks.LANTERN;default->wood?Blocks.OAK_PLANKS:Blocks.STONE;};
            var p=BlockBehaviour.Properties.ofFullCopy(base).setId(key);
            if(kind.equals("leaves"))p=BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).setId(key);
            if(Set.of("sapling","flower","edelwood_plant","fungyss").contains(kind))p=BlockBehaviour.Properties.ofFullCopy(kind.equals("flower")?Blocks.BLUE_ORCHID:Blocks.OAK_SAPLING).setId(key);
            if(!wood&&!Set.of("farmland","soulless_sand","glass","pane","jar","leaves","sapling","flower","pot").contains(kind))p.requiresCorrectToolForDrops();
            if(d.has("strength"))p.strength(d.get("strength").getAsFloat(),d.get("resistance").getAsFloat());
            else if(d.has("resistance"))p.explosionResistance(d.get("resistance").getAsFloat());
            if(!Set.of("block","pillar","mystery_log","ore").contains(kind)||d.get("cutout").getAsBoolean())p.noOcclusion();
            int light=d.get("light").getAsInt();if(light>0)p.lightLevel(s->light);
            Block b=switch(kind){
                case "heavy_core"->new HeavyCoreBlock(p);
                case "whirlwind"->new ArcaneWind.Root(p);
                case "upwind"->new ArcaneWind.Column(p);
                case "black_hole"->new ArcaneBlackHole.HoleBlock(p);
                case "injector"->new ArcaneInjector.InjectorBlock(p);
                case "mortar"->new ArcaneDecor.Mortar(p);
                case "arcane_egg"->new ArcaneDecor.Egg(p);
                case "clibano_core"->new ArcaneClibanoBlock.Core(p);
                case "clibano_main"->new ArcaneClibanoBlock(p.noCollision());
                case "clibano_center"->new ArcaneClibanoBlock.Center(p);
                case "clibano_side"->new ArcaneClibanoBlock.Side(p);
                case "clibano_corner"->new ArcaneClibanoBlock.Corner(p);
                case "skull"->new ArcaneSkulls.Skull(name.replace("_wall_skull","").replace("_skull",""),name.contains("_wall_"),BlockBehaviour.Properties.ofFullCopy(Blocks.SKELETON_SKULL).setId(key));
                case "obelisk"->new ArcaneObelisk(p);
                case "edelwood_plant","fungyss"->new ArcaneGrowth.Plant(kind.equals("fungyss"),p);
                case "edelwood_log"->new EdelwoodBlocks.Log(p);
                case "carved_edelwood"->new EdelwoodBlocks.Carved(p);
                case "edelwood_branch"->new EdelwoodBlocks.Branch(p);
                case "mushroom_cap"->new HugeMushroomBlock(p);
                case "jar"->new ArcaneJar.JarBlock(p);
                case "stella"->new ArcaneSources.Stella(p);
                case "leaves"->new TintedParticleLeavesBlock(.01F,p);
                case "flower"->new FlowerBlock(net.minecraft.world.effect.MobEffects.GLOWING,10,p);
                case "sapling"->new SaplingBlock(aurumGrower(),p);
                case "pot"->new FlowerPotBlock(block(name.substring(7)),BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWER_POT).setId(key));
                case "workshop"->new ArcaneWorkshopBlock(name,p);
                case "slab"->new SlabBlock(p);case "stairs"->new StairBlock(base.defaultBlockState(),p);case "wall"->new WallBlock(p);
                case "plate"->new PressurePlateBlock(wood?BlockSetType.OAK:BlockSetType.STONE,p.noCollision());
                case "button"->new ButtonBlock(wood?BlockSetType.OAK:BlockSetType.STONE,wood?30:20,p.noCollision());
                case "pillar"->name.equals("arcane_polished_darkstone_pillar")?new io.github.jimbozoomer.jugcraft.menagerie.OwnerDecorVariants.JoinedPillar(p):new RotatedPillarBlock(p);case "mystery_log"->new MysteryLog(p);
                case "ore"->new DropExperienceBlock(UniformInt.of(name.startsWith("runic_")?4:2,name.startsWith("runic_")?8:5),p);
                case "glass"->new TransparentBlock(p);case "pane"->new IronBarsBlock(p);case "lantern"->new LanternBlock(p);
                case "door"->new DoorBlock(wood?BlockSetType.OAK:BlockSetType.GOLD,p);case "trapdoor"->new TrapDoorBlock(wood?BlockSetType.OAK:BlockSetType.GOLD,p);
                case "fence"->new FenceBlock(p);case "gate"->new FenceGateBlock(WoodType.OAK,p);case "ladder"->new LadderBlock(p);
                case "chain"->new ChainBlock(p);case "farmland"->new FarmlandBlock(Blocks.DIRT,p.randomTicks());case "soulless_sand"->new SoullessSand(p);
                default->new Block(p);
            };
            Registry.register(BuiltInRegistries.BLOCK,key.identifier(),b);BLOCKS.put(name,b);
            if(kind.equals("skull")||d.get("no_item").getAsBoolean()&&!kind.equals("jar"))continue;
            var ik=ResourceKey.create(Registries.ITEM,key.identifier());var props=new Item.Properties().setId(ik).useBlockDescriptionPrefix();if(kind.equals("jar"))props.stacksTo(1);ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,ik.identifier(),new BlockItem(b,props)));
        }
        WORKSHOP=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("arcane_workshop"),FabricBlockEntityTypeBuilder.create(ArcaneWorkshop::new,BLOCKS.values().stream().filter(b->b instanceof ArcaneWorkshopBlock).toArray(Block[]::new)).build());
        JAR=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("arcane_jar"),FabricBlockEntityTypeBuilder.create(ArcaneJar::new,BLOCKS.values().stream().filter(b->b instanceof ArcaneJar.JarBlock).toArray(Block[]::new)).build());
        SKULL=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("arcane_skull"),FabricBlockEntityTypeBuilder.create(ArcaneSkulls.Head::new,BLOCKS.values().stream().filter(b->b instanceof ArcaneSkulls.Skull).toArray(Block[]::new)).build());
        CLIBANO=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("arcane_clibano"),FabricBlockEntityTypeBuilder.create(ArcaneClibano::new,BLOCKS.values().stream().filter(b->b instanceof ArcaneClibanoBlock).toArray(Block[]::new)).build());
        INJECTOR=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("arcane_injector"),FabricBlockEntityTypeBuilder.create(ArcaneInjector::new,block("quantum_injector")).build());
        WIND=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("arcane_wind"),FabricBlockEntityTypeBuilder.create(ArcaneWind::new,block("whirlwind")).build());
        BLACK_HOLE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("arcane_black_hole"),FabricBlockEntityTypeBuilder.create(ArcaneBlackHole::new,block("black_hole")).build());
        for(String name:ArcaneSkulls.NAMES){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("forbidden_"+name+"_skull"));ITEMS.put(name+"_skull",Registry.register(BuiltInRegistries.ITEM,key.identifier(),new ArcaneSkulls.SkullItem(name,block(name+"_skull"),block(name+"_wall_skull"),new Item.Properties().setId(key).useBlockDescriptionPrefix())));}
        for(var entry:data.getAsJsonObject("items").entrySet()){
            String name=entry.getKey();var d=entry.getValue().getAsJsonObject();String cls=d.get("source_class").getAsString();
            var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("forbidden_"+name));var p=new Item.Properties().setId(key);
            if(d.has("stack"))p.stacksTo(d.get("stack").getAsInt());if(d.has("fireproof")&&d.get("fireproof").getAsBoolean())p.fireResistant();
            if(cls.equals("EdelwoodOilItem"))p.craftRemainder(Items.GLASS_BOTTLE);
            if(cls.equals("SmelterPrismItem"))p.durability(200).component(DataComponents.COOKING_FUEL,new net.minecraft.world.item.component.CookingFuel(new net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.Constant(200),new net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat.Constant(1)));
            Item i;
            if(cls.equals("ArmorItem"))i=armor(name,p);
            else if(cls.equals("ModArrowItem"))i=ArcaneArrow.item(name,p);
            else if(cls.equals("SplashAurealBottleItem"))i=new ArcaneSplash.Bottle(p);
            else if(cls.equals("ModBoatItem"))i=ArcaneBoats.item(name,p);
            else if(cls.equals("SpectralEyeAmuletItem"))i=new ArcaneSpectralAmulet(p);
            else if(cls.equals("WhirlwindPrismItem"))i=new ArcaneWind.Prism(p);
            else if(cls.equals("CapacityBucketItem")||cls.equals("CapacityMilkBucketItem")||cls.equals("SolidCapacityBucketItem"))i=new ArcaneBuckets(name,p);
            else if(cls.equals("QuantumCatcherItem"))i=new ArcaneCapture(name.equals("boss_catcher"),p);
            else if(cls.equals("MagicWandItem")||cls.equals("DracoArcanusScepterItem"))i=new ArcaneMissile.Wand(cls.equals("DracoArcanusScepterItem"),p);
            else if(cls.equals("Food"))i=new ArcaneFood(name,p);
            else{
                ToolMaterial material=name.startsWith("draco_")?new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL,2661,12,7,20,repair("draco")):name.startsWith("reinforced_")?new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL,2561,9,3.5F,26,repair("deorum")):name.startsWith("wooden_")?ToolMaterial.WOOD:name.startsWith("stone_")?ToolMaterial.STONE:name.startsWith("golden_")?ToolMaterial.GOLD:name.startsWith("diamond_")?ToolMaterial.DIAMOND:name.startsWith("netherite_")?ToolMaterial.NETHERITE:ToolMaterial.IRON;
                p=switch(cls){case "PickaxeItem"->p.pickaxe(material,1,-2.8F);case "SwordItem"->p.sword(material,3,-2.4F);case "ShovelItem"->p.shovel(material,1.5F,-3);case "AxeItem"->p.axe(material,6,-3.1F);case "HoeItem"->p.hoe(material,-3,0);default->p;};
                i=switch(cls){case "ArcaneBoneMealItem"->new ArcaneGrowth.Meal(p);case "DarkSoulItem"->new ArcaneGrowth.CorruptSoul(p);case "SoulExtractorItem"->new ArcaneEssences.Extractor(p);case "XpetrifiedOrbItem"->new ArcaneEssences.Orb(p);case "AurealBottleItem"->new ArcaneEssences.Bottle(p);case "MundabiturDustItem"->new ArcaneDust(p);case "FerrogneticMixtureItem"->new ArcaneMixture(p);default->new Item(p);};
            }
            ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,key.identifier(),i));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(e->BLOCKS.values().stream().map(Block::asItem).filter(i->i!=Items.AIR).distinct().forEach(e::accept));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(e->ITEMS.forEach((name,i)->{if(!BLOCKS.containsKey(name))e.accept(i);}));
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(Jugcraft.id("arcane_rituals"),ArcaneRituals.INSTANCE);
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(Jugcraft.id("arcane_combustion"),ArcaneClibanoRecipes.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("arcane_aureal_bottle"),ArcaneAurealRecipe.SERIALIZER);
        ArcaneSources.register();
        ArcaneBuckets.registerStorage();
        for(var placement:data.getAsJsonArray("ore_placements"))BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),GenerationStep.Decoration.UNDERGROUND_ORES,ResourceKey.create(Registries.PLACED_FEATURE,Jugcraft.id("forbidden_"+placement.getAsString())));
        for(var placement:data.getAsJsonArray("vegetation"))BiomeModifications.addFeature(BiomeSelectors.includeByKey(net.minecraft.world.level.biome.Biomes.FLOWER_FOREST),GenerationStep.Decoration.VEGETAL_DECORATION,ResourceKey.create(Registries.PLACED_FEATURE,Jugcraft.id("forbidden_"+placement.getAsString())));
    }
    private static net.minecraft.world.level.block.grower.TreeGrower aurumGrower(){var key=ResourceKey.create(Registries.FEATURE,Jugcraft.id("forbidden_aurum"));return new net.minecraft.world.level.block.grower.TreeGrower("jugcraft_forbidden_aurum",net.minecraft.util.random.WeightedList.of(key),net.minecraft.util.random.WeightedList.of(),net.minecraft.util.random.WeightedList.of(),key);}
    private static TagKey<Item> repair(String family){return TagKey.create(Registries.ITEM,Jugcraft.id("forbidden_"+family+"_repair"));}
    private static Item armor(String name,Item.Properties p){
        String family=name.substring(0,name.lastIndexOf('_'));boolean tyr=family.equals("tyr"),mortem=family.equals("mortem");
        var type=name.endsWith("helmet")?ArmorType.HELMET:name.endsWith("chestplate")?ArmorType.CHESTPLATE:name.endsWith("leggings")?ArmorType.LEGGINGS:ArmorType.BOOTS;
        var material=new ArmorMaterial(tyr?42:40,Map.of(ArmorType.HELMET,tyr?8:mortem?1:6,ArmorType.CHESTPLATE,tyr?12:mortem?5:10,ArmorType.LEGGINGS,tyr?10:mortem?4:8,ArmorType.BOOTS,tyr?7:mortem?1:6,ArmorType.BODY,10),mortem?6:15,SoundEvents.ARMOR_EQUIP_DIAMOND,tyr?3:mortem?1:2,tyr?.1F:0,repair(mortem?"mortem":tyr?"tyr":"draco"),ResourceKey.create(EquipmentAssets.ROOT_ID,Jugcraft.id("forbidden_"+family)));
        if(mortem)p.stacksTo(1).attributes(material.createAttributes(type)).enchantable(material.enchantmentValue()).component(DataComponents.EQUIPPABLE,Equippable.builder(type.getSlot()).setEquipSound(material.equipSound()).setAsset(material.assetId()).setDamageOnHurt(false).build());
        else p.humanoidArmor(material,type);
        return new Item(p);
    }
    private static final class MysteryLog extends RotatedPillarBlock {
        MysteryLog(Properties p){super(p);}
        @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){if(r.nextInt(3)==0)l.addParticle(ParticleTypes.END_ROD,p.getX()+r.nextDouble(),p.getY()+r.nextDouble(),p.getZ()+r.nextDouble(),0,.01,0);}
    }
    private static final class SoullessSand extends SoulSandBlock {
        SoullessSand(Properties p){super(p);}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
            if(!stack.is(item("soul")))return InteractionResult.PASS;
            if(player instanceof ServerPlayer sp){if(!Authority.mayChange(sp.level(),sp,pos))return InteractionResult.FAIL;level.setBlockAndUpdate(pos,Blocks.SOUL_SAND.defaultBlockState());stack.consume(1,player);level.levelEvent(2001,pos,Block.getId(state));}
            return InteractionResult.SUCCESS;
        }
    }
}
