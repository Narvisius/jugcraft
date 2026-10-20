package io.github.jimbozoomer.jugcraft.menagerie;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Chunk-local new-generation chamber excavation, clipped to the selected cave biome. */
public record CavernChamber(String biome,String stone,String floor,int waterline) implements Feature {
    public static final MapCodec<CavernChamber> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Codec.STRING.fieldOf("biome").forGetter(CavernChamber::biome),Codec.STRING.fieldOf("stone").forGetter(CavernChamber::stone),Codec.STRING.fieldOf("floor").forGetter(CavernChamber::floor),Codec.intRange(-64,48).optionalFieldOf("waterline",-64).forGetter(CavernChamber::waterline)
    ).apply(i,CavernChamber::new));
    @Override public MapCodec<? extends Feature> codec(){return CODEC;}
    @Override public boolean place(WorldGenLevel l,ChunkGenerator generator,RandomSource random,BlockPos origin){
        int startX=Math.floorDiv(origin.getX(),16)*16,startZ=Math.floorDiv(origin.getZ(),16)*16;
        var wall=BuiltInRegistries.BLOCK.getValue(Identifier.parse(stone)).defaultBlockState();var ground=BuiltInRegistries.BLOCK.getValue(Identifier.parse(floor)).defaultBlockState();
        boolean placed=false;var key=Identifier.parse(biome);var pos=new BlockPos.MutableBlockPos();
        for(int x=startX;x<startX+16;x++)for(int z=startZ;z<startZ+16;z++){
            // Continuous floor/roof undulation avoids seams at chunk borders.
            int bottom=-36+(int)(Math.sin((x+(l.getSeed()&255))*.027)*3+Math.cos(z*.021)*3);
            int top=12+(int)(Math.sin(x*.019)*5+Math.cos((z+(l.getSeed()&127))*.025)*4);
            for(int y=bottom-3;y<=top+3;y++){
                pos.set(x,y,z);if(!l.ensureCanWrite(pos)||!l.getBiome(pos).is(key))continue;
                var state=l.getBlockState(pos);if(state.is(Blocks.BEDROCK)||l.getBlockEntity(pos)!=null)continue;
                if(y<=bottom){l.setBlock(pos,y==bottom?ground:wall,Block.UPDATE_CLIENTS);placed=true;}
                else if(y>=top){l.setBlock(pos,wall,Block.UPDATE_CLIENTS);placed=true;}
                else {l.setBlock(pos,y<=waterline?Blocks.WATER.defaultBlockState():Blocks.CAVE_AIR.defaultBlockState(),Block.UPDATE_CLIENTS);placed=true;}
            }
        }return placed;
    }
}
