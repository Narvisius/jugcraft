package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.biome.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.feature.*;

import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Bounded decorations in newly generated terrain, never a loaded-chunk retrofit. */
public final class MenagerieHabitats implements Feature {
    public static final com.mojang.serialization.MapCodec<MenagerieHabitats> CODEC=com.mojang.serialization.MapCodec.unit(MenagerieHabitats::new);
    @Override public com.mojang.serialization.MapCodec<MenagerieHabitats> codec(){return CODEC;}
    public static void register(){
        Registry.register(BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("menagerie_habitat"),CODEC);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),GenerationStep.Decoration.VEGETAL_DECORATION,ResourceKey.create(Registries.PLACED_FEATURE,Jugcraft.id("menagerie_habitat")));
    }
    @Override public boolean place(net.minecraft.world.level.WorldGenLevel l,net.minecraft.world.level.chunk.ChunkGenerator generator,net.minecraft.util.RandomSource r,BlockPos origin){
        if(!io.github.jimbozoomer.jugcraft.config.JugcraftConfig.isFeatureEnabled("agriculture"))return false;
        boolean changed=false;
        for(int i=0;i<4;i++){
            BlockPos at=l.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR_WG,origin.offset(r.nextInt(8),0,r.nextInt(8)));
            if(!l.ensureCanWrite(at))continue;
            if(l.getBlockState(at).is(Blocks.WATER)){
                if(r.nextBoolean())changed|=l.setBlock(at.below(),Menagerie.BLOCKS.get("wildlife_shellstone").defaultBlockState(),2);
            }else if(l.getBlockState(at).isAir()&&l.getBlockState(at.below()).is(net.minecraft.tags.BlockTags.DIRT)){
                String plant=r.nextInt(4)==0?"wildlife_ant_hill":"wildlife_snail_eggs";
                var state=Menagerie.BLOCKS.get(plant).defaultBlockState();
                if(plant.equals("wildlife_ant_hill")){
                    var queen=(net.minecraft.world.level.block.state.properties.BooleanProperty)state.getBlock().getStateDefinition().getProperty("has_queen");
                    if(queen!=null)state=state.setValue(queen,true);
                }
                changed|=l.setBlock(at,state,2);
            }
        }return changed;
    }
}
