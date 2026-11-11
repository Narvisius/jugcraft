package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhase;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Plasma wounds on every mob that isn't drawn from model parts (Justin, 8 November 2026: "one generic system ... no
 * per-mob code": the Fable Scarecrow and the Corpse Eater, GeckoLib models, got nothing): the same holes, worked on the
 * geometry the mob's renderer draws, whatever drew it.
 *
 * <p>Around the mob's renderer, its collector is wrapped: every face it draws as its own geometry passes through a
 * {@link Cutter}, in the mob's own frame (from its feet, its body's yaw taken out: the server's), and where a wound's
 * hole comes near a face, the face is drawn in small cells with the ones in the hole left out and those round its edge
 * seared, as the model-part wounds are. On the way it notes where each wound's line crosses the mob's surface, going in
 * and coming out; from the last frame's crossings the walls are drawn next frame (a tube from each place it goes in to
 * where it comes out, cut off at the surface there; a baton's trench from where it first meets the mob to its depth),
 * and the rims' glow round each mouth. The holes stay where they were burned in the mob's frame (they don't follow a
 * limb as it swings: GeckoLib's bones aren't model parts).
 */
public final class PlasmaWoundsMesh {
	static final RenderStateDataKey<Frame> FRAME = RenderStateDataKey.create(() -> "jugcraft:plasma_wound_mesh");
	/** The wall's reach past the hole (blocks: the half-pixel steps' worth at full size) and the rim's width (blocks). */
	static final float WALL = 0.4F / 16F, RIM = 0.35F / 16F, CHAR = 1.4F / 16F;
	/** How many lines across a gash's rod find the surface under it (so its trench follows the mob round an edge). */
	static final int SAMPLES = 9;

	private PlasmaWoundsMesh() {
	}

	/** The crossings each of a mob's wounds made with its surface: the last whole frame's, and this frame's so far. */
	static final class State {
		Map<PlasmaWoundsClient.Wound, List<float[]>> ready = new IdentityHashMap<>();
		Map<PlasmaWoundsClient.Wound, List<float[]>> building = new IdentityHashMap<>();

		synchronized void add(PlasmaWoundsClient.Wound w, float[] crossing) {
			building.computeIfAbsent(w, k -> new ArrayList<>()).add(crossing);
		}

		synchronized void swap() {
			if (!building.isEmpty()) {
				ready = building;
				building = new IdentityHashMap<>();
			}
		}

		synchronized @Nullable List<float[]> ready(PlasmaWoundsClient.Wound w) {
			return ready.get(w);
		}
	}

	/** What a generic mob's render state carries: its wounds, its body's yaw (radians) and its thinnest side (blocks). */
	record Frame(PlasmaWoundsClient.Marks marks, float yaw, float thin) {
	}

	/**
	 * A creature's part with wounds, drawn by a renderer of its own from model parts (a Murmur's head: menagerie's
	 * RecoveredCreaturePart; tower/crew/PlasmaWounds.hitPart): its render state carries its wounds as a mob's does, and
	 * as its model is submitted the holes are worked out on that model (PlasmaWoundsClient.woundModel), so it is cut as a
	 * mob is, its hair and all.
	 */
	static final RenderStateDataKey<PlasmaWoundsClient.Marks> PART = RenderStateDataKey.create(() -> "jugcraft:plasma_wound_part");

	/** At extraction (any entity): a living mob with wounds whose renderer isn't a living one gets its frame. */
	public static void extract(Entity entity, EntityRenderState state, float partialTick, boolean living) {
		if (!(state instanceof FabricRenderState data)) {
			return;
		}
		PlasmaWoundsClient.Marks marks = !living ? PlasmaWoundsClient.marks(entity.getId()) : null;
		data.setData(PART, null);
		if (marks == null) {
			data.setData(FRAME, null);
			return;
		}
		if (!(entity instanceof LivingEntity)) {
			data.setData(FRAME, null);
			data.setData(PART, marks);
			data.setData(PlasmaWoundsClient.MARKS, marks);
			data.setData(PlasmaWoundsClient.HOLES, null);
			return;
		}
		LivingEntity l = (LivingEntity) entity;
		float yaw = Mth.rotLerp(partialTick, l.yBodyRotO, l.yBodyRot) * Mth.DEG_TO_RAD;
		data.setData(FRAME, new Frame(marks, yaw, (float) Math.min(entity.getBbWidth(), entity.getBbHeight())));
	}

