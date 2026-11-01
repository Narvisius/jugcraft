package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerSupplementFlute;
import net.minecraft.world.entity.animal.parrot.Parrot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Use the tracked player's actual use state, so dancing stops on disconnect or song end. */
@Mixin(Parrot.class)
public abstract class OwnerFluteParrotMixin {
    @Inject(method="isPartyParrot",at=@At("RETURN"),cancellable=true)
    private void jugcraft$fluteDance(CallbackInfoReturnable<Boolean> ci){
        if(ci.getReturnValue())return;var parrot=(Parrot)(Object)this;
        for(var player:parrot.level().players())if(player.isAlive()&&!player.isSpectator()&&player.isUsingItem()&&player.getUseItem().getItem() instanceof OwnerSupplementFlute&&parrot.distanceToSqr(player)<=49){ci.setReturnValue(true);return;}
    }
}
