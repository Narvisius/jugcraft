package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.agriculture.FireworkShape;
import io.github.jimbozoomer.jugcraft.agriculture.SpookyBurstPayload;
import io.github.jimbozoomer.jugcraft.agriculture.SpookySparkOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a spooky firework's burst ({@link SpookyBurstPayload}): the picture of its {@link FireworkShape}, one spark a
 * pixel, flying out from where it burst so the picture grows to {@value #SPREAD} blocks a pixel. The picture faces this
 * client's player (upright, across their line of sight), so it reads the right way round from anywhere. A few loose
 * sparks in the picture's colour scatter round it.
 */
public final class SpookyBursts {
	/** How far apart neighbouring sparks end up, in blocks (their speed is this times one minus friction). */
	public static final double SPREAD = 0.45;
	private static final double FRICTION = 0.9;
	private static final int LOOSE_SPARKS = 16;
	/** Within this distance (blocks) the burst sounds near; beyond it, far. */
	private static final double NEAR = 16.0;

	private SpookyBursts() {
	}

	public static void receive(SpookyBurstPayload payload) {
		FireworkShape shape = FireworkShape.byOrdinal(payload.shape());
		Minecraft client = Minecraft.getInstance();
		if (shape != null && client.level != null) {
			burst(client.level, new Vec3(payload.x(), payload.y(), payload.z()), shape, payload.twinkle());
		}
	}

	/** Bursts {@code shape} at {@code at}, facing this client's player. */
	public static void burst(ClientLevel level, Vec3 at, FireworkShape shape, boolean twinkle) {
		Minecraft client = Minecraft.getInstance();
		Vec3 viewer = client.player == null ? at.add(0.0, 0.0, 10.0) : client.player.getEyePosition();
		Vec3 toViewer = viewer.subtract(at).multiply(1.0, 0.0, 1.0);
		if (toViewer.lengthSqr() < 1.0E-4) {
			toViewer = new Vec3(0.0, 0.0, 1.0);
		}
		toViewer = toViewer.normalize();
		// Across the viewer's line of sight, from their left to their right.
		Vec3 right = new Vec3(toViewer.z, 0.0, -toViewer.x);
		double speed = SPREAD * (1.0 - FRICTION);
		double cx = (shape.width() - 1) / 2.0;
		double cy = (shape.height() - 1) / 2.0;
		RandomSource random = level.getRandom();
		for (int row = 0; row < shape.height(); row++) {
			for (int column = 0; column < shape.width(); column++) {
				int colour = shape.colourAt(column, row);
				if (colour < 0) {
					continue;
				}
				double u = (column - cx) * speed;
				double v = (cy - row) * speed;
				level.addAlwaysVisibleParticle(new SpookySparkOptions(colour, twinkle), true, at.x, at.y, at.z,
						right.x * u + random.nextGaussian() * 0.002, v + random.nextGaussian() * 0.002, right.z * u + random.nextGaussian() * 0.002);
			}
		}
		for (int i = 0; i < LOOSE_SPARKS; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double reach = (Math.max(cx, cy) + 1.5 + random.nextDouble()) * speed;
			level.addAlwaysVisibleParticle(new SpookySparkOptions(shape.colour(), true), true, at.x, at.y, at.z,
					right.x * Math.cos(angle) * reach, Math.sin(angle) * reach, right.z * Math.cos(angle) * reach);
		}
		boolean near = viewer.distanceToSqr(at) < NEAR * NEAR;
		level.playLocalSound(at.x, at.y, at.z, near ? SoundEvents.FIREWORK_ROCKET_LARGE_BLAST : SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR,
				SoundSource.AMBIENT, 20.0F, 0.95F + random.nextFloat() * 0.1F, true);
		if (twinkle) {
			level.playLocalSound(at.x, at.y, at.z, near ? SoundEvents.FIREWORK_ROCKET_TWINKLE : SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR,
					SoundSource.AMBIENT, 20.0F, 0.9F + random.nextFloat() * 0.15F, true);
		}
	}
}
