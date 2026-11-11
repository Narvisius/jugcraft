package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * What plasma cuts off a creature (the power armour sheets, takes 6 and 7; Justin, 9 November 2026: "if it results in
 * something like let's say for example an arm being cut through completely, then either have the arm disappear or maybe
 * even fall to the ground on fire and burns out destroyed"; "Make sure to make this compatable with any creature modded
 * or vanilla").
 *
 * <p>{@link Flesh}: one part of a model in small cells, those the wounds' holes burn out, and the pieces the rest falls
 * into: the piece at the part's own pivot (where it hangs from the creature) stays, any other has come off. It works on a
 * model's boxes, never on names, so on any creature drawn from model parts.
 *
 * <p>{@link Body}: something that has come off, in the world on its own: the very faces the model drew for it (taken as
 * they are drawn, with each one's own texture and render type: the skin, clothes, armour, glowing spots) and its seared
 * cut faces. It falls, lies burning for about three seconds (flames, then smoke; charring black), and is gone by five.
 * A creature a plasma blow kills comes apart the same way, whole: every piece of it, those that touch together.
 */
final class PlasmaWoundsDebris {
	/** Seconds from coming off: charring from and by, flames and smoke until, shrinking to nothing from, gone at. */
	static final float BURN_FROM = 0.6F, BURNT_BY = 3.2F, FLAMES_UNTIL = 3.0F, SMOKE_UNTIL = 4.2F, FADE_FROM = 4.5F, GONE_AT = 5.0F;
	/** The most of them at once (the oldest goes first). */
	static final int MAX = 48;
	/** A part's cells at most (a huge box gets coarser ones). */
	static final int MAX_CELLS = 9000;

	private static final List<Body> BODIES = new ArrayList<>();
	private static int frame;
	private static @Nullable ClientLevel level;
	/**
	 * Something went wrong in here (said once in the log, with what): nothing more is cut off or drawn of it this
	 * session; the wounds themselves go on as they were. This runs in the middle of drawing every creature, where a
	 * fault would otherwise take the game down.
	 */
	static volatile boolean broken;

	/** Notes a fault in the cutting-off (once) and turns it off. */
	static void fail(String doing, Throwable t) {
		if (!broken) {
			broken = true;
			io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.error("Plasma wounds: cutting parts off is turned off for this session (a fault while {})", doing, t);
		}
		synchronized (BODIES) {
			BODIES.clear();
		}
	}

	private PlasmaWoundsDebris() {
	}

	static void register() {
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> draw(context.levelState().cameraRenderState.pos, context.poseStack(), context.submitNodeCollector()));
	}

	// ------------------------------------------------------------------ a part's flesh

	/** One part's boxes in cells (the part's frame, px): what's flesh, what's burned out, what has come off. */
	static final class Flesh {
		/** A cell: outside every box; flesh; in a hole; gone (a crumb, or come off before); PIECE + k: of piece k, coming off now. */
		static final byte AIR = 0, FLESH = 1, BURNED = 2, GONE = 3, PIECE = 4;
		final float x0, y0, z0, cell;
		final int nx, ny, nz;
		final byte[] s;
		/** For a burned cell: what has become of the flesh right by it (the skin over it, a wall on it, goes the same way). */
		private byte @Nullable [] by;
		/** The pieces coming off now (their cells). */
		final List<int[]> pieces = new ArrayList<>();
		boolean anyGone;

		private Flesh(float[] b, float cell) {
			this.cell = cell;
			x0 = b[0];
			y0 = b[1];
			z0 = b[2];
			nx = Math.max(1, Mth.ceil((b[3] - b[0]) / cell - 1e-3F));
			ny = Math.max(1, Mth.ceil((b[4] - b[1]) / cell - 1e-3F));
			nz = Math.max(1, Mth.ceil((b[5] - b[2]) / cell - 1e-3F));
			s = new byte[nx * ny * nz];
		}

		/** A part's flesh: its boxes' extents (min x, y, z, max x, y, z each), the holes through it, the wounds' size on it. */
		static Flesh of(List<float[]> cubes, @Nullable List<PlasmaWoundsClient.Hole> holes, float size) {
			float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
			for (float[] c : cubes) {
				for (int i = 0; i < 3; i++) {
					b[i] = Math.min(b[i], c[i]);
					b[i + 3] = Math.max(b[i + 3], c[i + 3]);
				}
			}
			float cell = PlasmaWoundsClient.CELL * Mth.clamp(size, 0.4F, 1F);
			double volume = Math.max(1e-6, (b[3] - b[0]) * (double) (b[4] - b[1]) * (b[5] - b[2]));
			if (volume / (cell * cell * cell) > MAX_CELLS) {
				cell = (float) Math.cbrt(volume / MAX_CELLS) * 1.02F;
			}
			Flesh f = new Flesh(b, cell);
			float[] p = new float[3];
			// (a cell any of a box lies in is flesh: the cells' edges needn't fall on the boxes' faces)
			float g = cell * 0.5F - 1e-3F;
			for (int c = 0; c < f.s.length; c++) {
				f.centre(c, p);
				for (float[] q : cubes) {
					if (p[0] >= q[0] - g && p[0] <= q[3] + g && p[1] >= q[1] - g && p[1] <= q[4] + g && p[2] >= q[2] - g && p[2] <= q[5] + g) {
						f.s[c] = FLESH;
						break;
					}
				}
			}
			if (holes != null) {
				for (PlasmaWoundsClient.Hole h : holes) {
					for (int c = 0; c < f.s.length; c++) {
						if (f.s[c] != FLESH) {
							continue;
						}
						f.centre(c, p);
						if (h.edge(p[0], p[1], p[2], cell, true) < cell && h.edge(p[0], p[1], p[2]) < 0.2F * cell) {
							f.s[c] = BURNED;
						}
					}
				}
			}
			return f;
		}

		int index(int i, int j, int k) {
			return (i * ny + j) * nz + k;
		}

		void centre(int c, float[] out) {
			out[0] = x0 + (c / (ny * nz) + 0.5F) * cell;
			out[1] = y0 + (c / nz % ny + 0.5F) * cell;
			out[2] = z0 + (c % nz + 0.5F) * cell;
		}

		/** The cell a point (the part's frame, px) is in, or the nearest to it. */
		int cellAt(float x, float y, float z) {
			return index(Mth.clamp((int) Math.floor((x - x0) / cell), 0, nx - 1), Mth.clamp((int) Math.floor((y - y0) / cell), 0, ny - 1),
					Mth.clamp((int) Math.floor((z - z0) / cell), 0, nz - 1));
		}

		/** What has become of the flesh at a point (the nearest cell's): a burned cell answers for the flesh right by it. */
		byte at(float x, float y, float z) {
			int c = cellAt(x, y, z);
			return s[c] == BURNED && by != null ? by[c] : s[c];
		}

		/**
		 * Once it's known what has come off: each burned cell takes after the flesh next to it (what stays, if any of that
		 * is; else what's gone), and those next to none, after the burned cells that did (twice over).
		 */
		void settle() {
			if (!anyGone) {
				return;
			}
			by = new byte[s.length];
			for (int pass = 0; pass < 3; pass++) {
				byte[] next = pass == 0 ? by : by.clone();
				for (int c = 0; c < s.length; c++) {
					if (s[c] != BURNED || by[c] != 0) {
						continue;
					}
					int i = c / (ny * nz), j = c / nz % ny, k = c % nz;
					byte found = 0;
					for (int side = 0; side < 6 && found != FLESH; side++) {
						int a = i + (side == 0 ? 1 : side == 1 ? -1 : 0), b = j + (side == 2 ? 1 : side == 3 ? -1 : 0), e = k + (side == 4 ? 1 : side == 5 ? -1 : 0);
						if (a < 0 || b < 0 || e < 0 || a >= nx || b >= ny || e >= nz) {
							continue;
						}
						int o = index(a, b, e);
						byte v = pass == 0 ? (s[o] == FLESH || s[o] >= GONE ? s[o] : 0) : by[o];
						if (v == FLESH || v >= GONE && found == 0) {
							found = v;
						}
					}
					next[c] = found;
				}
				by = next;
			}
			for (int c = 0; c < s.length; c++) {
				if (s[c] == BURNED && by[c] == 0) {
					by[c] = BURNED;
				}
			}
		}

		/** The flesh nearest a point that isn't one of {@code not}; -1 if there's none. */
		private int nearest(float x, float y, float z, boolean[] not) {
			int best = -1;
			float bestD = Float.MAX_VALUE;
			float[] p = new float[3];
			for (int c = 0; c < s.length; c++) {
				if (s[c] == FLESH && !not[c]) {
					centre(c, p);
					float d = (p[0] - x) * (p[0] - x) + (p[1] - y) * (p[1] - y) + (p[2] - z) * (p[2] - z);
					if (d < bestD) {
						bestD = d;
						best = c;
					}
				}
			}
			return best;
		}

		/** What came off it before stays off. */
		void shed(@Nullable BitSet was) {
			if (was == null) {
				return;
			}
			for (int c = was.nextSetBit(0); c >= 0 && c < s.length; c = was.nextSetBit(c + 1)) {
				if (s[c] == FLESH) {
					s[c] = GONE;
					anyGone = true;
				}
			}
		}

		/**
		 * The pieces the flesh has fallen into: the one at the part's pivot stays (unless nothing of the part does:
		 * {@code keep} false), the others come off ({@link #pieces}); a crumb just goes. Flesh right by a burn doesn't
		 * hold a piece on (a seared sliver left beside a hole: as good as cut through); it goes with what it's next to.
		 */
		void sever(boolean keep) {
			int[] stack = new int[s.length];
			boolean[] seen = new boolean[s.length];
			int flesh = 0;
			for (int c = 0; c < s.length; c++) {
				if (s[c] != FLESH) {
					continue;
				}
				flesh++;
				// seared: a burned cell beside it (corners too)
				int i = c / (ny * nz), j = c / nz % ny, k = c % nz;
				search:
				for (int a = Math.max(0, i - 1); a <= Math.min(nx - 1, i + 1); a++) {
					for (int b = Math.max(0, j - 1); b <= Math.min(ny - 1, j + 1); b++) {
						for (int e = Math.max(0, k - 1); e <= Math.min(nz - 1, k + 1); e++) {
							if (s[index(a, b, e)] == BURNED) {
								seen[c] = true;
								break search;
							}
						}
					}
				}
			}
			boolean[] seared = seen.clone();
			if (keep) {
				int seed = nearest(0, 0, 0, seared);
				if (seed >= 0) {
					fill(seed, stack, seen);
				}
			}
			int crumb = Math.max(6, Math.round(0.015F * flesh));
			for (int c = 0; c < s.length; c++) {
				if (s[c] != FLESH || seen[c]) {
					continue;
				}
				int[] cells = fill(c, stack, seen);
				byte mark = cells.length < crumb || pieces.size() >= 120 ? GONE : (byte) (PIECE + pieces.size());
				if (mark != GONE) {
					pieces.add(cells);
				}
				for (int o : cells) {
					s[o] = mark;
				}
				anyGone = true;
			}
			if (!anyGone) {
				return;
			}
			// the seared flesh: with what it's next to (what stays, if any of that is)
			List<List<Integer>> more = new ArrayList<>();
			for (int k = 0; k < pieces.size(); k++) {
				more.add(new ArrayList<>());
			}
			for (int pass = 0; pass < 3; pass++) {
				byte[] was = s.clone();
				for (int c = 0; c < s.length; c++) {
					if (!seared[c] || s[c] != FLESH) {
						continue;
					}
					int i = c / (ny * nz), j = c / nz % ny, k = c % nz;
					byte found = 0;
					boolean stays = false;
					for (int side = 0; side < 6; side++) {
						int a = i + (side == 0 ? 1 : side == 1 ? -1 : 0), b = j + (side == 2 ? 1 : side == 3 ? -1 : 0), e = k + (side == 4 ? 1 : side == 5 ? -1 : 0);
						if (a < 0 || b < 0 || e < 0 || a >= nx || b >= ny || e >= nz) {
							continue;
						}
						int o = index(a, b, e);
						if (was[o] == FLESH && !seared[o]) {
							stays = true;
						} else if (was[o] >= GONE && found == 0) {
							found = was[o];
						}
					}
					if (stays) {
						seared[c] = false;                          // held by what stays: it holds the next in turn
					} else if (found != 0) {
						s[c] = found;
						if (found >= PIECE) {
							more.get(found - PIECE).add(c);
						}
					}
				}
			}
			for (int k = 0; k < pieces.size(); k++) {
				if (!more.get(k).isEmpty()) {
					int[] cells = Arrays.copyOf(pieces.get(k), pieces.get(k).length + more.get(k).size());
					for (int i = 0; i < more.get(k).size(); i++) {
						cells[pieces.get(k).length + i] = more.get(k).get(i);
					}
					pieces.set(k, cells);
				}
			}
			if (!keep) {
				// nothing of the part stays: what's left of the seared flesh goes too
				for (int c = 0; c < s.length; c++) {
					if (s[c] == FLESH) {
						s[c] = GONE;
					}
				}
			}
		}

		private int[] fill(int from, int[] stack, boolean[] seen) {
			int top = 0, count = 0;
			int[] found = new int[16];
			stack[top++] = from;
			seen[from] = true;
			while (top > 0) {
				int c = stack[--top];
				if (count == found.length) {
					found = Arrays.copyOf(found, count * 2);
				}
				found[count++] = c;
				int i = c / (ny * nz), j = c / nz % ny, k = c % nz;
				for (int side = 0; side < 6; side++) {
					int a = i + (side == 0 ? 1 : side == 1 ? -1 : 0), b = j + (side == 2 ? 1 : side == 3 ? -1 : 0), e = k + (side == 4 ? 1 : side == 5 ? -1 : 0);
					if (a < 0 || b < 0 || e < 0 || a >= nx || b >= ny || e >= nz) {
						continue;
					}
					int o = index(a, b, e);
					if (!seen[o] && s[o] == FLESH) {
						seen[o] = true;
						stack[top++] = o;
					}
				}
			}
			return Arrays.copyOf(found, count);
		}

		/** Every cell that's gone from the part, for remembering ({@link #shed}). */
		void gone(BitSet into) {
			for (int c = 0; c < s.length; c++) {
				if (s[c] >= GONE) {
					into.set(c);
				}
			}
		}
	}

	// ------------------------------------------------------------------ something that has come off

	/** A face of something that's come off: four corners (from the body's middle, blocks), texture, colour, normal. */
	static final class Quad {
		final float[] p = new float[12], uv = new float[8];
		final int[] c = new int[4];
		float nx, ny, nz;
		/** A seared cut face's: how hot each corner is (near a mouth) and its grain; null for a face of the skin. */
		float @Nullable [] hot;
		float grain = 1;
	}

	/** The faces drawn with one render type. */
	static final class Batch {
		final RenderType type;
		final @Nullable UvMapping uv;
		final List<Quad> quads = new ArrayList<>();

		Batch(RenderType type, @Nullable UvMapping uv) {
			this.type = type;
			this.uv = uv;
		}
	}

	/** Something that has come off a creature: where, what it's made of, and how it falls and burns. */
	static final class Body {
		/** Its middle when it came off (world), the creature's own middle line then (which way is away from it) and feet. */
		final double ox, oy, oz, fromX, fromY, fromZ;
		final int seed, frame0;
		final Map<Object, Batch> skin = new LinkedHashMap<>();
		final List<Quad> seared = new ArrayList<>();
		/** Still being gathered from the model's drawing (it's drawn in its place meanwhile); then let go. */
		volatile boolean open = true;
		int light = LightCoordsUtil.FULL_BRIGHT;
		/** Game time (ticks) it let go at; how long it falls (s); where it comes to rest from its middle; how it has turned by then. */
		float t0, fall = 0.4F, restX, restY, restZ, lowest;
		/** The way the blow that cut it off was going (level; 0, 0: not known): it falls that way, and away from the creature. */
		float pushX, pushZ;
		/**
		 * So that it never sinks into the ground as it tips over: the corners of the box round it (from its middle), the
		 * ground (from its middle's height when it came off), and how much higher its own lowest point lies than that
		 * box's once it's at rest.
		 */
		private final float[] corners = new float[24];
		private float groundAt, slack;
		final Quaternionf turn = new Quaternionf();
		boolean hissed;

		Body(double ox, double oy, double oz, double fromX, double fromY, double fromZ, int seed) {
			this.ox = ox;
			this.oy = oy;
			this.oz = oz;
			this.fromX = fromX;
			this.fromY = fromY;
			this.fromZ = fromZ;
			this.seed = seed;
			this.frame0 = frame;
		}

		synchronized Batch batch(RenderType type, @Nullable UvMapping uv) {
			return skin.computeIfAbsent(uv == null ? type : Arrays.asList(type, uv), k -> new Batch(type, uv));
		}

		synchronized void add(Batch to, Quad q) {
			to.quads.add(q);
		}

		synchronized void sear(Quad q) {
			seared.add(q);
		}

		synchronized boolean empty() {
			for (Batch b : skin.values()) {
				if (!b.quads.isEmpty()) {
					return false;
				}
			}
			return true;
		}

		private void each(java.util.function.Consumer<Quad> f) {
			for (Batch b : skin.values()) {
				b.quads.forEach(f);
			}
			seared.forEach(f);
		}

		/** Lets go: from here it falls. Works out where it comes to rest, lying as a thing its shape would. */
		synchronized void letGo(@Nullable ClientLevel level, float now) {
			open = false;
			t0 = now;
			float[] lo = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE}, hi = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
			each(q -> {
				for (int i = 0; i < 12; i++) {
					lo[i % 3] = Math.min(lo[i % 3], q.p[i]);
					hi[i % 3] = Math.max(hi[i % 3], q.p[i]);
				}
			});
			if (lo[0] > hi[0]) {
				return;
			}
			float hx = (hi[0] - lo[0]) / 2, hy = (hi[1] - lo[1]) / 2, hz = (hi[2] - lo[2]) / 2;
			Random r = new Random(seed);
			// the ground under it (under its middle; under the creature's feet if it's in a block itself)
			double ground = fromY;
			if (level != null) {
				Vec3 from = new Vec3(ox, oy, oz);
				HitResult hit = level.clip(new ClipContext(from, from.add(0, -24, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
				if (hit.getType() != HitResult.Type.MISS && hit.getLocation().y < oy - 0.02) {
					ground = hit.getLocation().y;
				} else if (hit.getType() == HitResult.Type.MISS) {
					ground = oy - 24;
				}
			}
			boolean stands = oy + lo[1] - ground < 0.15;
			boolean tall = hy > 1.2F * Math.max(hx, hz);
			// away from the creature's middle line
			float ax = (float) (ox - fromX), az = (float) (oz - fromZ);
			float al = Mth.sqrt(ax * ax + az * az);
			if (pushX != 0 || pushZ != 0) {
				// away from the creature's middle (a limb's piece: mostly that), and the way the blow went
				float away = Mth.clamp(al / 0.3F, 0, 1);
				ax = (al < 1e-4F ? 0 : ax / al) * away + pushX * (1.2F - away);
				az = (al < 1e-4F ? 0 : az / al) * away + pushZ * (1.2F - away);
				al = Mth.sqrt(ax * ax + az * az);
			}
			if (al < 0.04F) {
				float a = r.nextFloat() * Mth.TWO_PI;
				ax = Mth.cos(a);
				az = Mth.sin(a);
			} else {
				ax /= al;
				az /= al;
			}
			// a tall thing ends lying down, tipped over away from the creature; a flat one only settles
			float tip = (tall || stands ? 78 + r.nextFloat() * 18 : 8 + r.nextFloat() * 26) * Mth.DEG_TO_RAD;
			turn.identity().rotateAxis(tip, az, 0, -ax).rotateY((r.nextFloat() - 0.5F) * 1.1F);
			Vector3f v = new Vector3f();
			float[] low = {Float.MAX_VALUE};
			each(q -> {
				for (int i = 0; i < 12; i += 3) {
					low[0] = Math.min(low[0], turn.transform(q.p[i], q.p[i + 1], q.p[i + 2], v).y);
				}
			});
			lowest = low[0];
			for (int k = 0; k < 8; k++) {
				corners[3 * k] = (k & 1) == 0 ? lo[0] : hi[0];
				corners[3 * k + 1] = (k & 2) == 0 ? lo[1] : hi[1];
				corners[3 * k + 2] = (k & 4) == 0 ? lo[2] : hi[2];
			}
			groundAt = (float) (ground - oy);
			slack = lowest - lowestCorner(turn);
			float slide = stands ? hy * 0.75F : tall ? 0.12F + 0.2F * r.nextFloat() + hy * 0.4F : 0.12F + 0.22F * r.nextFloat();
			restX = ax * slide;
			restZ = az * slide;
			restY = (float) (ground - oy) - lowest + 0.01F;
			float drop = Math.max(0, -restY);
			fall = stands ? 0.4F + 0.3F * hy : Mth.clamp(Mth.sqrt(drop / 10F), 0.22F, 1.6F);
			if (level != null) {
				level.playLocalSound(ox, oy, oz, SoundEvents.FIRE_AMBIENT, SoundSource.NEUTRAL, 0.55F, 1.2F + r.nextFloat() * 0.3F, false);
			}
		}

		/** Where its middle is and how it's turned, {@code age} seconds after it let go (out: x, y, z from where it came off, its size). */
		void placed(float age, float[] out, Quaternionf q) {
			float f = Mth.clamp(age / fall, 0, 1);
			float size = age < FADE_FROM ? 1 : Mth.clamp(1 - (age - FADE_FROM) / (GONE_AT - FADE_FROM), 0, 1);
			out[0] = restX * (1 - (1 - f) * (1 - f));
			out[2] = restZ * (1 - (1 - f) * (1 - f));
			q.identity().slerp(turn, f * f * (3 - 2 * f));
			// the fall speeds up; tipping over, it turns on whatever of it is lowest (never into the ground); shrinking at
			// the end, it stays on the ground
			float y = restY * f * f;
			if (f < 1) {
				y = Math.max(y, groundAt - lowestCorner(q) - slack + 0.01F);
			}
			out[1] = y + lowest * (1 - size);
			out[3] = size;
		}

		/** The lowest of the corners of the box round it, turned by {@code q} (from its middle). */
		private float lowestCorner(Quaternionf q) {
			Vector3f v = new Vector3f();
			float low = Float.MAX_VALUE;
			for (int k = 0; k < 8; k++) {
				low = Math.min(low, q.transform(corners[3 * k], corners[3 * k + 1], corners[3 * k + 2], v).y);
			}
			return low;
		}
	}

	/** A new body (gathered from this frame's drawing of the creature). */
	static Body body(double ox, double oy, double oz, double fromX, double fromY, double fromZ, int seed) {
		Body b = new Body(ox, oy, oz, fromX, fromY, fromZ, seed);
		synchronized (BODIES) {
			BODIES.add(b);
			while (BODIES.size() > MAX) {
				BODIES.remove(0).open = false;
			}
		}
		return b;
	}

	/** Each frame: lets go of what has been gathered, drops what has burned out, and draws the rest where it has got to. */
	private static void draw(Vec3 camera, PoseStack pose, SubmitNodeCollector collector) {
		if (broken) {
			return;
		}
		try {
			drawAll(camera, pose, collector);
		} catch (RuntimeException e) {
			fail("drawing what came off", e);
		}
	}

	private static void drawAll(Vec3 camera, PoseStack pose, SubmitNodeCollector collector) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != level) {
			synchronized (BODIES) {
				BODIES.clear();
			}
			level = client.level;
		}
		frame++;
		List<Body> now;
		synchronized (BODIES) {
			if (BODIES.isEmpty()) {
				return;
			}
			float time = PlasmaWoundsClient.now();
			for (Iterator<Body> it = BODIES.iterator(); it.hasNext(); ) {
				Body b = it.next();
				if (b.open) {
					// gathered over the frame it came off in and the next; nothing drawn of it by then: nothing to show
					if (frame - b.frame0 >= 2 && (!b.empty() || frame - b.frame0 >= 8)) {
						if (b.empty()) {
							b.open = false;
							it.remove();
						} else {
							b.letGo(level, time);
						}
					}
				} else if ((time - b.t0) / 20F >= GONE_AT || time < b.t0 - 1 || b.empty()) {
					it.remove();
				}
			}
			now = new ArrayList<>(BODIES);
		}
		float time = PlasmaWoundsClient.now();
		float[] at = new float[4];
		Quaternionf q = new Quaternionf();
		for (Body b : now) {
			if (b.open) {
				continue;
			}
			float age = Math.max(0, (time - b.t0) / 20F);
			b.placed(age, at, q);
			if (at[3] <= 0.01F) {
				continue;
			}
			float burnt = Mth.clamp((age - BURN_FROM) / (BURNT_BY - BURN_FROM), 0, 1);
			float tint = 1 - 0.88F * burnt;
			// lit by its own fire while it burns
			float fire = age < FLAMES_UNTIL - 0.6F ? 1 : Mth.clamp(1 - (age - (FLAMES_UNTIL - 0.6F)) / 1.2F, 0, 1);
			int light = LightCoordsUtil.pack(Math.max(LightCoordsUtil.block(b.light), Math.round(15 * fire)), LightCoordsUtil.sky(b.light));
			float heat = (float) Math.exp(-age / 2.0), ember = (float) Math.exp(-age / 6.0);
			pose.pushPose();
			pose.translate(b.ox + at[0] - camera.x, b.oy + at[1] - camera.y, b.oz + at[2] - camera.z);
			pose.rotate(q);
			pose.scale(at[3], at[3], at[3]);
			synchronized (b) {
				for (Batch batch : b.skin.values()) {
					if (batch.quads.isEmpty()) {
						continue;
					}
					List<Quad> quads = new ArrayList<>(batch.quads);
					collector.submitCustomGeometry(pose, batch.type, (p, out) -> emit(quads, p, batch.uv == null ? out : batch.uv.wrap(out), light, tint, -1, 0, 0));
				}
				if (!b.seared.isEmpty()) {
					List<Quad> quads = new ArrayList<>(b.seared);
					collector.submitCustomGeometry(pose, RenderTypes.entityCutout(PlasmaWoundsClient.WHITE), (p, out) -> emit(quads, p, out, light, 1, 0, heat, ember));
					if (heat > 0.01F || ember > 0.03F) {
						collector.order(2).submitCustomGeometry(pose, RenderTypes.eyes(PlasmaWoundsClient.WHITE), (p, out) -> emit(quads, p, out, 0xF000F0, 1, 1, heat, ember));
					}
				}
			}
			pose.popPose();
		}
	}

	/**
	 * The faces into a buffer: the skin's ({@code pass} below 0) with their own colours darkened by {@code tint}; the seared
	 * cut faces' ({@code pass} 0: the wall's colour; 1: its glow) by how hot the cut still is.
	 */
	private static void emit(List<Quad> quads, PoseStack.Pose pose, VertexConsumer out, int light, float tint, int pass, float heat, float ember) {
		Vector3f pos = new Vector3f(), n = new Vector3f();
		for (Quad q : quads) {
			pose.transformNormal(q.nx, q.ny, q.nz, n);
			for (int i = 0; i < 4; i++) {
				pose.pose().transformPosition(q.p[3 * i], q.p[3 * i + 1], q.p[3 * i + 2], pos);
				int c;
				if (pass < 0 || q.hot == null) {
					int col = q.c[i];
					c = col & 0xFF000000 | Math.round((col >> 16 & 0xFF) * tint) << 16 | Math.round((col >> 8 & 0xFF) * tint) << 8 | Math.round((col & 0xFF) * tint);
				} else {
					c = pass == 0 ? PlasmaWoundsClient.wallColour(q.hot[i], heat, ember, q.grain) : PlasmaWoundsClient.glowColour(q.hot[i], heat, ember);
				}
				out.addVertex(pos.x(), pos.y(), pos.z(), c, q.uv[2 * i], q.uv[2 * i + 1], OverlayTexture.NO_OVERLAY, light, n.x(), n.y(), n.z());
			}
		}
	}

	/** Each tick: the fire on what's burning (flames, then smoke, a hiss as it goes out), ash as it crumbles. */
	static void tick(Minecraft client) {
		if (broken) {
			return;
		}
		try {
			burn(client);
		} catch (RuntimeException e) {
			fail("burning what came off", e);
		}
	}

	private static void burn(Minecraft client) {
		ClientLevel l = client.level;
		if (l == null || client.isPaused()) {
			return;
		}
		List<Body> now;
		synchronized (BODIES) {
			if (BODIES.isEmpty()) {
				return;
			}
			now = new ArrayList<>(BODIES);
		}
		float time = l.getGameTime();
		float[] at = new float[4];
		Quaternionf q = new Quaternionf();
		Vector3f v = new Vector3f();
		for (Body b : now) {
			if (b.open) {
				continue;
			}
			float age = Math.max(0, (time - b.t0) / 20F);
			if (age >= GONE_AT) {
				continue;
			}
			b.placed(age, at, q);
			Random r = new Random(b.seed * 31L + (long) time);
			List<Quad> all = new ArrayList<>();
			synchronized (b) {
				b.each(all::add);
			}
			if (all.isEmpty()) {
				continue;
			}
			// more fire on a bigger piece
			int flames = age < FLAMES_UNTIL ? Mth.clamp(all.size() / 40 + 1, 1, 4) : 0;
			int smoke = age < SMOKE_UNTIL && (int) time % 2 == 0 ? 1 : 0;
			int ash = age >= FADE_FROM ? 2 : 0;
			for (int i = 0; i < flames + smoke + ash; i++) {
				Quad quad = all.get(r.nextInt(all.size()));
				int k = r.nextInt(4) * 3;
				q.transform(quad.p[k] * at[3], quad.p[k + 1] * at[3], quad.p[k + 2] * at[3], v);
				double x = b.ox + at[0] + v.x, y = b.oy + at[1] + v.y, z = b.oz + at[2] + v.z;
				if (i < flames) {
					l.addParticle(all.size() < 30 || r.nextInt(3) == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, x, y + 0.02, z, 0, 0.012 + r.nextFloat() * 0.02, 0);
				} else if (i < flames + smoke) {
					l.addParticle(ParticleTypes.SMOKE, x, y + 0.05, z, 0, 0.03, 0);
				} else {
					l.addParticle(ParticleTypes.ASH, x, y, z, 0, 0, 0);
				}
			}
			if (!b.hissed && age >= BURNT_BY) {
				b.hissed = true;
				l.playLocalSound(b.ox + at[0], b.oy + at[1], b.oz + at[2], SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.12F, 1.7F + r.nextFloat() * 0.3F, false);
			}
		}
	}

	/** How many are about (for checks). */
	static int count() {
		synchronized (BODIES) {
			return BODIES.size();
		}
	}

	/** Those about now (for checks). */
	static List<Body> bodies() {
		synchronized (BODIES) {
			return new ArrayList<>(BODIES);
		}
	}

	/** Lets go of everything gathered at once (for checks: no frames go by there). */
	static void letGoAll(float now) {
		synchronized (BODIES) {
			for (Body b : BODIES) {
				if (b.open) {
					b.letGo(null, now);
				}
			}
		}
	}

	static void clear() {
		synchronized (BODIES) {
			BODIES.clear();
		}
	}
}
