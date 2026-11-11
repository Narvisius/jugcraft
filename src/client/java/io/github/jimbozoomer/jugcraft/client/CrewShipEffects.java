package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.tower.crew.CrewShip;
import io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * The drop ship's thrusters throw real fire and smoke (Justin, 6 November 2026: "the flames ... It needs to be, like
 * actual thrusters"; then the rocket plume of the thruster sheet, "smoke rolls off the end and billows out along the
 * ground"): while they burn, sparks of flame stream out of each nozzle with the exhaust and thick smoke rolls off its
 * end, and near the ground the blast throws smoke and dust out along it in a widening ring. Only for ships within
 * sight, and fewer the farther off they are.
 */
final class CrewShipEffects {
	/** The thrusters' pivots (the ship's frame, tools/crew_ship.py PODS) and the nozzle's distance from it. */
	private static final float POD_X = 11.8F, POD_Y = CrewShipFlight.G + 4.2F, POD_Z = 12.5F, NOZZLE = 4.0F;
	/** How near (blocks) a ship must be for its effects, and how high over the ground its blast still raises dust. */
	private static final double NEAR = 160, DUST = 16;

	private CrewShipEffects() {
	}

	static void tick(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null || client.isPaused() || client.player == null) {
			return;
		}
		RandomSource r = level.getRandom();
		CrewShipFlight.Pose p = new CrewShipFlight.Pose();
		long now = level.getGameTime();
		for (Entity e : level.entitiesForRendering()) {
			if (!(e instanceof CrewShip ship) || ship.phase() >= CrewShipFlight.GONE) {
				continue;
			}
			ship.pose(now - ship.phaseStart(), p);
			if (p.thrust < 0.4F) {                     // idling: no fire thrown
				continue;
			}
			Vec3 mid = ship.world(p, 0, POD_Y, POD_Z);
			double far = mid.distanceTo(client.player.position());
			if (far > NEAR) {
				continue;
			}
			float many = (float) (1 - far / NEAR) * p.thrust;
			double th = Mth.HALF_PI * p.tilt;
			for (int side = -1; side <= 1; side += 2) {
				// the nozzle and the way the exhaust goes (the thruster's axis, turned down by its tilt)
				Vec3 at = ship.world(p, side * POD_X, POD_Y - NOZZLE * Math.sin(th), POD_Z + NOZZLE * Math.cos(th));
				Vec3 out = ship.world(p, side * POD_X, POD_Y - (NOZZLE + 1) * Math.sin(th), POD_Z + (NOZZLE + 1) * Math.cos(th)).subtract(at);
				for (int i = 0; i < 1 + (int) (3 * many); i++) {
					double sp = 0.5 + r.nextDouble() * 0.6;
					level.addParticle(r.nextFloat() < 0.6F ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, at.x + r.nextGaussian() * 0.45,
							at.y + r.nextGaussian() * 0.2, at.z + r.nextGaussian() * 0.45, out.x * sp + r.nextGaussian() * 0.06, out.y * sp,
							out.z * sp + r.nextGaussian() * 0.06);
				}
				// smoke rolling off the flame's end, more the harder it burns
				for (int i = 0; i < 1 + (int) (2 * many); i++) {
					Vec3 end = at.add(out.scale((2.5 + 6 * p.thrust) * (0.85 + 0.3 * r.nextFloat())));
					level.addParticle(r.nextFloat() < 0.3F ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.LARGE_SMOKE, end.x + r.nextGaussian() * 0.8,
							end.y + r.nextGaussian() * 0.3, end.z + r.nextGaussian() * 0.8, out.x * 0.12 + r.nextGaussian() * 0.04, out.y * 0.1 + 0.03,
							out.z * 0.12 + r.nextGaussian() * 0.04);
				}
				// the blast on the ground under it
				int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(at.x), Mth.floor(at.z));
				double high = at.y - ground;
				if (out.y < -0.5 && high < DUST && high > -1) {
					float k = (float) (1 - high / DUST) * many;
					for (int i = 0; i < (int) (8 * k) + (r.nextFloat() < k ? 1 : 0); i++) {
						double a = r.nextDouble() * Mth.TWO_PI, v = 0.3 + r.nextDouble() * 0.4, rr = 1.2 + r.nextDouble() * 1.5;
						float pick = r.nextFloat();
						level.addParticle(pick < 0.45F ? ParticleTypes.CAMPFIRE_COSY_SMOKE : pick < 0.8F ? ParticleTypes.LARGE_SMOKE : ParticleTypes.POOF,
								at.x + Math.cos(a) * rr, ground + 0.2 + r.nextDouble() * 0.4, at.z + Math.sin(a) * rr, Math.cos(a) * v,
								0.01 + r.nextDouble() * 0.03, Math.sin(a) * v);
					}
				}
			}
		}
	}
}
