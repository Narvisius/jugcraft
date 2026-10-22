package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import io.github.jimbozoomer.jugcraft.server.PlayerRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public class PlayerKeepInventoryMixin {
    @WrapOperation(method = {"dropEquipment", "getBaseExperienceReward"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
    private Object jugcraft$personalInventory(GameRules rules, GameRule<?> rule, Operation<Object> original) {
        return rule == GameRules.KEEP_INVENTORY && PlayerRules.keeps((Player)(Object)this) ? Boolean.TRUE : original.call(rules, rule);
    }
}
