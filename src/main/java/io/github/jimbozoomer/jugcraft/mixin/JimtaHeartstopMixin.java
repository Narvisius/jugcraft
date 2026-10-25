package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaWardSpells;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Applied after ordinary defense reducers; debt survives save/logout and is repaid once. */
@Mixin(value=LivingEntity.class,priority=900)
public abstract class JimtaHeartstopMixin {
    @ModifyReturnValue(method="getDamageAfterMagicAbsorb",at=@At("RETURN"))
    private float jimta$defer(float amount,@Local(argsOnly=true) DamageSource source){return JimtaWardSpells.defer((LivingEntity)(Object)this,source,amount);}
    @Inject(method="tick",at=@At("RETURN"))
    private void jimta$refreshConcealment(CallbackInfo info){JimtaWardSpells.refreshConcealment((LivingEntity)(Object)this);}
}
