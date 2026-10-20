package io.github.jimbozoomer.jugcraft.nouveau;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
public final class NouveauSpawns {
    private NouveauSpawns(){}
    public static <T extends Mob> void register(EntityType<T> type,boolean hostile){SpawnPlacements.register(type,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(t,world,reason,pos,random)->eligible(t,world,reason,pos,random,hostile));}
    private static boolean eligible(EntityType<? extends Mob> type,ServerLevelAccessor world,EntitySpawnReason reason,BlockPos pos,RandomSource random,boolean hostile){
        if(!JugNouveau.enabled())return false;
        if(hostile&&(world.getDifficulty()==Difficulty.PEACEFUL||world.getRawBrightness(pos,0)>7))return false;
        return Mob.checkMobSpawnRules(type,world,reason,pos,random)&&world.getBlockState(pos.below()).isSolidRender();
    }
}
