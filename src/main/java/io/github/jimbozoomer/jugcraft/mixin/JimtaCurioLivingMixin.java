package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaSpellbookCurios;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class JimtaCurioLivingMixin {
    @ModifyReturnValue(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"))
    private float jimta$curioDamage(float amount, @Local(argsOnly = true) DamageSource source) {
        return JimtaSpellbookCurios.damage((LivingEntity) (Object) this, source, amount);
    }
    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void jimta$wardedEffect(MobEffectInstance effect, CallbackInfoReturnable<Boolean> info) {
        if (JimtaSpellbookCurios.preventsEffect((LivingEntity) (Object) this, effect)) info.setReturnValue(false);
    }
    @ModifyReturnValue(method = "getExperienceReward", at = @At("RETURN"))
    private int jimta$experienceRing(int amount, @Local(argsOnly = true) Entity killer) {
        return JimtaSpellbookCurios.experience((LivingEntity) (Object) this, killer, amount);
    }
}
