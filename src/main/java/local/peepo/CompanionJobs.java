package local.peepo;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import io.github.jimbozoomer.jugcraft.town.TownProtection;

/** Explicit adapters for future machines. Resolution touches one loaded block; there is no worker scan. */
public final class CompanionJobs {
    public interface Adapter { CompanionJob resolve(PeepoEntity npc,BlockEntity block); }
    private static final List<Adapter> ADAPTERS=new ArrayList<>();
    public static void register(Adapter adapter){ADAPTERS.add(Objects.requireNonNull(adapter));}
    public static CompanionJob resolve(PeepoEntity npc,BlockPos pos){
        return npc.readiness.resolve(pos);
    }
    static CompanionJob resolveFresh(PeepoEntity npc,BlockPos pos){
        if(!npc.level().hasChunkAt(pos))return null;
        var livestock=npc.livestock.job(pos);if(livestock!=null)return livestock;
        var garden=npc.garden.job(pos);if(garden!=null)return garden;
        var be=npc.level().getBlockEntity(pos);if(be==null || be.isRemoved())return null;
        if(be instanceof io.github.jimbozoomer.jugcraft.kinetic.HandCrankBlockEntity crank)return crank.companionJob.prepare(npc);
        if(be instanceof io.github.jimbozoomer.jugcraft.agriculture.CiderPressBlockEntity press)return press.companionJob.prepare(npc);
        if(be instanceof io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity pot)return pot.companionJob.prepare(npc);
        if(be instanceof io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity machine){
            var job=machine.companionJob(npc);if(job!=null)return job;
        }
        if(be instanceof io.github.jimbozoomer.jugcraft.agriculture.CuttingBoardBlockEntity board)return board.companionKitchen.job.prepare(npc);
        if(be instanceof CompanionJob job)return job;
        for(var adapter:ADAPTERS){var job=adapter.resolve(npc,be);if(job!=null)return job;}
        return null;
    }
    public static boolean permitted(PeepoEntity npc,BlockPos pos){
        return npc.level().hasChunkAt(pos) && !TownProtection.shieldsBlock(npc.level(),pos);
    }
    public static CompanionStatus inspect(PeepoEntity npc,CompanionAssignments.Target target){
        if(target==null)return CompanionStatus.NONE;
        if(!target.local(npc.level()))return CompanionStatus.OTHER_DIMENSION;
        if(!npc.level().hasChunkAt(target.at().pos()))return CompanionStatus.UNLOADED;
        if(!target.present(npc.level()))return CompanionStatus.MISSING;
        if(!permitted(npc,target.at().pos()))return CompanionStatus.FORBIDDEN;
        var job=resolve(npc,target.at().pos());if(job!=null)return npc.readiness.status(job);
        var port=CompanionLogistics.resolve(npc,target);return port==null?CompanionStatus.UNSUPPORTED:port.status();
    }
}