	/**
	 * Around a mob's renderer drawing it: if it carries generic wounds, its collector wrapped to cut them, and the walls and
	 * rims (from last frame's crossings) submitted; else the collector as it is.
	 */
	public static SubmitNodeCollector wrap(EntityRenderState state, PoseStack pose, SubmitNodeCollector collector) {
		if (state instanceof FabricRenderState parts && parts.getData(PART) != null) {
			return new Wrapped(collector, collector, List.of(), new Matrix4f(), new State(), collector, state);
		}
		Frame f = state instanceof FabricRenderState data ? data.getData(FRAME) : null;
		if (f == null || f.marks().wounds.isEmpty()) {
			return collector;
		}
		PlasmaWoundsClient.Marks marks = f.marks();
		State st;
		synchronized (marks) {
			if (marks.mesh == null) {
				marks.mesh = new State();
			}
			st = marks.mesh;
		}
		st.swap();
		float now = PlasmaWoundsClient.now();
		float size = Mth.clamp(PlasmaWoundsClient.MOST * f.thin() * 16F / PlasmaWounds.BOLT_RADIUS, 0.15F, 1F);
		List<MeshHole> holes = new ArrayList<>();
		for (PlasmaWoundsClient.Wound w : marks.wounds) {
			w.age(Math.max(0, (now - w.born) / 20F));
			holes.add(MeshHole.of(w, size, st.ready(w)));
		}
		Matrix4f root = new Matrix4f(pose.last().pose());
		Matrix4f toLocal = new Matrix4f().rotateY(f.yaw()).mul(new Matrix4f(root).invert());
		// the walls and the rims' glow, from where the lines crossed the surface last frame
		Geometry geo = new Geometry();
		for (MeshHole h : holes) {
			h.walls(geo);
		}
		pose.pushPose();
		pose.mulPose(new Matrix4f().rotateY(-f.yaw()));
		if (!geo.base.isEmpty()) {
			collector.submitCustomGeometry(pose, RenderTypes.entityCutout(PlasmaWoundsClient.WHITE), (p, out) -> geo.emit(geo.base, p, out, 0xF000F0, true));
		}
		if (!geo.glow.isEmpty()) {
			collector.order(2).submitCustomGeometry(pose, RenderTypes.eyes(PlasmaWoundsClient.WHITE), (p, out) -> geo.emit(geo.glow, p, out, 0xF000F0, false));
		}
		pose.popPose();
		return new Wrapped(collector, collector, holes, toLocal, st, collector, null);
	}

	// ------------------------------------------------------------------ one wound on a generic mob

	/** A wound's hole in the mob's frame (blocks): its line, radius, ragged edge, a gash's rod, and the cut's reach. */
	static final class MeshHole {
		final PlasmaWoundsClient.Wound w;
		final Vector3f o, d, u, e1, e2;
		final float r, half, depth;
		final boolean gash;
		/** Where along the line it cuts (blocks); a gash's only once its first crossing is known. */
		float from = -1e9F, to = 1e9F;
		boolean cuts = true;
		final @Nullable List<float[]> crossings;
		/**
		 * A gash's lines across its rod (SAMPLES of them, parallel to the shot, from one end of the rod to the other): where
		 * each first goes into the mob (blocks along it, NaN where it misses) and that place and the surface's way there.
		 */
		final float[] sampleT = new float[SAMPLES];
		final float[][] sampleAt = new float[SAMPLES][];
		/** How deep the trench goes on each sample line: its depth, or less where the line comes out of the mob sooner. */
		final float[] sampleDepth = new float[SAMPLES];

		MeshHole(PlasmaWoundsClient.Wound w, float size, @Nullable List<float[]> crossings) {
			this.w = w;
			PlasmaWounds.Payload p = w.p;
			o = new Vector3f(p.hx(), p.hy(), p.hz());
			d = new Vector3f(p.dx(), p.dy(), p.dz()).normalize();
			gash = w.gash();
			r = p.radius() / 16F * size;
			// (a swing's fan, on a mob with no model parts to follow it through: a gash as wide as the sweep is where it
			// meets the mob, a bite deep or as far as the edge reached)
			depth = (w.fan() ? PlasmaWounds.BITE : p.depth()) / 16F * size;
			half = w.fan() ? Math.min(0.75F, p.depth() * Mth.sin(Math.min(p.length(), Mth.HALF_PI))) : gash ? p.length() / 32F * size : 0;
			Vector3f rod = new Vector3f(p.ux(), p.uy(), p.uz());
			rod.sub(new Vector3f(d).mul(rod.dot(d)));
			u = rod.lengthSquared() > 1e-6F ? rod.normalize() : PlasmaWoundsClient.perpendicular(d);
			e1 = gash ? new Vector3f(u) : PlasmaWoundsClient.perpendicular(d);
			e2 = new Vector3f(d).cross(e1).normalize();
			this.crossings = crossings == null ? null : sorted(crossings);
			Arrays.fill(sampleT, Float.NaN);
			Arrays.fill(sampleDepth, depth);
			if (gash && this.crossings != null) {
				for (float[] c : this.crossings) {
					int k = (int) c[8];
					if (c[7] > 0 && k >= 0 && k < SAMPLES && Float.isNaN(sampleT[k])) {
						sampleT[k] = c[0];
						sampleAt[k] = c;
					}
				}
				for (float[] c : this.crossings) {
					int k = (int) c[8];
					if (c[7] == 0 && k >= 0 && k < SAMPLES && !Float.isNaN(sampleT[k]) && c[0] > sampleT[k]) {
						sampleDepth[k] = Math.min(sampleDepth[k], Math.max(0.005F, c[0] - sampleT[k] - 0.004F));
					}
				}
			}
		}

		/** How far the sample lines reach along the rod either way (blocks): its length and its rounded ends. */
		float reach() {
			return half + r * (1 + PlasmaWoundsClient.LOBE_MAX);
		}

		/** The offset along the rod of a gash's sample line k (blocks). */
		float sampleU(int k) {
			return SAMPLES == 1 ? 0 : -reach() + 2 * reach() * k / (SAMPLES - 1);
		}

		static MeshHole of(PlasmaWoundsClient.Wound w, float size, @Nullable List<float[]> crossings) {
			MeshHole h = new MeshHole(w, size, crossings);
			if (h.gash) {
				float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
				for (float t : h.sampleT) {
					if (!Float.isNaN(t)) {
						lo = Math.min(lo, t);
						hi = Math.max(hi, t);
					}
				}
				if (lo > hi) {
					h.cuts = false;                                // not yet: its first frame only finds where it meets the mob
				} else {
					h.from = lo - 0.02F;
					h.to = w.fan() ? Math.max(hi + h.depth, w.p.depth()) : hi + h.depth;
				}
			}
			return h;
		}

