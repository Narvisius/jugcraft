package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauEquipment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class NouveauReactiveMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handlePunch",at=@At("TAIL"))
    private void nouveau$swing(CallbackInfo ci){NouveauEquipment.swing(player);}
}
