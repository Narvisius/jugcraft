package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.botany.BotanicalBrewing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Double fishing countdown progress without ticking the entire projectile twice. */
@Mixin(FishingHook.class)
public abstract class BotanicalFishingMixin {
    @Shadow private int timeUntilLured;
    @Shadow private int timeUntilHooked;
    @Inject(method="catchingFish",at=@At("HEAD"))
    private void jugcraft$allure(BlockPos pos,CallbackInfo ci){var p=((FishingHook)(Object)this).getPlayerOwner();if(p!=null&&p.hasEffect(BotanicalBrewing.ALLURE)){if(timeUntilLured>1)timeUntilLured--;if(timeUntilHooked>1)timeUntilHooked--;}}
}