		private static List<float[]> sorted(List<float[]> in) {
			List<float[]> out = new ArrayList<>(in);
			out.sort((a, b) -> Float.compare(a[0], b[0]));
			return out;
		}

		/** Where along the line it first goes into the mob (blocks), or NaN. */
		float firstIn() {
			if (crossings != null) {
				for (float[] c : crossings) {
					if (c[7] > 0) {
						return c[0];
					}
				}
			}
			return Float.NaN;
		}

		float ragged(float ang) {
			float[] lobes = w.lobes;
			float rr = 1;
			for (int k = 0; k < PlasmaWoundsClient.LOBES; k++) {
				rr += lobes[2 * k] * Mth.sin((k + 2) * ang + lobes[2 * k + 1]);
			}
			return rr;
		}

		/** How far (blocks) the point is outside the hole's ragged edge (below 0: inside); far past the cut's ends, a big number. */
		float edge(float x, float y, float z, boolean rough) {
			float px = x - o.x, py = y - o.y, pz = z - o.z;
			float t = px * d.x + py * d.y + pz * d.z;
			float wx = px - t * d.x, wy = py - t * d.y, wz = pz - t * d.z;
			float a = 0, along = 0;
			if (half > 0) {
				along = wx * u.x + wy * u.y + wz * u.z;
				a = Mth.clamp(along, -half, half);
				wx -= a * u.x;
				wy -= a * u.y;
				wz -= a * u.z;
			}
			float dist = Mth.sqrt(wx * wx + wy * wy + wz * wz);
			if (rough) {
				// to the cut's whole length (a quick lower bound for a whole face)
				float over = t < from ? from - t : t > to ? t - to : 0;
				return Mth.sqrt(dist * dist + over * over) - (1 + PlasmaWoundsClient.LOBE_MAX) * r;
			}
			if (t < from || t > to) {
				return 1e9F;
			}
			if (gash) {
				// as deep as the gash goes from the surface under this point (the nearest sample line's)
				float span = reach();
				int k = Mth.clamp(Math.round((Mth.clamp(along, -span, span) + span) / Math.max(1e-6F, 2 * span) * (SAMPLES - 1)), 0, SAMPLES - 1);
				float st = sampleT[k];
				if (Float.isNaN(st) || t < st - 0.02F || t > st + sampleDepth[k] + 0.004F) {
					return 1e9F;
				}
			}
			float ang = (float) Math.atan2(wx * e2.x + wy * e2.y + wz * e2.z, wx * e1.x + wy * e1.y + wz * e1.z);
			return dist - ragged(ang) * r;
		}

		/** The colour of a face's cell this far (blocks) outside the edge: seared at the rim, charred past it, else as it was. */
		int sear(int colour, float e, int i, int j) {
			return PlasmaWoundsClient.searBy(colour, e * 16F / Math.max(0.15F, r * 16F / w.p.radius()), w.heat, w.ember, w.p.seed(), i, j);
		}

		/** The walls (between where the line goes in and comes out, from last frame's crossings) and the rims' glow. */
		void walls(Geometry geo) {
			if (crossings == null || crossings.isEmpty()) {
				return;
			}
			if (gash) {
				trench(geo);
				return;
			}
			// inside where the count of ways in less ways out is above nothing
			int depthIn = 0;
			float[] open = null;
			for (float[] c : crossings) {
				if (c[7] > 0) {
					if (depthIn++ == 0) {
						open = c;
					}
				} else if (depthIn > 0 && --depthIn == 0 && open != null) {
					tube(geo, open, c);
					open = null;
				}
				rim(geo, c, false);
			}
		}

		/** A tube from the crossing a (in) to b (out), cut off at the surface at each. */
		private void tube(Geometry geo, float[] a, float[] b) {
			int sides = 12;
			float ta = a[0] - r * 2, tb = b[0] + r * 2;
			int lengths = Math.max(2, Math.min(24, Math.round((tb - ta) / (r * 0.6F))));
			for (int k = 0; k < sides; k++) {
				float a0 = Mth.TWO_PI * k / sides, a1 = Mth.TWO_PI * (k + 1) / sides, am = (a0 + a1) / 2;
				float r0 = ragged(a0) * r + WALL * r / (PlasmaWounds.BOLT_RADIUS / 16F), r1 = ragged(a1) * r + WALL * r / (PlasmaWounds.BOLT_RADIUS / 16F);
				Vector3f n = new Vector3f(e1).mul(-Mth.cos(am)).add(new Vector3f(e2).mul(-Mth.sin(am)));
				for (int c = 0; c < lengths; c++) {
					float t0 = ta + (tb - ta) * c / lengths, t1 = ta + (tb - ta) * (c + 1) / lengths;
					List<float[]> poly = new ArrayList<>(List.of(ring(t0, a0, r0), ring(t0, a1, r1), ring(t1, a1, r1), ring(t1, a0, r0)));
					poly = behind(poly, a);
					poly = behind(poly, b);
					if (poly.size() >= 3) {
						float[] hot = new float[poly.size()];
						for (int i = 0; i < hot.length; i++) {
							float[] q = poly.get(i);
							float da = depthBehind(q, a), db = depthBehind(q, b);
							float hh = Mth.clamp(1 - Math.min(da, db) / (r * 0.8F), 0, 1);
							hot[i] = hh * hh;
						}
						geo.wall(poly, hot, n, this);
					}
				}
			}
		}

