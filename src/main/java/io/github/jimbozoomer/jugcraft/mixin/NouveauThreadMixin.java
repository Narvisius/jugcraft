package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LivingEntity.class)
public abstract class NouveauThreadMixin {
    @Unique private ItemStack jugcraft$consumed=ItemStack.EMPTY;
    @ModifyReturnValue(method="getDamageAfterMagicAbsorb",at=@At("RETURN"))
    private float jugcraft$threadDefense(float amount,@Local(argsOnly=true) DamageSource source){LivingEntity e=(LivingEntity)(Object)this;float value=NouveauStatuses.incoming(e,source,amount);if(e instanceof Player p)value=NouveauThreads.absorb(p,source,value);return value;}
    @Inject(method="canGlide",at=@At("RETURN"),cancellable=true)
    private void jugcraft$threadGlide(CallbackInfoReturnable<Boolean> cir){LivingEntity e=(LivingEntity)(Object)this;if(NouveauStatuses.has(e,"gravity"))cir.setReturnValue(false);else if(NouveauStatuses.has(e,"glide")||e instanceof Player p&&NouveauThreads.level(p,"gliding")>=3)cir.setReturnValue(true);}
    @ModifyVariable(method="heal",at=@At("HEAD"),argsOnly=true)
    private float jugcraft$threadHealing(float amount){return NouveauStatuses.healing((LivingEntity)(Object)this,amount);}
    @Inject(method="completeUsingItem",at=@At("HEAD"))
    private void jugcraft$rememberFood(CallbackInfo ci){if((Object)this instanceof ServerPlayer p)jugcraft$consumed=p.getUseItem().copy();}
    @Inject(method="completeUsingItem",at=@At("RETURN"))
    private void jugcraft$threadConsumption(CallbackInfo ci){if((Object)this instanceof ServerPlayer p&&!p.isUsingItem())NouveauThreads.consumed(p,jugcraft$consumed);jugcraft$consumed=ItemStack.EMPTY;}
}
