package io.github.jimbozoomer.jugcraft.creatures.scary;

import net.fabricmc.fabric.api.biome.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;

/** Natural spawning policy and extension points. See docs/features/scary-creatures.md before changing seasons/dimensions. */
public final class ScarySpawns {
    public static final TagKey<EntityType<?>> HALLOWEEN = TagKey.create(Registries.ENTITY_TYPE, ScaryMod.id("season/halloween"));
    public static final TagKey<EntityType<?>> DISABLED = TagKey.create(Registries.ENTITY_TYPE, ScaryMod.id("disabled_natural_spawns"));
    public static final TagKey<Structure> KNIGHT_STRUCTURES = TagKey.create(Registries.STRUCTURE, ScaryMod.id("black_knight_spawn_structures"));
    public static final int LOCAL_RADIUS = 64;

    public static void initialize() {
        var biomes=BiomeSelectors.foundInOverworld().and(BiomeSelectors.excludeByKey(Biomes.MUSHROOM_FIELDS));
        BiomeModifications.addSpawn(biomes,MobCategory.MONSTER,ScaryMod.SPACE_KOOK,3,1,2);
        // All eligible biomes: includes rivers, oceans, lakes and player-made outdoor water.
        BiomeModifications.addSpawn(biomes,MobCategory.MONSTER,ScaryMod.CAPTAIN_CUTLER,8,1,2);
        BiomeModifications.addSpawn(biomes,MobCategory.MONSTER,ScaryMod.PHANTOM_SHADOW,30,1,2);
        BiomeModifications.addSpawn(biomes,MobCategory.MONSTER,ScaryMod.BLACK_KNIGHT,3,1,2);
        SpawnPlacements.register(ScaryMod.SPACE_KOOK,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,ScarySpawns::surface);
        SpawnPlacements.register(ScaryMod.PHANTOM_SHADOW,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,ScarySpawns::surface);
        SpawnPlacements.register(ScaryMod.BLACK_KNIGHT,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,ScarySpawns::surface);
        SpawnPlacements.register(ScaryMod.CAPTAIN_CUTLER,SpawnPlacementTypes.IN_WATER,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,ScarySpawns::water);
    }

    public static boolean isNight(long ticks) {
        long time=Math.floorMod(ticks,24000L);
        return time>=13000 && time<23000;
    }

    private static boolean eligible(EntityType<?> type,ServerLevelAccessor world,BlockPos pos) {
        var level=world.getLevel();
        // Future moon/planet policy belongs here; do not automatically inherit Earth night rules.
        return level.dimension().equals(Level.OVERWORLD)
            && io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason.active()
            && level.getDifficulty()!=Difficulty.PEACEFUL
            && !type.builtInRegistryHolder().is(DISABLED)
            && isNight(level.getOverworldClockTime())
            && belowLocalLimit(type,world,pos);
    }

    public static boolean belowLocalLimit(EntityType<?> type,ServerLevelAccessor world,BlockPos pos) {
        if(type==ScaryMod.PHANTOM_SHADOW)return true;
        var level=world.getLevel();
        var bounds=new AABB(pos.getX()-LOCAL_RADIUS,level.getMinY(),pos.getZ()-LOCAL_RADIUS,
            pos.getX()+LOCAL_RADIUS+1,level.getMaxY(),pos.getZ()+LOCAL_RADIUS+1);
        return level.getEntitiesOfClass(Mob.class,bounds,m->m.isAlive() && m.getType()==type
            && Math.pow(m.getX()-(pos.getX()+.5),2)+Math.pow(m.getZ()-(pos.getZ()+.5),2)<=LOCAL_RADIUS*LOCAL_RADIUS).size()<2;
    }

    public static boolean surface(EntityType<? extends Mob> type,ServerLevelAccessor world,EntitySpawnReason reason,BlockPos pos,RandomSource random) {
        if(reason!=EntitySpawnReason.NATURAL && reason!=EntitySpawnReason.CHUNK_GENERATION)
            return Monster.checkMonsterSpawnRules(type,world,reason,pos,random);
        if(!eligible(type,world,pos))return false;
        boolean outdoors=pos.getY()>=world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos.getX(),pos.getZ());
        boolean inKnightStructure=type==ScaryMod.BLACK_KNIGHT
            && world.getLevel().structureManager().getStructureWithPieceAt(pos,KNIGHT_STRUCTURES).isValid();
        return (outdoors || inKnightStructure) && Monster.checkMonsterSpawnRules(type,world,reason,pos,random);
    }

    public static boolean water(EntityType<? extends Mob> type,ServerLevelAccessor world,EntitySpawnReason reason,BlockPos pos,RandomSource random) {
        if(!world.getFluidState(pos).is(FluidTags.WATER) || !world.getFluidState(pos.above()).is(FluidTags.WATER))return false;
        if(reason!=EntitySpawnReason.NATURAL && reason!=EntitySpawnReason.CHUNK_GENERATION)return true;
        // OCEAN_FLOOR ignores water but catches roofs, including in pools above sea level.
        return eligible(type,world,pos)
            && pos.getY()>=world.getHeight(Heightmap.Types.OCEAN_FLOOR,pos.getX(),pos.getZ())
            && Monster.isDarkEnoughToSpawn(world,pos,random);
    }
}
