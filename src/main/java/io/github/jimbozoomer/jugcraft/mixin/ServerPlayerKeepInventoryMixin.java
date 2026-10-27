package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.jimbozoomer.jugcraft.server.PlayerRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Asked of the player who died (same UUID as the respawned one), so a death in a lair is restored as it was kept. */
@Mixin(ServerPlayer.class)
public class ServerPlayerKeepInventoryMixin {
    @WrapOperation(method = "restoreFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
    private Object jugcraft$restorePersonalInventory(GameRules rules, GameRule<?> rule, Operation<Object> original, @Local(argsOnly = true) ServerPlayer oldPlayer) {
        return rule == GameRules.KEEP_INVENTORY && PlayerRules.keeps(oldPlayer) ? Boolean.TRUE : original.call(rules, rule);
    }
}
