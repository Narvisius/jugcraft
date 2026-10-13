package local.peepo;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Planning snapshots only. Claims, processing and transfers always use their live validation paths. */
public final class CompanionReadiness {
    private record Fact(long tick, CompanionStatus status) {}
    // Weak keys; values never retain blocks, levels, players or companions.
    private static final Map<BlockEntity, Fact> FACTS = new WeakHashMap<>();
    public static CompanionStatus shared(BlockEntity block, Supplier<CompanionStatus> read) {
        if (block.getLevel()==null || block.isRemoved()) return CompanionStatus.MISSING;
        long now=block.getLevel().getGameTime();
        var fact=FACTS.get(block);
        if(fact==null || fact.tick!=now){fact=new Fact(now,read.get());FACTS.put(block,fact);}
        return fact.status;
    }
    public static void invalidate(BlockEntity block){FACTS.remove(block);}

    private final PeepoEntity npc;
    private record Resolved(BlockEntity block,CompanionJob job){}
    private final Map<BlockPos, Resolved> jobs=new HashMap<>();
    private final Map<CompanionJob, CompanionStatus> statuses=new IdentityHashMap<>();
    private long tick=Long.MIN_VALUE;
    CompanionReadiness(PeepoEntity npc){this.npc=npc;}
    public void clear(){jobs.clear();statuses.clear();tick=Long.MIN_VALUE;}
    private void refresh(){if(tick!=npc.level().getGameTime()){clear();tick=npc.level().getGameTime();}}
    CompanionJob resolve(BlockPos pos){
        refresh();
        if(!npc.level().hasChunkAt(pos))return null;
        var block=npc.level().getBlockEntity(pos);var found=jobs.get(pos);
        if(found==null || found.block!=block){
            if(jobs.size()>=32){jobs.clear();statuses.clear();}
            found=new Resolved(block,CompanionJobs.resolveFresh(npc,pos));jobs.put(pos.immutable(),found);
        }
        return found.job;
    }
    CompanionStatus status(CompanionJob job){
        refresh();
        // These checks must never inherit another companion's ownership or reservation result.
        if(!npc.level().hasChunkAt(job.stationPosition()))return CompanionStatus.UNLOADED;
        if(!CompanionJobs.permitted(npc,job.stationPosition()))return CompanionStatus.FORBIDDEN;
        if(!job.availableTo(npc))return CompanionStatus.OCCUPIED;
        return statuses.computeIfAbsent(job,j->j.planningFacts(npc));
    }
}
