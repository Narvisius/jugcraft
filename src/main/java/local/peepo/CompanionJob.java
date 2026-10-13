package local.peepo;

/** A productive station. The job owns its costs/atomic processing; the routine owns travel and cancellation. */
public interface CompanionJob extends CompanionStation {
    default WorkAnimation animation(){return WorkAnimation.INTERACT;}
    default net.minecraft.core.BlockPos animationTarget(){return stationPosition();}
    /** Cheap loaded-state query. Never search for workers or perform recipes here. */
    CompanionStatus workStatus(PeepoEntity npc);
    /** Reusable status for scheduler/UI reads, never for an actual work operation. */
    default CompanionStatus planningStatus(PeepoEntity npc){return npc.readiness.status(this);}
    default CompanionStatus planningFacts(PeepoEntity npc){return workStatus(npc);}
    /** Perform bounded work, or register presence for a ready processor to debit during its validated tick. */
    CompanionStatus work(PeepoEntity npc);
    /** Avoid travelling to a nearly full machine for a fraction of a second of work. */
    default boolean worthStarting(PeepoEntity npc){return planningStatus(npc)==CompanionStatus.READY;}
}
