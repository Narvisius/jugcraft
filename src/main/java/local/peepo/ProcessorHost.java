package local.peepo;

import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** Shared physical helper contract; each machine retains ownership of its paid processing steps. */
public interface ProcessorHost {
    BlockEntity companionBlock();
    MachineKind kind();
    CompanionStatus companionStatus();
    int companionHelperCount();
    boolean helperPositionAvailable(int slot, Vec3 point);
    void resetCompanionEffort();
    default boolean companionLocked(){return false;}
}
