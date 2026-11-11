package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.DroneDepotRenderer.Geometry;
import io.github.jimbozoomer.jugcraft.powerline.LineAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.powerline.LineGeometry;
import io.github.jimbozoomer.jugcraft.powerline.LineKind;
import io.github.jimbozoomer.jugcraft.powerline.LineRegistry;
import io.github.jimbozoomer.jugcraft.powerline.LineWires;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a power line structure (docs/features/power-lines.md). Since power lines round 2 (the approved redesign,
 * claude/jugcraft-power-lines-redesign.md; part 1 the supports, part 2 the superconducting line and the boxes, 20
 * October 2026) every structure is drawn from its model by client/LineSupports.java: its body turned to its line
 * (a box in 90-degree steps, LineGeometry's box facing rule), strain assemblies and jumpers per span, what a pole
 * carries for a box, footings on a slope, its number on its plates, at three levels of detail, nothing when out of
 * sight; and the wires it owns (each span drawn once, by the structure with the lower position), hanging where
 * {@link LineWires} says they hang, so what you see is what shocks you. The switchyard's line gantry is drawn by its
 * site; only the wires it owns are drawn here.
 *
 * <p>The stage 1 material sheet (tools/power_lines.py, its regions checked by tools/check_mod_data.py) is no longer
 * drawn by the structures; its regions and this class's box, panel, lathe and tube helpers are still used by other
 * renderers.
 */
public class LineRenderer implements BlockEntityRenderer<LineAnchorBlockEntity, LineRenderer.State> {
	public static final Identifier TEXTURE = Jugcraft.id("textures/entity/power_lines.png");
	private static final float PX = 1 / 512F;

	static final float[] WOOD = region(0, 0);
	static final float[] WOOD_BAND = region(1, 0);
	static final float[] CROSSARM = region(2, 0);
	static final float[] GALVANIZED = region(3, 0);
	static final float[] LATTICE = region(4, 0);
	static final float[] STEEL = region(5, 0);
	static final float[] STEEL_DARK = region(6, 0);
	static final float[] FOUNDATION = region(7, 0);
	static final float[] CONCRETE = region(0, 1);
	static final float[] GLAZE_TEAL = region(1, 1);
	static final float[] GLAZE_GREY = region(2, 1);
	static final float[] GLAZE_BROWN = region(3, 1);
	static final float[] CONDUCTOR = region(4, 1);
	static final float[] CARBON_CONDUCTOR = region(5, 1);
	static final float[] CRYO = region(6, 1);
	static final float[] CRYO_JACKET = region(7, 1);
	static final float[] RED_LIGHT = region(0, 2);
	static final float[] GREEN_ENCLOSURE = region(1, 2);
	static final float[] ENCLOSURE_DOOR = region(2, 2);
	static final float[] WARNING_LABEL = region(3, 2);
	static final float[] PAD = region(4, 2);
	static final float[] GRAVEL = region(5, 2);
	static final float[] CHAIN_LINK = region(6, 2);
	static final float[] BARBED_WIRE = region(7, 2);
	static final float[] CABINET = region(0, 3);
	static final float[] CABINET_DOOR = region(1, 3);
	static final float[] METER_FACE = region(2, 3);
	static final float[] VENT = region(3, 3);
	static final float[] TRANSFORMER_TANK = region(4, 3);
	static final float[] RADIATOR = region(5, 3);
	static final float[] BREAKER = region(6, 3);
	static final float[] POLE_CAN = region(7, 3);
	static final float[] CONDUIT = region(0, 4);
	static final float[] SENSOR = region(1, 4);
	static final float[] VALVE_HALL = region(2, 4);
	static final float[] COPPER = region(3, 4);

	/** A 64x64 material region (tools/power_lines.py MATERIALS and slot), inset half a pixel against bleeding. */
	private static float[] region(int col, int row) {
		return new float[] {(col * 64 + 0.5F) * PX, (row * 64 + 0.5F) * PX, (col * 64 + 63.5F) * PX, (row * 64 + 63.5F) * PX};
	}

	// Faces of a box, for the oriented helpers below.
	static final int UP = 0, DOWN = 1, NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;

	public static class State extends BlockEntityRenderState {
		LineKind kind = LineKind.WOODEN_POLE;
		/** What it draws (built when its wires change). */
		LineSupports.@org.jspecify.annotations.Nullable Built built;
		int lod;
		int[] lights = new int[1];
		boolean blink;
	}

	/** What each structure draws, built when its wires change (a few hundred kept). */
	private static final java.util.Map<BlockPos, LineSupports.Built> BUILT = new java.util.LinkedHashMap<>(256, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(java.util.Map.Entry<BlockPos, LineSupports.Built> eldest) {
			return size() > 512;
		}
	};
	private static final RenderType MODEL_TYPE = RenderTypes.entityCutout(LineModels.TEXTURE);

	/** The texture a structure's item and placement preview draw it with. */
	public static Identifier textureFor(LineKind kind) {
		return LineModels.TEXTURE;
	}

	/** The item texture of a structure's miniature. */
	public static Identifier itemTextureFor(LineKind kind) {
		return LineModels.TEXTURE;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 256;
	}

	@Override
	public void extractRenderState(LineAnchorBlockEntity anchor, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(anchor, state, crumbling);
		state.built = null;
		Level level = anchor.getLevel();
		if (level == null) {
			return;
		}
		BlockPos pos = anchor.getBlockPos();
		LineKind kind = anchor.kind();
		state.kind = kind;
		List<BlockPos> connections = anchor.connections();
		int samples = kind.height / LineModels.STEP + 2;
		if (state.lights.length != samples) {
			state.lights = new int[samples];
		}
		for (int i = 0; i < samples; i++) {
			state.lights[i] = LightCoordsUtil.getLightCoords(level, pos.above(Math.max(1, i * LineModels.STEP)));
		}
		state.blink = Math.floorMod(level.getGameTime(), 30L) < 12;
		double dx = cameraPos.x - (pos.getX() + 0.5), dy = cameraPos.y - (pos.getY() + kind.height / 2.0), dz = cameraPos.z - (pos.getZ() + 0.5);
		double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
		state.lod = dist < LineModels.LOD_NEAR ? 0 : dist < LineModels.LOD_MID ? 1 : 2;
		// The layout, the wires it owns, and what is built from them.
		LineGeometry.Layout layout = anchor.layout();
		List<Object> key = new ArrayList<>();
		key.add(kind);
		key.add(List.copyOf(connections));
		boolean supercon = kind == LineKind.SUPERCONDUCTING_PYLON;
		List<LineSupports.Far> owned = new ArrayList<>();
		for (BlockPos other : connections) {
			LineKind otherKind = LineRegistry.of(level).kindAt(other);
			key.add(otherKind);
			supercon |= otherKind == LineKind.SUPERCONDUCTING_PYLON;
			if (otherKind == null || !(level.getBlockEntity(other) instanceof LineAnchorBlockEntity partner)) {
				continue;
			}
			boolean owns = other.asLong() >= pos.asLong();
			if (!owns && !(kind == LineKind.UNIT_SUBSTATION && otherKind == LineKind.SUPERCONDUCTING_PYLON)) {
				continue;
			}
			Vec3 mid = Vec3.atCenterOf(pos).add(Vec3.atCenterOf(other)).scale(0.5).add(0, (kind.height + otherKind.height) * 0.35, 0);
			int light = LightCoordsUtil.getLightCoords(level, BlockPos.containing(mid));
			key.add(List.copyOf(partner.connections()));
			owned.add(new LineSupports.Far(partner.layout(), light, owns));
		}
		if (!LineSupports.modelled(kind) && owned.isEmpty()) {
			return;
		}
		LineSupports.Live live = new LineSupports.Live(anchor.load(), anchor.delivered());
		if (supercon) {
			key.add(live.load() > 0);       // the cables' stripe glows while the line carries power
		}
		if (kind == LineKind.SERVICE_PEDESTAL) {
			key.add(LineSupports.meterKey(live));
		}
		int[] drops = LineSupports.drops(level, layout);
		key.add(java.util.Arrays.stream(drops).boxed().toList());
		key.add(state.lod);
		LineSupports.Built built;
		synchronized (BUILT) {
			built = BUILT.get(pos);
			if (built == null || !built.key.equals(key)) {
				built = LineSupports.build(key, layout, drops, owned, state.lod, live);
				BUILT.put(pos.immutable(), built);
			}
		}
		state.built = built;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		LineSupports.Built built = state.built;
		if (built == null) {
			return;
		}
		if (built.bounds != null && camera.cullFrustum != null && !camera.cullFrustum.isVisible(built.bounds)) {
			// Out of sight: only what can be seen is drawn.
			return;
		}
		collector.submitCustomGeometry(poseStack, MODEL_TYPE,
				(pose, consumer) -> LineSupports.draw(built, state.lod, pose, consumer, state.lights, state.blink, 0xFFFFFFFF));
	}

	// ------------------------------------------------------------------ the item

	/**
	 * The stage 1 structures' item texture (tools/structure_items.py, the material sheet in flat colours); the
	 * structures' items draw from their models now.
	 */
	public static final Identifier ITEM_TEXTURE = Jugcraft.id("textures/entity/power_lines_item.png");

	/** Each kind's miniature (its layout alone at the far level of detail), built once. */
	private static final java.util.Map<LineKind, LineSupports.Built> ITEMS = new java.util.EnumMap<>(LineKind.class);

	/**
	 * The structure as its item shows it (client/StructureItemRenderers.java): standing alone on level ground, unwired,
	 * facing +z, at its far level of detail (flat colours: shrunk to a slot, texture would sparkle), its lamps lit.
	 */
	static void drawItem(LineKind kind, PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
		if (!LineSupports.modelled(kind)) {
			return;
		}
		LineSupports.Built built;
		synchronized (ITEMS) {
			built = ITEMS.computeIfAbsent(kind, k -> LineSupports.build(List.of(),
					LineGeometry.layout(k, BlockPos.ZERO, List.of(), p -> null), new int[4], List.of(), 2));
		}
		LineSupports.draw(built, 2, pose, consumer, new int[] {light}, true, 0xFFFFFFFF);
	}

	/**
	 * A structure where its item would place it (client/MbsPreview.java, the placement preview): at {@code anchor}, as
	 * it will stand once wired to {@code partner} (null: alone) - turned to it, its angle body at a line's end, its
	 * kit turned to the partner, a box in its 90-degree step - with the wire it would hang, lit evenly.
	 */
	static void drawGhost(Level level, BlockPos anchor, LineKind kind, @org.jspecify.annotations.Nullable BlockPos partner, PoseStack.Pose pose,
			com.mojang.blaze3d.vertex.VertexConsumer consumer) {
		List<BlockPos> connections = partner == null ? List.of() : List.of(partner);
		LineGeometry.Layout layout = LineGeometry.layout(kind, anchor, connections, LineWires.kinds(level));
		List<LineSupports.Far> owned = new ArrayList<>();
		if (partner != null && level.getBlockEntity(partner) instanceof LineAnchorBlockEntity other) {
			List<BlockPos> otherConnections = new ArrayList<>(other.connections());
			otherConnections.add(anchor);
			java.util.function.Function<BlockPos, @org.jspecify.annotations.Nullable LineKind> kinds =
					p -> p.equals(anchor) ? kind : LineWires.kinds(level).apply(p);
			owned.add(new LineSupports.Far(LineGeometry.layout(level, other.kind(), partner, otherConnections, kinds), LightCoordsUtil.FULL_BRIGHT));
		}
		LineSupports.Built built = LineSupports.build(List.of(), layout, LineSupports.drops(level, layout), owned, 1);
		LineSupports.draw(built, 1, pose, consumer, new int[] {LightCoordsUtil.FULL_BRIGHT}, true, 0xFFFFFFFF);
	}

	// ------------------------------------------------------------------ geometry helpers

	/**
	 * One face of a box, textured the right way up as seen from outside: {@code which} is {@link #UP}, {@link #DOWN},
	 * {@link #NORTH} (-z), {@link #SOUTH} (+z), {@link #WEST} (-x) or {@link #EAST} (+x).
	 */
	static void face(Geometry g, int which, double x0, double y0, double z0, double x1, double y1, double z1, float[] uv) {
		float ax = (float) x0, ay = (float) y0, az = (float) z0, bx = (float) x1, by = (float) y1, bz = (float) z1;
		switch (which) {
			case UP -> g.quad(ax, by, az, ax, by, bz, bx, by, bz, bx, by, az, 0, 1, 0, uv);
			case DOWN -> g.quad(bx, ay, az, bx, ay, bz, ax, ay, bz, ax, ay, az, 0, -1, 0, uv);
			case NORTH -> g.quad(bx, by, az, bx, ay, az, ax, ay, az, ax, by, az, 0, 0, -1, uv);
			case SOUTH -> g.quad(ax, by, bz, ax, ay, bz, bx, ay, bz, bx, by, bz, 0, 0, 1, uv);
			case WEST -> g.quad(ax, by, az, ax, ay, az, ax, ay, bz, ax, by, bz, -1, 0, 0, uv);
			default -> g.quad(bx, by, bz, bx, ay, bz, bx, ay, az, bx, by, az, 1, 0, 0, uv);
		}
	}

	/** A box with one material on every face, each the right way up. */
	static void box(Geometry g, double x0, double y0, double z0, double x1, double y1, double z1, float[] uv) {
		box(g, x0, y0, z0, x1, y1, z1, uv, uv, uv);
	}

	/** A box with one material round its sides and others on top and underneath. */
	static void box(Geometry g, double x0, double y0, double z0, double x1, double y1, double z1, float[] sides, float[] top, float[] bottom) {
		face(g, UP, x0, y0, z0, x1, y1, z1, top);
		face(g, DOWN, x0, y0, z0, x1, y1, z1, bottom);
		for (int which = NORTH; which <= EAST; which++) {
			face(g, which, x0, y0, z0, x1, y1, z1, sides);
		}
	}

	/** A thin plate with its picture on one face and plain material on its edges and back. */
	static void panel(Geometry g, double x0, double y0, double z0, double x1, double y1, double z1, int front, float[] picture, float[] edge) {
		for (int which = UP; which <= EAST; which++) {
			face(g, which, x0, y0, z0, x1, y1, z1, which == front ? picture : edge);
		}
	}

	/** A vertical sheet between two ground points, seen from both sides (fencing). */
	static void sheet(Geometry g, double x0, double y0, double z0, double x1, double y1, double z1, double nx, double nz, float[] uv) {
		float ax = (float) x0, ay = (float) y0, az = (float) z0, bx = (float) x1, by = (float) y1, bz = (float) z1;
		// The two faces sit a hair apart, so they can never fight for the same depth.
		float ox = (float) (nx * 0.003), oz = (float) (nz * 0.003);
		g.quad(ax + ox, by, az + oz, ax + ox, ay, az + oz, bx + ox, ay, bz + oz, bx + ox, by, bz + oz, (float) nx, 0, (float) nz, uv);
		g.quad(bx - ox, by, bz - oz, bx - ox, ay, bz - oz, ax - ox, ay, az - oz, ax - ox, by, az - oz, (float) -nx, 0, (float) -nz, uv);
	}

	/** The frame at a point of a curve: a unit normal and binormal round the tangent, carried along so the tube does not twist. */
	private static Vec3[] frame(Vec3 tangent, Vec3 previousNormal) {
		Vec3 t = tangent.normalize();
		Vec3 n = previousNormal == null ? (Math.abs(t.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)) : previousNormal;
		n = n.subtract(t.scale(n.dot(t)));
		if (n.lengthSqr() < 1e-8) {
			n = Math.abs(t.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
			n = n.subtract(t.scale(n.dot(t)));
		}
		n = n.normalize();
		return new Vec3[] {t, n, t.cross(n)};
	}

	/** A continuous tube of the given radius along a run of points, {@code sides} faces round. */
	static void tube(Geometry g, List<Vec3> points, double radius, int sides, float[] uv) {
		double[][] rings = new double[points.size()][];
		for (int i = 0; i < points.size(); i++) {
			rings[i] = new double[] {i == 0 ? 0 : rings[i - 1][0] + points.get(i).distanceTo(points.get(i - 1)), radius};
		}
		sweep(g, points, rings, sides, uv);
	}

	/**
	 * A shape turned on a lathe along the axis from {@code a} to {@code b}: {@code profile} is {distance along, radius}
	 * pairs from {@code a}, drawn in order and closed with a flat end at each end.
	 */
	static void lathe(Geometry g, Vec3 a, Vec3 b, double[][] profile, int sides, float[] uv) {
		Vec3 axis = b.subtract(a).normalize();
		List<Vec3> points = new ArrayList<>();
		for (double[] step : profile) {
			points.add(a.add(axis.scale(step[0])));
		}
		sweep(g, points, profile, sides, uv);
		cap(g, a, axis.scale(-1), profile[0][1], sides, uv);
		cap(g, points.getLast(), axis, profile[profile.length - 1][1], sides, uv);
	}

	/** An insulator between two points: a glazed core with sheds every {@code pitch}, turned on a lathe. */
	static void insulator(Geometry g, Vec3 a, Vec3 b, double core, double shed, double pitch, float[] uv) {
		double length = a.distanceTo(b);
		List<double[]> profile = new ArrayList<>();
		profile.add(new double[] {0, core});
		for (double d = pitch * 0.5; d + pitch * 0.45 < length; d += pitch) {
			profile.add(new double[] {d - pitch * 0.3, core});
			profile.add(new double[] {d - pitch * 0.05, shed});
			profile.add(new double[] {d + pitch * 0.05, shed});
			profile.add(new double[] {d + pitch * 0.12, core});
		}
		profile.add(new double[] {length, core});
		lathe(g, a, b, profile.toArray(new double[0][]), 8, uv);
	}

	/** The side faces of a swept ring profile: {@code rings[i]} = {distance (unused here), radius} at {@code points[i]}. */
	private static void sweep(Geometry g, List<Vec3> points, double[][] rings, int sides, float[] uv) {
		int n = points.size();
		if (n < 2) {
			return;
		}
		Vec3[][] verts = new Vec3[n][sides];
		Vec3[][] radial = new Vec3[n][sides];
		Vec3 normal = null;
		for (int i = 0; i < n; i++) {
			Vec3 tangent = i == 0 ? points.get(1).subtract(points.get(0))
					: i == n - 1 ? points.get(i).subtract(points.get(i - 1)) : points.get(i + 1).subtract(points.get(i - 1));
			if (tangent.lengthSqr() < 1e-10) {
				tangent = i == 0 ? new Vec3(0, 1, 0) : points.get(i).subtract(points.get(i - 1));
			}
			Vec3[] f = frame(tangent, normal);
			normal = f[1];
			for (int k = 0; k < sides; k++) {
				double angle = Math.PI * 2 * k / sides;
				radial[i][k] = f[1].scale(Math.cos(angle)).add(f[2].scale(Math.sin(angle)));
				verts[i][k] = points.get(i).add(radial[i][k].scale(rings[i][1]));
			}
		}
		float u0 = uv[0], u1 = uv[2];
		for (int i = 0; i + 1 < n; i++) {
			for (int k = 0; k < sides; k++) {
				int k1 = (k + 1) % sides;
				Vec3 nrm = radial[i][k].add(radial[i][k1]).add(radial[i + 1][k]).add(radial[i + 1][k1]).normalize();
				// Each side gets a slice of the material across, so a glaze highlight wraps round the shape.
				float[] slice = {u0 + (u1 - u0) * k / sides, uv[1], u0 + (u1 - u0) * (k + 1) / sides, uv[3]};
				g.quad((float) verts[i][k].x, (float) verts[i][k].y, (float) verts[i][k].z, (float) verts[i][k1].x, (float) verts[i][k1].y,
						(float) verts[i][k1].z, (float) verts[i + 1][k1].x, (float) verts[i + 1][k1].y, (float) verts[i + 1][k1].z,
						(float) verts[i + 1][k].x, (float) verts[i + 1][k].y, (float) verts[i + 1][k].z, (float) nrm.x, (float) nrm.y, (float) nrm.z, slice);
			}
		}
	}

	/** A flat end on a lathed shape, facing {@code outward}. */
	private static void cap(Geometry g, Vec3 centre, Vec3 outward, double radius, int sides, float[] uv) {
		if (radius < 1e-4) {
			return;
		}
		Vec3[] f = frame(outward, null);
		Vec3[] ring = new Vec3[sides];
		for (int k = 0; k < sides; k++) {
			double angle = Math.PI * 2 * k / sides;
			ring[k] = centre.add(f[1].scale(Math.cos(angle) * radius)).add(f[2].scale(Math.sin(angle) * radius));
		}
		for (int k = 0; k + 1 < sides; k += 2) {
			// Fan of quads from the centre, wound to face outward.
			Vec3 p0 = ring[k], p1 = ring[(k + 1) % sides], p2 = ring[(k + 2) % sides];
			g.quad((float) centre.x, (float) centre.y, (float) centre.z, (float) p0.x, (float) p0.y, (float) p0.z, (float) p1.x, (float) p1.y, (float) p1.z,
					(float) p2.x, (float) p2.y, (float) p2.z, (float) outward.x, (float) outward.y, (float) outward.z, uv);
		}
	}

	/** A square member from a to b, in pieces at most two blocks long so its texture is not stretched. */
	static void beam(Geometry g, double ax, double ay, double az, double bx, double by, double bz, double width, float[] uv) {
		double dx = bx - ax, dy = by - ay, dz = bz - az;
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1e-4) {
			return;
		}
		double yaw = Math.atan2(dx, dz);
		double pitch = -Math.atan2(dy, Math.hypot(dx, dz));
		g.push();
		g.translate(ax, ay, az).yaw(yaw).pitch(pitch);
		int pieces = Math.max(1, (int) Math.ceil(len / 2));
		double step = len / pieces, hw = width / 2;
		for (int i = 0; i < pieces; i++) {
			box(g, -hw, -hw, i * step, hw, hw, (i + 1) * step, uv);
		}
		g.pop();
	}
}
