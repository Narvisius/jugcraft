package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauEquipment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
public abstract class NouveauMeleeMixin {
    @Inject(method="attack",at=@At("HEAD")) private void jugcraft$enchanterTouch(Entity target,CallbackInfo ci){if((Object)this instanceof ServerPlayer p)NouveauEquipment.melee(p,target);}
}
