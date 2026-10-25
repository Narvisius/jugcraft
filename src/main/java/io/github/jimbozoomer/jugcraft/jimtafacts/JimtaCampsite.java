package io.github.jimbozoomer.jugcraft.jimtafacts;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Underground campsite assembled from the supplied weighted palette. Full footprint checked before any write. */
public record JimtaCampsite(Map<String,List<BlockState>> palette) implements Feature {
    public static final MapCodec<JimtaCampsite> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Codec.unboundedMap(Codec.STRING,BlockState.CODEC.listOf(1,128)).fieldOf("palette").forGetter(JimtaCampsite::palette)).apply(i,JimtaCampsite::new));
    public MapCodec<? extends Feature> codec(){return CODEC;}
    private BlockState choose(String name,RandomSource random,BlockState fallback){var states=palette.get(name);return states==null||states.isEmpty()?fallback:states.get(random.nextInt(states.size()));}
    @Override public boolean place(WorldGenLevel level,ChunkGenerator generator,RandomSource random,BlockPos origin){
        if(origin.getY()>40||origin.getY()<level.getMinY()+5)return false;
        BlockPos at=origin;
        for(int dy=0;dy<12&&level.getBlockState(at.below()).isAir();dy++)at=at.below();
        // Avoid water, skylit sites, fragile floors, buried rooms and existing constructions.
        if(level.canSeeSky(at))return false;
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
            var floor=at.offset(x,-1,z);
            if(!level.ensureCanWrite(floor)||!level.getBlockState(floor).isSolidRender())return false;
            for(int y=0;y<=3;y++){var p=at.offset(x,y,z);if(!level.ensureCanWrite(p)||!level.getBlockState(p).isAir())return false;}
        }
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(random.nextBoolean())level.setBlock(at.offset(x,-1,z),choose("floor",random,Blocks.COARSE_DIRT.defaultBlockState()),Block.UPDATE_CLIENTS);
        level.setBlock(at,choose("unlit_campfires",random,Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT,false)),Block.UPDATE_CLIENTS);
        level.setBlock(at.offset(-2,0,-1),choose("crafting_stations",random,Blocks.CRAFTING_TABLE.defaultBlockState()),Block.UPDATE_CLIENTS);
        level.setBlock(at.offset(-2,0,1),choose("furnaces",random,Blocks.FURNACE.defaultBlockState()),Block.UPDATE_CLIENTS);
        level.setBlock(at.offset(-2,1,1),choose("furnace_chimneys",random,Blocks.COBBLESTONE_WALL.defaultBlockState()),Block.UPDATE_CLIENTS);
        level.setBlock(at.offset(0,0,2),choose("light_sources",random,Blocks.LANTERN.defaultBlockState()),Block.UPDATE_CLIENTS);
        BlockPos foot=at.offset(2,0,-1);BlockState bed=choose("beds",random,Blocks.BED.red().defaultBlockState());
        if(bed.hasProperty(BedBlock.PART)){bed=bed.setValue(BedBlock.PART,BedPart.FOOT).setValue(BedBlock.FACING,Direction.SOUTH);level.setBlock(foot,bed,Block.UPDATE_CLIENTS);level.setBlock(foot.south(),bed.setValue(BedBlock.PART,BedPart.HEAD),Block.UPDATE_CLIENTS);}
        BlockPos treasure=at.offset(0,0,-2);
        if(random.nextInt(4)==0){var mimic=JimtaMimic.TYPE.create(level.getLevel(),EntitySpawnReason.STRUCTURE);if(mimic!=null){mimic.snapTo(treasure,180,0);level.addFreshEntity(mimic);}}
        else{level.setBlock(treasure,choose("chest",random,Blocks.CHEST.defaultBlockState()),Block.UPDATE_CLIENTS);
            if(level.getBlockEntity(treasure) instanceof RandomizableContainerBlockEntity container)container.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("jimtafacts/chests/campsite_chest")),random.nextLong());}
        BlockPos supplies=at.offset(-2,0,2);level.setBlock(supplies,Blocks.BARREL.defaultBlockState(),Block.UPDATE_CLIENTS);
        if(level.getBlockEntity(supplies) instanceof RandomizableContainerBlockEntity container)container.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("jimtafacts/chests/campsite_barrel")),random.nextLong());
        return true;
    }
}
