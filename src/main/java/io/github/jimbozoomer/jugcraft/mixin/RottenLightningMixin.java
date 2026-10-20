package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.menagerie.RottenCreatures;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplied Channelled effect redirects an existing storm strike; it never creates extra strikes. */
@Mixin(ServerLevel.class)
public abstract class RottenLightningMixin {
    @Inject(method="findLightningTargetAround",at=@At("RETURN"),cancellable=true)
    private void jugcraft$channelled(BlockPos proposed,CallbackInfoReturnable<BlockPos> cir){
        var level=(ServerLevel)(Object)this;
        var player=level.getNearestPlayer(proposed.getX(),proposed.getY(),proposed.getZ(),64,
                entity->entity.isAlive()&&!entity.isSpectator());
        if(player!=null&&RottenCreatures.CHANNELLED!=null&&player.hasEffect(RottenCreatures.CHANNELLED)
                &&level.canSeeSky(player.blockPosition()))cir.setReturnValue(player.blockPosition());
    }
}
