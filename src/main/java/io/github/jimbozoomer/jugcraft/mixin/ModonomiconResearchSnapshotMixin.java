package io.github.jimbozoomer.jugcraft.mixin;

import com.klikli_dev.modonomicon.networking.SyncResearchStateMessage;
import com.klikli_dev.modonomicon.research.state.PlayerResearchState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Snapshot on the sender thread before Netty serializes the packet asynchronously.
 * Modonomicon 2.16.0 otherwise retains the live mutable research collections. */
@Mixin(value = SyncResearchStateMessage.class, remap = false)
public abstract class ModonomiconResearchSnapshotMixin {
    @Shadow @Final @Mutable public PlayerResearchState state;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void jugcraft$snapshotResearch(PlayerResearchState source, CallbackInfo ci) {
        state = new PlayerResearchState(source.factIds(), source.unlockedNodeIds(),
                source.valuesMap(), source.nodeStagesMap());
    }
}
