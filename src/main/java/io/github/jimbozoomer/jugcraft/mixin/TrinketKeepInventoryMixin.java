package io.github.jimbozoomer.jugcraft.mixin;

import eu.pb4.trinkets.api.*;
import io.github.jimbozoomer.jugcraft.server.PlayerRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The required Trinkets library's worn slots, including the optional backpack, follow the same personal rule. */
@Mixin(TrinketsApi.class)
public class TrinketKeepInventoryMixin {
    @Inject(method="getDropRule",at=@At("HEAD"),cancellable=true,remap=false)
    private static void jugcraft$keepWornGear(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity, boolean keepInventory, CallbackInfoReturnable<TrinketDropRule> cir) {
        if(entity instanceof Player player && PlayerRules.keeps(player)) cir.setReturnValue(TrinketDropRule.KEEP);
    }
}
