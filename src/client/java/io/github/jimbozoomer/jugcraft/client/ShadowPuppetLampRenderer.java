package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ShadowPuppetLampBlock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Shadow Puppet Lamp's turning paper shade, three panels round the candle with a bat, a cat and a witch in
 * them (glowing warm while it is lit), and, while it is lit, the shadow each panel throws: along the panel's outward
 * direction to the first solid block within {@link ShadowPuppetLampBlock#RANGE} blocks, a dark shape on that wall,
 * bigger the further it falls, darker at night. Positions here are in blocks from the lamp's block.
 *
 * <p>Each panel is one quad: 26.3's cut-out entity type draws a quad from both sides (seen from inside, the picture is
 * mirrored, as real paper's would be), so a second, back-to-back quad would only fight it. The panels are just wide
 * enough to meet at the corners of their triangle.
 *
 * <p>Each shadow is looked for by five rays across its panel's spread, each walking the blocks at the flame's height to
 * the face it enters, and on the walls round the ends of those walls' runs (round a corner, a doorway's side or a
 * pillar). On each, a card is placed where the panel's middle ray meets the wall's plane and cut to the run of open wall,
 * to the part the flame can see past the blocks in between, and to the open wall's height block by block. So a shadow
 * falling across a doorway, a corner or a pillar's edge shows on each side of it and slides smoothly from wall to wall
 * as the shade turns, never hanging over a doorway or an outside corner nor lying where no light reaches. It fades out
 * over the last block of {@link ShadowPuppetLampBlock#RANGE} instead of vanishing.
 */
public class ShadowPuppetLampRenderer implements BlockEntityRenderer<DecorationBlockEntity, ShadowPuppetLampRenderer.State> {
	private static final String[] DESIGNS = {"bat", "cat", "witch"};
	private static final RenderType[] PAPER = new RenderType[DESIGNS.length];
	private static final RenderType[] SHADOW = new RenderType[DESIGNS.length];
	private static final int FULL_BRIGHT = 0xF000F0;
	/**
	 * The shade: how far its panels are from the middle, how wide they are (half; the panels of a triangle round a
	 * circle of RADIUS meet at RADIUS times the square root of three, and a hair more closes the corners), and their
	 * bottom and top (pixels).
	 */
	private static final float RADIUS = 2.6F;
	private static final float HALF_WIDTH = (float) (RADIUS * Math.sqrt(3.0)) + 0.02F;
	private static final float BOTTOM = 2.5F;
	private static final float TOP = 11.0F;
	/** The candle's flame, where the shadows are thrown from (blocks). */
	private static final double FLAME_Y = 0.42;
	/**
	 * The rays that find a shadow's walls: how far either side of its panel's middle they reach (radians, a little more
	 * than the widest shadow's half-angle, so a wall is found before the shadow reaches it), and where between.
	 */
	private static final double SPREAD = Math.toRadians(32.0);
	private static final double[] RAYS = {0.0, -0.5, 0.5, -1.0, 1.0};
	/** The most walls one shadow is looked for on (the rays' and those round the ends of their runs). */
	private static final int MOST_WALLS = 12;

	static {
		for (int i = 0; i < DESIGNS.length; i++) {
			PAPER[i] = RenderTypes.entityCutout(Jugcraft.id("textures/block/shadow_puppet_lamp_paper_" + DESIGNS[i] + ".png"));
			SHADOW[i] = RenderTypes.entityTranslucent(Jugcraft.id("textures/entity/shadow_puppet_lamp_" + DESIGNS[i] + "_shadow.png"));
		}
	}

	/**
	 * A shadow on a wall: which design, its middle, the wall's normal (toward the lamp) and its half size, in blocks; the
	 * part of it drawn (cut to its wall), from its middle: left and right as someone facing the wall sees them, bottom
	 * and top; and how strongly it is drawn (fading out at the end of its range), 0 to 1.
	 */
	private record Shadow(int design, Vec3 middle, Vec3 normal, float half, float left, float right, float bottom, float top, float strength) {
	}

	/** A ray's first solid block (relative to the lamp) and the normal of the face it enters by (toward the lamp). */
	private record Hit(BlockPos block, Vec3 normal) {
	}

	public static final class State extends BlockEntityRenderState {
		boolean lit;
		boolean night;
		float turn;
		List<Shadow> shadows = new ArrayList<>();
	}

	public ShadowPuppetLampRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity lamp, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(lamp, state, crumbling);
		BlockState block = lamp.getBlockState();
		Level level = lamp.getLevel();
		state.shadows = new ArrayList<>();
		state.lit = false;
		if (!(block.getBlock() instanceof ShadowPuppetLampBlock) || level == null) {
			return;
		}
		BlockPos pos = lamp.getBlockPos();
		state.lit = block.getValue(ShadowPuppetLampBlock.LIT);
		state.night = MourningAngelBlock.night(level);
		state.turn = ShadowPuppetLampBlock.turn(pos, level.getGameTime() + (double) partialTick);
		if (!state.lit) {
			return;
		}
		Vec3 flame = new Vec3(0.5, FLAME_Y, 0.5);
		for (int i = 0; i < DESIGNS.length; i++) {
			shadows(level, pos, flame, Math.toRadians(state.turn + i * 120.0), i, state.shadows);
		}
	}

	/**
	 * The shadow of panel {@code design}, which faces {@code angle}. The rays across its spread find the walls round it,
	 * and each wall leads on to the walls round the ends of its run of open wall (round an outside corner, or across an
	 * inside one) where the shadow runs on past them. On each wall a card is placed where the panel's middle ray meets
	 * its plane, cut to the run, to what the flame can see of it past the blocks in between and, block by block, to how
	 * high the open wall goes; two walls on one run give the same pieces, kept once.
	 */
	private static void shadows(Level level, BlockPos lamp, Vec3 flame, double angle, int design, List<Shadow> out) {
		Vec3 middle = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
		List<Hit> walls = new ArrayList<>();
		for (double ray : RAYS) {
			double turned = angle + ray * SPREAD;
			Hit hit = walk(level, lamp, flame, new Vec3(Math.cos(turned), 0.0, Math.sin(turned)));
			if (hit != null) {
				walls.add(hit);
			}
		}
		int first = out.size();
		int looked = 0;
		for (int i = 0; i < walls.size() && looked < MOST_WALLS; i++) {
			Hit hit = walls.get(i);
			if (listed(walls, i)) {
				continue;
			}
			looked++;
			// How far the middle ray runs to the wall's plane, if it heads toward it at all.
			boolean acrossX = hit.normal().x != 0.0;
			double heading = acrossX ? middle.x : middle.z;
			double facing = acrossX ? hit.normal().x : hit.normal().z;
			if (heading * facing >= 0.0) {
				continue;
			}
			double plane = (acrossX ? hit.block().getX() : hit.block().getZ()) + (facing > 0.0 ? 1.0 : 0.0);
			double distance = (plane - (acrossX ? flame.x : flame.z)) / heading;
			float strength = (float) Math.min(1.0, ShadowPuppetLampBlock.RANGE - distance);
			if (strength <= 0.0F) {
				continue;
			}
			Vec3 centre = flame.add(middle.scale(distance)).add(0.0, 0.2 + distance * 0.12, 0.0).add(hit.normal().scale(0.02));
			float half = (float) Math.min(2.2, Math.max(0.6, 0.3 + distance * 0.3)) / 2;
			onWall(level, lamp, flame, middle, plane, hit, design, centre, half, strength, walls, out, first);
		}
	}

	/** Whether the wall {@code walls[i]} was listed before it. */
	private static boolean listed(List<Hit> walls, int i) {
		Hit hit = walls.get(i);
		for (int j = 0; j < i; j++) {
			Hit other = walls.get(j);
			if (other.block().equals(hit.block()) && other.normal().x == hit.normal().x && other.normal().z == hit.normal().z) {
				return true;
			}
		}
		return false;
	}

	/** Whether {@code out}, from {@code first}, already holds the piece {@code shadow} (its run reached another way). */
	private static boolean already(List<Shadow> out, int first, Shadow shadow) {
		for (int i = first; i < out.size(); i++) {
			Shadow other = out.get(i);
			if (other.normal().x == shadow.normal().x && other.normal().z == shadow.normal().z
					&& other.middle().distanceToSqr(shadow.middle()) < 1.0E-10 && Math.abs(other.left() - shadow.left()) < 1.0E-5F
					&& Math.abs(other.right() - shadow.right()) < 1.0E-5F) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Follows {@code ray} from the flame to the first solid block at the flame's height, block by block (each block the
	 * ray passes is looked at, and the face it enters by is the face it hits); nothing when none is within range.
	 */
	private static @Nullable Hit walk(Level level, BlockPos lamp, Vec3 flame, Vec3 ray) {
		int stepX = ray.x > 0 ? 1 : -1;
		int stepZ = ray.z > 0 ? 1 : -1;
		double deltaX = Math.abs(ray.x) < 1.0E-9 ? Double.MAX_VALUE : Math.abs(1.0 / ray.x);
		double deltaZ = Math.abs(ray.z) < 1.0E-9 ? Double.MAX_VALUE : Math.abs(1.0 / ray.z);
		double nextX = (stepX > 0 ? 1.0 - flame.x : flame.x) * deltaX;
		double nextZ = (stepZ > 0 ? 1.0 - flame.z : flame.z) * deltaZ;
		int bx = 0;
		int bz = 0;
		int by = (int) Math.floor(flame.y);
		while (true) {
			boolean acrossX = nextX < nextZ;
			double hit;
			if (acrossX) {
				hit = nextX;
				bx += stepX;
				nextX += deltaX;
			} else {
				hit = nextZ;
				bz += stepZ;
				nextZ += deltaZ;
			}
			if (hit > ShadowPuppetLampBlock.RANGE) {
				return null;
			}
			BlockPos block = lamp.offset(bx, by, bz);
			if (!full(level, block)) {
				continue;
			}
			if (hit < 0.5) {
				return null;
			}
			return new Hit(new BlockPos(bx, by, bz), acrossX ? new Vec3(-stepX, 0.0, 0.0) : new Vec3(0.0, 0.0, -stepZ));
		}
	}

	/**
	 * The shadow centred on {@code centre} on the plane of {@code wall}'s face (its block relative to the lamp; the plane
	 * at {@code plane} along its axis), cut to the run of open wall through that block: from it along the wall to either
	 * side, as far as the blocks are full and the blocks in front of them are not; then to what the flame sees of it; and
	 * each block of it up as far as that block's own column of open wall goes, neighbours as high drawn as one piece.
	 * Where the shadow runs on past an end of the run, the wall round that end is added to {@code walls}.
	 */
	private static void onWall(Level level, BlockPos lamp, Vec3 flame, Vec3 middle, double plane, Hit wall, int design, Vec3 centre, float half,
			float strength, List<Hit> walls, List<Shadow> out, int first) {
		BlockPos block = wall.block();
		Vec3 normal = wall.normal();
		int nx = (int) normal.x;
		int nz = (int) normal.z;
		// Along the wall: "right" as someone facing it sees it, in whole blocks (normal x up, the other way round).
		int rx = nz;
		int rz = -nx;
		// Where the block starts along the wall, from the shadow's middle (blocks): its left-hand edge.
		double across = rx != 0 ? (centre.x - block.getX()) * rx : (centre.z - block.getZ()) * rz;
		double start = (rx + rz > 0 ? 0.0 : -1.0) - across;
		double bottom = centre.y - half;
		double top = centre.y + half;
		int least = (int) Math.floor(-half - start);
		int most = (int) Math.floor(half - start - 1.0E-6);
		int lo = 0;
		int hi = 0;
		while (hi < most && open(level, lamp, block, rx, rz, hi + 1, 0, normal)) {
			hi++;
		}
		while (lo > least && open(level, lamp, block, rx, rz, lo - 1, 0, normal)) {
			lo--;
		}
		if (hi < most) {
			round(level, lamp, block, rx, rz, nx, nz, hi, 1, walls);
		}
		if (lo > least) {
			round(level, lamp, block, rx, rz, nx, nz, lo, -1, walls);
		}
		// Only the blocks under the shadow count (the block may lie beside it, its run reaching under it).
		lo = Math.max(lo, least);
		hi = Math.min(hi, most);
		if (lo > hi) {
			return;
		}
		int highest = (int) Math.floor(top - block.getY() - 1.0E-6);
		int[] ups = new int[hi - lo + 1];
		for (int i = lo; i <= hi; i++) {
			int up = 0;
			while (up < highest && open(level, lamp, block, rx, rz, i, up + 1, normal)) {
				up++;
			}
			ups[i - lo] = up;
		}
		float low = (float) (Math.max(bottom, block.getY()) - centre.y);
		List<double[]> lit = seen(level, lamp, flame, middle, normal, plane, centre, rx, rz, Math.max(-half, start + lo), Math.min(half, start + hi + 1));
		int from = lo;
		for (int i = lo; i <= hi; i++) {
			if (i < hi && ups[i + 1 - lo] == ups[i - lo]) {
				continue;
			}
			double left = Math.max(-half, start + from);
			double right = Math.min(half, start + i + 1);
			float high = (float) (Math.min(top, block.getY() + ups[i - lo] + 1.0) - centre.y);
			from = i + 1;
			if (right <= left || high <= low) {
				continue;
			}
			for (double[] stretch : lit) {
				float l = (float) Math.max(left, stretch[0]);
				float r = (float) Math.min(right, stretch[1]);
				if (r - l <= 1.0E-6F) {
					continue;
				}
				Shadow piece = new Shadow(design, centre, normal, half, l, r, low, high, strength);
				if (!already(out, first, piece)) {
					out.add(piece);
				}
			}
		}
	}

	/**
	 * Adds to {@code walls} the wall round the end {@code end} of a run ({@code step} 1 its right-hand end, -1 its left):
	 * across an inside corner, the face of the block in front of the next block, toward the run; round an outside corner
	 * (the next block not full), the end block's own side.
	 */
	private static void round(Level level, BlockPos lamp, BlockPos block, int rx, int rz, int nx, int nz, int end, int step, List<Hit> walls) {
		BlockPos next = new BlockPos(block.getX() + rx * (end + step), block.getY(), block.getZ() + rz * (end + step));
		if (full(level, lamp.offset(next.getX(), next.getY(), next.getZ()))) {
			BlockPos front = next.offset(nx, 0, nz);
			if (full(level, lamp.offset(front.getX(), front.getY(), front.getZ()))) {
				walls.add(new Hit(front, new Vec3(-rx * step, 0.0, -rz * step)));
			}
		} else {
			walls.add(new Hit(new BlockPos(block.getX() + rx * end, block.getY(), block.getZ() + rz * end), new Vec3(rx * step, 0.0, rz * step)));
		}
	}

	/**
	 * The stretches of the wall from {@code left} to {@code right} (blocks along it from {@code centre}) that the flame
	 * sees past the full blocks between it and the wall's plane, at the flame's height: the angles those blocks cover,
	 * seen from the flame against the middle ray, are taken from the stretch's, and what is left is turned back into
	 * stretches of wall, as pairs {from, to}.
	 */
	private static List<double[]> seen(Level level, BlockPos lamp, Vec3 flame, Vec3 middle, Vec3 normal, double plane, Vec3 centre, int rx, int rz,
			double left, double right) {
		boolean acrossX = normal.x != 0.0;
		// The stretch's ends on the plane itself (the centre stands 0.02 in front of it).
		double px = centre.x - normal.x * 0.02;
		double pz = centre.z - normal.z * 0.02;
		double lx = px + rx * left;
		double lz = pz + rz * left;
		double hx = px + rx * right;
		double hz = pz + rz * right;
		double a = angle(flame, middle, lx, lz);
		double b = angle(flame, middle, hx, hz);
		double low = Math.min(a, b);
		double high = Math.max(a, b);
		List<double[]> blocked = new ArrayList<>();
		int y = (int) Math.floor(flame.y);
		for (int bx = (int) Math.floor(Math.min(flame.x, Math.min(lx, hx))); bx <= (int) Math.floor(Math.max(flame.x, Math.max(lx, hx))); bx++) {
			for (int bz = (int) Math.floor(Math.min(flame.z, Math.min(lz, hz))); bz <= (int) Math.floor(Math.max(flame.z, Math.max(lz, hz))); bz++) {
				// Only blocks wholly on the flame's side of the plane stand in between.
				boolean beyond = acrossX ? (normal.x < 0.0 ? bx + 1 > plane + 1.0E-9 : bx < plane - 1.0E-9)
						: (normal.z < 0.0 ? bz + 1 > plane + 1.0E-9 : bz < plane - 1.0E-9);
				if (beyond || !full(level, lamp.offset(bx, y, bz))) {
					continue;
				}
				double from = Double.MAX_VALUE;
				double to = -Double.MAX_VALUE;
				boolean ahead = false;
				for (int c = 0; c < 4; c++) {
					double cx = bx + (c & 1);
					double cz = bz + (c >> 1);
					ahead |= middle.x * (cx - flame.x) + middle.z * (cz - flame.z) > 0.0;
					double turn = angle(flame, middle, cx, cz);
					from = Math.min(from, turn);
					to = Math.max(to, turn);
				}
				if (ahead && to > low && from < high) {
					blocked.add(new double[] {from, to});
				}
			}
		}
		blocked.sort(Comparator.comparingDouble(span -> span[0]));
		List<double[]> stretches = new ArrayList<>();
		double free = low;
		for (double[] span : blocked) {
			if (span[0] > free) {
				stretch(stretches, flame, middle, normal, plane, px, pz, rx, rz, free, Math.min(span[0], high));
			}
			free = Math.max(free, span[1]);
			if (free >= high) {
				break;
			}
		}
		if (free < high) {
			stretch(stretches, flame, middle, normal, plane, px, pz, rx, rz, free, high);
		}
		return stretches;
	}

	/** The angle of the point (x, z) seen from the flame, from the middle ray (radians, counterclockwise seen from above). */
	private static double angle(Vec3 flame, Vec3 middle, double x, double z) {
		double dx = x - flame.x;
		double dz = z - flame.z;
		return Math.atan2(middle.x * dz - middle.z * dx, middle.x * dx + middle.z * dz);
	}

	/** Adds the stretch of wall between the angles {@code from} and {@code to} (blocks along it from (px, pz)). */
	private static void stretch(List<double[]> stretches, Vec3 flame, Vec3 middle, Vec3 normal, double plane, double px, double pz, int rx, int rz,
			double from, double to) {
		if (to - from < 1.0E-9) {
			return;
		}
		double a = along(flame, middle, normal, plane, px, pz, rx, rz, from);
		double b = along(flame, middle, normal, plane, px, pz, rx, rz, to);
		stretches.add(new double[] {Math.min(a, b), Math.max(a, b)});
	}

	/** Where the ray at {@code turn} from the middle ray meets the wall's plane: blocks along it from (px, pz). */
	private static double along(Vec3 flame, Vec3 middle, Vec3 normal, double plane, double px, double pz, int rx, int rz, double turn) {
		double cos = Math.cos(turn);
		double sin = Math.sin(turn);
		double dx = middle.x * cos - middle.z * sin;
		double dz = middle.z * cos + middle.x * sin;
		double t = normal.x != 0.0 ? (plane - flame.x) / dx : (plane - flame.z) / dz;
		return (flame.x + dx * t - px) * rx + (flame.z + dz * t - pz) * rz;
	}

	/**
	 * Whether the block {@code along} blocks along the wall and {@code up} up from {@code wall} is open wall: full, with
	 * nothing full in front of it.
	 */
	private static boolean open(Level level, BlockPos lamp, BlockPos wall, int rx, int rz, int along, int up, Vec3 normal) {
		BlockPos block = lamp.offset(wall.getX() + rx * along, wall.getY() + up, wall.getZ() + rz * along);
		return full(level, block) && !full(level, block.offset((int) normal.x, 0, (int) normal.z));
	}

	private static boolean full(Level level, BlockPos block) {
		return level.getBlockState(block).isCollisionShapeFullBlock(level, block);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int paperLight = state.lit ? FULL_BRIGHT : state.lightCoords;
		for (int i = 0; i < DESIGNS.length; i++) {
			double angle = Math.toRadians(state.turn + i * 120.0);
			Vec3 out = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
			Vec3 middle = new Vec3(0.5, 0.0, 0.5).add(out.scale(RADIUS / 16));
			collector.submitCustomGeometry(pose, PAPER[i], (matrix, buffer) ->
					panel(buffer, matrix, middle, out, HALF_WIDTH / 16, BOTTOM / 16, TOP / 16, 0xFFFFFFFF, paperLight));
		}
		int alpha = state.night ? 160 : 80;
		for (Shadow shadow : state.shadows) {
			int color = Math.round(alpha * shadow.strength()) << 24;
			collector.submitCustomGeometry(pose, SHADOW[shadow.design()],
					(matrix, buffer) -> card(buffer, matrix, shadow, color, state.lightCoords));
		}
	}

	/** A shadow's quad, the part of it left on its wall, its texture cut to match. */
	private static void card(VertexConsumer buffer, PoseStack.Pose matrix, Shadow shadow, int color, int light) {
		Vec3 normal = shadow.normal();
		float rx = (float) normal.z;
		float rz = (float) -normal.x;
		float x = (float) shadow.middle().x;
		float y = (float) shadow.middle().y;
		float z = (float) shadow.middle().z;
		float size = shadow.half() * 2;
		float u0 = (shadow.left() + shadow.half()) / size;
		float u1 = (shadow.right() + shadow.half()) / size;
		float v0 = (shadow.half() - shadow.top()) / size;
		float v1 = (shadow.half() - shadow.bottom()) / size;
		float[][] corners = {{x + rx * shadow.right(), y + shadow.bottom(), z + rz * shadow.right(), u1, v1},
				{x + rx * shadow.right(), y + shadow.top(), z + rz * shadow.right(), u1, v0},
				{x + rx * shadow.left(), y + shadow.top(), z + rz * shadow.left(), u0, v0},
				{x + rx * shadow.left(), y + shadow.bottom(), z + rz * shadow.left(), u0, v1}};
		DecorDraw.quad(buffer, matrix, corners, (float) normal.x, (float) normal.y, (float) normal.z, color, light);
	}

	/**
	 * A vertical quad facing {@code normal}, {@code halfWidth} either side of {@code middle} and from {@code bottom} to
	 * {@code top} above it, its texture the right way round to someone looking at it.
	 */
	private static void panel(VertexConsumer buffer, PoseStack.Pose matrix, Vec3 middle, Vec3 normal, float halfWidth, float bottom, float top,
			int color, int light) {
		// To someone facing the quad, their right is (-normal) x up.
		float rx = (float) normal.z;
		float rz = (float) -normal.x;
		float x = (float) middle.x;
		float y = (float) middle.y;
		float z = (float) middle.z;
		float[][] corners = {{x + rx * halfWidth, y + bottom, z + rz * halfWidth, 1, 1}, {x + rx * halfWidth, y + top, z + rz * halfWidth, 1, 0},
				{x - rx * halfWidth, y + top, z - rz * halfWidth, 0, 0}, {x - rx * halfWidth, y + bottom, z - rz * halfWidth, 0, 1}};
		DecorDraw.quad(buffer, matrix, corners, (float) normal.x, (float) normal.y, (float) normal.z, color, light);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // Its shadows fall on walls round the room.
	}
}
