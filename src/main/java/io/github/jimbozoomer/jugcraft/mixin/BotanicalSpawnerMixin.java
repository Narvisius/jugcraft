package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.botany.BotanicalDevices;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseSpawner.class)
public abstract class BotanicalSpawnerMixin {
    @Inject(method="isNearPlayer",at=@At("HEAD"),cancellable=true)
    private void jugcraft$imbueSpawner(Level level,BlockPos pos,CallbackInfoReturnable<Boolean> cir){
        if(level instanceof ServerLevel server&&BotanicalDevices.sustain(server,pos))cir.setReturnValue(true);
    }
}
