package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.client.NouveauScryingClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LocalPlayer.class)
public abstract class NouveauScryPlayerMixin {
    @Inject(method="aiStep",at=@At("HEAD"),cancellable=true)
    private void nouveau$holdBody(CallbackInfo ci){if(NouveauScryingClient.active())ci.cancel();}
}
