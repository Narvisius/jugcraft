package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Drone Service Arm (work list item 8, approved by Justin on 22 October 2026 from the take-2 mockup sheet): the service
 * bay's 6-axis industrial robot, its meshes from tools/service_arm.py (one a joint, each in its joint's frame) posed every
 * frame by its inverse kinematics, the gas-spring counterbalance and the dress pack of cables drawn between the joints.
 *
 * <p>It works the T4 on the cradle two blocks in front of it, lying along the cradle with its right side to the arm (the
 * stand faces north in tools/tripillar_hall.service_bay; HallFixtureRenderer centres the drone's plan on the cradle):
 * over the near rear fan, down onto its motor's nut with the runner turning, up, across to the near front fan, down, up,
 * a camera pass along the airframe with the ring light on, home. The stops are the drone's own fan hubs (its mesh), and
 * the whole cycle, these meshes and tubes posed as here every 0.05 s, was checked against the drone's mesh
 * (claude/src/dronetower/tools/mockups/arm_build_check.py): no part of the arm comes within 9 cm of the airframe; only
 * the socket goes down into a fan's duct, onto the hub.
 *
 * <p>Like a real robot cell's light curtain, the arm stops where it is while a player stands in the yellow and black
 * keep-clear square painted round its base, and carries on when they step out.
 */
final class ServiceArm {
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/service_arm.png");
	private static final RenderType MESH = RenderTypes.entityCutout(TEXTURE);
	/** The robot's dimensions (tools/service_arm.py). */
	static final double PLATE = 0.025, RISER = 0.30, BASE_H = 0.22, J1_Y = PLATE + RISER + BASE_H, SH_X = 0.16, SH_Y = 0.36, L1 = 0.95,
			EL_UP = 0.13, L2 = 1.02, TOOL = 0.30, RUNNER = 0.16;
	/** The keep-clear square's half size round the base's middle (the floor's stripes, tools/tripillar_hall). */
	static final double ZONE = 0.85;
	/** The runner's turn while it drives a nut (radians a second); the tip's speed between stops (m/s); the shortest move. */
	static final double SPIN = 12, SPEED = 0.5, MIN_MOVE = 0.9;
	static final String[] PARTS = {"base", "turret", "upper", "forearm", "wrist", "runner", "ring"};

	/**
	 * A stop of the cycle: where the runner's socket tip goes (the arm's frame: +x its front, the way it faces; y up from
	 * the floor; +z its right), the tool's tilt from straight down (radians, + away from the arm), how long it stays
	 * (seconds), whether the runner turns while it stays, whether the ring light is on (while it stays, and on the way
	 * to a stop that has it on too).
	 */
	record Stop(String name, double x, double y, double z, double tilt, double hold, boolean spin, boolean light) {
	}

	/**
	 * The T4's near fans, as the arm sees them: its rear outer fan's hub (1.20 out, 0.666 right) and its front inner fan's
	 * (1.56 out, 0.006 right), 1.528 over the floor (the cradle's deck 1.15 and the hub 0.378 over the drone's underside);
	 * their caps' tops 0.148 over that. Over a fan the tip is 0.16 above its cap, on it 0.005.
	 */
	static final double HUB_Y = 1.528, CAP = 0.148, OVER = HUB_Y + CAP + 0.16, ON = HUB_Y + CAP + 0.005;
	static final Stop[] CYCLE = {
			new Stop("home", 0.50, 2.30, 0.0, 0, 2.0, false, false),
			new Stop("over rear fan", 1.20, OVER, 0.666, 0, 1.0, false, false),
			new Stop("on rear fan", 1.20, ON, 0.666, 0, 2.5, true, false),
			new Stop("over rear fan", 1.20, OVER, 0.666, 0, 0.5, false, false),
			new Stop("over front fan", 1.56, OVER, 0.006, 0, 1.0, false, false),
			new Stop("on front fan", 1.56, ON, 0.006, 0, 2.5, true, false),
			new Stop("over front fan", 1.56, OVER, 0.006, 0, 0.5, false, false),
			new Stop("scan start", 1.35, 1.98, 0.80, Math.toRadians(20), 0.5, false, true),
			new Stop("scan end", 1.45, 1.98, -0.60, Math.toRadians(20), 0.5, false, true),
	};
	/** Each stop's arrival (seconds into the cycle), and the cycle's length. */
	static final double[] ARRIVE = new double[CYCLE.length];
	static final double[] MOVE = new double[CYCLE.length];
	static final double LENGTH;

	static {
		double t = 0;
		for (int i = 0; i < CYCLE.length; i++) {
			Stop a = CYCLE[i], b = CYCLE[(i + 1) % CYCLE.length];
			ARRIVE[i] = t;
			double d = Math.sqrt(sq(b.x - a.x) + sq(b.y - a.y) + sq(b.z - a.z));
			MOVE[i] = Math.max(MIN_MOVE, d / SPEED);
			t += a.hold + MOVE[i];
		}
		LENGTH = t;
	}

	private ServiceArm() {
	}

	private static double sq(double v) {
		return v * v;
	}

	/** Where the arm is at {@code t} seconds of running: its joints (j1, j2, forearm pitch, wrist tilt), the runner's turn, the light. */
	record Pose(double j1, double j2, double fore, double wrist, double spin, boolean light) {
	}

	static Pose pose(double t) {
		t = ((t % LENGTH) + LENGTH) % LENGTH;
		int i = CYCLE.length - 1;
		while (i > 0 && ARRIVE[i] > t) {
			i--;
		}
		Stop a = CYCLE[i], b = CYCLE[(i + 1) % CYCLE.length];
		double in = t - ARRIVE[i];
		if (in < a.hold) {
			double[] j = ik(a.x, a.y, a.z, a.tilt);
			return new Pose(j[0], j[1], j[2], j[3], a.spin ? in * SPIN : 0, a.light);
		}
		double s = Math.min(1, (in - a.hold) / MOVE[i]);
		double e = (1 - Math.cos(s * Math.PI)) / 2;
		double[] j = ik(a.x + (b.x - a.x) * e, a.y + (b.y - a.y) * e, a.z + (b.z - a.z) * e, a.tilt + (b.tilt - a.tilt) * e);
		return new Pose(j[0], j[1], j[2], j[3], 0, a.light && b.light);
	}

	/**
	 * The joints putting the socket's tip on (x, y, z) (the arm's frame) with the tool tilted {@code tilt} from straight
	 * down: axis 1 turns to the target; the upper arm (with the forearm's offset along it) and the forearm, elbow up, put
	 * the wrist TOOL back up the tool from the tip. {j1, j2 (the upper arm's lean from upright), the forearm's pitch from
	 * level, the tool's tilt}.
	 */
	static double[] ik(double x, double y, double z, double tilt) {
		double j1 = Math.atan2(z, x);
		double r = Math.hypot(x, z);
		double wr = r - TOOL * Math.sin(tilt) - SH_X;
		double wy = y + TOOL * Math.cos(tilt) - (J1_Y + SH_Y);
		double l1 = L1 + EL_UP;
		double d = Math.min(Math.hypot(wr, wy), l1 + L2 - 1e-4);
		double c = Math.max(-1, Math.min(1, (l1 * l1 + d * d - L2 * L2) / (2 * l1 * d)));
		double phi = Math.atan2(wy, wr) + Math.acos(c);
		double ex = l1 * Math.cos(phi), ey = l1 * Math.sin(phi);
		return new double[] {j1, Math.PI / 2 - phi, Math.atan2(wy - ey, wr - ex), tilt};
	}

	/** Where the socket's tip is for joints {@code j} (the arm's frame): the forward kinematics, as drawn. */
	static Vector3f tip(double[] j) {
		Matrix4f wrist = wristFrame(j);
		return wrist.transformPosition(new Vector3f(0, (float) TOOL, 0));
	}

	private static Matrix4f turretFrame(double[] j) {
		return new Matrix4f().translate(0, (float) J1_Y, 0).rotateY((float) -j[0]);
	}

	private static Matrix4f upperFrame(double[] j) {
		return turretFrame(j).translate((float) SH_X, (float) SH_Y, 0).rotateZ((float) -j[1]);
	}

	private static Matrix4f forearmFrame(double[] j) {
		double h = L1 + EL_UP;
		return turretFrame(j).translate((float) (SH_X + h * Math.sin(j[1])), (float) (SH_Y + h * Math.cos(j[1])), 0)
				.rotateZ((float) -(Math.PI / 2 - j[2]));
	}

	private static Matrix4f wristFrame(double[] j) {
		Matrix4f f = forearmFrame(j);
		Vector3f w = f.transformPosition(new Vector3f(0, (float) L2, 0));
		return turretFrame(j).setTranslation(w).rotateZ((float) -(Math.PI - j[3]));
	}

	// ------------------------------------------------------------------ running

	/** Each arm's own clock (seconds it has run) and when it was last looked at (game ticks). */
	private static final Map<Object, double[]> CLOCKS = new WeakHashMap<>();

	/**
	 * How long the arm at {@code pos} has run, now: it runs with the game's time except while a player stands in its
	 * keep-clear square.
	 */
	static double clock(Object owner, Level level, BlockPos pos, double now) {
		double[] c = CLOCKS.computeIfAbsent(owner, k -> new double[] {now / 20.0 % LENGTH, now});
		double dt = Math.max(0, Math.min(10, now - c[1]));
		c[1] = now;
		if (!blocked(level, pos)) {
			c[0] += dt / 20.0;
		}
		return c[0];
	}

	/** A player (not a spectator) inside the keep-clear square round the base, on its floor. */
	static boolean blocked(Level level, BlockPos pos) {
		double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5, reach = ZONE + 0.3;
		for (Player p : level.players()) {
			if (p.isSpectator()) {
				continue;
			}
			if (Math.abs(p.getX() - cx) < reach && Math.abs(p.getZ() - cz) < reach && p.getY() > pos.getY() - 0.6 && p.getY() < pos.getY() + 2.5) {
				return true;
			}
		}
		return false;
	}

	/** The way the arm faces, as a turn of its frame (+x its front). */
	static float facingTurn(Direction facing) {
		return switch (facing) {
			case NORTH -> (float) Math.PI / 2;
			case WEST -> (float) Math.PI;
			case SOUTH -> -(float) Math.PI / 2;
			default -> 0;
		};
	}

	// ------------------------------------------------------------------ drawing

	static void submit(PoseStack poses, SubmitNodeCollector collector, Direction facing, double time, int light) {
		ConsoleMesh[] mesh = new ConsoleMesh[PARTS.length];
		for (int i = 0; i < PARTS.length; i++) {
			mesh[i] = ConsoleMesh.get("service_arm_" + PARTS[i]);
			if (mesh[i] == null) {
				return;
			}
		}
		Pose p = pose(time);
		double[] j = {p.j1(), p.j2(), p.fore(), p.wrist()};
		poses.pushPose();
		poses.translate(0.5, 0, 0.5);
		poses.mulPose(new Matrix4f().rotationY(facingTurn(facing)));
		collector.submitCustomGeometry(poses, MESH, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			g.light = light;
			emit(g, mesh[0], light);
			g.push();
			g.translate(0, J1_Y, 0).yaw(-j[0]);
			emit(g, mesh[1], light);
			g.push();
			g.translate(SH_X, SH_Y, 0).roll(-j[1]);
			emit(g, mesh[2], light);
			g.pop();
			double h = L1 + EL_UP;
			double fx = SH_X + h * Math.sin(j[1]), fy = SH_Y + h * Math.cos(j[1]);
			g.push();
			g.translate(fx, fy, 0).roll(-(Math.PI / 2 - j[2]));
			emit(g, mesh[3], light);
			g.pop();
			double wx = fx + L2 * Math.cos(j[2]), wy = fy + L2 * Math.sin(j[2]);
			g.push();
			g.translate(wx, wy, 0).roll(-(Math.PI - j[3]));
			emit(g, mesh[4], light);
			ring(g, mesh[6], p.light(), light);
			g.push();
			g.translate(0, RUNNER, 0).yaw(p.spin());
			emit(g, mesh[5], light);
			g.pop();
			g.pop();
			g.pop();
			flexing(g, mesh[0], j, light);
		});
		poses.popPose();
	}

	private static void emit(DroneDepotRenderer.Geometry g, ConsoleMesh m, int light) {
		ConsoleMesh.emit(g, m.quads[ConsoleMesh.SOLID], light, false);
	}

	/** The camera's ring light: full bright while it scans, a dark ring otherwise. */
	private static void ring(DroneDepotRenderer.Geometry g, ConsoleMesh m, boolean on, int light) {
		float[] c = m.quads[ConsoleMesh.LIT];
		for (int o = 0; o + ConsoleMesh.CORNER <= c.length; o += ConsoleMesh.CORNER) {
			g.meshVertex(c[o], c[o + 1], c[o + 2], c[o + 3], c[o + 4], c[o + 5], c[o + 6], c[o + 7], on ? 0xFFFFFFFF : 0xFF26282C,
					on ? LightCoordsUtil.FULL_BRIGHT : light);
		}
	}

	// ------------------------------------------------------------------ the parts that flex between the joints

	/** The gas-spring counterbalance (turret to upper arm) and the dress pack (base, turret, upper arm, forearm). */
	private static void flexing(DroneDepotRenderer.Geometry g, ConsoleMesh base, double[] j, int light) {
		Matrix4f t = turretFrame(j), u = upperFrame(j), f = forearmFrame(j);
		float[] cable = base.edgeTiles[0], body = base.edgeTiles[1], rod = base.edgeTiles[2];
		Vector3f a = t.transformPosition(new Vector3f(-0.06f, (float) SH_Y + 0.02f, 0.24f));
		Vector3f b = u.transformPosition(new Vector3f(-0.13f, 0.36f, 0.23f));
		Vector3f d = new Vector3f(b).sub(a);
		float len = d.length();
		Vector3f mid = new Vector3f(d).mul(0.58f).add(a);
		tube(g, new Vector3f[] {a, mid}, 0.055f, 14, body, light, true);
		tube(g, new Vector3f[] {new Vector3f(d).mul(-0.02f / len).add(mid), b}, 0.024f, 10, rod, light, true);
		// the dress pack: from the base's connector up the turret's back, along the upper arm's right side in its two
		// clamps, round the elbow to the forearm's housing
		float yb = (float) (PLATE + RISER);
		Vector3f c0 = new Vector3f(-0.42f, yb + 0.09f, 0);
		Vector3f c1 = t.transformPosition(new Vector3f(-0.22f, 0.30f, 0.16f));
		Vector3f c2 = u.transformPosition(new Vector3f(0, 0.25f, 0.20f));
		Vector3f c3 = u.transformPosition(new Vector3f(0, 0.75f, 0.20f));
		Vector3f c4 = f.transformPosition(new Vector3f(0, -0.20f, 0.17f));
		java.util.List<Vector3f> path = new java.util.ArrayList<>();
		bezier(path, c0, new Vector3f(c0).add(-0.12f, 0.25f, 0.05f), dir(t, -0.1f, -0.1f, 0).add(c1), c1, 6, true);
		bezier(path, c1, dir(t, 0, 0.25f, 0.05f).add(c1), dir(u, 0, -0.2f, 0).add(c2), c2, 8, false);
		bezier(path, c2, dir(u, 0, 0.2f, 0).add(c2), dir(u, 0, -0.2f, 0).add(c3), c3, 6, false);
		bezier(path, c3, dir(u, 0, 0.25f, 0.04f).add(c3), dir(f, 0, -0.25f, 0.06f).add(c4), c4, 8, false);
		tube(g, path.toArray(new Vector3f[0]), 0.028f, 10, cable, light, false);
	}

	private static Vector3f dir(Matrix4f m, float x, float y, float z) {
		return m.transformDirection(new Vector3f(x, y, z));
	}

	private static void bezier(java.util.List<Vector3f> out, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, int n, boolean first) {
		for (int i = first ? 0 : 1; i <= n; i++) {
			float s = i / (float) n, r = 1 - s;
			out.add(new Vector3f(p0).mul(r * r * r).add(new Vector3f(p1).mul(3 * r * r * s)).add(new Vector3f(p2).mul(3 * r * s * s))
					.add(new Vector3f(p3).mul(s * s * s)));
		}
	}

	/** A round tube along {@code pts} (the arm's frame), {@code sides} faces round, textured from the patch {@code tile}. */
	static void tube(DroneDepotRenderer.Geometry g, Vector3f[] pts, float r, int sides, float[] tile, int light, boolean caps) {
		int n = pts.length;
		Vector3f[][] ring = new Vector3f[n][sides];
		Vector3f[][] out = new Vector3f[n][sides];
		for (int i = 0; i < n; i++) {
			Vector3f w = new Vector3f(pts[Math.min(i + 1, n - 1)]).sub(pts[Math.max(i - 1, 0)]).normalize();
			Vector3f u = new Vector3f(w).cross(Math.abs(w.y) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0)).normalize();
			Vector3f v = new Vector3f(w).cross(u);
			for (int k = 0; k < sides; k++) {
				double a = 2 * Math.PI * k / sides;
				out[i][k] = new Vector3f(u).mul((float) Math.cos(a)).add(new Vector3f(v).mul((float) Math.sin(a)));
				ring[i][k] = new Vector3f(out[i][k]).mul(r).add(pts[i]);
			}
		}
		float u0 = tile[0], v0 = tile[1], du = tile[2] - tile[0], dv = tile[3] - tile[1];
		int shade = 0xFFE4E4E4;
		for (int i = 0; i + 1 < n; i++) {
			float va = v0 + dv * (i % 4) / 4f, vb = va + dv / 4f;
			for (int k = 0; k < sides; k++) {
				int k1 = (k + 1) % sides;
				float ua = u0 + du * k / sides, ub = u0 + du * (k + 1) / sides;
				vertex(g, ring[i][k], out[i][k], ua, va, shade, light);
				vertex(g, ring[i][k1], out[i][k1], ub, va, shade, light);
				vertex(g, ring[i + 1][k1], out[i + 1][k1], ub, vb, shade, light);
				vertex(g, ring[i + 1][k], out[i + 1][k], ua, vb, shade, light);
			}
		}
		if (caps) {
			for (int end = 0; end < 2; end++) {
				int i = end == 0 ? 0 : n - 1;
				Vector3f w = new Vector3f(pts[n - 1]).sub(pts[0]).normalize().mul(end == 0 ? -1 : 1);
				for (int k = 0; k < sides; k++) {
					int k1 = (k + 1) % sides;
					vertex(g, pts[i], w, u0 + du / 2, v0 + dv / 2, shade, light);
					vertex(g, pts[i], w, u0 + du / 2, v0 + dv / 2, shade, light);
					vertex(g, ring[i][end == 0 ? k1 : k], w, u0, v0, shade, light);
					vertex(g, ring[i][end == 0 ? k : k1], w, u0 + du, v0, shade, light);
				}
			}
		}
	}

	private static void vertex(DroneDepotRenderer.Geometry g, Vector3f p, Vector3f n, float u, float v, int argb, int light) {
		g.meshVertex(p.x, p.y, p.z, u, v, n.x, n.y, n.z, argb, light);
	}
}
