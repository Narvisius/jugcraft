package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.jimtafacts.network.SpatialReservation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reserved building regions reject edits without falsifying lighting or chunk reads. */
@Mixin(Level.class)
public abstract class SpatialLevelReservationMixin {
    @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("HEAD"),cancellable=true)
    private void jugcraft$spatialWrite(BlockPos pos,BlockState state,int flags,int depth,CallbackInfoReturnable<Boolean> result){
        if(SpatialReservation.blocked((Level)(Object)this,pos))result.setReturnValue(false);
    }
    @Inject(method="destroyBlock",at=@At("HEAD"),cancellable=true)
    private void jugcraft$spatialDestroy(BlockPos pos,boolean drops,Entity entity,int depth,CallbackInfoReturnable<Boolean> result){
        if(SpatialReservation.blocked((Level)(Object)this,pos))result.setReturnValue(false);
    }
}
