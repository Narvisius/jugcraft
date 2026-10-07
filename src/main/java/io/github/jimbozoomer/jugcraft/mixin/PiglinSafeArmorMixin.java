package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.machine.Electroplating;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gold-plated armor (batch 34, electroplating) counts as gold for piglins, so they leave its wearer alone. */
@Mixin(PiglinAi.class)
public class PiglinSafeArmorMixin {
	@Inject(method = "isWearingSafeArmor", at = @At("RETURN"), cancellable = true)
	private static void jugcraft$goldPlating(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && Electroplating.wearsGold(entity)) {
			cir.setReturnValue(true);
		}
	}
}
