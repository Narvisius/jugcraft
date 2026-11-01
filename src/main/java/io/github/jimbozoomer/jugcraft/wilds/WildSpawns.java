package io.github.jimbozoomer.jugcraft.wilds;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/** Source settlement exclusions use placement seeds, never generate/load neighboring chunks. */
final class WildSpawns {
    // Vanilla ON_GROUND rejects leaves before the creature's own surface rule
    // runs. The supplied placement type deliberately permits solid leaf floors.
    static final net.minecraft.world.entity.SpawnPlacementType SOLID_FLOOR=(level,pos,type)->{
        if(type==null||!level.getWorldBorder().isWithinBounds(pos))return false;
        var floor=level.getBlockState(pos.below());
        if(floor.is(net.minecraft.world.level.block.Blocks.BEDROCK)||floor.is(net.minecraft.world.level.block.Blocks.BARRIER)||!floor.isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP))return false;
        var lower=level.getBlockState(pos);var upper=level.getBlockState(pos.above());
        return NaturalSpawner.isValidEmptySpawnBlock(level,pos,lower,lower.getFluidState(),type)&&NaturalSpawner.isValidEmptySpawnBlock(level,pos.above(),upper,upper.getFluidState(),type);
    };
    static boolean outsideSettlements(ServerLevelAccessor access,BlockPos pos,EntitySpawnReason reason,boolean grove){
        var level=access.getLevel();
        if(reason!=EntitySpawnReason.SPAWNER&&level.dimension()!=Level.OVERWORLD)return false;
        var lookup=level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);var state=level.getChunkSource().getGeneratorState();var chunk=ChunkPos.containing(pos);
        for(var id:new Identifier[]{Identifier.withDefaultNamespace("villages"),Identifier.withDefaultNamespace("pillager_outposts"),grove?Jugcraft.id("wilds_umvuthana_grove"):null}){
            if(id==null)continue;var set=lookup.get(ResourceKey.<StructureSet>create(Registries.STRUCTURE_SET,id));if(set.isPresent()&&state.hasStructureChunkInRange(set.get(),chunk.x(),chunk.z(),3))return false;
        }
        return true;
    }
}
