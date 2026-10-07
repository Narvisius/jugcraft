package io.github.jimbozoomer.jugcraft.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The attack charge of a two-handed swing (weapons/TwoHanded, batch 46). Its blow lands some ticks after the click, but
 * must be as strong as the charge was at the click, as vanilla's blows are; vanilla reads the charge from this
 * protected ticker inside Player.stabAttack, and has no other way to set it than resetting it to 0.
 */
@Mixin(LivingEntity.class)
public interface AttackStrengthAccessor {
	@Accessor("attackStrengthTicker")
	int jugcraft$attackStrengthTicker();

	@Accessor("attackStrengthTicker")
	void jugcraft$setAttackStrengthTicker(int ticker);
}