		/**
		 * A gash's trench, following the surface under it: between each pair of neighbouring sample lines that both met the
		 * mob, its two walls (from the surface down to its depth) and its floor; an end wall at each end of the run; the rim's
		 * glow along both edges.
		 */
		private void trench(Geometry geo) {
			Vector3f side = new Vector3f(d).cross(u).normalize();
			float rr = r * (1 + PlasmaWoundsClient.LOBE_MAX) + WALL * r / (PlasmaWounds.BOLT_RADIUS / 16F);
			float width = RIM * r / (PlasmaWounds.BOLT_RADIUS / 16F) * 1.4F;
			for (int k = 0; k < SAMPLES; k++) {
				boolean here = !Float.isNaN(sampleT[k]);
				boolean next = joined(k);
				boolean prev = k > 0 && joined(k - 1);
				if (!here) {
					continue;
				}
				float ua = sampleU(k), ta = sampleT[k], da = sampleDepth[k];
				if (!prev || !next) {
					// an end of the run: the trench's end wall, out to the rod's rounded end
					float e = ua;
					Vector3f n = new Vector3f(u).mul(!prev ? 1 : -1);
					geo.wall(List.of(q(ta, e, side, -rr), q(ta, e, side, rr), q(ta + da, e, side, rr), q(ta + da, e, side, -rr)),
							new float[] {1, 1, 0, 0}, n, this);
				}
				if (!next) {
					continue;
				}
				float ub = sampleU(k + 1), tb = sampleT[k + 1], db = sampleDepth[k + 1];
				float ea = ua, eb = ub;
				for (int sgn = -1; sgn <= 1; sgn += 2) {
					geo.wall(List.of(q(ta, ea, side, sgn * rr), q(tb, eb, side, sgn * rr), q(tb + db, eb, side, sgn * rr), q(ta + da, ea, side, sgn * rr)),
							new float[] {1, 1, 0, 0}, new Vector3f(side).mul(-sgn), this);
					// the rim's glow along this edge, on the surface
					if (w.heat > 0.02F || w.ember > 0.05F) {
						float[] na = sampleAt[k], nb = sampleAt[k + 1];
						geo.rim(List.of(lift(q(ta, ea, side, sgn * rr), na), lift(q(tb, eb, side, sgn * rr), nb),
								lift(q(tb, eb, side, sgn * (rr + width)), nb), lift(q(ta, ea, side, sgn * (rr + width)), na)),
								new Vector3f(na[4], na[5], na[6]), this);
					}
				}
				geo.wall(List.of(q(ta + da, ea, side, -rr), q(tb + db, eb, side, -rr), q(tb + db, eb, side, rr), q(ta + da, ea, side, rr)),
						new float[] {0, 0, 0, 0}, new Vector3f(d).negate(), this);
			}
		}

		/**
		 * Whether sample lines k and k + 1 met the same stretch of surface (both met the mob, not far apart along the shot,
		 * the surface turned little between): else the trench breaks there (across an edge onto a limb, say).
		 */
		private boolean joined(int k) {
			if (k < 0 || k + 1 >= SAMPLES || Float.isNaN(sampleT[k]) || Float.isNaN(sampleT[k + 1])) {
				return false;
			}
			float step = 2 * reach() / Math.max(1, SAMPLES - 1);
			float[] a = sampleAt[k], b = sampleAt[k + 1];
			return Math.abs(sampleT[k + 1] - sampleT[k]) <= 1.5F * step + 0.01F && a[4] * b[4] + a[5] * b[5] + a[6] * b[6] > 0.6F;
		}

		/** The point t along sample line at rod offset a, pushed s across it. */
		private float[] q(float t, float a, Vector3f side, float s) {
			return p(t, a, side, s);
		}

		private static float[] lift(float[] p, float[] c) {
			return new float[] {p[0] + c[4] * 0.002F, p[1] + c[5] * 0.002F, p[2] + c[6] * 0.002F};
		}

		/** The rim's glow round a mouth: the hole's edge where it meets the surface there, out to the rim's width, a hair off it. */
		private void rim(Geometry geo, float @Nullable [] c, boolean trench) {
			if (c == null || w.heat < 0.02F && w.ember < 0.05F) {
				return;
			}
			Vector3f n = new Vector3f(c[4], c[5], c[6]);
			float dn = d.dot(n);
			if (Math.abs(dn) < 0.08F) {
				return;
			}
			Vector3f at = new Vector3f(c[1], c[2], c[3]);
			int segs = 20;
			float lift = 0.002F, width = RIM * r / (PlasmaWounds.BOLT_RADIUS / 16F) * 1.4F;
			for (int k = 0; k < segs; k++) {
				float a0 = Mth.TWO_PI * k / segs, a1 = Mth.TWO_PI * (k + 1) / segs;
				float[] i0 = onFace(a0, 0, at, n, dn, lift), i1 = onFace(a1, 0, at, n, dn, lift);
				float[] o1 = onFace(a1, width, at, n, dn, lift), o0 = onFace(a0, width, at, n, dn, lift);
				geo.rim(List.of(i0, i1, o1, o0), n, this);
			}
		}

