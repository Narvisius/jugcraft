package io.github.jimbozoomer.jugcraft.concordance.ember;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Smoulder ({@code jugcraft:smoulder}): while it lasts the creature stays alight, so it burns as any burning creature
 * does: vanilla's fire damage, a point a second, nothing for the fire-immune or for Fire Resistance, and water or rain
 * puts it out (this effect ends then too). It relights the creature only once the last second's fire has burnt out, so
 * it never burns faster than vanilla fire. It never sets blocks alight. Milk cures it like any status. Its level does not
 * matter: how long it lasts does.
 */
final class SmoulderEffect extends MobEffect {
	/** The fire a relight sets: one second, which vanilla's burning opens with its point of damage. */
	static final int FIRE_TICKS = 20;

	SmoulderEffect() {
		super(MobEffectCategory.HARMFUL, 0xF0743C);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (entity.isInWaterOrRain() || entity.fireImmune()) {
			return false;
		}
		if (entity.getRemainingFireTicks() <= 0) {
			entity.setRemainingFireTicks(FIRE_TICKS);
		}
		return true;
	}
}
