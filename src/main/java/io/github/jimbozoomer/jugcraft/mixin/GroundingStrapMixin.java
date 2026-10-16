package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.GroundingStrap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The Grounding Strap (lair/dreadnought/GroundingStrap.java): held in the offhand, electric harm does half, after armour
 * and enchantments. Players and every other living thing work out a blow's harm through this method, so one hook covers
 * them all; it changes nothing for any other harm, or for anyone not holding the strap.
 */
@Mixin(LivingEntity.class)
public abstract class GroundingStrapMixin {
	@ModifyReturnValue(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"))
	private float jugcraft$grounded(float amount, @Local(argsOnly = true) DamageSource source) {
		return GroundingStrap.scale((LivingEntity) (Object) this, source, amount);
	}
}
