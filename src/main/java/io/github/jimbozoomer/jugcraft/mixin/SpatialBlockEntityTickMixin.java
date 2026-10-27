package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.jimtafacts.network.SpatialReservation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freeze machine work while its saved page is reserved, including during managed I/O waits. */
@Mixin(targets="net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class SpatialBlockEntityTickMixin {
    @Shadow @Final private BlockEntity blockEntity;
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void jugcraft$freezeSpatialMachine(CallbackInfo info){if(SpatialReservation.reserved(blockEntity.getLevel(),blockEntity.getBlockPos()))info.cancel();}
}
