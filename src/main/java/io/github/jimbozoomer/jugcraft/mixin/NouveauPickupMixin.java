package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauVoiding;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemEntity.class)
public abstract class NouveauPickupMixin {
    @Inject(method="playerTouch",at=@At("HEAD"),cancellable=true)
    private void jugcraft$voidFilter(Player p,CallbackInfo ci){if(p instanceof ServerPlayer server&&(io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaPocketRooms.readOnlyGuest(server)||NouveauVoiding.pickup(server,(ItemEntity)(Object)this)))ci.cancel();}
}
