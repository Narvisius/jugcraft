package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.tower.crew.CrewShip;
import io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight;
import io.github.jimbozoomer.jugcraft.tower.crew.CrewShipSeatPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Right-click to sit in a landed drop ship's seat (Justin's pick, 9 November 2026): the ship's seats are only drawn,
 * nothing one can click, so at the start of the game's right-click this looks for a seat in the way of the player's
 * look, within reach and nearer than any block it's looking at, and asks the server to sit them in it
 * ({@link CrewShipSeatPayload}; the server checks again). Sneak, as on any seat, to stand up.
 */
public final class CrewShipSeatUse {
	/** A seat's box about its cushion's top (the ship's frame, before it's turned to face its way): across, deep (out
	 * from what it's fixed to), and from below to above the cushion's top. */
	static final float HALF_W = 0.46F, DEEP = 0.66F, BELOW = 0.35F, ABOVE = 1.35F;

	private CrewShipSeatUse() {
	}

	/** True if the right-click went to sitting in a seat. */
	public static boolean tryUse(Minecraft mc) {
		if (mc.player == null || mc.level == null || mc.player.isPassenger() || mc.player.isShiftKeyDown()) {
			return false;
		}
		Vec3 eye = mc.player.getEyePosition();
		Vec3 look = mc.player.getViewVector(1.0F);
		double reach = CrewShip.SIT_REACH;
		HitResult hit = mc.hitResult;
		if (hit instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK) {
			reach = Math.min(reach, b.getLocation().distanceTo(eye));
		}
		CrewShip best = null;
		int bestSeat = -1;
		double bestT = reach;
		long now = mc.level.getGameTime();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof CrewShip ship) || !ship.seatsOpen() && ship.phase() != CrewShipFlight.UNLOAD && !ship.parked()
					|| e.distanceToSqr(eye) > 60 * 60) {
				continue;
			}
			CrewShipFlight.Pose p = ship.pose(now - ship.phaseStart(), new CrewShipFlight.Pose());
			// the look in the ship's frame
			Matrix4f m = new Matrix4f().translate((float) (ship.getX() + p.x), (float) (ship.getY() + p.y), (float) (ship.getZ() + p.z))
					.rotateY(p.yaw).rotateX(p.pitch).invertAffine();
			Vector3f o = m.transformPosition((float) eye.x, (float) eye.y, (float) eye.z, new Vector3f());
			Vector3f d = m.transformDirection((float) look.x, (float) look.y, (float) look.z, new Vector3f());
			for (int i = 0; i < CrewShip.SEATS.length; i++) {
				float[] s = CrewShip.SEATS[i];
				// the box: along its facing from the wall behind it (DEEP back from the cushion's middle) out past its front
				float fx = s[3], fz = s[4];
				float bx = s[0] - fx * DEEP * 0.5F, bz = s[2] - fz * DEEP * 0.5F;
				float ax = Math.abs(fz) * HALF_W + Math.abs(fx) * DEEP * 0.5F, az = Math.abs(fx) * HALF_W + Math.abs(fz) * DEEP * 0.5F;
				double t = slab(o, d, bx + fx * DEEP * 0.5F - ax, s[1] - BELOW, bz + fz * DEEP * 0.5F - az,
						bx + fx * DEEP * 0.5F + ax, s[1] + ABOVE, bz + fz * DEEP * 0.5F + az);
				if (t >= 0 && t < bestT) {
					bestT = t;
					best = ship;
					bestSeat = i;
				}
			}
		}
		if (best == null) {
			return false;
		}
		ClientPlayNetworking.send(new CrewShipSeatPayload(best.getId(), bestSeat));
		return true;
	}

	/** Where along the ray (o + t d) it enters the box, or -1. */
	private static double slab(Vector3f o, Vector3f d, float x0, float y0, float z0, float x1, float y1, float z1) {
		double tmin = 0, tmax = Double.MAX_VALUE;
		float[] os = {o.x, o.y, o.z}, ds = {d.x, d.y, d.z}, lo = {x0, y0, z0}, hi = {x1, y1, z1};
		for (int k = 0; k < 3; k++) {
			if (Math.abs(ds[k]) < 1e-7) {
				if (os[k] < lo[k] || os[k] > hi[k]) {
					return -1;
				}
				continue;
			}
			double a = (lo[k] - os[k]) / ds[k], b = (hi[k] - os[k]) / ds[k];
			tmin = Math.max(tmin, Math.min(a, b));
			tmax = Math.min(tmax, Math.max(a, b));
			if (tmin > tmax) {
				return -1;
			}
		}
		return tmin;
	}
}
