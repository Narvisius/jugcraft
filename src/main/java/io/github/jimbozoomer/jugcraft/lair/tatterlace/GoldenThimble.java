package io.github.jimbozoomer.jugcraft.lair.tatterlace;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ItemStack;

/**
 * The Golden Thimble (Madame Tatterlace's loot): held in the offhand, it turns aside the first projectile that would hit
 * its wearer (an arrow, a trident, her thimbles, pins and Binding Thread), with a ping of gold, and then needs
 * {@value #COOLDOWN} ticks before it can again (its cooldown shows on it). Decided on the server.
 */
public final class GoldenThimble {
	public static final int COOLDOWN = 300;

	private GoldenThimble() {
	}

	static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayer player
				&& source.is(DamageTypeTags.IS_PROJECTILE) && player.level() instanceof ServerLevel level && turnAside(level, player)));
	}

	/** Whether the Golden Thimble in {@code player}'s offhand turns aside what would hit them now; if it does, it is spent. */
	public static boolean turnAside(ServerLevel level, ServerPlayer player) {
		ItemStack offhand = player.getOffhandItem();
		if (!offhand.is(JugcraftTatterlace.GOLDEN_THIMBLE) || player.getCooldowns().isOnCooldown(offhand)) {
			return false;
		}
		player.getCooldowns().addCooldown(offhand, COOLDOWN);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0F, 2.0F);
		level.sendParticles(ParticleTypes.WAX_OFF, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.4, 0.5, 0.4, 0.1);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.golden_thimble.turned"));
		return true;
	}
}
