package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.mixin.client.SightLevelRendererAccessor;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * Skips drawing what can't be seen (Justin's frame rate, JFR of 8 November 2026: drones, the drop ship and the crystal
 * trees were drawn every frame wherever he looked, about 7 ms of his 24 ms frame). One test for everything:
 * {@link #visible} says whether a box in the world could be on screen this frame: in the camera's view (the frustum),
 * and in a chunk section the game's own occlusion graph reaches from the camera (not shut off behind solid walls), or,
 * when Sodium draws the world, a section Sodium found visible ({@link SightSodium}).
 *
 * <p>Block entities whose renderers draw off their own chunk section ({@code shouldRenderOffScreen}) are drawn by the
 * game every frame, wherever you look. For ours, each one's extent is learned from what it actually draws (every
 * vertex's position, on its first frames and now and then after), so no renderer needs to say how big it is; from then
 * on it is drawn only while that box is visible. A box out of sight is still drawn once a second, so something that
 * has moved off its learned box shows up and its box grows. Renderers that implement {@link Always} are never skipped
 * whole: those whose drawing does something besides drawing (smoke particles), and those that draw many separate things
 * over a wide area and test each one with {@link #visible} themselves (the depot's drones and hangar doors).
 */
public final class Sight {
	/** Renderers always run, never skipped whole: their drawing has side effects, or they test their own parts. */
	public interface Always {
	}

	/** On unless {@code -Djugcraft.sight=false}; the frame rate check turns it off for its "before" numbers. */
	public static boolean enabled = !"false".equals(System.getProperty("jugcraft.sight"));

	/** Margin round a learned box (blocks). */
	static final double PAD = 0.75;
	/** Frames each block entity is drawn and measured before it can be skipped. */
	static final int LEARN_FIRST = 6;
	/** While drawn, its extent is measured again one frame in this many. */
	static final int RELEARN_EVERY = 40;
	/** Out of sight, it is drawn anyway once in this long (ms), to catch a box that has grown. */
	static final long REFRESH_MS = 1000;
	/** A box covering more chunk sections than this is not checked against the occlusion graph. */
	static final int MAX_SECTIONS = 512;

	/** A block entity's learned extent (world coordinates) and when it was last drawn. */
	public static final class Bounds {
		double x0 = Double.POSITIVE_INFINITY, y0 = Double.POSITIVE_INFINITY, z0 = Double.POSITIVE_INFINITY;
		double x1 = Double.NEGATIVE_INFINITY, y1 = Double.NEGATIVE_INFINITY, z1 = Double.NEGATIVE_INFINITY;
		boolean known;
		int learned;
		long lastDrawn;
		final int stagger;

		Bounds(int stagger) {
			this.stagger = stagger;
		}

		/** Adds a point in the block entity's own frame ({@code inv} applied to (x, y, z)), offset by its block. */
		void add(Matrix4fc inv, float x, float y, float z, int ox, int oy, int oz) {
			double lx = inv.m00() * x + inv.m10() * y + inv.m20() * z + inv.m30() + ox;
			double ly = inv.m01() * x + inv.m11() * y + inv.m21() * z + inv.m31() + oy;
			double lz = inv.m02() * x + inv.m12() * y + inv.m22() * z + inv.m32() + oz;
			if (!(lx == lx && ly == ly && lz == lz)) {
				return;                                    // NaN: a degenerate vertex
			}
			if (!known) {                                  // its own block is always part of it
				x0 = ox;
				y0 = oy;
				z0 = oz;
				x1 = ox + 1;
				y1 = oy + 1;
				z1 = oz + 1;
				known = true;
			}
			if (lx < x0) x0 = lx;
			if (lx > x1) x1 = lx;
			if (ly < y0) y0 = ly;
			if (ly > y1) y1 = ly;
			if (lz < z0) z0 = lz;
			if (lz > z1) z1 = lz;
		}

		/** Adds a box round a point (a model, item or text with its pose's origin there). */
		void addAround(Matrix4fc inv, Matrix4fc pose, double r, int ox, int oy, int oz) {
			float x = pose.m30(), y = pose.m31(), z = pose.m32();
			add(inv, x, y, z, ox, oy, oz);
			if (known) {
				double lx = inv.m00() * x + inv.m10() * y + inv.m20() * z + inv.m30() + ox;
				double ly = inv.m01() * x + inv.m11() * y + inv.m21() * z + inv.m31() + oy;
				double lz = inv.m02() * x + inv.m12() * y + inv.m22() * z + inv.m32() + oz;
				x0 = Math.min(x0, lx - r);
				y0 = Math.min(y0, ly - r);
				z0 = Math.min(z0, lz - r);
				x1 = Math.max(x1, lx + r);
				y1 = Math.max(y1, ly + r);
				z1 = Math.max(z1, lz + r);
			}
		}

		public AABB box() {
			return new AABB(x0, y0, z0, x1, y1, z1);
		}

		public boolean known() {
			return known;
		}
	}

	private static final Map<BlockEntity, Bounds> BOUNDS = new WeakHashMap<>();
	private static final Map<BlockEntityRenderState, Bounds> LEARNING = new IdentityHashMap<>();
	private static final ClassValue<Boolean> OURS = new ClassValue<>() {
		@Override
		protected Boolean computeValue(Class<?> type) {
			return type.getName().startsWith("io.github.jimbozoomer.jugcraft.") && !Always.class.isAssignableFrom(type);
		}
	};
	private static Bounds pending;
	private static long frame;

	/** This frame's counts so far and the last whole frame's (the frame rate check reports them). */
	private static int checked, skipped, drawn;
	public static int lastChecked, lastSkipped, lastDrawn;

	private Sight() {
	}

	/** A new frame's extraction starts. */
	public static void beginFrame() {
		frame++;
		LEARNING.clear();
		pending = null;
		lastChecked = checked;
		lastSkipped = skipped;
		lastDrawn = drawn;
		checked = skipped = drawn = 0;
	}

	/**
	 * Before a globally drawn block entity's render state is extracted (it is in range): whether to skip it this frame.
	 * Remembers, for {@link #extracted}, whether this frame should measure what it draws.
	 */
	public static boolean skip(BlockEntity be, BlockEntityRenderer<?, ?> renderer) {
		pending = null;
		if (!enabled || renderer == null || !OURS.get(renderer.getClass())) {
			return false;
		}
		Bounds b = BOUNDS.get(be);
		if (b == null) {
			b = new Bounds(System.identityHashCode(be));
			BOUNDS.put(be, b);
		}
		checked++;
		long now = System.currentTimeMillis();
		if (!b.known || b.learned < LEARN_FIRST) {
			b.learned++;
			b.lastDrawn = now;
			pending = b;
			drawn++;
			return false;
		}
		if (visible(b.x0 - PAD, b.y0 - PAD, b.z0 - PAD, b.x1 + PAD, b.y1 + PAD, b.z1 + PAD)) {
			b.lastDrawn = now;
			if (Math.floorMod(frame + b.stagger, RELEARN_EVERY) == 0) {
				pending = b;
			}
			drawn++;
			return false;
		}
		if (now - b.lastDrawn >= REFRESH_MS) {
			b.lastDrawn = now;
			pending = b;
			drawn++;
			return false;
		}
		skipped++;
		return true;
	}

	/** After extraction: a state this frame measures. */
	public static void extracted(BlockEntityRenderState state) {
		if (pending != null && state != null) {
			LEARNING.put(state, pending);
		}
		pending = null;
	}

	/** At submit: the collector to draw {@code state} with (one that also measures, when this frame measures it). */
	public static SubmitNodeCollector wrap(BlockEntityRenderState state, com.mojang.blaze3d.vertex.PoseStack pose, SubmitNodeCollector collector) {
		if (LEARNING.isEmpty()) {
			return collector;
		}
		Bounds b = LEARNING.remove(state);
		if (b == null || state.blockPos == null) {
			return collector;
		}
		Matrix4f inv = new Matrix4f(pose.last().pose());
		if (Math.abs(inv.determinant()) < 1e-12) {
			return collector;
		}
		inv.invert();
		return new SightCollector(collector, b, inv, state.blockPos.getX(), state.blockPos.getY(), state.blockPos.getZ());
	}

	/** The learned box of a block entity (null until known; tests and the frame rate check). */
	public static AABB learnedBox(BlockEntity be) {
		Bounds b = BOUNDS.get(be);
		return b == null || !b.known ? null : b.box();
	}

	// ------------------------------------------------------------------ the test

	/** Whether any part of the box (world coordinates) could be seen this frame. */
	public static boolean visible(AABB box) {
		return visible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
	}

	/** Whether any part of the box (world coordinates) could be seen this frame. */
	public static boolean visible(double x0, double y0, double z0, double x1, double y1, double z1) {
		if (!enabled) {
			return true;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameRenderer == null || mc.levelRenderer == null) {
			return true;
		}
		Camera camera = mc.gameRenderer.mainCamera();
		if (camera == null || !camera.isInitialized()) {
			return true;
		}
		Vec3 c = camera.position();
		if (c.x >= x0 && c.x <= x1 && c.y >= y0 && c.y <= y1 && c.z >= z0 && c.z <= z1) {
			return true;
		}
		Frustum frustum = camera.getCullFrustum();
		if (frustum != null && !frustum.isVisible(new AABB(x0, y0, z0, x1, y1, z1))) {
			return false;
		}
		if (SODIUM) {
			try {
				return SightSodium.visible(x0, y0, z0, x1, y1, z1);
			} catch (Throwable t) {
				SODIUM = false;                            // a Sodium that has changed: the frustum alone from now on
				io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.warn("Sight: Sodium's visibility test failed; using the view alone", t);
				return true;
			}
		}
		return reached(mc.levelRenderer, c, x0, y0, z0, x1, y1, z1);
	}

	/** Sodium draws the world: its occlusion test, not the game's (which it leaves empty). */
	private static boolean SODIUM = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("sodium");

	/**
	 * Whether the game's occlusion graph reaches any chunk section the box touches (from the camera, through sections
	 * that aren't shut off by solid blocks). True whenever it can't tell: no graph yet, the camera's own section not in it,
	 * a section outside the loaded area, or a box too big to check.
	 */
	static boolean reached(LevelRenderer renderer, Vec3 c, double x0, double y0, double z0, double x1, double y1, double z1) {
		ViewArea area = ((SightLevelRendererAccessor) renderer).jugcraft$viewArea();
		SectionOcclusionGraph graph = renderer.sectionOcclusionGraph();
		if (area == null || graph == null) {
			return true;
		}
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(Mth.floor(c.x), Mth.floor(c.y), Mth.floor(c.z));
		SectionRenderDispatcher.RenderSection own = area.getRenderSectionAt(m);
		if (own == null || graph.getNode(own) == null) {
			return true;
		}
		int sx0 = Mth.floor(x0) >> 4, sx1 = Mth.floor(x1) >> 4;
		int sy0 = Mth.floor(y0) >> 4, sy1 = Mth.floor(y1) >> 4;
		int sz0 = Mth.floor(z0) >> 4, sz1 = Mth.floor(z1) >> 4;
		if (sy0 < area.minSectionY() || sy1 > area.maxSectionY()) {
			return true;                                   // reaches above or below the world: sky
		}
		long count = (long) (sx1 - sx0 + 1) * (sy1 - sy0 + 1) * (sz1 - sz0 + 1);
		if (count > MAX_SECTIONS) {
			return true;
		}
		for (int sx = sx0; sx <= sx1; sx++) {
			for (int sz = sz0; sz <= sz1; sz++) {
				for (int sy = sy0; sy <= sy1; sy++) {
					SectionRenderDispatcher.RenderSection s = area.getRenderSectionAt(m.set(sx << 4, sy << 4, sz << 4));
					if (s == null || graph.getNode(s) != null) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
