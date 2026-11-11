package io.github.jimbozoomer.jugcraft.tower.crew;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The plasma baton in a player's hands (the creative menu only, {@link PlasmaBaton}): always lit; a blow hits as a
 * diamond sword does, and sears with the gate guards' sound and sparks; it burns away what the rod swung through
 * (PlasmaWounds.sweep), on what was hit and on any foe of theirs in the rod's way.
 */
public class PlasmaBatonItem extends Item {
	public PlasmaBatonItem(Properties properties) {
		super(properties);
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(stack, target, attacker);
		if (target.level() instanceof ServerLevel level) {
			PlasmaBaton.hitEffects(level, target, attacker instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE);
			// what the rod swung through, about where their aim met it (tilted a little either way, as a swing is)
			Vec3[] aim = PlasmaWounds.aim(attacker, target);
			float tilt = (15 + level.getRandom().nextFloat() * 30) * (level.getRandom().nextBoolean() ? 1 : -1);
			PlasmaWounds.sweep(level, attacker, target, PlasmaWounds.swing(attacker, target, aim[0], tilt, PlasmaBaton.SWING_INNER, PlasmaBaton.SWING_OUTER,
					PlasmaBaton.SWING_HALF, PlasmaBaton.EDGE), attacker instanceof Player player ? attacker.damageSources().playerAttack(player)
					: attacker.damageSources().mobAttack(attacker), PlasmaBaton.DAMAGE);
		}
	}
}
