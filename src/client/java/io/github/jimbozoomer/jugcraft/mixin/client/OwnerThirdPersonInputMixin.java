package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.client.OwnerThirdPerson;
import net.minecraft.client.player.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardInput.class)
public abstract class OwnerThirdPersonInputMixin extends ClientInput {
 @Inject(method="tick",at=@At("TAIL"))
 private void jugcraft$walk(CallbackInfo ci){moveVector=OwnerThirdPerson.movement(moveVector);}
}
