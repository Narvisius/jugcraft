package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.sign.Presentation;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.sign.SignPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the {@link Sign}s the server sends (roadmap step 27): each event once, in this client's own particles, as many
 * as its visual intensity, the distance and this tick's budget allow ({@link Presentation}), and the warnings' own
 * sounds (which have subtitles, so a warning is never only a colour or a sound). Nothing here decides anything, and
 * nothing outlives the particles it spawns. Each kind keeps its own shape and movement:
 * <ul>
 * <li>preparation: enchanting glyphs drawn in to the place;</li>
 * <li>execution: light rising a little where the work was done, or travelling from a transfer's source;</li>
 * <li>success: a burst of light;</li>
 * <li>shortage: grey smoke hanging and ash sinking;</li>
 * <li>danger: flame and sparks thrown outwards;</li>
 * <li>effect marks: as the shared effect boundary always showed each effect kind.</li>
 * </ul>
 */
public final class SignClient {
	private SignClient() {
	}

	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(SignPayload.TYPE, (payload, context) -> show(Minecraft.getInstance(), payload));
		ClientTickEvents.START_CLIENT_TICK.register(client -> Presentation.newTick());
	}

	static void show(Minecraft client, SignPayload payload) {
		Sign sign = Sign.byIndex(payload.sign());
		ClientLevel level = client.level;
		if (sign == null || level == null || client.player == null) {
			return;
		}
		Vec3 at = payload.at();
		double distance = Math.sqrt(client.player.distanceToSqr(at));
		if (sign.sounds() && distance <= Presentation.FAR) {
			level.playLocalSound(at.x, at.y, at.z, sign.kind == Sign.Kind.DANGER ? JugcraftConcordance.SIGN_DANGER_SOUND
					: JugcraftConcordance.SIGN_SHORTAGE_SOUND, SoundSource.BLOCKS, 0.7F, 1.0F, false);
		}
		int count = Presentation.take(Presentation.count(sign, Presentation.intensity(), distance), sign.kind.warns());
		draw(level, sign, at, payload.from().orElse(null), count, Presentation.reducedMotion());
	}

	/** Spawns {@code count} of {@code sign}'s particles (calmer with reduced motion). */
	static void draw(ClientLevel level, Sign sign, Vec3 at, @Nullable Vec3 from, int count, boolean calm) {
		RandomSource random = level.getRandom();
		double speed = calm ? 0.4 : 1.0;
		for (int i = 0; i < count; i++) {
			double ox = random.nextDouble() - 0.5;
			double oy = random.nextDouble() - 0.5;
			double oz = random.nextDouble() - 0.5;
			switch (sign) {
				// The glyphs fly towards the point given as their position, from the offset given as their speed.
				case GATHER -> level.addParticle(ParticleTypes.ENCHANT, at.x, at.y, at.z, ox * 2.0, oy * 2.0 + 0.5, oz * 2.0);
				case WORK -> level.addParticle(ParticleTypes.END_ROD, at.x + ox * 0.6, at.y + oy * 0.4, at.z + oz * 0.6, 0.0, 0.03 * speed, 0.0);
				case FLOW -> {
					if (from == null) {
						level.addParticle(ParticleTypes.END_ROD, at.x + ox * 0.6, at.y + oy * 0.4, at.z + oz * 0.6, 0.0, 0.03 * speed, 0.0);
					} else {
						// Spread along the line from the source, each moving on towards the place it arrives.
						double t = (i + random.nextDouble()) / count;
						Vec3 way = at.subtract(from);
						level.addParticle(ParticleTypes.END_ROD, from.x + way.x * t, from.y + way.y * t, from.z + way.z * t,
								way.x * 0.04 * speed, way.y * 0.04 * speed, way.z * 0.04 * speed);
					}
				}
				case DONE -> level.addParticle(ParticleTypes.END_ROD, at.x + ox * 0.6, at.y + oy * 0.6, at.z + oz * 0.6,
						ox * 0.2 * speed, oy * 0.2 * speed + 0.02, oz * 0.2 * speed);
				case WANT -> {
					level.addParticle(ParticleTypes.SMOKE, at.x + ox * 0.8, at.y + oy * 0.3, at.z + oz * 0.8, 0.0, 0.0, 0.0);
					level.addParticle(ParticleTypes.ASH, at.x + ox * 0.8, at.y + 0.3, at.z + oz * 0.8, 0.0, -0.02, 0.0);
				}
				case PERIL -> {
					level.addParticle(ParticleTypes.FLAME, at.x + ox * 0.3, at.y + oy * 0.3, at.z + oz * 0.3, ox * 0.15 * speed, 0.05 * speed, oz * 0.15 * speed);
					level.addParticle(ParticleTypes.CRIT, at.x, at.y, at.z, ox * 0.6 * speed, Math.abs(oy) * 0.4 * speed, oz * 0.6 * speed);
				}
				default -> level.addParticle(mark(sign), at.x + ox * 0.5, at.y + oy * 0.5, at.z + oz * 0.5, ox * 0.04 * speed, oy * 0.04 * speed,
						oz * 0.04 * speed);
			}
		}
	}

	/** An effect mark's particle (each effect kind as the shared effect boundary always showed it). */
	private static ParticleOptions mark(Sign sign) {
		return switch (sign) {
			case HARM -> ParticleTypes.CRIT;
			case MEND -> ParticleTypes.HAPPY_VILLAGER;
			case SHOVE -> ParticleTypes.CLOUD;
			case LIGHT -> ParticleTypes.END_ROD;
			case CHARM -> ParticleTypes.WITCH;
			case TOUCH -> ParticleTypes.ENCHANT;
			default -> ParticleTypes.SMOKE;
		};
	}
}
