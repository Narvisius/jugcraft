package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Plasma wounds on this client (tower/crew/PlasmaWounds; the plasma wounds sheet, take 2), remade on 6 November 2026
 * (Justin: "it's not shown through the clothes ... it should have gone through its head. So there should be just a hole
 * in its head ... if you hit it at an angle, then even at an angle, it should be missing stuff ... I want this to be able
 * to apply to any mob ... it stretched the texture way out ... that long cylinder going through it"; and "it doesnt work
 * on some mobs like creepers").
 *
 * <p>A bolt's wound is a tunnel along the shot, the way it actually went, right through the mob: the first time the mob
 * is drawn after the hit, the shot's line is followed through every box of its model and kept, for each part it passes
 * through or close by (a head and the nose on it, a body, an arm behind), in that part's own frame, so each piece of the
 * tunnel moves with its part. Then any box of that part is drawn cut ({@link #cut}, from ModelPart.Cube.compile),
 * whichever model draws it: the mob's own, and the layers over it drawn from the same parts (clothes, an outer skin,
 * armour, glowing spots), so the hole goes through those too. A face the tunnel passes through is drawn in half-pixel
 * cells, those inside the hole left out (an oblique shot makes an oval hole, a grazing one a groove), those at its edge
 * tinted white-hot to orange to char as the wound cools, a ragged ring of char round it; the face's own texture is laid
 * on each cell from the face's corners, so it is never stretched. The wound layer ({@link WoundLayer}) draws the tunnel's
 * walls clipped to the inside of each box (never outside the mob), seared and glowing towards the mouths, and the rim's
 * glow just off the faces. The baton's gash is a trench along the rod, burned in a few pixels deep, the same way.
 *
 * <p>Every layer drawn over the mob (wool, clothes, armour) is cut too, and the wound layer draws the walls through it
 * from its surface down to the mob's own (Justin, 8 November 2026: the sheep's wound "see-through", the baton's to go
 * "through everything, not just one layer"): the gash goes through them into the flesh. On a little creature (the
 * thinner side of its game box under about 0.6 blocks: a chicken, a silverfish) the hole and the gash are smaller, never more than
 * a share of it across.
 *
 * <p>All of that is worked out once a wound (in each part's own frame, which doesn't change as the mob moves) and kept
 * with the mob; each frame only places it with the parts as they stand and colours it by how far it has cooled. Mobs
 * drawn some other way than from model parts (GeckoLib's, say) get the same wounds from {@link PlasmaWoundsMesh}.
 *
 * <p>Since r196 (the power armour sheets, takes 5 to 8): a swing's wound is the fan its edge swept (PlasmaWounds.Swing;
 * {@link #placeFan}, a {@link Hole} with a sweep), burned through every part that lies in it; and what a cut goes right
 * through comes off ({@link #sever}, {@link PlasmaWoundsDebris}): it's left out of the mob's drawing, and falls and
 * burns as a body of its own made from the very faces the mob's models drew for it ({@link #cut} gathers them). Its
 * trunk and head hold ({@link Vital}) unless the blow was the death of it; then it all comes apart.
 */
public final class PlasmaWoundsClient {
	static final float CELL = 0.5F, OFF = 0.09F;
	/** The rim's width and how far the char reaches past it (px, from the hole's edge). */
	static final float RIM = 0.35F, CHAR = 1.4F;
	/** How far past a part's own boxes along the shot its hole still cuts (px: layers drawn grown over them, armour). */
	static final float LAYER = 2.5F;
	/** How much wider the tunnel's walls are than the hole (px): past the face's half-pixel steps, so no chink shows. */
	static final float WALL = 0.4F;
	/** The ragged edge's lobes: how many, and the most they can push the edge out (a share of the radius). */
	static final int LOBES = 6;
	static final float LOBE_MAX = 0.31F;
	/**
	 * The most one mob keeps (the oldest goes first). Was 8 (Justin, 7 November 2026: "some of the holes will just like
	 * disappear out of the warden when I hit it ... its skin just appears normal again"): a tough mob takes many more.
	 */
	static final int MAX = 40;
	static final RenderStateDataKey<Marks> MARKS = RenderStateDataKey.create(() -> "jugcraft:plasma_wounds");
	static final RenderStateDataKey<Holes> HOLES = RenderStateDataKey.create(() -> "jugcraft:plasma_wound_holes");
	static final Identifier WHITE = Jugcraft.id("textures/entity/plasma_wound.png");

	private static final Map<Integer, Marks> BY_ENTITY = new HashMap<>();
	private static final Map<Model<?>, Map<ModelPart.Cube, String>> PATHS = Collections.synchronizedMap(new WeakHashMap<>());
	private static @Nullable ClientLevel level;
	private static final ThreadLocal<Drawing> DRAWING = new ThreadLocal<>();
	private static final ThreadLocal<Object[]> WITH = new ThreadLocal<>();

	/** A mob's wounds, and what's worked out from them for the model it was last drawn with. */
	static final class Marks {
		final ArrayDeque<Wound> wounds = new ArrayDeque<>();
		int version;
		@Nullable Holes holes;
		int holesVersion = -1;
		@Nullable Object holesModel;
		/**
		 * The boxes of the models drawn over the mob's own (its wool, clothes, armour) that a hole went through when it was
		 * last drawn, and their parts: the wound layer draws the tunnel's walls in them too (Justin, 8 November 2026: the
		 * sheep's wound "see-through", the baton's cut "through everything, not just one layer").
		 */
		final Map<ModelPart.Cube, String> layerCubes = Collections.synchronizedMap(new IdentityHashMap<>());
		/** The generic wounds' state, for a mob not drawn from model parts (PlasmaWoundsMesh). */
		PlasmaWoundsMesh.@Nullable State mesh;
		/** What has come off it already, by part (its flesh's cells: PlasmaWoundsDebris.Flesh): it stays off, whatever its wounds do. */
		final Map<String, BitSet> shed = new HashMap<>();
		/** A plasma blow killed it and it has come apart ({@link PlasmaWoundsDebris}): nothing of it is drawn any more. */
		volatile boolean vanished;
		/**
		 * For the pass it's being drawn in: where its feet are in that pass's space (the model's point (0, 1.501, 0)) and in
		 * the world, so that what's drawn of it can be placed in the world whatever camera the pass has.
		 */
		volatile float ax, ay, az;
		volatile double wx, wy, wz;
	}

	/** A wound as the server sent it, and once placed on a model: its hole through each part it crosses; how hot it is now. */
	static final class Wound {
		final PlasmaWounds.Payload p;
		final long born;
		final float[] lobes;
		/** It cut something off: it isn't dropped for a newer wound while others can be. */
		boolean pinned;
		@Nullable Object model;
		Map<String, Hole> holes = Map.of();
		float heat = 1, ember = 1;
		/** How much of its full size it's drawn at on this mob: a little one's thinnest part keeps it from swallowing it. */
		float size = 1;

		Wound(PlasmaWounds.Payload p, long born) {
			this.p = p;
			this.born = born;
			Random r = new Random(p.seed());
			lobes = new float[2 * LOBES];
			for (int k = 0; k < LOBES; k++) {
				lobes[2 * k] = (0.04F + r.nextFloat() * 0.06F) / Mth.sqrt(k + 2);
				lobes[2 * k + 1] = r.nextFloat() * Mth.TWO_PI;
			}
		}

		/** A cut burned in from the surface (the baton's gash, a swing's fan), not a tunnel right through. */
		boolean gash() {
			return p.kind() != PlasmaWounds.THROUGH;
		}

		/** A swing's fan: whatever lay in the edge's sweep (PlasmaWounds.Swing). */
		boolean fan() {
			return p.kind() == PlasmaWounds.FAN;
		}

		/** Cools from white-hot (heat) through glowing embers (ember) to char, by age (seconds). */
		void age(float seconds) {
			heat = (float) Math.exp(-seconds / 2.0);
			ember = (float) Math.exp(-seconds / 6.0);
		}
	}

	/**
	 * A wound's line through one part (that part's frame, px): a point on it and its way (unit), how far along it the
	 * part's boxes start and end (t0, t1: for a gash, where it first met the mob), and for a gash the rod's way across it
	 * (unit, square to the line) and half its length.
	 */
	record Line(Vector3f o, Vector3f d, float t0, float t1, Vector3f u, float half) {
	}

	/**
	 * A wound's hole through one part: its line, radius, ragged edge, kind, depth (a gash's) and frame round the line. A
	 * swing's fan ({@code sweep} above 0): the line's point is the swing's pivot, its way the fan's middle, its rod the way
	 * the edge swept; the edge burned from {@code inner} to {@code outer} px out, over {@code sweep} radians either side of
	 * the middle, {@code r} thick each side of the fan's plane (whose normal is e2).
	 */
	record Hole(Wound wound, Line line, float r, boolean gash, float depth, Vector3f e1, Vector3f e2, float inner, float outer, float sweep) {
		static Hole of(Line line, Wound w) {
			Vector3f d = line.d();
			Vector3f e1 = line.half() > 0 ? new Vector3f(line.u()) : perpendicular(d);
			Vector3f e2 = new Vector3f(d).cross(e1).normalize();
			return new Hole(w, line, w.p.radius() * w.size, w.gash(), w.p.depth() * w.size, e1, e2, 0, 0, 0);
		}

		/** A swing's fan through a part: the line as {@link #line} gives it (its rod square to its way), from inner to outer px out. */
		static Hole fan(Line line, Wound w, float inner, float outer) {
			Vector3f e1 = new Vector3f(line.u());
			Vector3f e2 = new Vector3f(line.d()).cross(e1).normalize();
			return new Hole(w, line, w.p.radius() * w.size, true, 0, e1, e2, inner, outer, w.p.length());
		}

		boolean fan() {
			return sweep > 0;
		}

		/** Whether the hole (grown by {@code grow} px) may reach into a box (min x, y, z, max x, y, z; the part's frame). */
		boolean near(float[] box, float grow) {
			if (!fan()) {
				return clip(line, box, grow + line.half()) != null;
			}
			// within the fan's reach of its pivot, and not all to one side of its plane
			Vector3f o = line.o();
			float dx = Math.max(Math.max(box[0] - o.x(), 0), o.x() - box[3]), dy = Math.max(Math.max(box[1] - o.y(), 0), o.y() - box[4]),
					dz = Math.max(Math.max(box[2] - o.z(), 0), o.z() - box[5]);
			float reach = outer + grow;
			if (dx * dx + dy * dy + dz * dz > reach * reach) {
				return false;
			}
			float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
			for (int k = 0; k < 8; k++) {
				float h = (((k & 1) == 0 ? box[0] : box[3]) - o.x()) * e2.x() + (((k & 2) == 0 ? box[1] : box[4]) - o.y()) * e2.y()
						+ (((k & 4) == 0 ? box[2] : box[5]) - o.z()) * e2.z();
				lo = Math.min(lo, h);
				hi = Math.max(hi, h);
			}
			return lo <= grow && hi >= -grow;
		}

		float heat() {
			return wound.heat;
		}

		float ember() {
			return wound.ember;
		}

		int seed() {
			return wound.p.seed();
		}

		/**
		 * Where along the line the hole cuts (px): a bolt's past the part's boxes by a layer's reach either side; a gash's
		 * from a layer's reach before the skin (through any wool, clothes or armour over it) to its depth into it.
		 */
		float from() {
			return line.t0() - LAYER;
		}

		float to() {
			return gash ? line.t0() + depth : line.t1() + LAYER;
		}

		/** The ragged edge's reach at an angle round the line (a share of the radius). */
		float ragged(float ang) {
			float[] lobes = wound.lobes;
			float rr = 1;
			for (int k = 0; k < LOBES; k++) {
				rr += lobes[2 * k] * Mth.sin((k + 2) * ang + lobes[2 * k + 1]);
			}
			return rr;
		}

		/**
		 * How far (px) the point (part frame, px) is outside the hole's ragged edge: below 0 inside. Past the ends of the
		 * cut along the line by more than {@code slack}, a large number; with {@code rough}, a quick lower bound (the
		 * ragged edge at its widest).
		 */
		float edge(float x, float y, float z, float slack, boolean rough) {
			Line l = line;
			float px = x - l.o().x(), py = y - l.o().y(), pz = z - l.o().z();
			float t = px * l.d().x() + py * l.d().y() + pz * l.d().z();
			if (sweep > 0) {
				// a swing's fan: how far off its plane, and how far outside the swept part of it in the plane
				float h = px * e2.x() + py * e2.y() + pz * e2.z();
				float most = (1 + LOBE_MAX) * r + slack;
				if (Math.abs(h) > most) {
					return 1e9F;
				}
				float b = px * e1.x() + py * e1.y() + pz * e1.z();
				float rho = Mth.sqrt(t * t + b * b);
				if (rho > outer + most || rho < inner - most) {
					return 1e9F;
				}
				float phi = (float) Math.atan2(b, t), off;
				if (Math.abs(phi) <= sweep) {
					off = Math.max(0, Math.max(inner - rho, rho - outer));
				} else {
					float e = Math.copySign(sweep, phi), ce = Mth.cos(e), se = Mth.sin(e);
					float along = Mth.clamp(t * ce + b * se, inner, outer);
					float ox = t - along * ce, oy = b - along * se;
					off = Mth.sqrt(ox * ox + oy * oy);
				}
				float dist = Mth.sqrt(off * off + h * h);
				if (rough) {
					return dist - (1 + LOBE_MAX) * r;
				}
				// its ragged edge wanders along the cut
				return dist - ragged((float) Math.atan2(h, off) + rho * 0.9F + phi * 4F) * r;
			}
			if (t < from() - slack || t > to() + slack) {
				return 1e9F;
			}
			float wx = px - t * l.d().x(), wy = py - t * l.d().y(), wz = pz - t * l.d().z();
			if (l.half() > 0) {
				float a = Mth.clamp(wx * l.u().x() + wy * l.u().y() + wz * l.u().z(), -l.half(), l.half());
				wx -= a * l.u().x();
				wy -= a * l.u().y();
				wz -= a * l.u().z();
			}
			float dist = Mth.sqrt(wx * wx + wy * wy + wz * wz);
			if (rough) {
				return dist - (1 + LOBE_MAX) * r;
			}
			float ang = (float) Math.atan2(wx * e2.x() + wy * e2.y() + wz * e2.z(), wx * e1.x() + wy * e1.y() + wz * e1.z());
			return dist - ragged(ang) * r;
		}

		float edge(float x, float y, float z) {
			return edge(x, y, z, 0, false);
		}
	}

	/**
	 * A mob's holes, by part; and kept as they're first needed (by box: the mob's own model and every layer over it that
	 * draws the same box share them): each face's cells round the holes, and the walls and rim glow inside each box.
	 */
	static final class Holes {
		final Map<String, List<Hole>> byPath;
		private final Map<ModelPart.Cube, Grid[]> grids = Collections.synchronizedMap(new IdentityHashMap<>());
		private final Map<ModelPart.Cube, List<Piece>> pieces = new IdentityHashMap<>();
		/** Each part's flesh that something has been cut off (or that's coming apart), by its path (PlasmaWoundsDebris.Flesh). */
		final Map<String, PlasmaWoundsDebris.Flesh> flesh = new HashMap<>();
		/** The mob's own boxes' extents by part (a layer's box is only walled outside them). */
		final Map<String, List<float[]>> own = new HashMap<>();
		/** What's coming off now, by part and piece: gathered from this frame's drawing, then let go; and what's been gathered. */
		final Map<String, PlasmaWoundsDebris.Body[]> bodies = new HashMap<>();
		private final Set<Object> gathered = Collections.synchronizedSet(new HashSet<>());
		final List<PlasmaWoundsDebris.Body> made = new ArrayList<>();
		/** Pieces have come off that no body has been made for yet (the wound layer makes them, with the mob's place). */
		boolean fresh;
		/** A plasma blow killed it: all of it comes apart. */
		boolean fatal;

		Holes(Map<String, List<Hole>> byPath) {
			this.byPath = byPath;
		}

		/** The body the piece of the part at {@code path} that a flesh cell's state names is coming off as, if it's still being gathered. */
		PlasmaWoundsDebris.@Nullable Body gathering(String path, byte state) {
			if (state < PlasmaWoundsDebris.Flesh.PIECE) {
				return null;
			}
			PlasmaWoundsDebris.Body[] of = bodies.get(path);
			int k = state - PlasmaWoundsDebris.Flesh.PIECE;
			PlasmaWoundsDebris.Body b = of == null || k >= of.length ? null : of[k];
			return b != null && b.open && !PlasmaWoundsDebris.broken ? b : null;
		}

		/** Whether any body made of it got something to show (if none did, a mob that came apart is drawn dying as ever). */
		boolean shown() {
			for (PlasmaWoundsDebris.Body b : made) {
				if (!b.empty()) {
					return true;
				}
			}
			return false;
		}

		/** Whether anything is still being gathered into a body (the holes aren't worked out afresh meanwhile). */
		boolean gathering() {
			if (PlasmaWoundsDebris.broken) {
				return false;
			}
			if (fresh) {
				return true;
			}
			for (PlasmaWoundsDebris.Body[] of : bodies.values()) {
				for (PlasmaWoundsDebris.Body b : of) {
					if (b != null && b.open) {
						return true;
					}
				}
			}
			return false;
		}

		/** The cells of each of a box's faces round the holes (a face no hole comes near: null); null if none do. */
		Grid @Nullable [] grids(ModelPart.Cube cube, List<Hole> holes, @Nullable String path) {
			Grid[] g = grids.get(cube);
			if (g == null) {
				g = new Grid[cube.polygons.length];
				boolean any = false;
				float[] box = bounds(cube);
				List<Hole> near = new ArrayList<>();
				for (Hole h : holes) {
					if (h.near(box, h.r() * (1 + LOBE_MAX) + CHAR * 1.3F)) {
						near.add(h);
					}
				}
				PlasmaWoundsDebris.Flesh f = path == null ? null : flesh.get(path);
				if (f != null && !f.anyGone) {
					f = null;
				}
				if (!near.isEmpty() || f != null) {
					for (int i = 0; i < g.length; i++) {
						g[i] = Grid.of(cube.polygons[i], near, f);
						any |= g[i] != null;
					}
				}
				if (!any) {
					g = new Grid[0];
				}
				grids.put(cube, g);
			}
			return g.length == 0 ? null : g;
		}

		/**
		 * The walls and rim glow inside one box (its part frame, px): one of the mob's own, or ({@code layer}) one of a model
		 * drawn over it, where only the tunnel between its surface and the mob's own is drawn.
		 */
		List<Piece> pieces(ModelPart.Cube cube, List<Hole> holes, boolean layer, String path) {
			List<Piece> list = pieces.get(cube);
			if (list == null) {
				list = new ArrayList<>();
				float[] box = bounds(cube);
				for (Hole h : holes) {
					walls(list, box, h, holes, layer, layer ? own.get(path) : null);
				}
				Grid[] g = grids(cube, holes, path);
				if (g != null) {
					rims(list, cube, g);
				}
				pieces.put(cube, list);
			}
			return list;
		}
	}

	/**
	 * One face's half-pixel cells round the holes: how many along each edge, the window of them a hole or its char
	 * touches, and for each cell there how far it is from the nearest hole's edge (below 0: in the hole) and which hole.
	 */
	record Grid(int ns, int nt, int s0, int s1, int t0, int t1, float[] e, Hole[] who, byte @Nullable [] st) {
		static final int BLOCK = 4;

		/**
		 * A face's cells round the holes; and, where something has been cut off the part ({@code flesh}), what has become
		 * of the flesh under each cell (st: gone, or of a piece coming off: PlasmaWoundsDebris.Flesh's states).
		 */
		static @Nullable Grid of(ModelPart.Polygon poly, List<Hole> holes, PlasmaWoundsDebris.@Nullable Flesh flesh) {
			ModelPart.Vertex[] v = poly.vertices();
			if (v.length != 4) {
				return null;
			}
			float ox = v[0].x(), oy = v[0].y(), oz = v[0].z();
			float ex = v[1].x() - ox, ey = v[1].y() - oy, ez = v[1].z() - oz;
			float fx = v[3].x() - ox, fy = v[3].y() - oy, fz = v[3].z() - oz;
			float ls = Mth.sqrt(ex * ex + ey * ey + ez * ez), lt = Mth.sqrt(fx * fx + fy * fy + fz * fz);
			if (ls < 1e-4F || lt < 1e-4F) {
				return null;
			}
			// finer cells for a wound shrunk to a little creature
			float size = 1;
			for (Hole h : holes) {
				size = Math.min(size, h.wound().size);
			}
			float cell = CELL * Mth.clamp(size, 0.4F, 1F);
			int ns = Math.max(1, Math.round(ls / cell)), nt = Math.max(1, Math.round(lt / cell));
			float ds = 1F / ns, dt = 1F / nt;
			float[] e = new float[ns * nt];
			Hole[] who = new Hole[ns * nt];
			Arrays.fill(e, 1e9F);
			int s0 = ns, s1 = -1, t0 = nt, t1 = -1;
			float far = CHAR * 1.25F + 0.05F;
			// blocks of cells first, roughly: only the blocks a hole might reach are looked at cell by cell
			float half = BLOCK * CELL * 0.75F;
			for (int bi = 0; bi < ns; bi += BLOCK) {
				for (int bj = 0; bj < nt; bj += BLOCK) {
					int ei = Math.min(ns, bi + BLOCK), ej = Math.min(nt, bj + BLOCK);
					float s = (bi + ei) * 0.5F * ds, t = (bj + ej) * 0.5F * dt;
					float cx = ox + ex * s + fx * t, cy = oy + ey * s + fy * t, cz = oz + ez * s + fz * t;
					boolean maybe = false;
					for (Hole h : holes) {
						if (h.edge(cx, cy, cz, half, true) < far + half) {
							maybe = true;
							break;
						}
					}
					if (!maybe) {
						continue;
					}
					for (int i = bi; i < ei; i++) {
						for (int j = bj; j < ej; j++) {
							float si = (i + 0.5F) * ds, tj = (j + 0.5F) * dt;
							float x = ox + ex * si + fx * tj, y = oy + ey * si + fy * tj, z = oz + ez * si + fz * tj;
							float best = 1e9F;
							Hole by = null;
							for (Hole h : holes) {
								float d = h.edge(x, y, z);
								if (d < best) {
									best = d;
									by = h;
								}
							}
							e[i * nt + j] = best;
							who[i * nt + j] = by;
							if (best < far) {
								s0 = Math.min(s0, i); s1 = Math.max(s1, i); t0 = Math.min(t0, j); t1 = Math.max(t1, j);
							}
						}
					}
				}
			}
			byte[] st = null;
			if (flesh != null) {
				// the flesh just under each cell (a layer's box stands off the mob's own: the nearest of its flesh)
				float nx = poly.normal().x() * 0.3F, ny = poly.normal().y() * 0.3F, nz = poly.normal().z() * 0.3F;
				for (int i = 0; i < ns; i++) {
					for (int j = 0; j < nt; j++) {
						float si = (i + 0.5F) * ds, tj = (j + 0.5F) * dt;
						byte state = flesh.at(ox + ex * si + fx * tj - nx, oy + ey * si + fy * tj - ny, oz + ez * si + fz * tj - nz);
						if (state >= PlasmaWoundsDebris.Flesh.GONE) {
							if (st == null) {
								st = new byte[ns * nt];
							}
							st[i * nt + j] = state;
							s0 = Math.min(s0, i); s1 = Math.max(s1, i); t0 = Math.min(t0, j); t1 = Math.max(t1, j);
						}
					}
				}
			}
			return s1 < 0 ? null : new Grid(ns, nt, s0, s1, t0, t1, e, who, st);
		}
	}

	/**
	 * A piece the wound layer draws on or in a box (its part frame, px): a convex polygon, how hot each corner is (near a
	 * mouth), its normal, its hole and grain, and what it is: a piece of wall, a rim's glow, or the top or a side of one of
	 * the little raised blocks of a baton gash's lip.
	 */
	record Piece(float[][] pts, float[] hot, Vector3f n, Hole hole, float grain, int kind, float[] back) {
		static final int WALL = 0, RIM = 1, LIP = 2, LIP_SIDE = 3;

		/** A piece with the flesh it's on: a point just behind its middle (the part's frame, px). */
		static Piece on(float[][] pts, float[] hot, Vector3f n, Hole hole, float grain, int kind, float behind) {
			float[] back = new float[3];
			for (float[] q : pts) {
				back[0] += q[0] / pts.length;
				back[1] += q[1] / pts.length;
				back[2] += q[2] / pts.length;
			}
			back[0] -= n.x() * behind;
			back[1] -= n.y() * behind;
			back[2] -= n.z() * behind;
			return new Piece(pts, hot, n, hole, grain, kind, back);
		}
	}

	/**
	 * What's being drawn now (ModelFeatureRenderer): this mob's holes, the model's boxes' parts, and what it's drawn
	 * with (so that what has come off it can be drawn the same way: PlasmaWoundsDebris).
	 */
	private record Drawing(Holes holes, Map<ModelPart.Cube, String> paths, @Nullable Marks marks, boolean layer, @Nullable RenderType type,
			@Nullable UvMapping uv) {
	}

	/** A box being drawn whose part something is coming off: where what's gathered of it goes. */
	private record Gather(Holes holes, String path, Marks marks, RenderType type, @Nullable UvMapping uv, boolean take, int light) {
	}

	/** A box and what it was drawn with: gathered already. */
	private record Taken(ModelPart.Cube cube, Object with) {
		@Override
		public boolean equals(Object o) {
			return o instanceof Taken t && t.cube == cube && t.with.equals(with);
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(cube) * 31 + with.hashCode();
		}
	}

	private PlasmaWoundsClient() {
	}

	static Vector3f perpendicular(Vector3f d) {
		Vector3f up = Math.abs(d.y()) < 0.9F ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
		return new Vector3f(d).cross(up).normalize();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(PlasmaWounds.Payload.TYPE, (payload, context) -> add(payload));
		ClientTickEvents.END_CLIENT_TICK.register(PlasmaWoundsClient::tick);
		ClientTickEvents.END_CLIENT_TICK.register(PlasmaWoundsDebris::tick);
		PlasmaWoundsDebris.register();
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) ->
				helper.register(new WoundLayer((RenderLayerParent) renderer)));
	}

	private static void add(PlasmaWounds.Payload p) {
		ClientLevel l = Minecraft.getInstance().level;
		if (l == null) {
			return;
		}
		Marks marks = BY_ENTITY.computeIfAbsent(p.entity(), id -> new Marks());
		marks.wounds.addLast(new Wound(p, l.getGameTime()));
		while (marks.wounds.size() > MAX) {
			// the oldest goes; but not one that cut something off (its stump would lose its seared face), while others can
			Wound oldest = null;
			for (Wound w : marks.wounds) {
				if (!w.pinned) {
					oldest = w;
					break;
				}
			}
			if (oldest == null || marks.wounds.size() > MAX + 24) {
				marks.wounds.pollFirst();
			} else {
				marks.wounds.remove(oldest);
			}
		}
		marks.version++;
	}

	private static void tick(Minecraft client) {
		if (client.level != level) {
			BY_ENTITY.clear();
			level = client.level;
		}
		if (level == null || BY_ENTITY.isEmpty() || client.isPaused()) {
			return;
		}
		long now = level.getGameTime();
		for (Iterator<Map.Entry<Integer, Marks>> it = BY_ENTITY.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Integer, Marks> e = it.next();
			Entity entity = level.getEntity(e.getKey());
			if (entity == null || entity.isRemoved()) {
				it.remove();                                  // dead and gone, or out of sight: its wounds go with it
				continue;
			}
			// (a living mob's frame turns with its body; a creature's part, a Murmur's head, with its own turn)
			float bodyYaw = entity instanceof LivingEntity living ? living.yBodyRot : entity.getYRot();
			// a wisp of smoke off a fresh one
			for (Wound w : e.getValue().wounds) {
				if (now - w.born < 60 && (now + w.p.seed()) % 3 == 0) {
					float yaw = bodyYaw * Mth.DEG_TO_RAD;
					float c = Mth.cos(yaw), s = Mth.sin(yaw);
					// (a swing's fan: from out along its middle, where it met the mob, not from the swordsman's shoulder)
					float out = w.fan() ? Math.max(0, w.p.depth() - 0.15F) : 0;
					float hx = w.p.hx() + w.p.dx() * out, hy = w.p.hy() + w.p.dy() * out, hz = w.p.hz() + w.p.dz() * out;
					double x = entity.getX() + hx * c - hz * s, z = entity.getZ() + hx * s + hz * c;
					level.addParticle(ParticleTypes.SMOKE, x, entity.getY() + hy, z, 0, 0.02, 0);
				}
			}
		}
	}

	/** A mob's wounds (the entity's id), if it has any. */
	static @Nullable Marks marks(int id) {
		Marks m = BY_ENTITY.isEmpty() ? null : BY_ENTITY.get(id);
		return m == null || m.wounds.isEmpty() ? null : m;
	}

	/** The game time now (ticks, with the partial tick): what the wounds' cooling is timed by. */
	static float now() {
		ClientLevel l = Minecraft.getInstance().level;
		return l == null ? 0 : l.getGameTime() + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
	}

	/** At extraction: the mob's wounds onto its render state (none: nothing, so the model draws as ever). */
	public static void extract(LivingEntity entity, LivingEntityRenderState state) {
		FabricRenderState data = (FabricRenderState) (Object) state;
		Marks marks = BY_ENTITY.isEmpty() ? null : BY_ENTITY.get(entity.getId());
		data.setData(MARKS, marks == null || marks.wounds.isEmpty() ? null : marks);
		data.setData(HOLES, null);
	}

	/** Around a model being drawn (ModelFeatureRenderer): this mob's holes, and which part each of the model's boxes is in. */
	public static void begin(Object state, Model<?> model) {
		Object[] with = WITH.get();
		WITH.remove();
		begin(state, model, with == null ? null : (RenderType) with[0], with == null ? null : (UvMapping) with[1]);
	}

	/** What the model about to be drawn is drawn with (ModelFeatureRenderer.prepareModel's submission), for {@link #begin(Object, Model)}. */
	public static void with(@Nullable RenderType type, @Nullable UvMapping uv) {
		WITH.set(new Object[] {type, uv});
	}

	/** {@link #begin(Object, Model)}, with what the model is drawn with (its render type, and its texture's mapping if any). */
	public static void begin(Object state, Model<?> model, @Nullable RenderType type, @Nullable UvMapping uv) {
		Holes holes = state instanceof FabricRenderState data ? data.getData(HOLES) : null;
		Marks marks = holes == null ? null : ((FabricRenderState) state).getData(MARKS);
		DRAWING.set(holes == null ? null : new Drawing(holes, paths(model), marks, marks != null && marks.holesModel != model, type, uv));
	}

	/** Whether a mob (its render state) has come apart and isn't drawn any more. */
	public static boolean vanished(Object state) {
		Marks marks = state instanceof FabricRenderState data ? data.getData(MARKS) : null;
		return marks != null && marks.vanished;
	}

	/**
	 * Whether a mob (its render state) has lost some of the part at {@code path} (or has come apart altogether): what's
	 * drawn on that part some other way than from its boxes (the power armour's plates) goes with it.
	 */
	public static boolean lost(Object state, String path) {
		Marks marks = state instanceof FabricRenderState data ? data.getData(MARKS) : null;
		if (marks == null) {
			return false;
		}
		Holes holes = marks.holes;
		PlasmaWoundsDebris.Flesh f = holes == null ? null : holes.flesh.get(path);
		return marks.vanished || f != null && f.anyGone;
	}

	public static void end() {
		DRAWING.remove();
	}

	/** Each of a model's boxes and the path of the part it's in (worked out once a model). */
	static Map<ModelPart.Cube, String> paths(Model<?> model) {
		Map<ModelPart.Cube, String> map = PATHS.get(model);
		if (map == null) {
			Map<ModelPart.Cube, String> found = new IdentityHashMap<>();
			model.root().visit(new PoseStack(), (p, path, index, cube) -> found.put(cube, path));
			map = found;
			PATHS.put(model, map);
		}
		return map;
	}

	/** Draws {@code cube} cut, if a hole goes through it; returns whether it did (and the cube's own drawing is skipped). */
	public static boolean cut(ModelPart.Cube cube, PoseStack.Pose pose, VertexConsumer out, int light, int overlay, int color) {
		Drawing dr = DRAWING.get();
		if (dr == null) {
			return false;
		}
		Marks marks = dr.marks();
		if (marks != null && marks.vanished) {
			return true;                                      // it has come apart: nothing of it is drawn
		}
		String path = dr.paths().get(cube);
		if (path == null) {
			return false;
		}
		List<Hole> holes = dr.holes().byPath.get(path);
		PlasmaWoundsDebris.Flesh flesh = dr.holes().flesh.get(path);
		boolean off = flesh != null && flesh.anyGone;
		if ((holes == null || holes.isEmpty()) && !off) {
			return false;
		}
		Grid[] grids = dr.holes().grids(cube, holes == null ? List.of() : holes, path);
		if (grids == null) {
			return false;
		}
		if (dr.layer() && marks != null && holes != null && !holes.isEmpty()) {
			marks.layerCubes.put(cube, path);                   // a layer over the mob: its walls drawn with the rest next frame
		}
		// what's coming off this part: its faces as they're drawn now, taken once for what they're drawn with
		Gather gather = null;
		if (off && marks != null && dr.holes().bodies.containsKey(path)) {
			RenderType type = dr.type();
			boolean take = type != null && !type.isOutline()                  // (not from a glowing mob's outline pass)
					&& dr.holes().gathered.add(new Taken(cube, dr.uv() == null ? type : Arrays.asList(type, dr.uv())));
			gather = new Gather(dr.holes(), path, marks, type, dr.uv(), take, light);
		}
		Matrix4f m = pose.pose();
		Vector3f n = new Vector3f(), pos = new Vector3f();
		for (int i = 0; i < cube.polygons.length; i++) {
			ModelPart.Polygon poly = cube.polygons[i];
			pose.transformNormal(poly.normal(), n);
			face(poly, grids[i], m, n, pos, out, light, overlay, color, gather);
		}
		return true;
	}

	/** A box's drawn extent (its faces' corners, a grown layer's too), px: min x, y, z, max x, y, z. */
	static float[] bounds(ModelPart.Cube cube) {
		float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
		for (ModelPart.Polygon poly : cube.polygons) {
			for (ModelPart.Vertex v : poly.vertices()) {
				b[0] = Math.min(b[0], v.x()); b[1] = Math.min(b[1], v.y()); b[2] = Math.min(b[2], v.z());
				b[3] = Math.max(b[3], v.x()); b[4] = Math.max(b[4], v.y()); b[5] = Math.max(b[5], v.z());
			}
		}
		return b;
	}

	/** Where the line enters and leaves the box (grown by {@code grow}), along it (t, px); null if it misses. */
	static float @Nullable [] clip(Line l, float[] box, float grow) {
		float t0 = -1e9F, t1 = 1e9F;
		float[] o = {l.o().x(), l.o().y(), l.o().z()}, d = {l.d().x(), l.d().y(), l.d().z()};
		for (int i = 0; i < 3; i++) {
			float lo = box[i] - grow, hi = box[i + 3] + grow;
			if (Math.abs(d[i]) < 1e-6F) {
				if (o[i] < lo || o[i] > hi) {
					return null;
				}
				continue;
			}
			float a = (lo - o[i]) / d[i], b = (hi - o[i]) / d[i];
			t0 = Math.max(t0, Math.min(a, b));
			t1 = Math.min(t1, Math.max(a, b));
		}
		return t1 >= t0 ? new float[] {t0, t1} : null;
	}

	/**
	 * One face drawn round the holes: the parts of it no hole comes near whole, the rest in half-pixel cells (runs of
	 * alike ones drawn together), each with the face's texture laid on from its corners, those inside a hole left out and
	 * those round its edge seared.
	 */
	private static void face(ModelPart.Polygon poly, @Nullable Grid g, Matrix4f m, Vector3f n, Vector3f pos, VertexConsumer out, int light, int overlay,
			int color, @Nullable Gather gather) {
		ModelPart.Vertex[] v = poly.vertices();
		if (v.length != 4) {
			return;
		}
		Rect f = new Rect(v, m, n, pos, out, light, overlay);
		if (g == null) {
			f.rect(0, 0, 1, 1, color);
			return;
		}
		float ds = 1F / g.ns(), dt = 1F / g.nt();
		int s0 = g.s0(), s1 = g.s1(), t0 = g.t0(), t1 = g.t1();
		// whole strips round the touched window, then the window cell by cell
		f.rect(0, 0, s0 * ds, 1, color);
		f.rect((s1 + 1) * ds, 0, 1, 1, color);
		f.rect(s0 * ds, 0, (s1 + 1) * ds, t0 * dt, color);
		f.rect(s0 * ds, (t1 + 1) * dt, (s1 + 1) * ds, 1, color);
		for (int i = s0; i <= s1; i++) {
			int run = -1;
			int runColour = 0;
			PlasmaWoundsDebris.Body runBody = null;
			for (int j = t0; j <= t1 + 1; j++) {
				boolean hole = true;
				int c = 0;
				PlasmaWoundsDebris.Body body = null;
				if (j <= t1) {
					float e = g.e()[i * g.nt() + j];
					Hole h = g.who()[i * g.nt() + j];
					hole = e < 0;
					byte st = g.st() == null ? 0 : g.st()[i * g.nt() + j];
					if (!hole && st >= PlasmaWoundsDebris.Flesh.GONE) {
						// gone from the part; a piece just coming off is drawn in its place while its body is gathered
						body = gather == null ? null : gather.holes().gathering(gather.path(), st);
						hole = body == null;
					}
					c = h == null ? color : sear(color, e, h, i, j);
				}
				if (run >= 0 && (hole || c != runColour || body != runBody)) {
					f.rect(i * ds, run * dt, (i + 1) * ds, j * dt, runColour);
					if (runBody != null && gather.take() && !PlasmaWoundsDebris.broken) {
						try {
							f.take(i * ds, run * dt, (i + 1) * ds, j * dt, runColour, runBody, gather);
						} catch (RuntimeException e) {
							PlasmaWoundsDebris.fail("gathering what came off from the model's drawing", e);
						}
					}
					run = -1;
				}
				if (!hole && run < 0) {
					run = j;
					runColour = c;
					runBody = body;
				}
			}
		}
	}

	/** A cell's colour by how far it is from a hole's edge: the rim white-hot cooling to dull red then black, then char. */
	static int sear(int color, float e, Hole h, int i, int j) {
		return searBy(color, e, h.heat(), h.ember(), h.seed(), i, j);
	}

	/** {@link #sear} for a wound this hot ({@code e} px from its edge, its seed for the char's grain). */
	static int searBy(int color, float e, float heat, float ember, int seed, int i, int j) {
		int a = color >>> 24, cr = color >> 16 & 0xFF, cg = color >> 8 & 0xFF, cb = color & 0xFF;
		if (e < RIM) {
			// the rim's own colour (laid over the skin's texture), not tinted by the layer's
			float r = 1.0F * heat + 1.0F * (1 - heat) * ember + 0.13F * (1 - ember);
			float g = 0.86F * heat + 0.42F * (1 - heat) * ember + 0.07F * (1 - ember);
			float b = 0.58F * heat + 0.10F * (1 - heat) * ember + 0.05F * (1 - ember);
			return a << 24 | Mth.clamp((int) (r * 255), 0, 255) << 16 | Mth.clamp((int) (g * 255), 0, 255) << 8 | Mth.clamp((int) (b * 255), 0, 255);
		}
		float grain = hash(i, j, seed);
		float reach = CHAR * (0.75F + 0.5F * grain);
		if (e > reach) {
			return color;
		}
		float k = (e - RIM) / Math.max(0.01F, reach - RIM);       // 0 at the rim, 1 at the char's edge
		float dark = 0.12F + 0.25F * k * k;
		float embers = e < 0.8F ? ember * 0.35F : 0;
		float r = dark + embers, g = dark * 0.75F + embers * 0.25F, b = dark * 0.6F;
		float mix = Mth.clamp(k * k, 0, 1) * 0.7F;               // the char fades into the skin at its edge
		r = r * (1 - mix) + mix;
		g = g * (1 - mix) + mix;
		b = b * (1 - mix) + mix;
		return a << 24 | Mth.clamp((int) (cr * r), 0, 255) << 16 | Mth.clamp((int) (cg * g), 0, 255) << 8 | Mth.clamp((int) (cb * b), 0, 255);
	}

	/** A face as a rectangle (corner 0, along edge s to corner 1, edge t to corner 3; px), its texture laid on from its corners. */
	private record Rect(ModelPart.Vertex[] v, Matrix4f m, Vector3f n, Vector3f pos, VertexConsumer out, int light, int overlay) {
		/** The part of the face from (s0, t0) to (s1, t1) (0-1 along its edges), wound as the face is. */
		void rect(float s0, float t0, float s1, float t1, int c) {
			if (s1 - s0 < 1e-5F || t1 - t0 < 1e-5F) {
				return;
			}
			corner(s0, t0, c);
			corner(s1, t0, c);
			corner(s1, t1, c);
			corner(s0, t1, c);
		}

		/** The same part of the face, into a body that's coming off: placed in the world, from the body's middle. */
		void take(float s0, float t0, float s1, float t1, int c, PlasmaWoundsDebris.Body body, Gather g) {
			PlasmaWoundsDebris.Quad q = new PlasmaWoundsDebris.Quad();
			float[] st = {s0, t0, s1, t0, s1, t1, s0, t1};
			ModelPart.Vertex a = v[0], b = v[1], d = v[3];
			Marks marks = g.marks();
			float dx = (float) (marks.wx - body.ox) - marks.ax, dy = (float) (marks.wy - body.oy) - marks.ay, dz = (float) (marks.wz - body.oz) - marks.az;
			for (int k = 0; k < 4; k++) {
				float s = st[2 * k], t = st[2 * k + 1];
				m.transformPosition((a.x() + (b.x() - a.x()) * s + (d.x() - a.x()) * t) / 16F, (a.y() + (b.y() - a.y()) * s + (d.y() - a.y()) * t) / 16F,
						(a.z() + (b.z() - a.z()) * s + (d.z() - a.z()) * t) / 16F, pos);
				q.p[3 * k] = pos.x() + dx;
				q.p[3 * k + 1] = pos.y() + dy;
				q.p[3 * k + 2] = pos.z() + dz;
				q.uv[2 * k] = a.u() + (b.u() - a.u()) * s + (d.u() - a.u()) * t;
				q.uv[2 * k + 1] = a.v() + (b.v() - a.v()) * s + (d.v() - a.v()) * t;
				q.c[k] = c;
			}
			q.nx = n.x();
			q.ny = n.y();
			q.nz = n.z();
			body.light = g.light();
			body.add(body.batch(g.type(), g.uv()), q);
		}

		private void corner(float s, float t, int c) {
			ModelPart.Vertex a = v[0], b = v[1], d = v[3];
			float x = a.x() + (b.x() - a.x()) * s + (d.x() - a.x()) * t;
			float y = a.y() + (b.y() - a.y()) * s + (d.y() - a.y()) * t;
			float z = a.z() + (b.z() - a.z()) * s + (d.z() - a.z()) * t;
			float u = a.u() + (b.u() - a.u()) * s + (d.u() - a.u()) * t;
			float w = a.v() + (b.v() - a.v()) * s + (d.v() - a.v()) * t;
			m.transformPosition(x / 16F, y / 16F, z / 16F, pos);
			out.addVertex(pos.x(), pos.y(), pos.z(), c, u, w, overlay, light, n.x(), n.y(), n.z());
		}
	}

	/** One box as ModelPart.visit gives it: its part's path, the box, its frame (box px / 16 to the model's space), drawn or not. */
	record Box(String path, ModelPart.Cube cube, Matrix4f frame, boolean shown, boolean layer) {
		Box(String path, ModelPart.Cube cube, Matrix4f frame, boolean shown) {
			this(path, cube, frame, shown, false);
		}
	}

	/** A model's boxes as it stands now (after its setupAnim): each with its part's path, frame and whether it's drawn. */
	static List<Box> boxes(Model<?> model) {
		List<Box> boxes = new ArrayList<>();
		ModelPart root = model.root();
		Map<String, Boolean> shown = new HashMap<>();
		root.visit(new PoseStack(), (p, path, index, cube) -> boxes.add(new Box(path, cube, new Matrix4f(p.pose()),
				shown.computeIfAbsent(path, k -> shown(root, k)))));
		return boxes;
	}

	/** Whether the part at a path is drawn: it and all the parts it hangs from shown, its own boxes not skipped. */
	static boolean shown(ModelPart root, String path) {
		ModelPart part = root;
		if (!part.visible) {
			return false;
		}
		for (String name : path.split("/")) {
			if (name.isEmpty()) {
				continue;
			}
			if (!part.hasChild(name)) {
				return true;                                      // can't tell: draw its walls
			}
			part = part.getChild(name);
			if (!part.visible) {
				return false;
			}
		}
		return !part.skipDraw;
	}

	/**
	 * The wound layer: on every living mob's renderer. For a mob with wounds: follows new ones through the model, hands
	 * the holes to the model's drawing (on the render state), and draws the tunnels' walls and the rims' glow.
	 */
	static final class WoundLayer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
		WoundLayer(RenderLayerParent<S, M> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
			woundModel(state, getParentModel(), pose, collector, light);
		}
	}

	/**
	 * For a mob with wounds drawn with {@code model} at {@code pose} (the model's own space, as it is submitted): follows
	 * new wounds through the model, hands the holes to the model's drawing (on the render state), and draws the tunnels'
	 * walls and the rims' glow. The wound layer does this for every living mob; a creature's part drawn by a renderer of
	 * its own (a Murmur's head) has it done as its model is submitted (PlasmaWoundsMesh's parts).
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	static void woundModel(net.minecraft.client.renderer.entity.state.EntityRenderState state, EntityModel model, PoseStack pose,
			SubmitNodeCollector collector, int light) {
		{
			FabricRenderState data = (FabricRenderState) (Object) state;
			Marks marks = data.getData(MARKS);
			if (marks == null || state.isInvisible) {
				return;
			}
			model.setupAnim(state);
			List<Box> boxes = boxes(model);
			if (boxes.isEmpty()) {
				return;
			}
			Matrix4f mp = pose.last().pose();
			float scale = new Vector3f(mp.m00(), mp.m01(), mp.m02()).length();
			ClientLevel l = Minecraft.getInstance().level;
			float now = l == null ? 0 : l.getGameTime() + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
			// its thinnest side in the model's own pixels (the game's box: a silverfish, a fly)
			float thin = Math.min(state.boundingBoxWidth, state.boundingBoxHeight) * 16F / scale;
			// where it stands in this pass's space and in the world (what's drawn of it can be placed in the world from that)
			Vector3f feet = mp.transformPosition(new Vector3f(0, 1.501F, 0));
			marks.ax = feet.x();
			marks.ay = feet.y();
			marks.az = feet.z();
			marks.wx = state.x;
			marks.wy = state.y;
			marks.wz = state.z;
			Holes holes = holes(marks, model, boxes, scale, now, thin, 1.501F - state.eyeHeight / scale);
			data.setData(HOLES, holes);
			if (holes == null) {
				return;
			}
			if (holes.fresh) {
				try {
					spawn(holes, boxes, marks, mp);
				} catch (RuntimeException e) {
					holes.fresh = false;
					PlasmaWoundsDebris.fail("making bodies of what came off", e);
				}
			} else if (holes.fatal && !marks.vanished && !holes.gathering() && holes.shown()) {
				marks.vanished = true;                          // what's left of it is lying about, burning: the model's drawn no more
				return;
			}
			Geometry geo = build(holes, boxes, marks, mp);
			if (!geo.base.isEmpty()) {
				collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, out) -> geo.emit(geo.base, p, out, light));
			}
			if (!geo.glow.isEmpty()) {
				collector.order(2).submitCustomGeometry(pose, RenderTypes.eyes(WHITE), (p, out) -> geo.emit(geo.glow, p, out, 0xF000F0));
			}
		}
	}

	/**
	 * A mob's holes: worked out again only when it has a new wound or is drawn with another model (a baby grown up), its
	 * new wounds placed on the model as it stands; and each wound's heat now.
	 */
	static @Nullable Holes holes(Marks marks, Object model, List<Box> boxes, float scale, float now, float thin) {
		return holes(marks, model, boxes, scale, now, thin, Float.NaN);
	}

	/**
	 * {@link #holes(Marks, Object, List, float, float, float)}, knowing how high its eyes are (the model's space: y down
	 * from 1.501 at its feet; NaN: not known), to tell its head by. Once the holes are known, what they cut off the mob
	 * ({@link #sever}).
	 */
	static @Nullable Holes holes(Marks marks, Object model, List<Box> boxes, float scale, float now, float thin, float eye) {
		// (not while something that's just come off is still being gathered from the mob's drawing: a frame or two)
		if ((marks.holesVersion != marks.version || marks.holesModel != model) && !(marks.holes != null && marks.holes.gathering())) {
			boolean other = marks.holesModel != null && marks.holesModel != model;
			if (other) {
				marks.shed.clear();                               // another model (a baby grown up): its parts aren't these
			}
			Vital vital = Vital.of(boxes, eye);
			boolean fatal = false;
			for (Wound w : marks.wounds) {
				fatal |= w.p.fatal();
			}
			Map<String, List<Hole>> byPath = new HashMap<>();
			for (Wound w : marks.wounds) {
				if (w.model != model) {
					place(w, model, boxes, scale, thin, vital);
				}
				for (Map.Entry<String, Hole> e : w.holes.entrySet()) {
					byPath.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(e.getValue());
				}
			}
			Holes holes = new Holes(byPath);
			holes.fatal = fatal;
			if (!PlasmaWoundsDebris.broken) {
				try {
					sever(holes, marks, boxes, vital, other);
				} catch (RuntimeException e) {
					PlasmaWoundsDebris.fail("working out what a wound cut off", e);
				}
			}
			if (PlasmaWoundsDebris.broken) {
				holes.flesh.clear();                              // nothing comes off: the wounds as they always were
				holes.fresh = false;
				holes.fatal = false;
			}
			marks.holes = byPath.isEmpty() && holes.flesh.isEmpty() ? null : holes;
			marks.holesVersion = marks.version;
			marks.holesModel = model;
			marks.layerCubes.clear();
		}
		for (Wound w : marks.wounds) {
			w.age(Math.max(0, (now - w.born) / 20F));
		}
		return marks.holes;
	}

	/**
	 * The tunnels' walls and the rims' glow inside each drawn box with a hole, placed with the parts as they stand: the
	 * mob's own boxes, and the boxes of the layers drawn over them (with their parts' frames: a layer's parts stand as the
	 * mob's do).
	 */
	static Geometry build(Holes holes, List<Box> boxes, @Nullable Marks marks) {
		return build(holes, boxes, marks, null);
	}

	/**
	 * {@link #build(Holes, List, Marks)}, with the model's place in the pass being drawn ({@code mp}; null: not known):
	 * a wall on flesh that has come off isn't drawn with the mob; while that piece's body is being gathered it goes into
	 * it (its seared cut face), placed in the world.
	 */
	static Geometry build(Holes holes, List<Box> boxes, @Nullable Marks marks, @Nullable Matrix4f mp) {
		Geometry geo = new Geometry();
		List<Box> all = boxes;
		if (marks != null && !marks.layerCubes.isEmpty()) {
			Map<String, Box> byPath = new HashMap<>();
			for (Box b : boxes) {
				byPath.putIfAbsent(b.path(), b);
			}
			all = new ArrayList<>(boxes);
			synchronized (marks.layerCubes) {
				for (Map.Entry<ModelPart.Cube, String> e : marks.layerCubes.entrySet()) {
					Box own = byPath.get(e.getValue());
					if (own != null) {
						all.add(new Box(e.getValue(), e.getKey(), own.frame(), own.shown(), true));
					}
				}
			}
		}
		for (Box b : all) {
			List<Hole> here = b.shown() ? holes.byPath.get(b.path()) : null;
			if (here == null) {
				continue;
			}
			PlasmaWoundsDebris.Flesh flesh = holes.flesh.get(b.path());
			boolean take = flesh != null && flesh.anyGone && marks != null && mp != null && holes.bodies.containsKey(b.path())
					&& holes.gathered.add(new Taken(b.cube(), "walls"));
			for (Piece piece : holes.pieces(b.cube(), here, b.layer(), b.path())) {
				Hole h = piece.hole();
				int k = piece.pts().length;
				int[] colours = new int[k];
				if (flesh != null && flesh.anyGone) {
					byte st = flesh.at(piece.back()[0], piece.back()[1], piece.back()[2]);
					if (st >= PlasmaWoundsDebris.Flesh.GONE) {
						PlasmaWoundsDebris.Body body = holes.gathering(b.path(), st);
						if (body == null) {
							continue;                                 // on flesh that's gone
						}
						if (take && piece.kind() != Piece.RIM && !PlasmaWoundsDebris.broken) {
							try {
								sear(body, piece, b.frame(), mp, marks);
							} catch (RuntimeException e) {
								PlasmaWoundsDebris.fail("gathering a cut face", e);
							}
						}
					}
				}
				if (piece.kind() == Piece.RIM) {
					if (h.heat() < 0.02F && h.ember() < 0.05F) {
						continue;
					}
					Arrays.fill(colours, argb(0.85F * h.heat() + 0.7F * (1 - h.heat()) * h.ember(), 0.50F * h.heat() + 0.18F * (1 - h.heat()) * h.ember(),
							0.16F * h.heat()));
					geo.poly(geo.glow, b.frame(), piece.pts(), colours, piece.n());
					continue;
				}
				if (piece.kind() != Piece.WALL) {
					// the gash's lip: molten hot, cooling to char
					float heat = h.heat(), ember = h.ember(), side = piece.kind() == Piece.LIP_SIDE ? 1 : 0;
					float cr = 1.0F * heat + 0.95F * (1 - heat) * ember + 0.12F * (1 - ember), cg = 0.80F * heat + 0.36F * (1 - heat) * ember + 0.07F * (1 - ember),
							cb = 0.50F * heat + 0.08F * (1 - heat) * ember + 0.05F * (1 - ember);
					float gr = 0.9F * heat + 0.65F * (1 - heat) * ember, gg = 0.48F * heat + 0.15F * (1 - heat) * ember, gb = 0.12F * heat;
					Arrays.fill(colours, side > 0 ? argb(cr * 0.8F, cg * 0.75F, cb * 0.7F) : argb(cr * (0.9F + 0.1F * piece.grain()), cg, cb));
					geo.poly(geo.base, b.frame(), piece.pts(), colours, piece.n());
					if (heat > 0.01F || ember > 0.03F) {
						Arrays.fill(colours, side > 0 ? argb(gr * 0.6F, gg * 0.5F, gb * 0.4F) : argb(gr, gg, gb));
						geo.poly(geo.glow, b.frame(), piece.pts(), colours, piece.n());
					}
					continue;
				}
				for (int i = 0; i < k; i++) {
					colours[i] = wallColour(piece.hot()[i], h.heat(), h.ember(), piece.grain());
				}
				geo.poly(geo.base, b.frame(), piece.pts(), colours, piece.n());
				if (h.heat() > 0.01F || h.ember() > 0.03F) {
					for (int i = 0; i < k; i++) {
						colours[i] = glowColour(piece.hot()[i], h.heat(), h.ember());
					}
					geo.poly(geo.glow, b.frame(), piece.pts(), colours, piece.n());
				}
			}
		}
		return geo;
	}

	/**
	 * Follows a wound's shot through the model as it stands now: for each part whose boxes the tunnel passes through or
	 * close by, its hole, along the line in that part's frame. A bolt's goes right along the line through the whole model
	 * (before where it met the mob's bounds too: a warden's arms stand out past them); a baton's gash only its first few
	 * pixels, from where the line first meets the model.
	 */
	static void place(Wound w, Object model, List<Box> boxes, float scale, float thin, Vital vital) {
		w.model = model;
		w.holes = Map.of();
		if (scale < 1e-4F) {
			return;
		}
		if (w.fan()) {
			placeFan(w, boxes, scale, thin, vital);
			return;
		}
		// the server's frame (from the feet, the front +z, blocks) to the model's (LivingEntityRenderer: turned to face, y and
		// z flipped, 1.501 down, scaled)
		Vector3f at = new Vector3f(w.p.hx() / scale, 1.501F - w.p.hy() / scale, -w.p.hz() / scale);
		Vector3f way = new Vector3f(w.p.dx(), -w.p.dy(), -w.p.dz()).normalize();
		Vector3f rod = new Vector3f(w.p.ux(), -w.p.uy(), -w.p.uz());
		// on a little creature, smaller: never more across than a share of its thinnest (Justin, 8 November 2026: "the
		// baton looks wrong on small creatures")
		w.size = thin > 0 ? sizeFor(thin) : size(boxes);
		float half = w.gash() ? w.p.length() / 2 * w.size : 0;
		float grow = w.p.radius() * w.size * (1 + LOBE_MAX) + WALL;
		// for a gash: where the line first meets the model, its trench that deep from there
		Vector3f first = null;
		if (w.gash()) {
			float best = Float.MAX_VALUE;
			for (Box b : boxes) {
				if (b.shown()) {
					Line l = line(b, at, way, rod, 0);
					float[] t = clip(l, bounds(b.cube()), 0);
					if (t != null) {
						Vector3f in = b.frame().transformPosition(new Vector3f(l.d()).mul(t[0]).add(l.o()).div(16F));
						float along = new Vector3f(in).sub(at).dot(way);
						if (along < best) {
							best = along;
							first = in;
						}
					}
				}
			}
			if (first == null) {
				return;
			}
		}
		Map<String, Line> lines = new HashMap<>();
		for (Box b : boxes) {
			Line l = line(b, at, way, rod, half);
			float[] t = clip(l, bounds(b.cube()), grow + half);
			if (t == null) {
				continue;                                     // the tunnel misses this box
			}
			float ta = t[0], tb = t[1];
			if (first != null) {
				// the gash: from where it first met the model, in this part's own measure along the line
				float tf = new Matrix4f(b.frame()).invert().transformPosition(new Vector3f(first)).mul(16F).sub(l.o()).dot(l.d());
				if (tb < tf - 0.6F || ta > tf + w.p.depth() * w.size) {
					continue;                                 // not as deep as the gash goes
				}
				ta = tb = tf;
			}
			Line old = lines.get(b.path());
			lines.put(b.path(), old == null ? new Line(l.o(), l.d(), ta, tb, l.u(), l.half())
					: new Line(l.o(), l.d(), Math.min(old.t0(), ta), Math.max(old.t1(), tb), l.u(), l.half()));
		}
		Map<String, Hole> holes = new HashMap<>();
		lines.forEach((path, l) -> holes.put(path, Hole.of(l, w)));
		w.holes = holes;
	}

	/**
	 * A swing's fan on the model as it stands now (PlasmaWounds.Swing: the cut is where the edge went): each part any of
	 * the fan lies in gets it, in that part's own frame. However short the edge fell, a blow that landed burns in a bite
	 * deep from where the fan first meets the model (as the baton's gash always did); and on one that can't be killed
	 * outright (a player, a boss: the wound says so) only that deep into its trunk and head, unless it was the death of it.
	 */
	static void placeFan(Wound w, List<Box> boxes, float scale, float thin, Vital vital) {
		Vector3f at = new Vector3f(w.p.hx() / scale, 1.501F - w.p.hy() / scale, -w.p.hz() / scale);
		Vector3f way = new Vector3f(w.p.dx(), -w.p.dy(), -w.p.dz()).normalize();
		Vector3f rod = new Vector3f(w.p.ux(), -w.p.uy(), -w.p.uz());
		w.size = thin > 0 ? sizeFor(thin) : size(boxes);
		float inner = w.p.inner() * 16F / scale, outer = w.p.depth() * 16F / scale, sweep = w.p.length();
		float bite = PlasmaWounds.BITE * w.size, r = w.p.radius() * w.size;
		// where the fan first meets the model: along lines out from its pivot across the sweep; and each part by itself
		int rays = 25;
		float first = Float.MAX_VALUE;
		Map<String, Float> firstOf = new HashMap<>();
		Map<String, Line> lines = new HashMap<>();
		for (Box b : boxes) {
			if (!b.shown()) {
				continue;
			}
			Line l = line(b, at, way, rod, 1);
			if (l.half() <= 0) {
				continue;
			}
			lines.putIfAbsent(b.path(), l);
			float[] box = bounds(b.cube());
			float here = Float.MAX_VALUE;
			for (int k = 0; k < rays; k++) {
				float phi = sweep * (2F * k / (rays - 1) - 1), c = Mth.cos(phi), sn = Mth.sin(phi);
				Line ray = new Line(l.o(), new Vector3f(l.d()).mul(c).add(new Vector3f(l.u()).mul(sn)), 0, 0, l.u(), 0);
				float[] t = clip(ray, box, r);
				if (t != null && t[1] >= 0) {
					here = Math.min(here, Math.max(0, t[0]));
				}
			}
			if (here < Float.MAX_VALUE) {
				first = Math.min(first, here);
				firstOf.merge(b.path(), here, Math::min);
			}
		}
		if (first == Float.MAX_VALUE) {
			return;                                           // nothing of the model lies in the sweep
		}
		float reach = Math.max(outer, first + bite);
		boolean shallow = w.p.guarded() && !w.p.fatal();
		Map<String, Hole> holes = new HashMap<>();
		for (Map.Entry<String, Float> e : firstOf.entrySet()) {
			float to = shallow && vital.has(e.getKey()) ? Math.min(reach, e.getValue() + bite) : reach;
			if (e.getValue() <= to + r) {
				holes.put(e.getKey(), Hole.fan(lines.get(e.getKey()), w, Math.min(inner, Math.max(0, e.getValue() - LAYER)), to));
			}
		}
		w.holes = holes;
	}

	/**
	 * The parts a creature can't lose and live (the server judges a kill on its own measure, PlasmaWounds.Vitals; here the
	 * model only follows): its head (the part named so; failing that the part nearest its eyes), its trunk (the parts named
	 * a body, torso or chest; failing that the part of its biggest box that isn't its head), and whatever lies over those
	 * (a jacket, a hat: a part hanging from one, its middle inside it). Nothing comes off these unless the blow was the
	 * death of it.
	 */
	record Vital(Set<String> paths) {
		static Vital of(List<Box> boxes, float eye) {
			Set<String> paths = new HashSet<>();
			Map<String, float[]> extent = new HashMap<>();
			Map<String, Float> biggest = new HashMap<>();
			for (Box b : boxes) {
				if (!b.shown() || b.layer()) {
					continue;
				}
				float[] box = bounds(b.cube());
				biggest.merge(b.path(), (box[3] - box[0]) * (box[4] - box[1]) * (box[5] - box[2]), Math::max);
				float[] e = extent.computeIfAbsent(b.path(), k -> new float[] {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE,
						-Float.MAX_VALUE, -Float.MAX_VALUE});
				Vector3f p = new Vector3f();
				for (int k = 0; k < 8; k++) {
					b.frame().transformPosition(((k & 1) == 0 ? box[0] : box[3]) / 16F, ((k & 2) == 0 ? box[1] : box[4]) / 16F, ((k & 4) == 0 ? box[2] : box[5]) / 16F, p);
					e[0] = Math.min(e[0], p.x); e[1] = Math.min(e[1], p.y); e[2] = Math.min(e[2], p.z);
					e[3] = Math.max(e[3], p.x); e[4] = Math.max(e[4], p.y); e[5] = Math.max(e[5], p.z);
				}
			}
			// its head: the part named so (the one nearest the model's root); failing that the part nearest its eyes
			String head = named(extent.keySet(), "head");
			if (head == null && !Float.isNaN(eye)) {
				float near = Float.MAX_VALUE;
				for (Map.Entry<String, float[]> e : extent.entrySet()) {
					float[] x = e.getValue();
					float cx = (x[0] + x[3]) / 2, cy = (x[1] + x[4]) / 2 - eye, cz = (x[2] + x[5]) / 2;
					float d = cx * cx + cy * cy + cz * cz;
					if (d < near) {
						near = d;
						head = e.getKey();
					}
				}
			}
			// its trunk: the parts named a body, torso or chest (nearest the root: a spider's two); failing that the part of
			// its biggest box that isn't its head or on it
			List<String> trunk = new ArrayList<>();
			for (String word : new String[] {"body", "torso", "chest"}) {
				String first = named(extent.keySet(), word);
				if (first != null) {
					long depth = first.chars().filter(c -> c == '/').count();
					for (String path : extent.keySet()) {
						if (path.chars().filter(c -> c == '/').count() == depth && name(path).contains(word) && !path.equals(head)) {
							trunk.add(path);
						}
					}
					break;
				}
			}
			if (trunk.isEmpty()) {
				String big = null;
				float most = 0;
				for (Map.Entry<String, Float> e : biggest.entrySet()) {
					boolean ofHead = head != null && (e.getKey().equals(head) || e.getKey().startsWith(head + "/"));
					if (!ofHead && e.getValue() > most) {
						most = e.getValue();
						big = e.getKey();
					}
				}
				if (big != null) {
					trunk.add(big);
				}
			}
			paths.addAll(trunk);
			if (head != null) {
				paths.add(head);
			}
			// what lies over them (a jacket, a hat, a warden's ribs: a part hanging from one, its middle inside it)
			List<String> core = new ArrayList<>(paths);
			for (Map.Entry<String, float[]> e : extent.entrySet()) {
				for (String v : core) {
					float[] of = extent.get(v);
					if (of == null || !e.getKey().startsWith(v + "/")) {
						continue;
					}
					float[] x = e.getValue();
					float cx = (x[0] + x[3]) / 2, cy = (x[1] + x[4]) / 2, cz = (x[2] + x[5]) / 2, g = 0.5F / 16F;
					if (cx >= of[0] - g && cx <= of[3] + g && cy >= of[1] - g && cy <= of[4] + g && cz >= of[2] - g && cz <= of[5] + g) {
						paths.add(e.getKey());
					}
				}
			}
			return new Vital(paths);
		}

		private static String name(String path) {
			return path.substring(path.lastIndexOf('/') + 1).toLowerCase(java.util.Locale.ROOT);
		}

		/** The part whose name has {@code word} in it that's nearest the model's root (the shortest path of those); null if none. */
		private static @Nullable String named(Set<String> paths, String word) {
			String best = null;
			long bestDepth = Long.MAX_VALUE;
			for (String path : paths) {
				long depth = path.chars().filter(c -> c == '/').count();
				if (name(path).contains(word) && (depth < bestDepth || depth == bestDepth && path.length() < best.length())) {
					best = path;
					bestDepth = depth;
				}
			}
			return best;
		}

		boolean has(String path) {
			return paths.contains(path);
		}
	}

	/**
	 * What the holes cut off the mob (PlasmaWoundsDebris.Flesh, part by part, those nearer the model's root first): a
	 * part's flesh falls into pieces where holes go right through it; the piece at its pivot stays, the others come off
	 * (not off its trunk or head: {@code vital}); a part hanging from a piece that came off goes with it, whole. A mob a
	 * plasma blow killed comes apart altogether. What came off before stays off ({@code marks.shed}); what comes off now
	 * is marked for the wound layer to make bodies of ({@link #spawn}), unless {@code quiet} (the model's another: no show).
	 */
	static void sever(Holes holes, Marks marks, List<Box> boxes, Vital vital, boolean quiet) {
		Map<String, Box> parts = new HashMap<>();
		for (Box b : boxes) {
			if (b.shown() && !b.layer()) {
				parts.putIfAbsent(b.path(), b);
				holes.own.computeIfAbsent(b.path(), k -> new ArrayList<>()).add(bounds(b.cube()));
			}
		}
		if (!holes.fatal && holes.byPath.isEmpty() && marks.shed.isEmpty()) {
			return;
		}
		float size = 1;
		for (Wound w : marks.wounds) {
			size = Math.min(size, w.size);
		}
		List<String> paths = new ArrayList<>(parts.keySet());
		paths.sort((a, b) -> Long.compare(a.chars().filter(c -> c == '/').count(), b.chars().filter(c -> c == '/').count()));
		Set<String> off = new HashSet<>();
		for (String path : paths) {
			List<Hole> here = holes.byPath.get(path);
			// the part it hangs from (the nearest above it with boxes of its own): gone with a piece of that, if its pivot's in it
			String parent = path;
			do {
				int cut = parent.lastIndexOf('/');
				parent = cut <= 0 ? null : parent.substring(0, cut);
			} while (parent != null && !parts.containsKey(parent));
			boolean whole = false;
			if (parent != null) {
				PlasmaWoundsDebris.Flesh above = holes.flesh.get(parent);
				if (off.contains(parent)) {
					whole = true;
				} else if (above != null && above.anyGone) {
					Vector3f pivot = new Matrix4f(parts.get(parent).frame()).invert().transformPosition(parts.get(path).frame().transformPosition(new Vector3f()))
							.mul(16F);
					whole = above.at(pivot.x(), pivot.y(), pivot.z()) >= PlasmaWoundsDebris.Flesh.GONE;
				}
			}
			if (here == null && !holes.fatal && !whole && !marks.shed.containsKey(path)) {
				continue;
			}
			PlasmaWoundsDebris.Flesh flesh = PlasmaWoundsDebris.Flesh.of(holes.own.get(path), here, size);
			flesh.shed(marks.shed.get(path));
			if (whole) {
				off.add(path);
			}
			if (holes.fatal || whole) {
				flesh.sever(false);
			} else if (!vital.has(path)) {
				flesh.sever(true);
			}
			if (!flesh.anyGone) {
				continue;
			}
			if (quiet) {
				for (int[] cells : flesh.pieces) {
					for (int c : cells) {
						flesh.s[c] = PlasmaWoundsDebris.Flesh.GONE;
					}
				}
				flesh.pieces.clear();
			}
			flesh.settle();
			holes.flesh.put(path, flesh);
			holes.fresh |= !flesh.pieces.isEmpty();
			flesh.gone(marks.shed.computeIfAbsent(path, k -> new BitSet()));
		}
		// a wound through a part something came off keeps (its seared face on the stump)
		for (Wound w : marks.wounds) {
			for (String path : w.holes.keySet()) {
				w.pinned |= holes.flesh.containsKey(path);
			}
		}
	}

	/**
	 * Bodies for the pieces just come off (PlasmaWoundsDebris.Body): pieces that touch (in the model's space, as the mob
	 * stands) are one body; each starts where it is in the world ({@code mp}: the model's place in the pass being drawn,
	 * with the mob's feet there and in the world kept on {@code marks}). The model's drawing this frame fills them.
	 */
	static void spawn(Holes holes, List<Box> boxes, Marks marks, Matrix4f mp) {
		holes.fresh = false;
		Map<String, Box> parts = new HashMap<>();
		for (Box b : boxes) {
			if (b.shown() && !b.layer()) {
				parts.putIfAbsent(b.path(), b);
			}
		}
		// every piece's cells in the model's space, to the pixel: pieces sharing or next to each other's pixels touch
		List<String> pathOf = new ArrayList<>();
		List<Integer> pieceOf = new ArrayList<>();
		List<float[]> middle = new ArrayList<>();
		Map<Long, Integer> owner = new HashMap<>();
		List<long[]> pixels = new ArrayList<>();
		float[] c = new float[3];
		Vector3f v = new Vector3f();
		for (Map.Entry<String, PlasmaWoundsDebris.Flesh> e : holes.flesh.entrySet()) {
			Box box = parts.get(e.getKey());
			PlasmaWoundsDebris.Flesh f = e.getValue();
			if (box == null) {
				continue;
			}
			for (int k = 0; k < f.pieces.size(); k++) {
				int id = pathOf.size();
				pathOf.add(e.getKey());
				pieceOf.add(k);
				float[] sum = new float[4];
				Set<Long> mine = new HashSet<>();
				for (int cell : f.pieces.get(k)) {
					f.centre(cell, c);
					box.frame().transformPosition(c[0] / 16F, c[1] / 16F, c[2] / 16F, v);
					sum[0] += v.x;
					sum[1] += v.y;
					sum[2] += v.z;
					sum[3]++;
					mine.add(pixel(Math.round(v.x * 16F), Math.round(v.y * 16F), Math.round(v.z * 16F)));
				}
				middle.add(sum);
				long[] px = new long[mine.size()];
				int i = 0;
				for (long key : mine) {
					px[i++] = key;
					owner.putIfAbsent(key, id);
				}
				pixels.add(px);
			}
		}
		int n = pathOf.size();
		int[] group = new int[n];
		for (int i = 0; i < n; i++) {
			group[i] = i;
		}
		for (int i = 0; i < n; i++) {
			for (long key : pixels.get(i)) {
				for (int side = 0; side < 7; side++) {
					Integer other = owner.get(key + (side == 0 ? 0 : side == 1 ? 1L : side == 2 ? -1L : side == 3 ? 1L << 20 : side == 4 ? -(1L << 20)
							: side == 5 ? 1L << 40 : -(1L << 40)));
					if (other != null && other != i) {
						int a = find(group, i), b = find(group, other);
						if (a != b) {
							group[a] = b;
						}
					}
				}
			}
		}
		int seed = marks.wounds.isEmpty() ? 0 : marks.wounds.peekLast().p.seed();
		// the way the newest blow was going, in the world (level)
		Vector3f push = new Vector3f();
		if (!marks.wounds.isEmpty()) {
			PlasmaWounds.Payload last = marks.wounds.peekLast().p;
			mp.transformDirection(push.set(last.dx(), -last.dy(), -last.dz()));
			push.y = 0;
			if (push.lengthSquared() > 1e-6F) {
				push.normalize();
			}
		}
		Map<Integer, PlasmaWoundsDebris.Body> made = new HashMap<>();
		for (int i = 0; i < n; i++) {
			int g = find(group, i);
			PlasmaWoundsDebris.Body body = made.get(g);
			if (body == null) {
				// its middle: all its pieces' cells together
				float[] sum = new float[4];
				for (int j = 0; j < n; j++) {
					if (find(group, j) == g) {
						for (int k = 0; k < 4; k++) {
							sum[k] += middle.get(j)[k];
						}
					}
				}
				mp.transformPosition(sum[0] / sum[3], sum[1] / sum[3], sum[2] / sum[3], v);
				body = PlasmaWoundsDebris.body(marks.wx + v.x - marks.ax, marks.wy + v.y - marks.ay, marks.wz + v.z - marks.az, marks.wx, marks.wy, marks.wz,
						seed * 31 + g * 7919);
				body.pushX = push.x;
				body.pushZ = push.z;
				made.put(g, body);
			}
			PlasmaWoundsDebris.Flesh f = holes.flesh.get(pathOf.get(i));
			holes.bodies.computeIfAbsent(pathOf.get(i), k -> new PlasmaWoundsDebris.Body[f.pieces.size()])[pieceOf.get(i)] = body;
		}
		holes.made.addAll(made.values());
	}

	private static long pixel(int x, int y, int z) {
		return (x + 512L) + ((y + 512L) << 20) + ((z + 512L) << 40);
	}

	private static int find(int[] group, int i) {
		while (group[i] != i) {
			group[i] = group[group[i]];
			i = group[i];
		}
		return i;
	}

	/** A seared wall on a piece that's coming off, into its body: placed in the world, from the body's middle. */
	private static void sear(PlasmaWoundsDebris.Body body, Piece piece, Matrix4f frame, Matrix4f mp, Marks marks) {
		float[][] pts = piece.pts();
		int k = pts.length;
		if (k < 3) {
			return;
		}
		Vector3f n = piece.n();
		float[] a = pts[0], b = pts[1], c = pts[2];
		float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
		boolean flip = (uy * vz - uz * vy) * n.x() + (uz * vx - ux * vz) * n.y() + (ux * vy - uy * vx) * n.z() < 0;
		Vector3f wn = mp.transformDirection(frame.transformDirection(new Vector3f(n))).normalize();
		float dx = (float) (marks.wx - body.ox) - marks.ax, dy = (float) (marks.wy - body.oy) - marks.ay, dz = (float) (marks.wz - body.oz) - marks.az;
		float[][] w = new float[k][];
		float[] hot = new float[k];
		Vector3f p = new Vector3f();
		for (int i = 0; i < k; i++) {
			int from = flip ? k - 1 - i : i;
			mp.transformPosition(frame.transformPosition(pts[from][0] / 16F, pts[from][1] / 16F, pts[from][2] / 16F, p));
			w[i] = new float[] {p.x + dx, p.y + dy, p.z + dz};
			hot[i] = piece.kind() == Piece.WALL ? piece.hot()[from] : 0.9F;
		}
		for (int i = 1; i + 1 < k; i += 2) {
			int[] idx = {0, i, i + 1, Math.min(i + 2, k - 1)};
			PlasmaWoundsDebris.Quad q = new PlasmaWoundsDebris.Quad();
			q.hot = new float[4];
			for (int j = 0; j < 4; j++) {
				System.arraycopy(w[idx[j]], 0, q.p, 3 * j, 3);
				q.hot[j] = hot[idx[j]];
				q.uv[2 * j] = 0.5F;
				q.uv[2 * j + 1] = 0.5F;
			}
			q.nx = wn.x();
			q.ny = wn.y();
			q.nz = wn.z();
			q.grain = piece.grain();
			body.sear(q);
		}
	}

	/** The most of a creature's thickness a wound's hole may be across (its radius a share of the model's thinnest extent). */
	static final float MOST = 0.21F;

	/** A wound's size on a creature whose thinnest side is {@code thin} model px: 1, or less where that's small. */
	static float sizeFor(float thin) {
		return Mth.clamp(MOST * Math.max(1F, thin) / PlasmaWounds.BOLT_RADIUS, 0.15F, 1F);
	}

	/** A wound's size on this model: 1, or less where the model's thinnest extent (model px) is small (a silverfish, a fly). */
	static float size(List<Box> boxes) {
		float[] lo = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE}, hi = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
		Vector3f p = new Vector3f();
		for (Box b : boxes) {
			if (!b.shown()) {
				continue;
			}
			float[] box = bounds(b.cube());
			for (int k = 0; k < 8; k++) {
				b.frame().transformPosition(((k & 1) == 0 ? box[0] : box[3]) / 16F, ((k & 2) == 0 ? box[1] : box[4]) / 16F, ((k & 4) == 0 ? box[2] : box[5]) / 16F, p);
				lo[0] = Math.min(lo[0], p.x); lo[1] = Math.min(lo[1], p.y); lo[2] = Math.min(lo[2], p.z);
				hi[0] = Math.max(hi[0], p.x); hi[1] = Math.max(hi[1], p.y); hi[2] = Math.max(hi[2], p.z);
			}
		}
		if (lo[0] > hi[0]) {
			return 1;
		}
		float thin = Math.max(1F, 16F * Math.min(hi[0] - lo[0], Math.min(hi[1] - lo[1], hi[2] - lo[2])));
		return Mth.clamp(MOST * thin / PlasmaWounds.BOLT_RADIUS, 0.15F, 1F);
	}

	/** The wound's line in a box's part frame (px): through where it went in, along its way; a gash's rod across it. */
	static Line line(Box b, Vector3f at, Vector3f way, Vector3f rod, float half) {
		Matrix4f inv = new Matrix4f(b.frame()).invert();
		Vector3f o = inv.transformPosition(new Vector3f(at)).mul(16F);
		Vector3f d = inv.transformDirection(new Vector3f(way)).normalize();
		Vector3f u = rod.lengthSquared() > 1e-6F ? inv.transformDirection(new Vector3f(rod)) : new Vector3f();
		u.sub(new Vector3f(d).mul(u.dot(d)));                 // square to the line
		if (u.lengthSquared() < 1e-6F || half <= 0) {
			return new Line(o, d, 0, 0, perpendicular(d), 0);
		}
		return new Line(o, d, 0, 0, u.normalize(), half);
	}

	/**
	 * The tunnel's walls inside one box: a tube round the line following the hole's ragged edge (a gash: a trench, its
	 * walls, ends and floor), each piece clipped to the inside of the box (a touch in from its faces) so nothing of it
	 * ever shows outside the mob; hotter towards the mouths.
	 */
	static void walls(List<Piece> out, float[] box, Hole h, List<Hole> all, boolean layer, @Nullable List<float[]> own) {
		Line l = h.line();
		// the other holes through this box: where they cross this one, its walls are in their empty space, so left out
		// (Justin, 7 November 2026: shot after shot in one place on a tough mob, "the inside starts to get really messed up")
		List<Hole> others = new ArrayList<>();
		for (Hole o : all) {
			if (o != h) {
				others.add(o);
			}
		}
		float in = 0.03F;
		float[] inner = {box[0] + in, box[1] + in, box[2] + in, box[3] - in, box[4] - in, box[5] - in};
		if (inner[3] <= inner[0] || inner[4] <= inner[1] || inner[5] <= inner[2]) {
			return;                                           // a flat box: nothing inside it
		}
		if (h.fan()) {
			if (h.near(inner, h.r() * (1 + LOBE_MAX) + WALL + 0.5F)) {
				List<Piece> made = new ArrayList<>();
				fanWalls(made, inner, h, others);
				for (Piece piece : made) {
					// a layer over the mob (wool, clothes, armour): only what of the cut's walls lies outside the mob's own boxes
					if (!layer || own == null || !within(piece, own)) {
						out.add(piece);
					}
				}
			}
			return;
		}
		float reach = h.r() * (1 + LOBE_MAX) + WALL + l.half() + 0.5F;
		float[] t = clip(l, inner, reach);
		if (t == null) {
			return;
		}
		float ta = Math.max(t[0], h.from()), tb = Math.min(t[1], h.to());
		if (layer) {
			// a layer over the mob (wool, clothes, armour): only between its surface and the mob's own, where the line
			// goes in (and for a bolt where it comes out); the mob's own boxes draw the rest
			if (h.gash()) {
				trench(out, inner, h, others, ta, Math.min(tb, l.t0()), false);
			} else {
				tube(out, inner, h, others, ta, Math.min(tb, l.t0()));
				tube(out, inner, h, others, Math.max(ta, l.t1()), tb);
			}
			return;
		}
		if (h.gash()) {
			ta = Math.max(ta, l.t0() - 0.6F);
			trench(out, inner, h, others, ta, Math.min(tb, l.t0() + h.depth()), true);
			return;
		}
		tube(out, inner, h, others, ta, tb);
	}

	/** Whether a piece's middle lies inside one of these boxes (min x, y, z, max x, y, z). */
	private static boolean within(Piece piece, List<float[]> boxes) {
		float x = 0, y = 0, z = 0;
		for (float[] q : piece.pts()) {
			x += q[0] / piece.pts().length;
			y += q[1] / piece.pts().length;
			z += q[2] / piece.pts().length;
		}
		for (float[] b : boxes) {
			if (x > b[0] && x < b[3] && y > b[1] && y < b[4] && z > b[2] && z < b[5]) {
				return true;
			}
		}
		return false;
	}

	/**
	 * A swing's cut through one box: the fan's two faces (a wall each side of its plane, the width of the edge apart), its
	 * far end (the arc the point drew), its near end and the two ends of the sweep, in pieces a couple of pixels across,
	 * each clipped to the inside of the box.
	 */
	static void fanWalls(List<Piece> out, float[] inner, Hole h, List<Hole> others) {
		float rw = h.r() * (1 + LOBE_MAX) + WALL;
		float from = Math.max(0, h.inner() - rw), to = h.outer() + rw;
		float sweep = Math.min(Mth.PI, h.sweep() + rw / Math.max(1F, h.inner()));
		int na = Math.max(2, Mth.ceil(2 * sweep * to / 2.5F)), nr = Math.max(1, Mth.ceil((to - from) / 2.5F));
		Vector3f nrm = h.e2();
		for (int a = 0; a < na; a++) {
			float p0 = -sweep + 2 * sweep * a / na, p1 = -sweep + 2 * sweep * (a + 1) / na, pm = (p0 + p1) / 2;
			for (int k = 0; k < nr; k++) {
				float r0 = from + (to - from) * k / nr, r1 = from + (to - from) * (k + 1) / nr;
				for (int side = -1; side <= 1; side += 2) {
					strip(out, inner, h, others, new float[][] {fanPoint(h, r0, p0, side * rw), fanPoint(h, r1, p0, side * rw), fanPoint(h, r1, p1, side * rw),
							fanPoint(h, r0, p1, side * rw)}, new Vector3f(nrm).mul(-side));
				}
			}
			// the far end, and the near one
			Vector3f radial = new Vector3f(h.line().d()).mul(Mth.cos(pm)).add(new Vector3f(h.e1()).mul(Mth.sin(pm)));
			strip(out, inner, h, others, new float[][] {fanPoint(h, to, p0, -rw), fanPoint(h, to, p1, -rw), fanPoint(h, to, p1, rw), fanPoint(h, to, p0, rw)},
					new Vector3f(radial).negate());
			if (from > 0.01F) {
				strip(out, inner, h, others, new float[][] {fanPoint(h, from, p0, -rw), fanPoint(h, from, p1, -rw), fanPoint(h, from, p1, rw),
						fanPoint(h, from, p0, rw)}, radial);
			}
		}
		for (int side = -1; side <= 1; side += 2) {
			float phi = side * sweep;
			Vector3f inward = new Vector3f(h.line().d()).mul(Mth.sin(phi)).add(new Vector3f(h.e1()).mul(-Mth.cos(phi))).mul(side);
			for (int k = 0; k < nr; k++) {
				float r0 = from + (to - from) * k / nr, r1 = from + (to - from) * (k + 1) / nr;
				strip(out, inner, h, others, new float[][] {fanPoint(h, r0, phi, -rw), fanPoint(h, r1, phi, -rw), fanPoint(h, r1, phi, rw), fanPoint(h, r0, phi, rw)},
						inward);
			}
		}
	}

	/** The point of a fan {@code rho} px out, {@code phi} round from its middle, {@code off} px off its plane (the part's frame). */
	private static float[] fanPoint(Hole h, float rho, float phi, float off) {
		Line l = h.line();
		float c = Mth.cos(phi) * rho, sn = Mth.sin(phi) * rho;
		return new float[] {l.o().x() + l.d().x() * c + h.e1().x() * sn + h.e2().x() * off, l.o().y() + l.d().y() * c + h.e1().y() * sn + h.e2().y() * off,
				l.o().z() + l.d().z() * c + h.e1().z() * sn + h.e2().z() * off};
	}

	/**
	 * A gash's trench from ta to tb along the line: the rod's length by its depth, a wall each side, an end at each end and
	 * (with {@code floor}) a floor at tb; the hole's rounded ends lie inside it.
	 */
	static void trench(List<Piece> out, float[] inner, Hole h, List<Hole> others, float ta, float tb, boolean floor) {
		if (tb - ta < 0.05F) {
			return;
		}
		Line l = h.line();
		Vector3f u = l.u(), side = new Vector3f(l.d()).cross(u).normalize();
		float r = h.r() * (1 + LOBE_MAX) + WALL, ua = -l.half() - r, ub = l.half() + r;
		for (int sgn = -1; sgn <= 1; sgn += 2) {
			strip(out, inner, h, others, new float[][] {p(l, ta, u, ua, side, sgn * r), p(l, ta, u, ub, side, sgn * r),
					p(l, tb, u, ub, side, sgn * r), p(l, tb, u, ua, side, sgn * r)}, new Vector3f(side).mul(-sgn));
			float e = sgn < 0 ? ua : ub;
			strip(out, inner, h, others, new float[][] {p(l, ta, u, e, side, -r), p(l, ta, u, e, side, r), p(l, tb, u, e, side, r),
					p(l, tb, u, e, side, -r)}, new Vector3f(u).mul(-sgn));
		}
		if (floor) {
			strip(out, inner, h, others, new float[][] {p(l, tb, u, ua, side, -r), p(l, tb, u, ub, side, -r), p(l, tb, u, ub, side, r),
					p(l, tb, u, ua, side, r)}, new Vector3f(l.d()).negate());
		}
	}

	/**
	 * A tube from ta to tb along the line: twelve sides round the ragged edge, in short lengths near the mouths so their
	 * glow fades inward (with other holes crossing it, 20 sides in pixel lengths all along, so where it meets them the cut
	 * is close).
	 */
	static void tube(List<Piece> out, float[] inner, Hole h, List<Hole> others, float ta, float tb) {
		if (tb - ta < 0.05F) {
			return;
		}
		Line l = h.line();
		int sides = others.isEmpty() ? 12 : 20;
		Vector3f e1 = h.e1(), e2 = h.e2();
		float[] cuts = others.isEmpty() ? lengths(ta, tb) : even(ta, tb, 1.0F);
		for (int k = 0; k < sides; k++) {
			float a0 = Mth.TWO_PI * k / sides, a1 = Mth.TWO_PI * (k + 1) / sides, am = (a0 + a1) / 2;
			float r0 = h.ragged(a0) * h.r() + WALL, r1 = h.ragged(a1) * h.r() + WALL;
			Vector3f n = new Vector3f(e1).mul(-Mth.cos(am)).add(new Vector3f(e2).mul(-Mth.sin(am)));
			for (int c = 0; c + 1 < cuts.length; c++) {
				strip(out, inner, h, others, new float[][] {ring(l, cuts[c], e1, e2, a0, r0), ring(l, cuts[c], e1, e2, a1, r1),
						ring(l, cuts[c + 1], e1, e2, a1, r1), ring(l, cuts[c + 1], e1, e2, a0, r0)}, n);
			}
		}
	}

	/** Even lengths of at most {@code step} from ta to tb. */
	static float[] even(float ta, float tb, float step) {
		int n = Math.max(1, (int) Math.ceil((tb - ta) / step));
		float[] out = new float[n + 1];
		for (int i = 0; i <= n; i++) {
			out[i] = ta + (tb - ta) * i / n;
		}
		return out;
	}

	/** Where to break a tube along its length: short lengths for the first and last couple of pixels, one long one between. */
	static float[] lengths(float ta, float tb) {
		List<Float> at = new ArrayList<>();
		float step = 0.5F, edge = 2.0F;
		at.add(ta);
		if (tb - ta <= 2 * edge) {
			int n = Math.max(1, (int) Math.ceil((tb - ta) / step));
			for (int i = 1; i <= n; i++) {
				at.add(ta + (tb - ta) * i / n);
			}
		} else {
			for (float s = step; s <= edge + 1e-4F; s += step) {
				at.add(ta + s);
			}
			for (float s = edge; s >= -1e-4F; s -= step) {
				at.add(tb - s);
			}
		}
		float[] out = new float[at.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = at.get(i);
		}
		return out;
	}

	/**
	 * One piece of wall, clipped to the inside of the box, each corner marked by how near it is to the box's faces; left
	 * out where it lies in another hole's empty space (its middle inside that hole's edge).
	 */
	private static void strip(List<Piece> out, float[] inner, Hole h, List<Hole> others, float[][] quad, Vector3f n) {
		List<float[]> poly = clipToBox(quad, inner);
		if (poly.size() < 3) {
			return;
		}
		if (!others.isEmpty()) {
			float cx = 0, cy = 0, cz = 0;
			for (float[] q : poly) {
				cx += q[0];
				cy += q[1];
				cz += q[2];
			}
			cx /= poly.size();
			cy /= poly.size();
			cz /= poly.size();
			for (Hole o : others) {
				if (o.edge(cx, cy, cz) < -0.02F) {
					return;
				}
			}
		}
		int k = poly.size();
		float[] hot = new float[k];
		for (int i = 0; i < k; i++) {
			float[] p = poly.get(i);
			float depth = Math.min(Math.min(Math.min(p[0] - inner[0], inner[3] - p[0]), Math.min(p[1] - inner[1], inner[4] - p[1])),
					Math.min(p[2] - inner[2], inner[5] - p[2]));
			float hh = Mth.clamp(1 - depth / 1.6F, 0, 1);
			hot[i] = hh * hh;
		}
		float grain = 0.85F + 0.15F * hash(Math.round(quad[0][0] * 8), Math.round(quad[0][2] * 8 + quad[0][1] * 3), h.seed());
		out.add(Piece.on(poly.toArray(new float[0][]), hot, n, h, grain, Piece.WALL, 0.75F));
	}

	/** A convex polygon (px) cut down to the inside of a box (min x, y, z, max x, y, z). */
	static List<float[]> clipToBox(float[][] quad, float[] box) {
		List<float[]> poly = new ArrayList<>(List.of(quad));
		for (int axis = 0; axis < 3 && poly.size() >= 3; axis++) {
			poly = clipPlane(poly, axis, box[axis], 1);
			if (poly.size() >= 3) {
				poly = clipPlane(poly, axis, box[axis + 3], -1);
			}
		}
		return poly;
	}

	/** Keeps the part of a polygon on the inner side of one face of a box ({@code sign} 1: at least {@code at}; -1: at most). */
	private static List<float[]> clipPlane(List<float[]> in, int axis, float at, int sign) {
		List<float[]> out = new ArrayList<>(in.size() + 2);
		for (int i = 0; i < in.size(); i++) {
			float[] a = in.get(i), b = in.get((i + 1) % in.size());
			float da = (a[axis] - at) * sign, db = (b[axis] - at) * sign;
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

	private static float[] ring(Line l, float t, Vector3f e1, Vector3f e2, float a, float r) {
		float c = Mth.cos(a) * r, s = Mth.sin(a) * r;
		return new float[] {l.o().x() + l.d().x() * t + e1.x() * c + e2.x() * s, l.o().y() + l.d().y() * t + e1.y() * c + e2.y() * s,
				l.o().z() + l.d().z() * t + e1.z() * c + e2.z() * s};
	}

	private static float[] p(Line l, float t, Vector3f u, float a, Vector3f side, float s) {
		return new float[] {l.o().x() + l.d().x() * t + u.x() * a + side.x() * s, l.o().y() + l.d().y() * t + u.y() * a + side.y() * s,
				l.o().z() + l.d().z() * t + u.z() * a + side.z() * s};
	}

	/** The tunnel's wall: seared dark red, orange and hot at the mouths while it's fresh; cooling to char. */
	static int wallColour(float hot, float heat, float ember, float grain) {
		float r = (0.42F + 0.55F * hot * heat) * ember + 0.09F * (1 - ember);
		float g = (0.08F + 0.42F * hot * heat) * ember + 0.06F * (1 - ember);
		float b = (0.03F + 0.12F * hot * heat) * ember + 0.05F * (1 - ember);
		return argb(r * grain, g * grain, b * grain);
	}

	static int glowColour(float hot, float heat, float ember) {
		float g = 0.04F + 0.6F * hot;
		return argb(0.9F * g * heat + 0.45F * (1 - heat) * ember * g, 0.38F * g * heat + 0.07F * (1 - heat) * ember * g, 0.07F * g * heat);
	}

	/**
	 * Round each mouth of the tunnel in a box: the rim's glow, a face's half-pixel cells just outside the hole's edge, a
	 * little off the face, for the glow layer (the face's own cells are seared by {@link #cut}); and round a baton's gash,
	 * its lip instead: the flesh pushed up and blistered along its edge as the rod burned in, a ridge of little raised
	 * blocks (each a cell across, standing 0.3-0.6 px proud) on the cells next to the trench.
	 */
	static void rims(List<Piece> out, ModelPart.Cube cube, Grid[] grids) {
		ModelPart.Polygon[] polys = cube.polygons;
		for (int f = 0; f < polys.length && f < grids.length; f++) {
			Grid g = grids[f];
			if (g == null) {
				continue;
			}
			ModelPart.Vertex[] v = polys[f].vertices();
			float ox = v[0].x(), oy = v[0].y(), oz = v[0].z();
			float ex = v[1].x() - ox, ey = v[1].y() - oy, ez = v[1].z() - oz, fx = v[3].x() - ox, fy = v[3].y() - oy, fz = v[3].z() - oz;
			Vector3f nn = new Vector3f(polys[f].normal());
			for (int i = g.s0(); i <= g.s1(); i++) {
				for (int j = g.t0(); j <= g.t1(); j++) {
					float e = g.e()[i * g.nt() + j];
					Hole h = g.who()[i * g.nt() + j];
					if (e < 0 || h == null) {
						continue;
					}
					float s0 = (float) i / g.ns(), s1 = (float) (i + 1) / g.ns(), t0 = (float) j / g.nt(), t1 = (float) (j + 1) / g.nt();
					if (h.gash()) {
						if (!(hole(g, i - 1, j) || hole(g, i + 1, j) || hole(g, i, j - 1) || hole(g, i, j + 1))) {
							continue;
						}
						float grain = hash(i, j, h.seed() + 7), top = (0.3F + 0.3F * grain) * Math.max(0.4F, h.wound().size), base = OFF * 0.5F;
						// a block at the face's edge kept a little in from it, so its side never lies in the next face's plane
						float ls = Mth.sqrt(ex * ex + ey * ey + ez * ez), lt = Mth.sqrt(fx * fx + fy * fy + fz * fz), in = 0.1F;
						float a0 = i == 0 ? s0 + in / ls : s0, a1 = i == g.ns() - 1 ? s1 - in / ls : s1;
						float b0 = j == 0 ? t0 + in / lt : t0, b1 = j == g.nt() - 1 ? t1 - in / lt : t1;
						if (a1 <= a0 || b1 <= b0) {
							continue;
						}
						float[][] cell = {at(ox, oy, oz, ex, ey, ez, fx, fy, fz, a0, b0), at(ox, oy, oz, ex, ey, ez, fx, fy, fz, a1, b0),
								at(ox, oy, oz, ex, ey, ez, fx, fy, fz, a1, b1), at(ox, oy, oz, ex, ey, ez, fx, fy, fz, a0, b1)};
						Piece lip = Piece.on(lift(cell, nn, top), new float[4], nn, h, grain, Piece.LIP, top + 0.3F);
						out.add(lip);
						for (int k = 0; k < 4; k++) {
							float[] a = cell[k], b = cell[(k + 1) % 4];
							float[] mid = {(a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2};
							float[] centre = {(cell[0][0] + cell[2][0]) / 2, (cell[0][1] + cell[2][1]) / 2, (cell[0][2] + cell[2][2]) / 2};
							Vector3f sn = new Vector3f(mid[0] - centre[0], mid[1] - centre[1], mid[2] - centre[2]).normalize();
							float[][] wall = {lift1(a, nn, base), lift1(b, nn, base), lift1(b, nn, top), lift1(a, nn, top)};
							out.add(new Piece(wall, new float[4], sn, h, grain, Piece.LIP_SIDE, lip.back()));
						}
						continue;
					}
					if (e >= RIM) {
						continue;
					}
					float[][] c = {at(ox, oy, oz, ex, ey, ez, fx, fy, fz, s0, t0), at(ox, oy, oz, ex, ey, ez, fx, fy, fz, s1, t0),
							at(ox, oy, oz, ex, ey, ez, fx, fy, fz, s1, t1), at(ox, oy, oz, ex, ey, ez, fx, fy, fz, s0, t1)};
					out.add(Piece.on(lift(c, nn, OFF), new float[4], nn, h, 1, Piece.RIM, OFF + 0.3F));
				}
			}
		}
	}

	private static boolean hole(Grid g, int i, int j) {
		return i >= 0 && j >= 0 && i < g.ns() && j < g.nt() && g.e()[i * g.nt() + j] < 0;
	}

	private static float[] at(float ox, float oy, float oz, float ex, float ey, float ez, float fx, float fy, float fz, float s, float t) {
		return new float[] {ox + ex * s + fx * t, oy + ey * s + fy * t, oz + ez * s + fz * t};
	}

	private static float[][] lift(float[][] c, Vector3f n, float by) {
		float[][] out = new float[c.length][];
		for (int k = 0; k < c.length; k++) {
			out[k] = lift1(c[k], n, by);
		}
		return out;
	}

	private static float[] lift1(float[] p, Vector3f n, float by) {
		return new float[] {p[0] + n.x() * by, p[1] + n.y() * by, p[2] + n.z() * by};
	}

	/** The vertices the layer draws (model space), with their colours and normals, for the base pass and the glow. */
	static final class Geometry {
		/** Each quad: four corners (x, y, z, colour bits), then its normal. */
		final List<float[]> base = new ArrayList<>(), glow = new ArrayList<>();

		/** A convex polygon (box frame, px) as quads in the model's space, wound to face along {@code n} (box frame). */
		void poly(List<float[]> to, Matrix4f frame, float[][] pts, int[] colours, Vector3f n) {
			int k = pts.length;
			if (k < 3) {
				return;
			}
			// wound as the model's own faces are (counter-clockwise seen from the side the normal points to)
			float[] a = pts[0], b = pts[1], c = pts[2];
			float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
			boolean flip = (uy * vz - uz * vy) * n.x() + (uz * vx - ux * vz) * n.y() + (ux * vy - uy * vx) * n.z() < 0;
			Vector3f wn = frame.transformDirection(new Vector3f(n)).normalize();
			Vector3f[] w = new Vector3f[k];
			int[] col = new int[k];
			for (int i = 0; i < k; i++) {
				int from = flip ? k - 1 - i : i;
				float[] p = pts[from];
				w[i] = frame.transformPosition(new Vector3f(p[0] / 16F, p[1] / 16F, p[2] / 16F));
				col[i] = colours[from];
			}
			// a fan of quads (the last one's fourth corner doubled when the count is odd)
			for (int i = 1; i + 1 < k; i += 2) {
				int[] idx = {0, i, i + 1, Math.min(i + 2, k - 1)};
				float[] q = new float[4 * 4 + 3];
				for (int j = 0; j < 4; j++) {
					q[4 * j] = w[idx[j]].x();
					q[4 * j + 1] = w[idx[j]].y();
					q[4 * j + 2] = w[idx[j]].z();
					q[4 * j + 3] = Float.intBitsToFloat(col[idx[j]]);
				}
				q[16] = wn.x();
				q[17] = wn.y();
				q[18] = wn.z();
				to.add(q);
			}
		}

		void emit(List<float[]> quads, PoseStack.Pose pose, VertexConsumer out, int light) {
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

	static int argbOf(float r, float g, float b) {
		return argb(r, g, b);
	}

	private static int argb(float r, float g, float b) {
		return 0xFF000000 | (Mth.clamp((int) (r * 255), 0, 255) << 16) | (Mth.clamp((int) (g * 255), 0, 255) << 8) | Mth.clamp((int) (b * 255), 0, 255);
	}

	static float hash(int a, int b, int seed) {
		int h = a * 73856093 ^ b * 19349663 ^ seed * 83492791;
		h ^= h >>> 13;
		h *= 0x5bd1e995;
		h ^= h >>> 15;
		return (h & 0xFFFF) / 65535F;
	}
}
