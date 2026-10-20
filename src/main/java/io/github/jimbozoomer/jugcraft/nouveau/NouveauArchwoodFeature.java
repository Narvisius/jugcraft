package io.github.jimbozoomer.jugcraft.nouveau;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Broad-crowned archwoods with branch-attached fruit. The complete footprint is checked before placement. */
public record NouveauArchwoodFeature(String color,int height,boolean fruit) implements Feature {
    public static final MapCodec<NouveauArchwoodFeature> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Codec.STRING.fieldOf("color").forGetter(NouveauArchwoodFeature::color),Codec.intRange(6,16).optionalFieldOf("height",9).forGetter(NouveauArchwoodFeature::height),Codec.BOOL.optionalFieldOf("fruit",true).forGetter(NouveauArchwoodFeature::fruit)).apply(i,NouveauArchwoodFeature::new));
    public MapCodec<? extends Feature> codec(){return CODEC;}
    public boolean place(WorldGenLevel level,ChunkGenerator generator,RandomSource random,BlockPos origin){
        if(!JugNouveau.enabled()||!NouveauNature.COLORS.contains(color)||!level.getBlockState(origin.below()).is(BlockTags.DIRT))return false;
        int tall=height+random.nextInt(3);var log=NouveauCatalog.block(color+"_archwood_log");var leaf=NouveauCatalog.block(color+"_archwood_leaves");var logs=new LinkedHashMap<BlockPos,BlockState>();
        for(int y=0;y<tall;y++)logs.put(origin.above(y),log.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Y));
        for(var d:Direction.Plane.HORIZONTAL)for(int n=1;n<=2;n++)logs.put(origin.above(tall-3+(n/2)).relative(d,n),log.defaultBlockState().setValue(RotatedPillarBlock.AXIS,d.getAxis()));
        var places=new LinkedHashMap<BlockPos,BlockState>(logs);
        for(int y=tall-4;y<=tall+1;y++){int radius=y==tall+1?1:y==tall?2:3;for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(x*x+z*z>radius*radius+1)continue;var pos=origin.offset(x,y,z);if(logs.containsKey(pos))continue;int distance=logs.keySet().stream().mapToInt(p->Math.abs(p.getX()-pos.getX())+Math.abs(p.getY()-pos.getY())+Math.abs(p.getZ()-pos.getZ())).min().orElse(6);
            places.put(pos,leaf.defaultBlockState().setValue(LeavesBlock.DISTANCE,Math.clamp(distance,1,6)).setValue(LeavesBlock.PERSISTENT,false));
        }}
        for(var at:places.keySet()){if(at.getY()<level.getMinY()||at.getY()>=level.getMaxY()||!level.isStateAtPosition(at,s->s.isAir()||s.is(BlockTags.REPLACEABLE_BY_TREES)||s.is(BlockTags.LEAVES)))return false;}
        places.forEach((pos,state)->level.setBlock(pos,state,Block.UPDATE_CLIENTS));
        if(fruit&&random.nextBoolean()){
            var d=Direction.Plane.HORIZONTAL.getRandomDirection(random);var at=origin.above(2+random.nextInt(Math.max(1,tall-4))).relative(d);String pod=switch(color){case "red"->"bombegranate_pod";case "blue"->"frostaya_pod";case "green"->"mendosteen_pod";default->"bastion_pod";};
            if(level.getBlockState(at).isAir())level.setBlock(at,NouveauCatalog.block(pod).defaultBlockState().setValue(NouveauNature.Archfruit.AGE,2).setValue(NouveauNature.Archfruit.FACING,d),Block.UPDATE_CLIENTS);
        }
        return true;
    }
}
