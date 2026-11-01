package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.botany.BotanicalBrewing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Only removes darkness requirements; collision, spawn caps and protected sanctuaries still apply. */
@Mixin(Monster.class)
public abstract class BotanicalBloodthirstMixin {
    @Inject(method="isDarkEnoughToSpawn",at=@At("HEAD"),cancellable=true)
    private static void jugcraft$bloodthirst(ServerLevelAccessor level,BlockPos pos,RandomSource random,CallbackInfoReturnable<Boolean> cir){if(BotanicalBrewing.ignoresDarkness(level,pos))cir.setReturnValue(true);}
}
