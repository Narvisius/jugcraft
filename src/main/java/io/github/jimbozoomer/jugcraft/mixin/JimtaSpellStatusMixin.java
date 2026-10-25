package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaSpellStatuses;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** No duplicated eating event or inventory scan: only an actually consumed food can restore Focus. */
@Mixin(LivingEntity.class)
public abstract class JimtaSpellStatusMixin {
    @Unique private ItemStack jimtafacts$foodBefore=ItemStack.EMPTY;
    @Unique private ItemStack jimtafacts$foodReference=ItemStack.EMPTY;
    @ModifyReturnValue(method="getDamageAfterMagicAbsorb",at=@At("RETURN"))
    private float jimtafacts$spellDefense(float amount,@Local(argsOnly=true) DamageSource source) {
        return JimtaSpellStatuses.incoming((LivingEntity)(Object)this,source,amount);
    }
    @ModifyVariable(method="heal",at=@At("HEAD"),argsOnly=true)
    private float jimtafacts$spellHealing(float amount) {return JimtaSpellStatuses.healing((LivingEntity)(Object)this,amount);}
    @Inject(method="completeUsingItem",at=@At("HEAD"))
    private void jimtafacts$rememberFood(CallbackInfo info) {
        if((Object)this instanceof ServerPlayer player){jimtafacts$foodReference=player.getUseItem();jimtafacts$foodBefore=jimtafacts$foodReference.copy();}
    }
    @Inject(method="completeUsingItem",at=@At("RETURN"))
    private void jimtafacts$foodFocus(CallbackInfo info) {
        if((Object)this instanceof ServerPlayer player&&!player.isUsingItem())
            JimtaSpellStatuses.consumed(player,jimtafacts$foodBefore,jimtafacts$foodReference.getCount()<jimtafacts$foodBefore.getCount());
        jimtafacts$foodBefore=ItemStack.EMPTY;jimtafacts$foodReference=ItemStack.EMPTY;
    }
}
