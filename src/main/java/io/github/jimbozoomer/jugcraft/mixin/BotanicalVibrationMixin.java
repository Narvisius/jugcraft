package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.botany.BotanicalFields;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(VibrationSystem.Listener.class)
public abstract class BotanicalVibrationMixin {
    @Inject(method="isOccluded",at=@At("HEAD"),cancellable=true)
    private static void jugcraft$muffle(Level level,Vec3 source,Vec3 destination,CallbackInfoReturnable<Boolean> cir){if(BotanicalFields.mufflesVibration(level,source,destination))cir.setReturnValue(true);}
}
