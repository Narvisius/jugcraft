package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.client.OwnerThirdPerson;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Minecraft.class)
public abstract class OwnerThirdPersonInteractionMixin {
 @Shadow private void pick(float delta){}
 @Inject(method="startAttack",at=@At("HEAD"))
 private void jugcraft$attack(CallbackInfoReturnable<Boolean> ci){if(OwnerThirdPerson.aim())pick(1);}
 @Inject(method="startUseItem",at=@At("HEAD"))
 private void jugcraft$use(CallbackInfo ci){if(OwnerThirdPerson.aim())pick(1);}
 @Inject(method="continueAttack",at=@At("HEAD"))
 private void jugcraft$mine(boolean attacking,CallbackInfo ci){if(attacking&&OwnerThirdPerson.aim())pick(1);}
}
