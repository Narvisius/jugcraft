package io.github.jimbozoomer.jugcraft.lair.tyrant;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Salamander Charm (docs/features/cinder-tyrant.md): held in the offhand, hot ground never burns its wearer. A magma
 * block's heat and the Cinder Kiln's molten slag (its own, and the slag his surges spill) are hot-floor damage, which the
 * charm turns aside; the slag does not set them burning either ({@code MoltenSlagBlock#harms}). It does nothing against
 * the Tyrant's fire, his blows, lava or burning. Decided on the server, as the damage is.
 */
public final class SalamanderCharm {
	private SalamanderCharm() {
	}

	static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(source.is(DamageTypes.HOT_FLOOR) && warded(entity)));
	}

	/** Whether {@code living} holds a Salamander Charm in their offhand. */
	public static boolean warded(LivingEntity living) {
		return JugcraftTyrant.SALAMANDER_CHARM != null && living.getOffhandItem().is(JugcraftTyrant.SALAMANDER_CHARM);
	}
}
