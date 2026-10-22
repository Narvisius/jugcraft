package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Water, lava and native custom liquids cannot enter a different owner's land through their border. */
@Mixin({FlowingFluid.class, LavaFluid.class})
public class ClaimFluidMixin {
    @Inject(method="spreadTo",at=@At("HEAD"),cancellable=true)
    private void jugcraft$claimBoundary(LevelAccessor world, BlockPos target, BlockState state, Direction direction, FluidState fluid, CallbackInfo ci) {
        if(world instanceof Level level && LandRegistry.crossesBoundary(level,target.relative(direction.getOpposite()),target)) ci.cancel();
    }
}
