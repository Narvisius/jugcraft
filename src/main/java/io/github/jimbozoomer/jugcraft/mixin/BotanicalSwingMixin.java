package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.botany.*;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Empty-air swings reach the server through the native, thread-checked swing packet. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class BotanicalSwingMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handlePunch",at=@At("TAIL"))
    private void jugcraft$botanicalSwing(ServerboundPunchPacket packet,CallbackInfo ci){if(!player.isSpectator()&&player.isAlive()&&!BotanicalCombatRods.throwHeld(player))BotanicalAdvancedTools.fire(player,player.getMainHandItem());}
}
