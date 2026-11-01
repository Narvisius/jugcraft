package io.github.jimbozoomer.jugcraft.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FenceBlock.class,WallBlock.class})
public abstract class OwnerFenceConnectionsMixin {
    @Inject(method="connectsTo",at=@At("RETURN"),cancellable=true)
    private void jugcraft$join(BlockState state,boolean solid,Direction direction,CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValueZ())return;
        var block=state.getBlock();
        if(block instanceof FenceBlock||block instanceof WallBlock||block instanceof IronBarsBlock
                ||(Object)this instanceof FenceBlock&&block instanceof AbstractCauldronBlock
                ||block instanceof WallSignBlock&&state.getValue(WallSignBlock.FACING)==direction)cir.setReturnValue(true);
    }
}