		/** A point of the hole's edge (pushed out by {@code out}) at angle a, carried along the line onto the face. */
		private float[] onFace(float a, float out, Vector3f at, Vector3f n, float dn, float lift) {
			float rr = (ragged(a) * r + out);
			float cx = Mth.cos(a), sx = Mth.sin(a);
			Vector3f q;
			if (gash) {
				Vector3f side = new Vector3f(d).cross(u).normalize();
				float along = Mth.clamp(cx * (half + rr), -half - rr, half + rr);
				q = new Vector3f(o).add(new Vector3f(u).mul(along)).add(new Vector3f(side).mul(sx * rr));
			} else {
				q = new Vector3f(o).add(new Vector3f(e1).mul(cx * rr)).add(new Vector3f(e2).mul(sx * rr));
			}
			float k = new Vector3f(at).sub(q).dot(n) / dn;
			q.add(new Vector3f(d).mul(k)).add(new Vector3f(n).mul(lift));
			return new float[] {q.x, q.y, q.z};
		}

		private float[] ring(float t, float a, float rr) {
			float c = Mth.cos(a) * rr, s = Mth.sin(a) * rr;
			return new float[] {o.x + d.x * t + e1.x * c + e2.x * s, o.y + d.y * t + e1.y * c + e2.y * s, o.z + d.z * t + e1.z * c + e2.z * s};
		}

		private float[] p(float t, float a, Vector3f side, float s) {
			return new float[] {o.x + d.x * t + u.x * a + side.x * s, o.y + d.y * t + u.y * a + side.y * s, o.z + d.z * t + u.z * a + side.z * s};
		}

		/** How far behind the surface at crossing c a point lies (blocks; below 0: in front of it). */
		private static float depthBehind(float[] q, float[] c) {
			return -((q[0] - c[1]) * c[4] + (q[1] - c[2]) * c[5] + (q[2] - c[3]) * c[6]);
		}

		/** A polygon cut to what lies behind the surface at crossing c (inside the mob there). */
		private static List<float[]> behind(List<float[]> in, float[] c) {
			List<float[]> out = new ArrayList<>(in.size() + 2);
			for (int i = 0; i < in.size(); i++) {
				float[] a = in.get(i), b = in.get((i + 1) % in.size());
				float da = depthBehind(a, c), db = depthBehind(b, c);
				if (da >= 0) {
					out.add(a);
				}
				if (da >= 0 != db >= 0) {
					float k = da / (da - db);
					out.add(new float[] {a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k, a[2] + (b[2] - a[2]) * k});
				}
			}
			return out;
		}
	}

	// ------------------------------------------------------------------ what's drawn

	/** The walls' and rims' quads in the mob's frame (blocks): four corners (x, y, z, colour bits), then the normal. */
	static final class Geometry {
		final List<float[]> base = new ArrayList<>(), glow = new ArrayList<>();

		void wall(List<float[]> poly, float[] hot, Vector3f n, MeshHole h) {
			int k = poly.size();
			int[] c = new int[k], g = new int[k];
			float grain = 0.85F + 0.15F * PlasmaWoundsClient.hash(Math.round(poly.get(0)[0] * 128), Math.round(poly.get(0)[2] * 128), h.w.p.seed());
			for (int i = 0; i < k; i++) {
				c[i] = PlasmaWoundsClient.wallColour(hot[i], h.w.heat, h.w.ember, grain);
				g[i] = PlasmaWoundsClient.glowColour(hot[i], h.w.heat, h.w.ember);
			}
			add(base, poly, c, n);
			if (h.w.heat > 0.01F || h.w.ember > 0.03F) {
				add(glow, poly, g, n);
			}
		}

		void rim(List<float[]> poly, Vector3f n, MeshHole h) {
			float heat = h.w.heat, ember = h.w.ember;
			int c = PlasmaWoundsClient.argbOf(0.85F * heat + 0.7F * (1 - heat) * ember, 0.50F * heat + 0.18F * (1 - heat) * ember, 0.16F * heat);
			int[] cs = new int[poly.size()];
			Arrays.fill(cs, c);
			add(glow, poly, cs, n);
		}

		/** A convex polygon as quads (a fan), wound to face along n (counter-clockwise seen from that side). */
		private static void add(List<float[]> to, List<float[]> pts, int[] colours, Vector3f n) {
			int k = pts.size();
			if (k < 3) {
				return;
			}
			float[] a = pts.get(0), b = pts.get(1), c = pts.get(2);
			float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
			boolean flip = (uy * vz - uz * vy) * n.x() + (uz * vx - ux * vz) * n.y() + (ux * vy - uy * vx) * n.z() < 0;
			for (int i = 1; i + 1 < k; i += 2) {
				int[] idx = {0, i, i + 1, Math.min(i + 2, k - 1)};
				float[] q = new float[19];
				for (int j = 0; j < 4; j++) {
					int from = flip ? (k - idx[j]) % k : idx[j];
					float[] p = pts.get(from);
					q[4 * j] = p[0];
					q[4 * j + 1] = p[1];
					q[4 * j + 2] = p[2];
					q[4 * j + 3] = Float.intBitsToFloat(colours[from]);
				}
				q[16] = n.x();
				q[17] = n.y();
				q[18] = n.z();
				to.add(q);
			}
		}

