package io.github.jimbozoomer.jugcraft.mixin;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IronBarsBlock.class)
public abstract class OwnerBarConnectionsMixin {
    @Inject(method="attachsTo",at=@At("RETURN"),cancellable=true)
    private void jugcraft$join(BlockState state,boolean solid,CallbackInfoReturnable<Boolean> cir){if(state.getBlock() instanceof FenceBlock)cir.setReturnValue(true);}
}
