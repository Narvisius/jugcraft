package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.wilds.WildCold;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class WildFrozenTurnMixin {
    @Inject(method="turn",at=@At("HEAD"),cancellable=true)
    private void jugcraft$frozenView(double yaw,double pitch,CallbackInfo ci){if((Object)this instanceof LivingEntity e&&WildCold.frozen(e))ci.cancel();}
}