		void emit(List<float[]> quads, PoseStack.Pose pose, VertexConsumer out, int light, boolean normals) {
			Vector3f pos = new Vector3f(), n = new Vector3f();
			for (float[] q : quads) {
				pose.transformNormal(q[16], q[17], q[18], n);
				for (int i = 0; i < 4; i++) {
					pose.pose().transformPosition(q[4 * i], q[4 * i + 1], q[4 * i + 2], pos);
					out.addVertex(pos.x(), pos.y(), pos.z(), Float.floatToRawIntBits(q[4 * i + 3]), 0.5F, 0.5F, OverlayTexture.NO_OVERLAY, light,
							n.x(), n.y(), n.z());
				}
			}
		}
	}

	// ------------------------------------------------------------------ cutting what the mob draws

	/**
	 * Takes the mob's own faces as they're written, four corners at a time (its render type draws quads), and passes them
	 * on: whole where no hole comes near, else in cells with the hole's left out and its edge seared; notes where each
	 * wound's line crosses each face.
	 */
	static final class Cutter implements VertexConsumer {
		private final VertexConsumer out;
		private final List<MeshHole> holes;
		private final Matrix4f toLocal;
		private final State st;
		/** Up to four corners: x, y, z (as drawn), colour, u, v, overlay, light, normal x, y, z; and the one being written. */
		private final float[][] v = new float[4][11];
		private int n;
		private final Vector3f[] local = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};

		Cutter(VertexConsumer out, List<MeshHole> holes, Matrix4f toLocal, State st) {
			this.out = out;
			this.holes = holes;
			this.toLocal = toLocal;
			this.st = st;
		}

		private float[] cur() {
			return v[Math.max(0, n - 1)];
		}

		private void begin(float x, float y, float z) {
			if (n == 4) {
				quad();
				n = 0;
			}
			float[] c = v[n++];
			c[0] = x;
			c[1] = y;
			c[2] = z;
			c[3] = Float.intBitsToFloat(-1);
			c[4] = 0;
			c[5] = 0;
			c[6] = Float.intBitsToFloat(OverlayTexture.NO_OVERLAY);
			c[7] = Float.intBitsToFloat(0xF000F0);
			c[8] = 0;
			c[9] = 1;
			c[10] = 0;
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			begin(x, y, z);
			return this;
		}

		@Override
		public void addVertex(float x, float y, float z, int colour, float u, float w, int overlay, int light, float nx, float ny, float nz) {
			begin(x, y, z);
			float[] c = v[n - 1];
			c[3] = Float.intBitsToFloat(colour);
			c[4] = u;
			c[5] = w;
			c[6] = Float.intBitsToFloat(overlay);
			c[7] = Float.intBitsToFloat(light);
			c[8] = nx;
			c[9] = ny;
			c[10] = nz;
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			cur()[3] = Float.intBitsToFloat(a << 24 | r << 16 | g << 8 | b);
			return this;
		}

		@Override
		public VertexConsumer setColor(int argb) {
			cur()[3] = Float.intBitsToFloat(argb);
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float w) {
			cur()[4] = u;
			cur()[5] = w;
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int w) {
			cur()[6] = Float.intBitsToFloat(u & 0xFFFF | w << 16);
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int w) {
			cur()[7] = Float.intBitsToFloat(u & 0xFFFF | w << 16);
			return this;
		}

		@Override
		public VertexConsumer setUv3(float u, float w) {
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			float[] c = cur();
			c[8] = x;
			c[9] = y;
			c[10] = z;
			return this;
		}

		@Override
		public VertexConsumer setLineWidth(float width) {
			return this;
		}

		/** The last corners written (a quad, or a part of one that never finished: passed on as it was). */
		void finish() {
			if (n == 4) {
				quad();
			} else {
				for (int i = 0; i < n; i++) {
					pass(v[i]);
				}
			}
			n = 0;
		}

		private void pass(float[] c) {
			out.addVertex(c[0], c[1], c[2], Float.floatToRawIntBits(c[3]), c[4], c[5], Float.floatToRawIntBits(c[6]), Float.floatToRawIntBits(c[7]),
					c[8], c[9], c[10]);
		}

		private void quad() {
			for (int i = 0; i < 4; i++) {
				toLocal.transformPosition(v[i][0], v[i][1], v[i][2], local[i]);
			}
			Vector3f a = local[0], b = local[1], d = local[3];
			Vector3f es = new Vector3f(b).sub(a), et = new Vector3f(d).sub(a);
			Vector3f fn = new Vector3f(es).cross(new Vector3f(local[2]).sub(a));
			if (fn.lengthSquared() < 1e-14F) {
				fn = new Vector3f(es).cross(et);
			}
			float area = fn.length();
			if (area < 1e-9F) {
				for (int i = 0; i < 4; i++) {
					pass(v[i]);
				}
				return;
			}
			fn.div(area);
			Vector3f centre = new Vector3f(a).add(local[1]).add(local[2]).add(d).div(4);
			float rad = 0;
			for (Vector3f p : local) {
				rad = Math.max(rad, p.distance(centre));
			}
			List<MeshHole> near = null;
			for (MeshHole h : holes) {
				cross(h, fn);
				if (h.cuts && h.edge(centre.x, centre.y, centre.z, true) < rad + CHAR * 1.3F) {
					if (near == null) {
						near = new ArrayList<>(2);
					}
					near.add(h);
				}
			}
			if (near == null) {
				for (int i = 0; i < 4; i++) {
					pass(v[i]);
				}
				return;
			}
			cells(near, es.length(), et.length());
		}

