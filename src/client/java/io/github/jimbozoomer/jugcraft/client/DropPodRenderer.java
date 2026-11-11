package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.tower.squad.DropPod;
import io.github.jimbozoomer.jugcraft.tower.squad.JugcraftSquads;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a drop pod (tower/squad/DropPod; the dart of sheet 3, assets/jugcraft/drop_pod/pod.bin.gz) on its way down and
 * landed. Before it leaves the ship only the flare shows (the leader's pod draws it: one flare over the mark), hanging
 * high over the mark; then each pod comes down its own weaving path, heat shield first, wrapped in re-entry fire with a
 * burning trail behind, the eight retro motors firing hard over the last {@value DropPod#RETRO} ticks. Landed it sits a
 * block in the ground, scorched; the hatch blows off on its bolts, tumbles out and lies where it falls, and the cocoon
 * inside shows. The server never moves it: everything here is worked out from when it was launched.
 */
public class DropPodRenderer extends EntityRenderer<DropPod, DropPodRenderer.State> {
	public static final class State extends EntityRenderState {
		/** Ticks since the squad's launch, and since this pod's own (less its delay). */
		float age, fly;
		final Vector3f flare = new Vector3f();
		int seed, place;
		float facing;
		@Nullable Vec3 camera;
	}

	/** The hatch's middle in the pod's frame (its part's pivot), how it flies when its bolts blow (blocks a tick out and up,
	 * the fall a tick), and its half thickness lying on the ground. */
	private static final Vector3f DOOR = new Vector3f(0, 2.05F, -1.25F);
	private static final float DOOR_OUT = 0.42F, DOOR_UP = 0.30F, DOOR_FALL = 0.045F, DOOR_HALF = 0.07F;
	/** The retro motors (tools/drop_pod_export.py): where, and the way they fire (down and out). */
	private static final float[][] MOTORS = motors();
	/** The re-entry trail: how many ticks back, sampled every so many. */
	private static final int TRAIL = 24, TRAIL_STEP = 1;
	/** How much bigger the flare is drawn than at first (it hangs some 530 blocks off now, not 215). */
	private static final float FLARE_SIZE = 2.5F;
	private static final net.minecraft.resources.Identifier WHITE = io.github.jimbozoomer.jugcraft.Jugcraft.id("textures/entity/crew_ship/white.png");

	public DropPodRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 0;
	}

	private static float[][] motors() {
		float[][] out = new float[8][];
		float a = 0.553F, b = 1.335F;
		float[][] xz = {{a, b}, {-a, b}, {a, -b}, {-a, -b}, {b, a}, {-b, a}, {b, -a}, {-b, -a}};
		for (int i = 0; i < 8; i++) {
			float x = xz[i][0], z = xz[i][1];
			float h = (float) Math.sqrt(x * x + z * z);
			float out_ = (float) Math.sqrt(1 - 0.927 * 0.927);
			out[i] = new float[] {x, 0.323F, z, x / h * out_, -0.927F, z / h * out_};
		}
		return out;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	/** Drawn wherever it is on its way (the entity stands where it lands, perhaps far below the view). */
	@Override
	protected boolean affectedByCulling(DropPod pod) {
		return false;
	}

	/**
	 * The box tested against the view, by the game and by Sodium's culling (which tests every entity's, whatever its
	 * renderer says): round the pod where it's drawn now and its trail back, and the flare while it shows; landed, round
	 * the pod and its thrown hatch. The entity itself stands where it lands, so testing its own box hid a pod coming
	 * down whenever the ground it was making for was out of view (Justin, 10 November 2026: "they disappear if you look
	 * at the wrong angles").
	 */
	@Override
	protected AABB getBoundingBoxForCulling(DropPod pod, float partialTick) {
		float age = pod.level().getGameTime() - pod.start() + partialTick, fly = age - pod.delay();
		AABB box = null;
		if (pod.place() == 0 && age < JugcraftSquads.FLARE_TICKS + 40) {
			box = around(pod, pod.flare().x(), pod.flare().y(), pod.flare().z(), 10);
		}
		if (fly >= JugcraftSquads.FLARE_TICKS && fly < DropPod.FLIGHT + 2) {
			Vector3f now = DropPod.path(pod.flare(), pod.seed(), fly, new Vector3f());
			Vector3f back = DropPod.path(pod.flare(), pod.seed(), Math.max(JugcraftSquads.FLARE_TICKS, fly - TRAIL), new Vector3f());
			AABB way = around(pod, now.x, now.y, now.z, 5).minmax(around(pod, back.x, back.y, back.z, 4));
			box = box == null ? way : box.minmax(way);
		} else if (fly >= DropPod.FLIGHT) {
			box = pod.getBoundingBox().inflate(4);
		}
		return box != null ? box : around(pod, pod.flare().x(), pod.flare().y(), pod.flare().z(), 1);
	}

	/** A cube {@code r} each way round a point given from where the pod lands. */
	private static AABB around(DropPod pod, double x, double y, double z, double r) {
		double cx = pod.getX() + x, cy = pod.getY() + y, cz = pod.getZ() + z;
		return new AABB(cx - r, cy - r, cz - r, cx + r, cy + r, cz + r);
	}

	@Override
	public void extractRenderState(DropPod pod, State state, float partialTick) {
		super.extractRenderState(pod, state, partialTick);
		state.age = pod.level().getGameTime() - pod.start() + partialTick;
		state.fly = state.age - pod.delay();
		state.flare.set(pod.flare());
		state.seed = pod.seed();
		state.place = pod.place();
		state.facing = pod.facing();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		CrewShipMesh mesh = CrewShipMesh.of("drop_pod/pod.bin.gz", "drop_pod");
		int land = DropPod.FLIGHT;
		Vec3 cam = camera.pos.subtract(state.x, state.y, state.z);       // the camera from where it lands
		if (state.place == 0 && state.age < JugcraftSquads.FLARE_TICKS + 40) {
			flare(state, state.age, cam, pose, collector);          // (the squad's: from its launch)
		}
		float age = state.fly;                                       // the pod's own flight from here on
		if (age < JugcraftSquads.FLARE_TICKS || mesh == null) {
			return;
		}
		Matrix4f m = new Matrix4f();
		int light;
		if (age < land) {
			// on its way: heat shield first along its path, the hatch turned its way about its axis
			Vector3f at = DropPod.path(state.flare, state.seed, age, new Vector3f());
			Vector3f ahead = DropPod.path(state.flare, state.seed, age + 1, new Vector3f()).sub(at);
			if (ahead.lengthSquared() < 1e-8F) {
				ahead.set(0, -1, 0);
			}
			float sink = DropPod.BURIED * smooth((age - (land - 3)) / 3F);
			Quaternionf tilt = new Quaternionf().rotationTo(0, 1, 0, -ahead.x, -ahead.y, -ahead.z);
			m.translate(at.x, at.y - sink, at.z).rotate(tilt).rotateY(state.facing);
			light = LightCoordsUtil.FULL_SKY;
			trail(state, age, cam, pose, collector);
		} else {
			m.translate(0, -DropPod.BURIED, 0).rotateY(state.facing);
			light = state.lightCoords;
		}
		boolean open = age >= land + DropPod.HATCH_AFTER;
		part(mesh, "body", m, light, pose, collector);
		if (open) {
			part(mesh, "inner", m, light, pose, collector);
		}
		part(mesh, "door", open ? door(m, age - land - DropPod.HATCH_AFTER) : m, light, pose, collector);
		if (age < land) {
			sheath(m, age, pose, collector);
		}
		if (age >= land - DropPod.RETRO && age < land + 2) {
			retros(m, age, land, pose, collector);
		}
	}

	/** The hatch blown off {@code t} ticks ago: out ahead of the pod and up, falling, tumbling forward to lie flat. */
	private static Matrix4f door(Matrix4f pod, float t) {
		float ground = DropPod.BURIED + DOOR_HALF;                       // the ground in the pod's frame
		float vy = DOOR_UP, g = DOOR_FALL;
		float hit = (vy + (float) Math.sqrt(vy * vy + 2 * g * (DOOR.y - ground))) / g;   // when its middle comes down to the ground
		float s = Math.min(t, hit);
		float y = DOOR.y + vy * s - g * s * s / 2, z = DOOR.z - DOOR_OUT * s * (1 - 0.15F * s / hit);
		float turn = -Mth.HALF_PI * smooth(s / hit);
		return new Matrix4f(pod).translate(0, y - DOOR.y, z - DOOR.z).translate(DOOR).rotateX(turn).translate(-DOOR.x, -DOOR.y, -DOOR.z);
	}

	/** A part of the pod with its transform {@code m} (in the pod's frame: its corners are about its pivot), lit by
	 * {@code light} (its glowing faces full bright). */
	private static void part(CrewShipMesh mesh, String name, Matrix4f m, int light, PoseStack pose, SubmitNodeCollector collector) {
		CrewShipMesh.Part part = mesh.parts.get(name);
		if (part == null) {
			return;
		}
		pose.pushPose();
		pose.mulPose(new Matrix4f(m).translate(part.px(), part.py(), part.pz()));
		for (CrewShipMesh.Batch b : part.batches()) {
			RenderType type = mesh.types[b.material()];
			int lit = b.glow() ? LightCoordsUtil.FULL_BRIGHT : light;
			collector.submitCustomGeometry(pose, type, (matrix, buffer) -> {
				float[] pos = b.pos(), uv = b.uv(), n = b.normal();
				Vector3f at = new Vector3f(), nn = new Vector3f();
				for (int v = 0, nv = pos.length / 3; v < nv; v++) {
					matrix.normal().transform(n[v * 3], n[v * 3 + 1], n[v * 3 + 2], nn).normalize();
					matrix.pose().transformPosition(pos[v * 3], pos[v * 3 + 1], pos[v * 3 + 2], at);
					buffer.addVertex(at.x, at.y, at.z, 0xFFFFFFFF, uv[v * 2], uv[v * 2 + 1], OverlayTexture.NO_OVERLAY, lit, nn.x, nn.y, nn.z);
				}
			});
		}
		pose.popPose();
	}

	/** The flare: a hard white point in a red glow high over the mark, hanging, then fading as the pods split off it. */
	private static void flare(State state, float age, Vec3 cam, PoseStack pose, SubmitNodeCollector collector) {
		float k = age < JugcraftSquads.FLARE_TICKS ? smooth(age / 8F) : 1 - smooth((age - JugcraftSquads.FLARE_TICKS) / 40F);
		if (k <= 0.01F) {
			return;
		}
		float flick = 1 + 0.12F * Mth.sin(age * 2.3F) + 0.08F * Mth.sin(age * 5.1F + 0.7F);
		Vector3f c = new Vector3f(state.flare);
		float z = FLARE_SIZE;
		glow(c, cam, new float[][] {{9.0F * z * flick, 1.0F, 0.25F, 0.12F, 0.35F * k}, {3.6F * z * flick, 1.0F, 0.55F, 0.35F, 0.7F * k},
				{1.2F * z, 1.0F, 0.96F, 0.9F, 1.0F * k}}, pose, collector);
	}

	/** The re-entry trail: a ribbon of fire and smoke back along its path, white-hot at the pod, red, then grey and gone. */
	private static void trail(State state, float age, Vec3 cam, PoseStack pose, SubmitNodeCollector collector) {
		int n = TRAIL / TRAIL_STEP;
		Vector3f[] pts = new Vector3f[n + 1];
		for (int i = 0; i <= n; i++) {
			float a = Math.max(JugcraftSquads.FLARE_TICKS, age - i * TRAIL_STEP);
			pts[i] = DropPod.path(state.flare, state.seed, a, new Vector3f());
			pts[i].y += 1.2F;                                          // (from the shield, behind the hull)
		}
		// fading as the retros brake it (the fire's from the air it's tearing through)
		float k = 1 - smooth((age - (DropPod.FLIGHT - DropPod.RETRO - 10)) / 14F);
		if (k <= 0.01F) {
			return;
		}
		Vector3f c = new Vector3f((float) cam.x, (float) cam.y, (float) cam.z);
		collector.submitCustomGeometry(pose, RenderTypes.eyes(CrewShipRenderer.FLAME), (matrix, buffer) -> {
			for (int i = 0; i < n; i++) {
				Vector3f a = pts[i], b = pts[i + 1];
				if (a.distanceSquared(b) < 1e-6F) {
					continue;
				}
				float t0 = (float) i / n, t1 = (float) (i + 1) / n;
				float w0 = 0.9F + 3.2F * t0, w1 = 0.9F + 3.2F * t1;
				Vector3f d = new Vector3f(b).sub(a).normalize();
				Vector3f s0 = side(d, a, c).mul(w0), s1 = side(d, b, c).mul(w1);
				int c0 = trailColour(t0, k), c1 = trailColour(t1, k);
				CrewShipRenderer.flame(buffer, matrix, new float[] {a.x - s0.x, a.y - s0.y, a.z - s0.z}, new float[] {a.x + s0.x, a.y + s0.y, a.z + s0.z},
						new float[] {b.x + s1.x, b.y + s1.y, b.z + s1.z}, new float[] {b.x - s1.x, b.y - s1.y, b.z - s1.z}, 0, 1, t0 * 3 - age * 0.15F,
						t1 * 3 - age * 0.15F, c0, c1);
			}
		});
	}

	private static int trailColour(float t, float k) {
		if (t < 0.35F) {
			float u = t / 0.35F;
			return CrewShipRenderer.colour(1.0F, Mth.lerp(u, 0.92F, 0.45F), Mth.lerp(u, 0.75F, 0.12F), Mth.lerp(u, 0.95F, 0.6F) * k);
		}
		float u = (t - 0.35F) / 0.65F;
		return CrewShipRenderer.colour(Mth.lerp(u, 1.0F, 0.32F), Mth.lerp(u, 0.45F, 0.3F), Mth.lerp(u, 0.12F, 0.29F), Mth.lerp(u, 0.6F, 0.0F) * k);
	}

	/** Across the ribbon at {@code p}: square to its run {@code d} and to the camera {@code cam}. */
	private static Vector3f side(Vector3f d, Vector3f p, Vector3f cam) {
		Vector3f view = new Vector3f(cam).sub(p);
		Vector3f s = new Vector3f(d).cross(view);
		if (s.lengthSquared() < 1e-8F) {
			s.set(1, 0, 0);
		}
		return s.normalize();
	}

	/** Re-entry fire round the heat shield: a white-hot cap under it, a sheath of flame streaming up round the hull. */
	private static void sheath(Matrix4f m, float age, PoseStack pose, SubmitNodeCollector collector) {
		float k = 1 - smooth((age - (DropPod.FLIGHT - DropPod.RETRO - 10)) / 14F);
		if (k <= 0.01F) {
			return;
		}
		float flick = 1 + 0.08F * Mth.sin(age * 2.9F) + 0.05F * Mth.sin(age * 6.3F);
		// in the pod's frame (y up its axis): rings round it from under the shield up past its shoulders
		float[][] layers = {{-0.35F, 0.9F, 0.4F, 1.55F, 1.0F, 0.95F, 0.85F, 0.95F}, {-0.2F, 1.45F, 2.2F, 1.75F, 1.0F, 0.6F, 0.22F, 0.55F},
				{0.2F, 1.6F, 5.5F * flick, 2.4F, 1.0F, 0.42F, 0.12F, 0.32F}};
		pose.pushPose();
		pose.mulPose(m);
		collector.submitCustomGeometry(pose, RenderTypes.eyes(WHITE), (matrix, buffer) -> {
			for (float[] L : layers) {
				int c0 = CrewShipRenderer.colour(L[4], L[5], L[6], L[7] * k), c1 = CrewShipRenderer.colour(L[4], L[5], L[6], 0);
				for (int j = 0; j < 16; j++) {
					double a0 = Mth.TWO_PI * j / 16, a1 = Mth.TWO_PI * (j + 1) / 16;
					float[] p0 = ring(a0, L[1], L[0]), p1 = ring(a1, L[1], L[0]), p2 = ring(a1, L[3], L[2]), p3 = ring(a0, L[3], L[2]);
					CrewShipRenderer.cone(buffer, matrix, p0, p1, p2, p3, c0, c1);
				}
			}
		});
		pose.popPose();
	}

	private static float[] ring(double a, float r, float y) {
		return new float[] {(float) Math.cos(a) * r, y, (float) Math.sin(a) * r};
	}

	/** The retro burn: each motor's flame down and out, white-hot at the nozzle, long at its peak, kicking up on landing. */
	private static void retros(Matrix4f m, float age, int land, PoseStack pose, SubmitNodeCollector collector) {
		float t = (age - (land - DropPod.RETRO)) / DropPod.RETRO;
		float burn = smooth(t / 0.15F) * (age < land ? 1 : 1 - (age - land) / 2F);
		if (burn <= 0.01F) {
			return;
		}
		float flick = 1 + 0.1F * Mth.sin(age * 4.1F) + 0.06F * Mth.sin(age * 9.3F);
		pose.pushPose();
		pose.mulPose(m);
		collector.submitCustomGeometry(pose, RenderTypes.eyes(CrewShipRenderer.FLAME), (matrix, buffer) -> {
			for (float[] mo : MOTORS) {
				Vector3f o = new Vector3f(mo[0], mo[1], mo[2]), d = new Vector3f(mo[3], mo[4], mo[5]).normalize();
				Vector3f a = Math.abs(d.y) > 0.9F ? new Vector3f(d).cross(0, 0, 1).normalize() : new Vector3f(d).cross(0, 1, 0).normalize();
				Vector3f b = new Vector3f(d).cross(a).normalize();
				for (float[] L : CrewShipRenderer.FLAME_LAYERS) {
					float len = 2.6F * burn * flick * L[2], r0 = 0.11F * L[0], r1 = 0.26F * L[1];
					int c0 = CrewShipRenderer.colour(L[3], L[4], L[5], L[9] * burn), c1 = CrewShipRenderer.colour(L[6], L[7], L[8], L[10] * burn);
					float v1 = len / L[12] - age * L[11];
					for (int i = 0; i < 8; i++) {
						double t0 = Mth.TWO_PI * i / 8, t1 = Mth.TWO_PI * (i + 1) / 8;
						CrewShipRenderer.flame(buffer, matrix, at(o, a, b, d, t0, r0, 0), at(o, a, b, d, t1, r0, 0), at(o, a, b, d, t1, r1, len),
								at(o, a, b, d, t0, r1, len), i / 8F, (i + 1) / 8F, -age * L[11], v1, c0, c1);
					}
				}
			}
		});
		pose.popPose();
	}

	private static float[] at(Vector3f o, Vector3f a, Vector3f b, Vector3f d, double th, float r, float t) {
		float ca = (float) Math.cos(th), sa = (float) Math.sin(th);
		return new float[] {o.x + (a.x * ca + b.x * sa) * r + d.x * t, o.y + (a.y * ca + b.y * sa) * r + d.y * t, o.z + (a.z * ca + b.z * sa) * r + d.z * t};
	}

	/** A round glow facing the camera at {@code c}: layers of (radius, r, g, b, opacity), each bright at its middle fading out. */
	private static void glow(Vector3f c, Vec3 cam, float[][] layers, PoseStack pose, SubmitNodeCollector collector) {
		Vector3f view = new Vector3f((float) cam.x, (float) cam.y, (float) cam.z).sub(c).normalize();
		Vector3f a = Math.abs(view.y) > 0.9F ? new Vector3f(view).cross(0, 0, 1).normalize() : new Vector3f(view).cross(0, 1, 0).normalize();
		Vector3f b = new Vector3f(view).cross(a).normalize();
		collector.submitCustomGeometry(pose, RenderTypes.eyes(WHITE), (matrix, buffer) -> {
			float[] mid = {c.x, c.y, c.z};
			for (float[] L : layers) {
				int c0 = CrewShipRenderer.colour(L[1], L[2], L[3], L[4]), c1 = CrewShipRenderer.colour(L[1], L[2], L[3], 0);
				for (int j = 0; j < 16; j++) {
					double t0 = Mth.TWO_PI * j / 16, t1 = Mth.TWO_PI * (j + 1) / 16;
					float[] p0 = edge(c, a, b, t0, L[0]), p1 = edge(c, a, b, t1, L[0]);
					CrewShipRenderer.cone(buffer, matrix, mid, mid, p1, p0, c0, c1);
				}
			}
		});
	}

	private static float[] edge(Vector3f c, Vector3f a, Vector3f b, double th, float r) {
		float ca = (float) Math.cos(th) * r, sa = (float) Math.sin(th) * r;
		return new float[] {c.x + a.x * ca + b.x * sa, c.y + a.y * ca + b.y * sa, c.z + a.z * ca + b.z * sa};
	}

	private static float smooth(float t) {
		t = Mth.clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}
}
