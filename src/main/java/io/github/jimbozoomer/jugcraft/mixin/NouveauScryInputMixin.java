package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauScrying;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class NouveauScryInputMixin {
    @Shadow public ServerPlayer player;
    @Inject(method={"handleMovePlayer","handlePlayerAction","handleUseItemOn","handleUseItem","handleInteract","handleAttack","handlePunch"},at=@At("HEAD"),cancellable=true)
    private void nouveau$viewOnly(CallbackInfo ci){if(NouveauScrying.active(player))ci.cancel();}
}