		/**
		 * Where hole h's line crosses this face (inside it), noted with the face's outward way and whether it goes in; for a
		 * gash, each of its sample lines across the rod.
		 */
		private void cross(MeshHole h, Vector3f fn) {
			float dn = h.d.dot(fn);
			if (Math.abs(dn) < 1e-5F) {
				return;
			}
			int n = h.gash ? SAMPLES : 1;
			for (int k = 0; k < n; k++) {
				Vector3f o = h.gash ? new Vector3f(h.u).mul(h.sampleU(k)).add(h.o) : h.o;
				float t = new Vector3f(local[0]).sub(o).dot(fn) / dn;
				Vector3f x = new Vector3f(h.d).mul(t).add(o);
				if (!inside(x, local[0], local[1], local[2], fn) && !inside(x, local[0], local[2], local[3], fn)) {
					continue;
				}
				st.add(h.w, new float[] {t, x.x, x.y, x.z, fn.x, fn.y, fn.z, dn < 0 ? 1 : 0, k});
			}
		}

		private static boolean inside(Vector3f x, Vector3f a, Vector3f b, Vector3f c, Vector3f n) {
			return side(x, a, b, n) && side(x, b, c, n) && side(x, c, a, n);
		}

		private static boolean side(Vector3f x, Vector3f a, Vector3f b, Vector3f n) {
			float ex = b.x - a.x, ey = b.y - a.y, ez = b.z - a.z, px = x.x - a.x, py = x.y - a.y, pz = x.z - a.z;
			return (ey * pz - ez * py) * n.x + (ez * px - ex * pz) * n.y + (ex * py - ey * px) * n.z >= -1e-7F;
		}

		/** The face in cells about the size of a half pixel of the wound's (smaller on a small creature): holes left out. */
		private void cells(List<MeshHole> near, float ls, float lt) {
			float cell = Float.MAX_VALUE;
			for (MeshHole h : near) {
				cell = Math.min(cell, Math.max(0.004F, h.r / 4F));
			}
			int ns = Mth.clamp(Math.round(ls / cell), 1, 96), nt = Mth.clamp(Math.round(lt / cell), 1, 96);
			for (int i = 0; i < ns; i++) {
				int run = -1;
				int runColour = 0;
				for (int j = 0; j <= nt; j++) {
					boolean hole = true;
					int c = 0;
					if (j < nt) {
						float s = (i + 0.5F) / ns, t = (j + 0.5F) / nt;
						Vector3f p = bilinear(local, s, t);
						float best = 1e9F;
						MeshHole by = null;
						for (MeshHole h : near) {
							float e = h.edge(p.x, p.y, p.z, false);
							if (e < best) {
								best = e;
								by = h;
							}
						}
						hole = best < 0;
						int own = Float.floatToRawIntBits(v[0][3]);
						c = by == null || best > CHAR * 2 ? own : by.sear(own, best, i, j);
					}
					if (run >= 0 && (hole || c != runColour)) {
						rect((float) i / ns, (float) run / nt, (float) (i + 1) / ns, (float) j / nt, runColour);
						run = -1;
					}
					if (!hole && run < 0) {
						run = j;
						runColour = c;
					}
				}
			}
		}

		private static Vector3f bilinear(Vector3f[] q, float s, float t) {
			// corner 0, along to corner 1 (s), corner 3 (t): as the face's own corners lie
			return new Vector3f(q[0]).mul((1 - s) * (1 - t)).add(new Vector3f(q[1]).mul(s * (1 - t))).add(new Vector3f(q[2]).mul(s * t))
					.add(new Vector3f(q[3]).mul((1 - s) * t));
		}

		/** The part of the face from (s0, t0) to (s1, t1), wound as the face is, coloured c. */
		private void rect(float s0, float t0, float s1, float t1, int c) {
			corner(s0, t0, c);
			corner(s1, t0, c);
			corner(s1, t1, c);
			corner(s0, t1, c);
		}

