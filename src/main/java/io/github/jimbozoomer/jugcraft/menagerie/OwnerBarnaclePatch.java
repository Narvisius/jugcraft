package io.github.jimbozoomer.jugcraft.menagerie;

import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.biome.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Source 50 attempts / 5x2 spread / 20-block surface search, only in new terrain. */
public record OwnerBarnaclePatch() implements Feature {
    static final MapCodec<OwnerBarnaclePatch> CODEC=MapCodec.unit(OwnerBarnaclePatch::new);
    static void register(){
        Registry.register(BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("supps_barnacles_patch"),CODEC);
        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OCEAN),GenerationStep.Decoration.VEGETAL_DECORATION,ResourceKey.create(Registries.PLACED_FEATURE,Jugcraft.id("supps_ocean_barnacles")));
        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_BEACH),GenerationStep.Decoration.VEGETAL_DECORATION,ResourceKey.create(Registries.PLACED_FEATURE,Jugcraft.id("supps_shore_barnacles")));
    }
    @Override public MapCodec<OwnerBarnaclePatch> codec(){return CODEC;}
    @Override public boolean place(WorldGenLevel level,ChunkGenerator generator,RandomSource random,BlockPos origin){
        var block=(OwnerBarnacles)OwnerSupplementaries.BLOCKS.get("barnacles");boolean changed=false;
        for(int i=0;i<50;i++){
            var start=origin.offset(random.nextInt(6)-random.nextInt(6),random.nextInt(3)-random.nextInt(3),random.nextInt(6)-random.nextInt(6));
            var search=Direction.values()[random.nextInt(6)];
            for(int n=0;n<20;n++){
                var pos=start.relative(search,n);if(!level.ensureCanWrite(pos)||!level.ensureCanWrite(pos.relative(search)))break;
                var old=level.getBlockState(pos);if(!old.is(Blocks.WATER)&&!old.is(block))break;
                boolean grew=false;
                for(var face:Direction.values()){
                    if(!level.ensureCanWrite(pos.relative(face)))continue;
                    var next=block.getStateForPlacement(old,level,pos,face);
                    if(next!=null&&!next.equals(old)){grew|=level.setBlock(pos,next,2);old=next;}
                }
                changed|=grew;if(grew)break;
            }
        }return changed;
    }
}
