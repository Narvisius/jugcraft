package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.tags.*;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.vehicle.boat.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

/** Native building, flora and wood families from the supplied landscape collections. */
public final class NaturalCollections {
    public static final Map<String,EntityType<Boat>> BOATS=new LinkedHashMap<>();
    public static final Map<String,EntityType<ChestBoat>> CHEST_BOATS=new LinkedHashMap<>();
    public static final Map<String,String> BOAT_TEXTURES=new LinkedHashMap<>();
    public static final List<net.minecraft.core.particles.SimpleParticleType> PARTICLES=new ArrayList<>();
    private NaturalCollections(){}
    public static net.minecraft.core.particles.SimpleParticleType particle(String id,net.minecraft.core.particles.SimpleParticleType fallback){var type=BuiltInRegistries.PARTICLE_TYPE.getValue(Jugcraft.id(id));return type instanceof net.minecraft.core.particles.SimpleParticleType simple?simple:fallback;}

    static void register(){
        for(String name:List.of("binary","wisp_bubble","end_spore"))PARTICLES.add(Registry.register(BuiltInRegistries.PARTICLE_TYPE,Jugcraft.id("landscape_"+name),net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple()));
        try(var in=NaturalCollections.class.getResourceAsStream("/jugcraft/menagerie/particles.json")){if(in!=null)for(var name:JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonArray())PARTICLES.add(Registry.register(BuiltInRegistries.PARTICLE_TYPE,Jugcraft.id(name.getAsString()),net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple()));}catch(IOException e){throw new IllegalStateException(e);}
        Registry.register(BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("landscape_tree"),LandscapeTree.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("landscape_detail"),LandscapeFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("owner_template"),OwnerTemplateFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("owner_patch"),OwnerPatchFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("cavern_chamber"),CavernChamber.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("candy_cavity"),CandyCavity.CODEC);
        CandyLandmark.register();
        for(String group:List.of("landscape","cavern"))try(var in=NaturalCollections.class.getResourceAsStream("/jugcraft/menagerie/"+group+".json")){
            if(in!=null)register(group,JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject());
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    private static Block block(String group,String source){return Objects.requireNonNull(Menagerie.BLOCKS.get(group+"_"+source),group+"_"+source);}
    private static void register(String group,JsonObject root){
        for(var entry:root.getAsJsonObject("fluids").entrySet()){
            var fluid=PetroFluids.registerOwnerLiquid(group+"_"+entry.getKey(),entry.getValue().getAsInt());Menagerie.BLOCKS.put(fluid.id(),fluid.block());Menagerie.ITEMS.put(fluid.id()+"_bucket",fluid.bucket());
        }
        var blocks=new ArrayList<>(root.getAsJsonObject("blocks").entrySet());blocks.sort(Comparator.comparingInt(e->e.getKey().contains("potted_")?1:0));
        for(var entry:blocks){
            String id=entry.getKey();if(Menagerie.BLOCKS.containsKey(id))continue;
            var d=entry.getValue().getAsJsonObject();String n=d.get("source").getAsString(),kind=d.get("kind").getAsString();
            var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(id));var p=BlockBehaviour.Properties.of().setId(key).strength(2).sound(SoundType.STONE);
            if(d.get("wood").getAsBoolean())p.sound(SoundType.WOOD);if(d.get("light").getAsInt()>0)p.lightLevel(s->d.get("light").getAsInt());
            Block b=group.equals("cavern")&&n.equals("nuclear_furnace")?new BlastFurnaceBlock(p):switch(kind){
                case "log"->n.startsWith("stripped_")?new RotatedPillarBlock(p):new StrippableLogBlock(p,n){@Override public Block stripped(){return block(group,"stripped_"+n);}};
                case "leaves"->n.equals("bramble_leaves")?new OwnerBuildingStates.FacingLeaves(p.strength(.2F).randomTicks().noOcclusion().sound(SoundType.GRASS)):new TintedParticleLeavesBlock(.01F,p.strength(.2F).randomTicks().noOcclusion().sound(SoundType.GRASS));
                case "sapling"->{String tree=group+"_sapling_"+n.replace("_sapling","");var f=ResourceKey.create(Registries.FEATURE,Jugcraft.id(tree));var grower=new TreeGrower(tree,WeightedList.of(f),WeightedList.of(),WeightedList.of(),f);
                    yield new SaplingBlock(grower,p.instabreak().randomTicks().noCollision().sound(SoundType.GRASS)){
                        @Override protected boolean mayPlaceOn(BlockState s,BlockGetter l,BlockPos at){return super.mayPlaceOn(s,l,at)||n.contains("palm")&&s.is(BlockTags.SAND)||n.contains("hellbark")&&(s.is(Blocks.NETHERRACK)||s.is(BlockTags.NYLIUM))||n.contains("empyreal")&&s.is(Blocks.END_STONE);}
                    };
                }
                case "stairs"->n.equals("abyssmarine_brick_stairs")?new OwnerBuildingStates.ActiveStair(p):new StairBlock(Blocks.STONE.defaultBlockState(),p);
                case "slab"->n.equals("abyssmarine_brick_slab")?new OwnerBuildingStates.ActiveSlab(p):new SlabBlock(p);
                case "wall"->n.equals("abyssmarine_brick_wall")?new OwnerBuildingStates.ActiveWall(p):new WallBlock(p);
                case "fence"->new FenceBlock(p);
                case "fence_gate"->new FenceGateBlock(WoodType.OAK,p);
                case "door"->new DoorBlock(BlockSetType.OAK,p.noOcclusion());
                case "trapdoor"->new TrapDoorBlock(BlockSetType.OAK,p.noOcclusion());
                case "button"->new ButtonBlock(BlockSetType.OAK,30,p.noCollision()){};
                case "pressure_plate"->new PressurePlateBlock(BlockSetType.OAK,p.noCollision()){};
                case "sign"->new StandingSignBlock(WoodType.OAK,p.noCollision());
                case "wall_sign"->new WallSignBlock(WoodType.OAK,p.noCollision());
                case "hanging_sign"->new CeilingHangingSignBlock(WoodType.OAK,p.noCollision());
                case "wall_hanging_sign"->new WallHangingSignBlock(WoodType.OAK,p.noCollision());
                case "potted"->n.equals("potted_flytrap")?new OwnerPlants.FlytrapPot(block(group,n.substring(7)),p.instabreak().noOcclusion()):new FlowerPotBlock(block(group,n.substring(7)),p.instabreak().noOcclusion());
                case "flower"->OwnerPlants.create(p.instabreak().noCollision().sound(SoundType.GRASS),d.getAsJsonObject("definition"));
                case "double_plant"->new DoublePlantBlock(p.instabreak().noCollision().sound(SoundType.GRASS));
                case "glass"->n.equals("depth_glass")?new OwnerBuildingStates.ConnectedGlass(p.strength(.3F).sound(SoundType.GLASS).noOcclusion()):new TransparentBlock(p.strength(.3F).sound(SoundType.GLASS).noOcclusion());
                case "sand"->new SandBlock(new net.minecraft.util.ColorRGBA(0xFFC6B38E),p.strength(.5F).sound(SoundType.SAND));
                case "campfire"->new CampfireBlock(n.contains("soul_campfire"),n.contains("soul_campfire")?2:1,p.strength(2).noOcclusion().lightLevel(s->s.getValue(CampfireBlock.LIT)?12:0));
                case "building"->new Block(p);
                case "cavern"->n.equals("nuclear_furnace")?new BlastFurnaceBlock(p):HouseholdBlock.create(p.noOcclusion().randomTicks(),d.getAsJsonObject("definition"));
                // These supplied cube models inherit their geometry from vanilla parents.
                // The furniture importer has no explicit boxes for them, so its fallback
                // is a two-pixel slab. Use native solid pillars and preserve saved axis IDs.
                default->group.equals("cavern")&&Set.of("cake_layer","block_of_chocolate","licoroot","candy_cane_block","stripped_candy_cane_block").contains(n)
                        ?new RotatedPillarBlock(p):HouseholdBlock.create(p.noOcclusion(),d.getAsJsonObject("definition"));
            };
            Registry.register(BuiltInRegistries.BLOCK,key,b);Menagerie.BLOCKS.put(id,b);
            if(kind.equals("shelf"))((FabricBlockEntityType)JugcraftAgriculture.KITCHEN_CABINET_ENTITY).addValidBlock(b);
            if(kind.equals("sign")||kind.equals("wall_sign"))((FabricBlockEntityType)BlockEntityTypes.SIGN).addValidBlock(b);
            if(kind.equals("hanging_sign")||kind.equals("wall_hanging_sign"))((FabricBlockEntityType)BlockEntityTypes.HANGING_SIGN).addValidBlock(b);
            if(kind.equals("campfire"))((FabricBlockEntityType)BlockEntityTypes.CAMPFIRE).addValidBlock(b);
            if(b instanceof BlastFurnaceBlock)((FabricBlockEntityType)BlockEntityTypes.BLAST_FURNACE).addValidBlock(b);
            if(d.get("wood").getAsBoolean())FlammableBlockRegistry.getDefaultInstance().add(b,5,kind.equals("leaves")?60:20);
        }
        for(var entry:root.getAsJsonObject("items").entrySet()){
            String id=entry.getKey(),n=entry.getValue().getAsJsonObject().get("source").getAsString();if(Menagerie.ITEMS.containsKey(id))continue;
            if(n.endsWith("_chest_boat")){boat(group,n.replace("_chest_boat",""),true);continue;}if(n.endsWith("_boat")){boat(group,n.replace("_boat",""),false);continue;}
            Menagerie.item(id,p->{
                if(n.endsWith("_hanging_sign")&&!n.endsWith("_wall_hanging_sign"))return new HangingSignItem(block(group,n),block(group,n.replace("_hanging_sign","_wall_hanging_sign")),p.stacksTo(16));
                if(n.endsWith("_sign")&&!n.endsWith("_wall_sign"))return new StandingAndWallBlockItem(block(group,n),block(group,n.replace("_sign","_wall_sign")),Direction.DOWN,p.stacksTo(16));
                if(Menagerie.BLOCKS.containsKey(id))return new BlockItem(Menagerie.BLOCKS.get(id),p.useBlockDescriptionPrefix());
                if(n.startsWith("music_disc_"))p.stacksTo(1).jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG,Jugcraft.id(group+"_"+n.substring(11))));
                return group.equals("cavern")?Caverns.item(n,entry.getValue().getAsJsonObject(),p):LandscapeRelics.create(n,entry.getValue().getAsJsonObject(),p);
            });
        }
    }
    private static void boat(String group,String wood,boolean chest){
        String id=group+"_"+wood+(chest?"_chest_boat":"_boat");var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id(id));
        if(chest){var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<ChestBoat>of((t,l)->new ChestBoat(t,l,()->Menagerie.item(id)),MobCategory.MISC).sized(1.375F,.5625F).clientTrackingRange(10).build(key));CHEST_BOATS.put(id,type);Menagerie.item(id,p->new BoatItem(type,p.stacksTo(1)));}
        else{var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Boat>of((t,l)->new Boat(t,l,()->Menagerie.item(id)),MobCategory.MISC).sized(1.375F,.5625F).clientTrackingRange(10).build(key));BOATS.put(id,type);Menagerie.item(id,p->new BoatItem(type,p.stacksTo(1)));}
        BOAT_TEXTURES.put(id,"textures/menagerie/"+group+"/entity/"+(chest?"chest_boat/":"boat/")+wood+".png");
    }
}