		private void corner(float s, float t, int c) {
			float w0 = (1 - s) * (1 - t), w1 = s * (1 - t), w2 = s * t, w3 = (1 - s) * t;
			float[] a = v[0], b = v[1], e = v[2], f = v[3];
			out.addVertex(a[0] * w0 + b[0] * w1 + e[0] * w2 + f[0] * w3, a[1] * w0 + b[1] * w1 + e[1] * w2 + f[1] * w3,
					a[2] * w0 + b[2] * w1 + e[2] * w2 + f[2] * w3, c, a[4] * w0 + b[4] * w1 + e[4] * w2 + f[4] * w3,
					a[5] * w0 + b[5] * w1 + e[5] * w2 + f[5] * w3, Float.floatToRawIntBits(a[6]), Float.floatToRawIntBits(a[7]), a[8], a[9], a[10]);
		}
	}

	// ------------------------------------------------------------------ the wrapped collector

	/**
	 * Passes everything on; the mob's own geometry (quads) through a {@link Cutter}; and for a creature's part
	 * ({@code part}: its render state), the holes worked out on its model as that is submitted ({@link #PART}).
	 */
	static final class Wrapped implements SubmitNodeCollector {
		private final OrderedSubmitNodeCollector out;
		private final @Nullable SubmitNodeCollector root;
		private final List<MeshHole> holes;
		private final Matrix4f toLocal;
		private final State st;
		/** The renderer's own collector (what the walls of a part's wounds go on), and the part's render state, if it's one. */
		private final SubmitNodeCollector top;
		private final @Nullable EntityRenderState part;

		Wrapped(OrderedSubmitNodeCollector out, @Nullable SubmitNodeCollector root, List<MeshHole> holes, Matrix4f toLocal, State st,
				SubmitNodeCollector top, @Nullable EntityRenderState part) {
			this.out = out;
			this.root = root;
			this.holes = holes;
			this.toLocal = toLocal;
			this.st = st;
			this.top = top;
			this.part = part;
		}

		@Override
		public OrderedSubmitNodeCollector order(int order) {
			if (root == null) {
				throw new UnsupportedOperationException();
			}
			return new Wrapped(root.order(order), null, holes, toLocal, st, top, part);
		}

		@Override
		public void submitCustomGeometry(PoseStack pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer) {
			if (holes.isEmpty() || type.primitiveTopology() != PrimitiveTopology.QUADS) {
				out.submitCustomGeometry(pose, type, renderer);
				return;
			}
			List<MeshHole> h = holes;
			Matrix4f m = toLocal;
			State s = st;
			out.submitCustomGeometry(pose, type, (p, consumer) -> {
				Cutter cut = new Cutter(consumer, h, m, s);
				renderer.render(p, cut);
				cut.finish();
			});
		}

		@Override
		public void submitShadow(PoseStack pose, float radius, List<EntityRenderState.ShadowPiece> pieces) {
			out.submitShadow(pose, radius, pieces);
		}

		@Override
		public void submitNameTag(PoseStack pose, Vec3 at, int y, Component text, boolean seeThrough, int light, CameraRenderState camera) {
			out.submitNameTag(pose, at, y, text, seeThrough, light, camera);
		}

		@Override
		public void submitText(PoseStack pose, float x, float y, FormattedCharSequence text, boolean shadow, Font.DisplayMode mode, int light, int colour,
				int background, int outline) {
			out.submitText(pose, x, y, text, shadow, mode, light, colour, background, outline);
		}

		@Override
		public void submitTextBackground(PoseStack pose, float x0, float y0, float x1, float y1, int colour, Font.DisplayMode mode, int light) {
			out.submitTextBackground(pose, x0, y0, x1, y1, colour, mode, light);
		}

		@Override
		public void submitFlame(PoseStack pose, EntityRenderState state, Quaternionf rotation) {
			out.submitFlame(pose, state, rotation);
		}

		@Override
		public void submitLeash(PoseStack pose, EntityRenderState.LeashState leash) {
			out.submitLeash(pose, leash);
		}

		@Override
		public <S> void submitModel(Model<? super S> model, S state, PoseStack pose, RenderType type, int light, int overlay, int colour, UvMapping uv,
				int outline) {
			if (part != null && (Object) state == part && model instanceof net.minecraft.client.model.EntityModel<?> creature) {
				PlasmaWoundsClient.woundModel(part, creature, pose, top, light);
			}
			out.submitModel(model, state, pose, type, light, overlay, colour, uv, outline);
		}

		@Override
		public <S> void submitCrumblingOverlay(Model<? super S> model, S state, PoseStack pose, RenderType type, int light, int overlay, int colour,
				ModelFeatureRenderer.CrumblingOverlay crumbling) {
			out.submitCrumblingOverlay(model, state, pose, type, light, overlay, colour, crumbling);
		}

		@Override
		public void submitMovingBlock(PoseStack pose, MovingBlockRenderState state, int light) {
			out.submitMovingBlock(pose, state, light);
		}

		@Override
		public void submitBlockModel(PoseStack pose, RenderType type, List<BlockStateModelPart> parts, int[] tints, int light, int overlay, int outline) {
			out.submitBlockModel(pose, type, parts, tints, light, overlay, outline);
		}

		@Override
		public void submitBreakingBlockModel(PoseStack pose, List<BlockStateModelPart> parts, int progress, boolean flag) {
			out.submitBreakingBlockModel(pose, parts, progress, flag);
		}

		@Override
		public void submitShapeOutline(PoseStack pose, VoxelShape shape, RenderType type, int colour, float width, boolean flag) {
			out.submitShapeOutline(pose, shape, type, colour, width, flag);
		}

		@Override
		public void submitItem(PoseStack pose, ItemDisplayContext context, int light, int overlay, int outline, int[] tints, ItemQuads quads,
				ItemStackRenderState.FoilType foil) {
			out.submitItem(pose, context, light, overlay, outline, tints, quads, foil);
		}

		@Override
		public void submitQuadParticleGroup(QuadParticleRenderState state) {
			out.submitQuadParticleGroup(state);
		}

		@Override
		public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean flag) {
			out.submitGizmoPrimitives(group, camera, flag);
		}

		@Override
		public void submitBlockModel(PoseStack pose, Function<ChunkSectionLayer, RenderType> types, boolean flag, List<BlockStateModelPart> parts, Mesh mesh,
				int[] tints, int light, int overlay, int outline) {
			out.submitBlockModel(pose, types, flag, parts, mesh, tints, light, overlay, outline);
		}

		@Override
		public void submitBreakingBlockModel(PoseStack pose, List<BlockStateModelPart> parts, Mesh mesh, int progress, boolean flag) {
			out.submitBreakingBlockModel(pose, parts, mesh, progress, flag);
		}

		@Override
		public void submitItem(PoseStack pose, ItemDisplayContext context, int light, int overlay, int outline, int[] tints, ItemQuads quads, MeshView mesh,
				ItemStackRenderState.FoilType foil) {
			out.submitItem(pose, context, light, overlay, outline, tints, quads, mesh, foil);
		}

		@Override
		public <T extends SubmitNode> void submitCustom(SubmitRenderPhase<T> phase, T node) {
			out.submitCustom(phase, node);
		}
	}
}
