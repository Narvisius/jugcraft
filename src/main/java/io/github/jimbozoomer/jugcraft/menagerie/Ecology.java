package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.phys.BlockHitResult;

/** Native ecology wood, sap, fruit, fauna supplies and boats, sharing the existing creature/storage systems. */
public final class Ecology {
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static final Map<String,EntityType<Boat>> BOATS=new LinkedHashMap<>();
    public static final Map<String,EntityType<ChestBoat>> CHEST_BOATS=new LinkedHashMap<>();
    private Ecology(){}
    public static Block block(String name){return Objects.requireNonNull(BLOCKS.get("ecology_"+name),name);}
    static void register(){
        JsonObject catalog;
        try(var in=Ecology.class.getResourceAsStream("/jugcraft/menagerie/ecology.json")){catalog=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException e){throw new IllegalStateException(e);}
        Registry.register(BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("ecology_tree"),EcologyTree.CODEC);
        var entries=new ArrayList<>(catalog.getAsJsonObject("blocks").entrySet());entries.sort(Comparator.comparingInt(e->e.getKey().contains("potted_")?1:0));
        for(var entry:entries){
            String id=entry.getKey();var d=entry.getValue().getAsJsonObject();String name=d.get("source").getAsString(),kind=d.get("kind").getAsString();
            var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(id));var p=BlockBehaviour.Properties.of().setId(key).strength(2).sound(SoundType.WOOD);
            Block b=switch(kind){
                case "log" -> name.startsWith("stripped_")?new RotatedPillarBlock(p):new StrippableLogBlock(p,"stripped_"+name){@Override public Block stripped(){return block(name.equals("sappy_maple_log")?"stripped_maple_log":"stripped_"+name);}};
                case "leaves" -> new TintedParticleLeavesBlock(.01F,p.noOcclusion().randomTicks().strength(.2F).sound(SoundType.GRASS));
                case "sapling" -> new SaplingBlock(grower(name.replace("_sapling","").replace("_seedling","")),p.noCollision().randomTicks().instabreak().sound(SoundType.GRASS)){
                    @Override protected boolean mayPlaceOn(BlockState s,BlockGetter l,BlockPos at){return name.startsWith("coconut_")&&s.is(BlockTags.SAND)||super.mayPlaceOn(s,l,at);}
                };
                case "stairs" -> new StairBlock(Blocks.OAK_PLANKS.defaultBlockState(),p);
                case "slab" -> new SlabBlock(p);
                case "wall" -> new WallBlock(p.sound(SoundType.STONE));
                case "fence" -> new FenceBlock(p);
                case "fence_gate" -> new FenceGateBlock(WoodType.OAK,p);
                case "door" -> new DoorBlock(BlockSetType.OAK,p.noOcclusion());
                case "trapdoor" -> new TrapDoorBlock(BlockSetType.OAK,p.noOcclusion());
                case "button" -> new ButtonBlock(BlockSetType.OAK,30,p.noCollision()){};
                case "pressure_plate" -> new PressurePlateBlock(BlockSetType.OAK,p.noCollision()){};
                case "sign" -> new StandingSignBlock(WoodType.OAK,p.noCollision());
                case "wall_sign" -> new WallSignBlock(WoodType.OAK,p.noCollision());
                case "hanging_sign" -> new CeilingHangingSignBlock(WoodType.OAK,p.noCollision());
                case "wall_hanging_sign" -> new WallHangingSignBlock(WoodType.OAK,p.noCollision());
                case "flower" -> new FlowerBlock(MobEffects.REGENERATION,5,p.noCollision().instabreak().sound(SoundType.GRASS));
                case "potted" -> new FlowerPotBlock(block(name.substring(7)),p.noOcclusion().instabreak());
                case "hanging_fruit" -> new Fruit(p.noOcclusion().randomTicks(),true);
                case "cactus_fruit" -> new Fruit(p.noOcclusion().randomTicks(),false);
                case "ice" -> name.equals("thin_ice")?new FrostedIceBlock(p.friction(.98F).noOcclusion().strength(.25F).sound(SoundType.GLASS).randomTicks()):new IceBlock(p.friction(.98F).noOcclusion().strength(.25F).sound(SoundType.GLASS));
                case "layer" -> new SnowLayerBlock(p.noOcclusion().strength(.1F).sound(SoundType.MOSS));
                case "building" -> new Block(p);
                default -> HouseholdBlock.create(p.noOcclusion().randomTicks(),d.getAsJsonObject("definition"));
            };
            Registry.register(BuiltInRegistries.BLOCK,key,b);BLOCKS.put(id,b);Menagerie.BLOCKS.put(id,b);
            if(kind.equals("cabinet")||kind.equals("shelf"))((FabricBlockEntityType)JugcraftAgriculture.KITCHEN_CABINET_ENTITY).addValidBlock(b);
            if(kind.equals("sign")||kind.equals("wall_sign"))((FabricBlockEntityType)BlockEntityTypes.SIGN).addValidBlock(b);
            if(kind.equals("hanging_sign")||kind.equals("wall_hanging_sign"))((FabricBlockEntityType)BlockEntityTypes.HANGING_SIGN).addValidBlock(b);
            if(Set.of("log","leaves","fence","slab","stairs","building").contains(kind)&&!name.contains("ice")&&!name.contains("snow")&&!name.contains("shell"))FlammableBlockRegistry.getDefaultInstance().add(b,5,kind.equals("leaves")?60:20);
        }
        for(String wood:List.of("azalea","flowering_azalea","coconut","maple","walnut"))registerBoats(wood);
        for(var entry:catalog.getAsJsonObject("items").entrySet()){
            String id=entry.getKey();var d=entry.getValue().getAsJsonObject();String name=d.get("source").getAsString();
            if(Menagerie.ITEMS.containsKey(id))continue;
            Menagerie.item(id,p->{
                if(d.has("species"))return new MenagerieItems.Spawn(d.get("species").getAsString(),p);
                if(name.equals("bug_net")||name.startsWith("bug_habitat"))return new MenagerieItems.Captured(name,p.stacksTo(1));
                if(name.endsWith("wall_sign")||name.endsWith("wall_hanging_sign"))return new BlockItem(BLOCKS.get(id),p);
                if(name.endsWith("_hanging_sign"))return new HangingSignItem(block(name),block(name.replace("_hanging_sign","_wall_hanging_sign")),p.stacksTo(16));
                if(name.endsWith("_sign"))return new StandingAndWallBlockItem(block(name),block(name.replace("_sign","_wall_sign")),Direction.DOWN,p.stacksTo(16));
                if(d.get("food").getAsBoolean())p.food(new FoodProperties.Builder().nutrition(name.contains("stew")?8:name.startsWith("cooked_")||name.endsWith("_cooked")?6:3).saturationModifier(.5F).build());
                if(name.equals("maple_sap_bucket"))return new SapBucket(p.stacksTo(1).craftRemainder(Items.BUCKET));
                if(name.equals("maple_syrup_bottle"))return new Item(p.food(new FoodProperties.Builder().nutrition(6).saturationModifier(.1F).build()).stacksTo(16).usingConvertsTo(Items.GLASS_BOTTLE).component(net.minecraft.core.component.DataComponents.CONSUMABLE,net.minecraft.world.item.component.Consumables.HONEY_BOTTLE));
                if(name.equals("music_disc_coconut"))return new Item(p.stacksTo(1).jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG,Jugcraft.id("ecology_coconut"))));
                if(BLOCKS.containsKey(id))return new BlockItem(BLOCKS.get(id),p.useBlockDescriptionPrefix());
                return new Item(p);
            });
        }
        for(String tree:List.of("coconut","maple","walnut","azalea","flowering_azalea"))BiomeModifications.addFeature(BiomeSelectors.foundInOverworld().and(c->switch(tree){case "coconut"->c.hasTag(BiomeTags.IS_BEACH);case "maple"->c.hasTag(BiomeTags.IS_FOREST);case "walnut"->c.hasTag(BiomeTags.IS_TAIGA);default->c.getBiomeKey().identifier().getPath().equals("lush_caves");}),GenerationStep.Decoration.VEGETAL_DECORATION,ResourceKey.create(Registries.PLACED_FEATURE,Jugcraft.id("ecology/"+tree)));
    }
    private static TreeGrower grower(String wood){var key=ResourceKey.create(Registries.FEATURE,Jugcraft.id("ecology/"+wood));return new TreeGrower("jugcraft_ecology_"+wood,WeightedList.of(key),WeightedList.of(),WeightedList.of(),key);}
    private static void registerBoats(String wood){
        String name="ecology_"+wood+"_boat";var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id(name));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Boat>of((t,l)->new Boat(t,l,()->Menagerie.item(name)),MobCategory.MISC).sized(1.375F,.5625F).clientTrackingRange(10).build(key));BOATS.put(wood,type);Menagerie.item(name,p->new BoatItem(type,p.stacksTo(1)));
        String chest="ecology_"+wood+"_chest_boat";var chestKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id(chest));
        var chestType=Registry.register(BuiltInRegistries.ENTITY_TYPE,chestKey,EntityType.Builder.<ChestBoat>of((t,l)->new ChestBoat(t,l,()->Menagerie.item(chest)),MobCategory.MISC).sized(1.375F,.5625F).clientTrackingRange(10).build(chestKey));CHEST_BOATS.put(wood,chestType);Menagerie.item(chest,p->new BoatItem(chestType,p.stacksTo(1)));
    }
    private static final class SapBucket extends Item {
        SapBucket(Properties p){super(p);}
        @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext c){
            if(c.getPlayer()==null||!c.getLevel().getBlockState(c.getClickedPos()).is(Blocks.CAULDRON))return InteractionResult.PASS;
            if(!Menagerie.mayChange(c.getLevel(),c.getPlayer(),c.getClickedPos()))return InteractionResult.FAIL;
            if(!c.getLevel().isClientSide()){var state=HouseholdBlock.set(block("maple_sap_cauldron").defaultBlockState(),"level","3");c.getLevel().setBlock(c.getClickedPos(),state,3);c.getPlayer().setItemInHand(c.getHand(),ItemUtils.createFilledResult(c.getItemInHand(),c.getPlayer(),new ItemStack(Items.BUCKET)));}return InteractionResult.SUCCESS;
        }
    }
    private static final class Fruit extends Block {
        private final boolean coconut;
        Fruit(Properties p,boolean coconut){super(p);this.coconut=coconut;registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.AGE_3,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.AGE_3);}
        @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){return coconut?l.getBlockState(p.above()).is(BlockTags.LEAVES):l.getBlockState(p.below()).is(Blocks.CACTUS)||l.getBlockState(p.below()).is(BlockTags.SAND);}
        @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){if(!canSurvive(s,l,p)){l.destroyBlock(p,true);return;}int age=s.getValue(BlockStateProperties.AGE_3);if(age<(coconut?2:3)&&r.nextInt(6)==0)l.setBlock(p,s.setValue(BlockStateProperties.AGE_3,age+1),2);}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
            if(s.getValue(BlockStateProperties.AGE_3)<(coconut?2:3))return InteractionResult.PASS;if(!Menagerie.mayChange(l,player,p))return InteractionResult.FAIL;
            if(!l.isClientSide()){Block.popResource(l,p,new ItemStack(Menagerie.item(coconut?"ecology_coconut":"ecology_prickly_pear"),1+l.getRandom().nextInt(2)));l.setBlock(p,s.setValue(BlockStateProperties.AGE_3,0),2);}return InteractionResult.SUCCESS;
        }
    }
}
