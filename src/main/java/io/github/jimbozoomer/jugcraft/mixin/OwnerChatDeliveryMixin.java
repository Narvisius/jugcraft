package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.minecraft.network.chat.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Server-selected non-reportable delivery, after moderation, recipient filtering and visibility checks. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class OwnerChatDeliveryMixin {
    @Shadow public abstract void sendDisguisedChatMessage(Component content,ChatType.Bound type);
    @Inject(method="sendPlayerChatMessage",at=@At("HEAD"),cancellable=true)
    private void jugcraft$systemDelivery(PlayerChatMessage message,ChatType.Bound type,CallbackInfo ci){
        if(!JugcraftConfig.option("chat.system_delivery"))return;
        ci.cancel();
        if(message.isFullyFiltered())return;
        var content=message.filterMask().isEmpty()?message.decoratedContent():message.filterMask().applyWithFormatting(message.signedBody().content());
        if(content!=null)sendDisguisedChatMessage(content,type);
    }
}
