package io.github.jimbozoomer.jugcraft.menagerie;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Bounded native patch placement retaining the supplied vegetation predicates. */
public record OwnerPatchFeature(PlacedFeature feature,int tries,int horizontal,int vertical) implements Feature {
    public static final MapCodec<OwnerPatchFeature> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        PlacedFeature.DIRECT_CODEC.fieldOf("feature").forGetter(OwnerPatchFeature::feature),
        Codec.intRange(1,128).fieldOf("tries").forGetter(OwnerPatchFeature::tries),
        Codec.intRange(0,16).fieldOf("xz_spread").forGetter(OwnerPatchFeature::horizontal),
        Codec.intRange(0,16).fieldOf("y_spread").forGetter(OwnerPatchFeature::vertical)
    ).apply(i,OwnerPatchFeature::new));
    @Override public MapCodec<? extends Feature> codec(){return CODEC;}
    @Override public boolean place(WorldGenLevel level,ChunkGenerator generator,RandomSource random,BlockPos origin){
        boolean placed=false;
        for(int i=0;i<tries;i++){
            var at=origin.offset(random.nextInt(horizontal+1)-random.nextInt(horizontal+1),random.nextInt(vertical+1)-random.nextInt(vertical+1),random.nextInt(horizontal+1)-random.nextInt(horizontal+1));
            if(level.ensureCanWrite(at))placed|=feature.place(level,generator,random,at);
        }
        return placed;
    }
}
