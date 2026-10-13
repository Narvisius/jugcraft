package local.peepo;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.phys.Vec3;

/** Implement on a loaded wheel/chair/bed block entity. No global registry or chunk loading. */
public interface CompanionStation {
    enum Kind { WHEEL, WORK, CHAIR, BED }
    Kind kind();
    default net.minecraft.core.BlockPos stationPosition(){return this instanceof net.minecraft.world.level.block.entity.BlockEntity be?be.getBlockPos():net.minecraft.core.BlockPos.containing(approachPosition());}
    /** Safe reachable standing/mounting point, outside the block's collision shape. */
    Vec3 approachPosition();
    /** Let a job choose another entrance after a budgeted path attempt failed. */
    default void approachFailed(PeepoEntity npc){}
    boolean availableTo(PeepoEntity companion);
    /** Reserve atomically for one NPC; implementations must expire abandoned reservations. */
    boolean claim(PeepoEntity companion);
    void release(PeepoEntity companion);
    /** Mount/seat the NPC if needed; return false if occupancy cannot be established. */
    boolean occupy(PeepoEntity companion);
    default int energySpace() { return 0; }
    /** Wheel buffer insertion must participate in the supplied transaction. */
    default int insertEnergy(int maximum, TransactionContext transaction) { return 0; }
}
