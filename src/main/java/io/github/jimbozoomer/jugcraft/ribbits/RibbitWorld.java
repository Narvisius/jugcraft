package io.github.jimbozoomer.jugcraft.ribbits;

import com.google.gson.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/** Adapts supplied palettes to native generation, never a loaded-world retrofit. */
final class RibbitWorld {
    private static final Map<String,JsonArray> PALETTES=new HashMap<>();
    static void register(){
        PALETTES.put("main",OwnerBackpacks.source("ribbits_processors/main.json").getAsJsonArray("processors"));
        Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR,Jugcraft.id("ribbits_village"),Village.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("ribbits_vegetation"),Vegetation.CODEC);
    }
    private static BlockState state(JsonObject d){
        var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse(d.get("Name").getAsString()));var state=block.defaultBlockState();
        if(d.has("Properties"))for(var entry:d.getAsJsonObject("Properties").entrySet()){var property=block.getStateDefinition().getProperty(entry.getKey());if(property!=null)state=value(state,property,entry.getValue().getAsString());}return state;
    }
    private static <T extends Comparable<T>> BlockState value(BlockState state,Property<T> prop,String value){return prop.getValue(value).map(v->state.setValue(prop,v)).orElse(state);}
    private static <T extends Comparable<T>> BlockState copy(BlockState from,BlockState to,Property<T> prop){return to.hasProperty(prop)?to.setValue(prop,from.getValue(prop)):to;}
    private static BlockState choose(JsonObject d,RandomSource random){
        double roll=random.nextDouble();for(var value:d.getAsJsonArray("entries")){var e=value.getAsJsonObject();roll-=e.get("probability").getAsDouble();if(roll<0)return state(e.getAsJsonObject("blockState"));}return state(d.getAsJsonObject("defaultBlockState"));
    }
    record Village(String palette) implements StructureProcessor {
        static final MapCodec<Village> CODEC=Codec.STRING.fieldOf("palette").xmap(Village::new,Village::palette);
        @Override public MapCodec<Village> codec(){return CODEC;}
        @Override public StructureTemplate.StructureBlockInfo processBlock(LevelReader level,BlockPos origin,BlockPos center,BlockPos local,StructureTemplate.StructureBlockInfo info,StructurePlaceSettings settings){
            var rules=PALETTES.get(palette);if(rules==null)return info;var state=info.state();var random=settings.getRandom(info.pos());
            for(var v:rules){
                var d=v.getAsJsonObject();String kind=d.get("processor_type").getAsString();
                if(kind.endsWith("pillar_processor")&&state.is(state(d.getAsJsonObject("target_block")).getBlock())){
                    state=state(d.getAsJsonObject("target_block_output"));
                    // Authored marker becomes its floor block; supports descend only in this generating chunk.
                    if(level instanceof WorldGenRegion region&&region.getCenter().equals(ChunkPos.containing(info.pos()))){
                        var p=info.pos().below();for(int i=0;i<32&&p.getY()>level.getMinY();i++,p=p.below()){
                            if(!region.ensureCanWrite(p))break;var old=region.getBlockState(p);
                            if(!old.isAir()&&old.getFluidState().isEmpty()&&!old.canBeReplaced())break;
                            region.setBlock(p,choose(d.getAsJsonObject("pillar_states"),random),2);
                        }
                    }
                }else if(kind.endsWith("block_replace_processor")&&state.is(state(d.getAsJsonObject("target_block")).getBlock())){
                    var next=choose(d.getAsJsonObject("output"),random);if(d.has("copy_input_properties")&&d.get("copy_input_properties").getAsBoolean())for(var property:state.getProperties())next=copy(state,next,property);state=next;
                }else if(kind.endsWith("lapis_block_processor")&&state.is(Blocks.LAPIS_BLOCK))state=Blocks.WATER.defaultBlockState();
                else if(kind.endsWith("warped_nylium_processor")&&state.is(Blocks.WARPED_NYLIUM))state=Blocks.MOSS_BLOCK.defaultBlockState();
            }
            return new StructureTemplate.StructureBlockInfo(info.pos(),state,info.nbt());
        }
    }
    record Vegetation(String kind,int tries,int spread,int height) implements Feature {
        static final MapCodec<Vegetation> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.STRING.fieldOf("kind").forGetter(Vegetation::kind),Codec.intRange(1,64).fieldOf("tries").forGetter(Vegetation::tries),Codec.intRange(0,8).fieldOf("spread").forGetter(Vegetation::spread),Codec.intRange(0,4).fieldOf("height").forGetter(Vegetation::height)).apply(i,Vegetation::new));
        @Override public MapCodec<Vegetation> codec(){return CODEC;}
        @Override public boolean place(WorldGenLevel level,ChunkGenerator generator,RandomSource random,BlockPos origin){
            boolean placed=false;for(int i=0;i<tries;i++){
                var pos=origin.offset(random.nextInt(spread+1)-random.nextInt(spread+1),random.nextInt(height+1)-random.nextInt(height+1),random.nextInt(spread+1)-random.nextInt(spread+1));
                if(!level.ensureCanWrite(pos)||!level.getBlockState(pos).isAir())continue;
                var below=level.getBlockState(pos.below());boolean water=below.getFluidState().is(FluidTags.WATER);String name;
                if(kind.equals("veg_patch")){
                    if(below.is(Blocks.PODZOL)||below.is(Blocks.COARSE_DIRT))continue;
                    int choice=random.nextInt(5);name=water?"giant_lilypad":choice==0?"umbrella_leaf":choice==4?"swamp_daisy":"toadstool";
                }else {name=kind.replace("_patch","");if(water!=name.equals("giant_lilypad"))continue;}
                var block=OwnerRibbits.BLOCKS.get(name);if(block==null)continue;var state=block.defaultBlockState();
                if(state.canSurvive(level,pos))placed|=level.setBlock(pos,state,2);
            }return placed;
        }
    }
}
