package io.github.jimbozoomer.jugcraft.menagerie;
import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.biome.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Supplied bounded riverbank-patch shape; new terrain only. */
public record OwnerSupplementFlora() implements Feature {
    static final MapCodec<OwnerSupplementFlora> CODEC=MapCodec.unit(OwnerSupplementFlora::new);
    static void register(){
        Registry.register(BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("supps_wild_flax_patch"),CODEC);
        BiomeModifications.addFeature(BiomeSelectors.tag(TagKey.create(Registries.BIOME,Jugcraft.id("supps_has_wild_flax"))),GenerationStep.Decoration.VEGETAL_DECORATION,ResourceKey.create(Registries.PLACED_FEATURE,Jugcraft.id("supps_wild_flax")));
    }
    @Override public MapCodec<OwnerSupplementFlora> codec(){return CODEC;}
    @Override public boolean place(WorldGenLevel level,ChunkGenerator generator,RandomSource random,BlockPos origin){
        boolean placed=false;var plant=OwnerSupplementaries.BLOCKS.get("wild_flax").defaultBlockState();
        for(int i=0;i<35;i++){
            var pos=origin.offset(random.nextInt(5)-random.nextInt(5),0,random.nextInt(5)-random.nextInt(5));
            if(!level.ensureCanWrite(pos)||!level.getBlockState(pos).isAir()||!plant.canSurvive(level,pos))continue;
            boolean wet=false;for(var dir:Direction.Plane.HORIZONTAL)if(level.getFluidState(pos.below().relative(dir)).is(FluidTags.WATER)){wet=true;break;}
            if(wet)placed|=level.setBlock(pos,plant,2);
        }return placed;
    }
}
