package local.peepo;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Small per-companion route memory. No shared paths, world references or chunk tickets. */
final class CompanionNavigation {
    private record Key(BlockPos station,BlockPos entrance){}
    private final Map<Key,Long> failures=new HashMap<>();
    boolean failed(PeepoEntity npc,BlockPos station,Vec3 point){
        return failures.getOrDefault(new Key(station,BlockPos.containing(point)),0L)>npc.level().getGameTime();
    }
    void reject(PeepoEntity npc,BlockPos station,Vec3 point){
        long now=npc.level().getGameTime();failures.entrySet().removeIf(e->e.getValue()<=now);
        if(failures.size()>=32)failures.clear();
        failures.put(new Key(station.immutable(),BlockPos.containing(point)),now+200);
    }
    void clear(){failures.clear();}

    static final class Progress {
        private Vec3 target,position;
        private long lastProgress,nextCheck;
        void reset(){target=position=null;nextCheck=0;}
        void started(PeepoEntity npc,Vec3 destination){
            target=destination;position=npc.position();lastProgress=npc.level().getGameTime();nextCheck=lastProgress+10;
        }
        void failed(PeepoEntity npc){nextCheck=npc.level().getGameTime()+80;}
        boolean needsPath(PeepoEntity npc,Vec3 destination){
            long now=npc.level().getGameTime();
            if(now<nextCheck)return false;
            if(target==null || destination.distanceToSqr(target)>.25 || npc.getNavigation().isDone())return true;
            nextCheck=now+10;
            if(position.distanceToSqr(npc.position())>.0625){position=npc.position();lastProgress=now;}
            return now-lastProgress>=40;
        }
    }
}
