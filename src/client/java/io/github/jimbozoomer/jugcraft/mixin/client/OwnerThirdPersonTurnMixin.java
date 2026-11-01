package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.client.OwnerThirdPerson;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class OwnerThirdPersonTurnMixin {
 @Inject(method="turn",at=@At("HEAD"),cancellable=true)
 private void jugcraft$orbit(double yaw,double pitch,CallbackInfo ci){if(OwnerThirdPerson.turn((Entity)(Object)this,yaw,pitch))ci.cancel();}
}
