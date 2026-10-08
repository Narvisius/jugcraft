package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.DroneDepotRenderer.Geometry;
import io.github.jimbozoomer.jugcraft.powerline.LineAnchorBlockEntity;
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
 * Draws a power line structure from its kind (docs/features/power-lines.md, matching the approved mock-ups): wooden
 * and concrete poles, steel monopoles, lattice towers (their legs reaching down to uneven ground), the UHV, UHVDC
 * and superconducting supports, and the grid-connection boxes; plus the wires to the structures it is joined to
 * (each wire drawn once, by the structure with the lower position), hanging where {@link LineWires} says they hang,
 * so what you see is what shocks you. Supports turn to face along their line; boxes keep their place in the world.
 * Materials come from one sheet made by tools/power_lines.py (regions checked by tools/check_mod_data.py); every
 * face is drawn the right way up, insulators are turned on a lathe, and wires are continuous tubes.
 */
public class LineRenderer implements BlockEntityRenderer<LineAnchorBlockEntity, LineRenderer.State> {
	public static final Identifier TEXTURE = Jugcraft.id("textures/entity/power_lines.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);
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
		float yaw;
		int load;
		int bodyLight;
		int topLight;
		/** How far below the anchor each lattice leg's ground is (x-z-, x+z-, x-z+, x+z+). */
		final int[] legDrop = new int[4];
		/** A Pad-Mount Transformer is wired to this support: draw the riser down the pole. */
		boolean riser;
		/** A superconducting line ends at this substation: draw its cryogenic termination vessels. */
		boolean cryoTermination;
		/** Wires owned by this structure: {ax, ay, az, bx, by, bz, sag, bundle, carbon, light, supercon} relative to the anchor block. */
		final List<float[]> wires = new ArrayList<>();
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
		state.wires.clear();
		state.riser = false;
		state.cryoTermination = false;
		Level level = anchor.getLevel();
		if (level == null) {
			return;
		}
		BlockPos pos = anchor.getBlockPos();
		LineKind kind = anchor.kind();
		state.kind = kind;
		state.load = anchor.load();
		List<BlockPos> connections = anchor.connections();
		state.yaw = kind.isBox() ? 0 : (float) LineWires.yaw(pos, connections);
		state.bodyLight = LightCoordsUtil.getLightCoords(level, pos.above());
		state.topLight = LightCoordsUtil.getLightCoords(level, pos.above(Math.max(1, kind.height - 1)));
		java.util.Arrays.fill(state.legDrop, 0);
		if (hasLegs(kind)) {
			int r = kind.halfWidth();
			int i = 0;
			for (int sz : new int[] {-1, 1}) {
				for (int sx : new int[] {-1, 1}) {
					state.legDrop[i++] = groundDrop(level, pos.offset(sx * r, 0, sz * r));
				}
			}
		}
		for (BlockPos other : connections) {
			LineKind otherKind = LineRegistry.of(level).kindAt(other);
			if (otherKind == LineKind.PAD_MOUNT_TRANSFORMER) {
				state.riser = true;
			}
			if (otherKind == LineKind.SUPERCONDUCTING_PYLON && kind == LineKind.UNIT_SUBSTATION) {
				state.cryoTermination = true;
			}
			if (otherKind == null || other.asLong() < pos.asLong() || !(level.getBlockEntity(other) instanceof LineAnchorBlockEntity partner)) {
				continue;
			}
			LineKind support = kind.isBox() ? otherKind : kind;
			int bundle = bundle(support);
			boolean carbon = support == LineKind.RECONDUCTORED_LATTICE_TOWER || otherKind == LineKind.RECONDUCTORED_LATTICE_TOWER;
			boolean supercon = support == LineKind.SUPERCONDUCTING_PYLON;
			for (LineWires.Wire wire : LineWires.between(pos, kind, connections, other, otherKind, partner.connections())) {
				Vec3 mid = wire.at(0.5);
				int light = LightCoordsUtil.getLightCoords(level, BlockPos.containing(mid));
				state.wires.add(new float[] {(float) (wire.a().x - pos.getX()), (float) (wire.a().y - pos.getY()), (float) (wire.a().z - pos.getZ()),
						(float) (wire.b().x - pos.getX()), (float) (wire.b().y - pos.getY()), (float) (wire.b().z - pos.getZ()), (float) wire.sag(),
						bundle, carbon ? 1 : 0, Float.intBitsToFloat(light), supercon ? 1 : 0});
			}
		}
	}

	private static boolean hasLegs(LineKind kind) {
		return kind.family == LineKind.Family.LATTICE || kind.family == LineKind.Family.UHV || kind.family == LineKind.Family.UHVDC;
	}

	/** How many blocks below the anchor's level the ground under a leg is (0 to 12). */
	private static int groundDrop(Level level, BlockPos foot) {
		for (int d = 0; d <= 12; d++) {
			BlockPos below = foot.below(d + 1);
			if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
				return d;
			}
		}
		return 12;
	}

	/** Sub-conductors per phase: one on poles and monopoles, bundles on the big towers. */
	private static int bundle(LineKind kind) {
		return switch (kind.family) {
			case LATTICE -> 4;
			case UHV -> 6;
			case UHVDC -> 8;
			default -> 1;
		};
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
			Geometry g = new Geometry(pose, consumer);
			g.light = state.bodyLight;
			g.translate(0.5, 0, 0.5);
			drawStructure(g, state);
			g.reset();
			for (float[] wire : state.wires) {
				g.light = Float.floatToRawIntBits(wire[9]);
				drawWire(g, wire, state.load);
			}
		});
	}

	// ------------------------------------------------------------------ the item

	/**
	 * The structures' item texture: the material sheet with every region one flat colour (its average), drawn by
	 * tools/structure_items.py. Shrunk to a slot, a block-sized texture tile is smaller than a pixel and would sparkle.
	 */
	public static final Identifier ITEM_TEXTURE = Jugcraft.id("textures/entity/power_lines_item.png");

	/**
	 * The structure as its item shows it (client/StructureItemRenderers.java): standing on level ground, unwired, its
	 * crossarms along x, its warning lights lit.
	 */
	static void drawItem(LineKind kind, PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
		State s = new State();
		s.kind = kind;
		s.bodyLight = light;
		s.topLight = light;
		Geometry g = new Geometry(pose, consumer);
		g.light = light;
		g.translate(0.5, 0, 0.5);
		drawStructure(g, s);
	}

	// ------------------------------------------------------------------ structures

	private static void drawStructure(Geometry g, State s) {
		LineKind kind = s.kind;
		switch (kind.family) {
			case WOOD -> {
				g.yaw(s.yaw);
				switch (kind) {
					case WOODEN_POLE -> woodenPole(g, s);
					case DOUBLE_ARM_POLE -> doubleArmPole(g, s);
					default -> concretePole(g, s);
				}
			}
			case MONOPOLE -> {
				g.yaw(s.yaw);
				monopole(g, s);
			}
			case LATTICE, UHV, UHVDC -> lattice(g, s);
			case SUPERCONDUCTING -> {
				g.yaw(s.yaw);
				superconductingPylon(g, s);
			}
			case BOX -> {
				switch (kind) {
					case SERVICE_PEDESTAL -> pedestal(g);
					case PAD_MOUNT_TRANSFORMER -> padMount(g);
					case UNIT_SUBSTATION -> substation(g, s);
					default -> converterStation(g);
				}
			}
		}
	}

	private static void riser(Geometry g, State s, double top, double offset) {
		if (s.riser) {
			// The cable to a pad-mount transformer comes down the pole in a conduit and goes underground.
			tube(g, List.of(new Vec3(offset + 0.05, -0.3, offset - 0.03), new Vec3(offset + 0.05, top, offset - 0.03)), 0.05, 6, CONDUIT);
			box(g, offset - 0.04, top, offset - 0.1, offset + 0.14, top + 0.25, offset + 0.04, STEEL);
		}
	}

	/** A pin insulator standing on a crossarm or pole top, glass, with its steel pin. */
	private static void pinInsulator(Geometry g, double x, double y0, double y1, double z) {
		box(g, x - 0.04, y0 - 0.05, z - 0.04, x + 0.04, y0 + 0.12, z + 0.04, STEEL);
		insulator(g, new Vec3(x, y0 + 0.1, z), new Vec3(x, y1, z), 0.07, 0.13, 0.16, GLAZE_TEAL);
	}

	private static void woodenPole(Geometry g, State s) {
		box(g, -0.16, -0.6, -0.16, 0.16, 1.2, 0.16, WOOD_BAND);
		box(g, -0.15, 1.2, -0.15, 0.15, 12.1, 0.15, WOOD);
		box(g, -1.3, 11.55, -0.09, 1.3, 11.8, 0.09, CROSSARM);
		beam(g, 0, 10.9, 0.12, -0.8, 11.55, 0.12, 0.05, STEEL);
		beam(g, 0, 10.9, 0.12, 0.8, 11.55, 0.12, 0.05, STEEL);
		pinInsulator(g, -1, 11.8, 12.5, 0);
		pinInsulator(g, 1, 11.8, 12.5, 0);
		pinInsulator(g, 0, 12.1, 12.5, 0);
		// The pole-top transformer can, with its bushing and fuse cutout.
		lathe(g, new Vec3(0, 9.0, 0.42), new Vec3(0, 10.4, 0.42), new double[][] {{0, 0.24}, {0.05, 0.27}, {1.3, 0.27}, {1.4, 0.22}}, 8, POLE_CAN);
		insulator(g, new Vec3(0, 10.4, 0.42), new Vec3(0, 10.75, 0.42), 0.04, 0.08, 0.12, GLAZE_BROWN);
		box(g, -0.3, 10.95, 0.15, 0.3, 11.05, 0.25, STEEL);
		tube(g, List.of(new Vec3(0, 10.75, 0.42), new Vec3(0.5, 11.2, 0.3), new Vec3(1, 11.9, 0.08)), 0.03, 4, CONDUCTOR);
		riser(g, s, 9.0, 0.16);
	}

	private static void doubleArmPole(Geometry g, State s) {
		box(g, -0.18, -0.6, -0.18, 0.18, 1.2, 0.18, WOOD_BAND);
		box(g, -0.17, 1.2, -0.17, 0.17, 13.2, 0.17, WOOD);
		for (double y : new double[] {12.25, 10.6}) {
			// Double crossarms: one each side of the pole, bolted through.
			box(g, -1.8, y, -0.3, 1.8, y + 0.25, -0.18, CROSSARM);
			box(g, -1.8, y, 0.18, 1.8, y + 0.25, 0.3, CROSSARM);
			beam(g, 0, y - 0.9, 0.24, -1.2, y, 0.24, 0.05, STEEL);
			beam(g, 0, y - 0.9, 0.24, 1.2, y, 0.24, 0.05, STEEL);
		}
		pinInsulator(g, -1.5, 12.5, 13.0, 0);
		pinInsulator(g, 1.5, 12.5, 13.0, 0);
		pinInsulator(g, 0, 13.2, 13.5, 0);
		for (int side : new int[] {-1, 1}) {
			// Dead-end strings on the lower arms, where the line used to be terminated.
			insulator(g, new Vec3(side * 1.7, 10.72, 0.3), new Vec3(side * 1.7, 10.72, 1.1), 0.05, 0.1, 0.14, GLAZE_GREY);
		}
		lathe(g, new Vec3(0, 8.0, 0.45), new Vec3(0, 9.6, 0.45), new double[][] {{0, 0.26}, {0.05, 0.3}, {1.5, 0.3}, {1.6, 0.24}}, 8, POLE_CAN);
		insulator(g, new Vec3(0, 9.6, 0.45), new Vec3(0, 9.95, 0.45), 0.04, 0.08, 0.12, GLAZE_BROWN);
		tube(g, List.of(new Vec3(0, 9.95, 0.45), new Vec3(0.8, 10.4, 0.4), new Vec3(1.5, 12.95, 0.05)), 0.03, 4, CONDUCTOR);
		riser(g, s, 8.0, 0.18);
	}

	private static void concretePole(Geometry g, State s) {
		lathe(g, new Vec3(0, -0.6, 0), new Vec3(0, 14.0, 0), new double[][] {{0, 0.3}, {7.6, 0.25}, {14.6, 0.17}}, 8, CONCRETE);
		double[][] posts = {{13.5, -1}, {12.0, 1}, {10.5, -1}};
		for (double[] post : posts) {
			double y = post[0], side = post[1];
			// Horizontal polymer post insulators on steel brackets.
			box(g, side < 0 ? -0.34 : 0.2, y - 0.2, -0.12, side < 0 ? -0.2 : 0.34, y + 0.2, 0.12, STEEL);
			insulator(g, new Vec3(side * 0.34, y, 0), new Vec3(side * 1.9, y, 0), 0.06, 0.12, 0.14, GLAZE_GREY);
			box(g, side * 2.0 - 0.1, y - 0.1, -0.1, side * 2.0 + 0.1, y + 0.1, 0.1, STEEL);
		}
		riser(g, s, 9.0, 0.25);
	}

	private static void monopole(Geometry g, State s) {
		LineKind kind = s.kind;
		int h = kind.height;
		box(g, -0.9, -0.6, -0.9, 0.9, 0.3, 0.9, FOUNDATION);
		// The tapered tubular pole, with a flanged joint every ten metres.
		lathe(g, new Vec3(0, 0.3, 0), new Vec3(0, h, 0), new double[][] {{0, 0.55}, {h - 0.3, 0.22}}, 8, GALVANIZED);
		for (double y = 10; y < h - 2; y += 10) {
			double r = 0.55 - 0.33 * (y / h) + 0.08;
			lathe(g, new Vec3(0, y - 0.15, 0), new Vec3(0, y + 0.15, 0), new double[][] {{0, r}, {0.3, r}}, 8, STEEL);
		}
		box(g, -0.08, h, -0.08, 0.08, h + 0.5, 0.08, STEEL);                     // shield wire peak
		switch (kind) {
			case STEEL_MONOPOLE -> {
				double[][] arms = {{27, -1}, {23, 1}, {19, -1}};
				for (double[] arm : arms) {
					suspensionArm(g, arm[0], arm[1], 3.0, 2.0);
				}
			}
			case DOUBLE_CIRCUIT_MONOPOLE -> {
				for (double y : new double[] {29, 25, 21}) {
					suspensionArm(g, y, -1, 3.0, 2.0);
					suspensionArm(g, y, 1, 3.0, 2.0);
				}
			}
			default -> {
				for (double y : new double[] {33, 28.5, 24}) {
					for (int side : new int[] {-1, 1}) {
						// Braced horizontal post insulators: the compact line needs no swinging strings.
						box(g, side < 0 ? -0.45 : 0.25, y - 0.15, -0.15, side < 0 ? -0.25 : 0.45, y + 0.15, 0.15, STEEL);
						insulator(g, new Vec3(side * 0.45, y, 0), new Vec3(side * 3.35, y, 0), 0.08, 0.16, 0.16, GLAZE_GREY);
						beam(g, side * 0.3, y - 2.0, 0, side * 3.1, y - 0.12, 0, 0.1, STEEL);
						box(g, side * 3.5 - 0.15, y - 0.12, -0.12, side * 3.5 + 0.15, y + 0.12, 0.12, STEEL);
					}
				}
			}
		}
		riser(g, s, 6.0, 0.4);
	}

	/** A steel arm out to one side with an insulator string hanging from its tip, a clamp at the bottom. */
	private static void suspensionArm(Geometry g, double y, double side, double reach, double string) {
		beam(g, side * 0.3, y, 0, side * reach, y + 0.3, 0, 0.2, STEEL);
		beam(g, side * 0.3, y - 1.4, 0, side * (reach - 0.6), y + 0.2, 0, 0.1, STEEL);
		double x = side * reach;
		insulator(g, new Vec3(x, y + 0.25, 0), new Vec3(x, y - string + 0.15, 0), 0.07, 0.16, 0.16, GLAZE_TEAL);
		box(g, x - 0.14, y - string - 0.05, -0.14, x + 0.14, y - string + 0.15, 0.14, STEEL);
	}

	private static void lattice(Geometry g, State s) {
		LineKind kind = s.kind;
		double base = kind.halfWidth();
		double waist = kind.family == LineKind.Family.LATTICE ? 1.0 : 1.5;
		double h = kind.height;
		double bodyTop = switch (kind.family) {
			case UHV -> 58.5;
			case UHVDC -> 66;
			default -> h - 3;
		};
		double taper = h * 0.66;
		java.util.function.DoubleUnaryOperator half = y -> base - (base - waist) * Math.min(1.0, y / taper);
		// The body (square, so it stands the same way whichever way the line runs): four legs, X bracing, struts.
		int corner = 0;
		for (int sz : new int[] {-1, 1}) {
			for (int sx : new int[] {-1, 1}) {
				int drop = s.legDrop[corner++];
				double x = sx * base, z = sz * base;
				box(g, x - 0.7, -drop - 0.6, z - 0.7, x + 0.7, -drop + 0.4, z + 0.7, FOUNDATION);
				if (drop > 0) {
					// A leg extension down to the ground on a slope.
					box(g, x - 0.18, -drop + 0.4, z - 0.18, x + 0.18, 0, z + 0.18, LATTICE);
				}
				double prev = 0;
				for (double y = 3; prev < bodyTop; y += 3) {
					double y1 = Math.min(y, bodyTop);
					double r0 = half.applyAsDouble(prev), r1 = half.applyAsDouble(y1);
					beam(g, sx * r0, prev, sz * r0, sx * r1, y1, sz * r1, 0.3, LATTICE);
					prev = y1;
				}
			}
		}
		for (double y = 0; y < bodyTop - 0.5; y += 4) {
			double y1 = Math.min(y + 4, bodyTop);
			double r0 = half.applyAsDouble(y), r1 = half.applyAsDouble(y1);
			for (int f = 0; f < 4; f++) {
				// Corners of this face at the two heights (faces: -z, +x, +z, -x).
				double[] a0 = faceCorner(f, 0, r0), b0 = faceCorner(f, 1, r0);
				double[] a1 = faceCorner(f, 0, r1), b1 = faceCorner(f, 1, r1);
				beam(g, a0[0], y, a0[1], b1[0], y1, b1[1], 0.12, LATTICE);
				beam(g, b0[0], y, b0[1], a1[0], y1, a1[1], 0.12, LATTICE);
				if (((int) y / 4) % 2 == 0) {
					beam(g, a0[0], y, a0[1], b0[0], y, b0[1], 0.14, LATTICE);
				}
			}
		}
		// The crossarms, earth-wire peaks and strings turn to face along the line.
		g.push();
		g.yaw(s.yaw);
		g.light = s.topLight;
		double w = waist;
		switch (kind.family) {
			case LATTICE -> {
				double[][] levels = {{49.5, 6.5}, {42.5, 8.0}, {35.0, 6.5}};
				for (double[] level : levels) {
					double y = level[0], r = level[1];
					double hw = half.applyAsDouble(y);
					for (int side : new int[] {-1, 1}) {
						latticeArm(g, side, y, hw, r);
						stringWithYoke(g, side * r, y, 4.0);
						if (kind == LineKind.MONITORED_LATTICE_TOWER) {
							// A line sensor clamped to the yoke plate, its lens looking down the span.
							box(g, side * r - 0.1, y - 4.15, 0.4, side * r + 0.1, y - 4.05, 0.62, STEEL_DARK);
							g.light = LightCoordsUtil.FULL_BRIGHT;
							panel(g, side * r - 0.16, y - 4.5, 0.42, side * r + 0.16, y - 4.15, 0.72, SOUTH, SENSOR, STEEL_DARK);
							g.light = s.topLight;
						}
					}
				}
				for (int side : new int[] {-1, 1}) {
					beam(g, side * w, bodyTop, -w, side * 2.2, h, 0, 0.2, LATTICE);
					beam(g, side * w, bodyTop, w, side * 2.2, h, 0, 0.2, LATTICE);
				}
			}
			case UHV -> {
				double y = 58.5;
				for (int zz : new int[] {-1, 1}) {
					// A box-truss crossarm 28 m wide.
					beam(g, -14, y, zz, 14, y, zz, 0.25, LATTICE);
					beam(g, -14, y + 2, zz * 0.6, 14, y + 2, zz * 0.6, 0.22, LATTICE);
					for (double x = -14; x < 14; x += 2) {
						beam(g, x, y, zz, x + 2, y + 2, zz * 0.6, 0.1, LATTICE);
					}
				}
				for (int side : new int[] {-1, 1}) {
					beam(g, side * 12, y + 2, 0, side * 12.5, h - 0.5, 0, 0.22, LATTICE);    // earth-wire peaks
				}
				for (double cx : new double[] {-11, 0, 11}) {
					// V strings: two insulator strings meet at a yoke under each bundle.
					insulator(g, new Vec3(cx - 2, y - 0.1, 0), new Vec3(cx - 0.2, 51.7, 0), 0.08, 0.18, 0.18, GLAZE_TEAL);
					insulator(g, new Vec3(cx + 2, y - 0.1, 0), new Vec3(cx + 0.2, 51.7, 0), 0.08, 0.18, 0.18, GLAZE_TEAL);
					box(g, cx - 0.45, 51.4, -0.45, cx + 0.45, 51.6, 0.45, STEEL);
				}
				// The warning beacon stands on a plate across the top chords.
				box(g, -0.35, y + 2, -0.75, 0.35, y + 2.15, 0.75, LATTICE);
				g.light = LightCoordsUtil.FULL_BRIGHT;
				box(g, -0.2, y + 2.15, -0.2, 0.2, y + 2.55, 0.2, RED_LIGHT);
				g.light = s.topLight;
			}
			default -> {
				double y = 56;
				double hw = half.applyAsDouble(y);
				for (int side : new int[] {-1, 1}) {
					latticeArm(g, side, y, hw, 10);
					stringWithYoke(g, side * 10, y, 11.0);
					beam(g, side * w, bodyTop, -w, side * 3, h, 0, 0.22, LATTICE);
					beam(g, side * w, bodyTop, w, side * 3, h, 0, 0.22, LATTICE);
				}
				// A cap plate joins the leg tops; the beacon stands on a mast in its middle.
				box(g, -w - 0.2, bodyTop, -w - 0.2, w + 0.2, bodyTop + 0.25, w + 0.2, LATTICE);
				box(g, -0.12, bodyTop + 0.25, -0.12, 0.12, bodyTop + 1.4, 0.12, GALVANIZED);
				g.light = LightCoordsUtil.FULL_BRIGHT;
				box(g, -0.2, bodyTop + 1.4, -0.2, 0.2, bodyTop + 1.8, 0.2, RED_LIGHT);
				g.light = s.topLight;
			}
		}
		g.pop();
	}

	/** One corner of a body face (0 or 1) at half-width r: faces run -z, +x, +z, -x around the tower. */
	private static double[] faceCorner(int face, int which, double r) {
		double[][] corners = {{-r, -r}, {r, -r}, {r, r}, {-r, r}};
		return corners[(face + which) % 4];
	}

	/** A lattice crossarm: two lower chords from the body to the tip and a top tie. */
	private static void latticeArm(Geometry g, int side, double y, double hw, double reach) {
		beam(g, side * hw, y, -hw, side * reach, y, 0, 0.22, LATTICE);
		beam(g, side * hw, y, hw, side * reach, y, 0, 0.22, LATTICE);
		beam(g, side * hw, y + 2.2, 0, side * reach, y + 0.1, 0, 0.18, LATTICE);
		for (double t = 0.25; t < 1; t += 0.25) {
			double x = side * (hw + (reach - hw) * t);
			double zz = hw * (1 - t);
			beam(g, x, y, -zz, x, y + 2.2 * (1 - t) + 0.1 * t, 0, 0.08, LATTICE);
			beam(g, x, y, zz, x, y + 2.2 * (1 - t) + 0.1 * t, 0, 0.08, LATTICE);
		}
	}

	/** A glass insulator string hanging from an arm tip to a yoke plate {@code length} below. */
	private static void stringWithYoke(Geometry g, double x, double y, double length) {
		box(g, x - 0.12, y - 0.1, -0.12, x + 0.12, y + 0.2, 0.12, STEEL);
		insulator(g, new Vec3(x, y - 0.1, 0), new Vec3(x, y - length + 0.15, 0), 0.08, 0.18, 0.18, GLAZE_TEAL);
		box(g, x - 0.45, y - length - 0.02, -0.45, x + 0.45, y - length + 0.15, 0.45, STEEL);
	}

	private static void superconductingPylon(Geometry g, State s) {
		box(g, -1.0, -0.6, -0.8, 1.0, 0.3, 0.8, FOUNDATION);
		box(g, -0.75, 0.3, -0.5, 0.75, 19.6, 0.5, STEEL);
		for (double y = 2; y < 19; y += 4) {
			box(g, -0.78, y, -0.53, 0.78, y + 0.15, 0.53, GALVANIZED);
		}
		box(g, -3.0, 19.6, -0.35, 3.0, 20.2, 0.35, GALVANIZED);
		for (int side : new int[] {-1, 1}) {
			// Cable saddles, and a coupling sleeve where the cryogenic pipe of one span meets the next.
			box(g, side * 2.5 - 0.45, 20.2, -0.4, side * 2.5 + 0.45, 20.65, 0.4, STEEL);
			beam(g, side * 0.75, 17.5, 0, side * 2.6, 19.6, 0, 0.18, STEEL);
			lathe(g, new Vec3(side * 2.5, 21, -0.7), new Vec3(side * 2.5, 21, 0.7),
					new double[][] {{0, 0.34}, {0.08, 0.42}, {0.4, 0.42}, {0.45, 0.36}, {0.95, 0.36}, {1.0, 0.42}, {1.32, 0.42}, {1.4, 0.34}}, 8, STEEL);
		}
		g.light = LightCoordsUtil.FULL_BRIGHT;
		box(g, -0.15, 20.2, -0.15, 0.15, 20.5, 0.15, RED_LIGHT);
		g.light = s.bodyLight;
	}

	private static void pedestal(Geometry g) {
		box(g, -0.45, 0, -0.4, 0.45, 0.08, 0.4, PAD);
		// The enclosure: brushed steel, its door and meter on the front, vents on the sides, a cap on top.
		box(g, -0.34, 0.08, -0.28, 0.34, 1.5, 0.28, CABINET, STEEL, STEEL);
		panel(g, -0.24, 0.82, 0.28, 0.24, 1.3, 0.3, SOUTH, METER_FACE, CABINET);
		panel(g, -0.27, 0.16, 0.28, 0.27, 0.72, 0.3, SOUTH, CABINET_DOOR, CABINET);
		panel(g, 0.34, 0.3, -0.16, 0.355, 0.62, 0.16, EAST, VENT, CABINET);
		panel(g, -0.355, 0.3, -0.16, -0.34, 0.62, 0.16, WEST, VENT, CABINET);
		panel(g, -0.1, 1.05, -0.3, 0.1, 1.25, -0.28, NORTH, WARNING_LABEL, CABINET);
		box(g, -0.37, 1.5, -0.31, 0.37, 1.56, 0.31, STEEL);
		// The service mast and weatherhead the drop wire lands on.
		tube(g, List.of(new Vec3(0, 1.56, 0), new Vec3(0, 1.92, 0)), 0.05, 6, CONDUIT);
		lathe(g, new Vec3(0, 1.88, 0), new Vec3(0, 2.0, 0), new double[][] {{0, 0.07}, {0.08, 0.12}, {0.12, 0.12}}, 8, STEEL);
		insulator(g, new Vec3(0, 1.98, 0), new Vec3(0, 2.1, 0), 0.03, 0.055, 0.06, GLAZE_GREY);
		tube(g, List.of(new Vec3(0.42, -0.2, 0.2), new Vec3(0.42, 0.14, 0.2)), 0.02, 4, COPPER);   // ground rod
	}

	private static void padMount(Geometry g) {
		box(g, -0.5, 0, -0.5, 1.5, 0.1, 1.5, PAD);
		box(g, -0.42, 0.1, -0.42, 1.42, 1.6, 1.32, GREEN_ENCLOSURE, GREEN_ENCLOSURE, STEEL_DARK);
		box(g, -0.46, 1.6, -0.46, 1.46, 1.68, 1.36, GREEN_ENCLOSURE, GREEN_ENCLOSURE, STEEL_DARK);
		panel(g, -0.3, 0.2, 1.32, 1.1, 1.45, 1.35, SOUTH, ENCLOSURE_DOOR, GREEN_ENCLOSURE);
		panel(g, 0.72, 0.98, 1.35, 1.02, 1.28, 1.36, SOUTH, WARNING_LABEL, GREEN_ENCLOSURE);
		panel(g, 0.3, 0.7, -0.43, 0.7, 1.1, -0.42, NORTH, WARNING_LABEL, GREEN_ENCLOSURE);
		for (double z = -0.3; z < 1.2; z += 0.25) {
			box(g, 1.42, 0.3, z, 1.5, 1.4, z + 0.06, STEEL_DARK);               // cooling fins, inside the footprint
		}
		tube(g, List.of(new Vec3(-0.46, -0.3, 0.45), new Vec3(-0.46, 0.25, 0.45)), 0.06, 6, CONDUIT);   // the buried feeder rising in
	}

	/** A chain-link fence along one side of a yard, posts every two blocks, with a gap where the cabinet stands. */
	private static void fence(Geometry g, double x0, double z0, double x1, double z1, boolean gap) {
		double len = Math.hypot(x1 - x0, z1 - z0);
		double dx = (x1 - x0) / len, dz = (z1 - z0) / len;
		for (int i = 0; i < Math.round(len); i++) {
			double ax = x0 + dx * i, az = z0 + dz * i, bx = ax + dx, bz = az + dz;
			double cx = (ax + bx) / 2, cz = (az + bz) / 2;
			if (gap && Math.abs(cx) < 0.6 && Math.abs(cz) < 0.6) {
				continue;
			}
			sheet(g, ax, 0.08, az, bx, 2.08, bz, -dz, dx, CHAIN_LINK);
			sheet(g, ax, 2.08, az, bx, 2.48, bz, -dz, dx, BARBED_WIRE);
			if (i % 2 == 0) {
				box(g, ax - 0.05, 0.08, az - 0.05, ax + 0.05, 2.53, az + 0.05, GALVANIZED);
			}
		}
	}

	private static void yard(Geometry g, int r) {
		double x0 = -0.5, x1 = 2 * r + 0.5, z0 = -r - 0.5, z1 = r + 0.5;
		// A gravel bed with a concrete kerb, raised clear of the ground so it never fights the grass for depth.
		double top = 0.08;
		for (double x = x0; x < x1; x++) {
			for (double z = z0; z < z1; z++) {
				face(g, UP, x, 0, z, x + 1, top, z + 1, GRAVEL);
				if (x == x0) {
					face(g, WEST, x, 0, z, x + 1, top, z + 1, PAD);
				}
				if (x + 1 == x1) {
					face(g, EAST, x, 0, z, x + 1, top, z + 1, PAD);
				}
				if (z == z0) {
					face(g, NORTH, x, 0, z, x + 1, top, z + 1, PAD);
				}
				if (z + 1 == z1) {
					face(g, SOUTH, x, 0, z, x + 1, top, z + 1, PAD);
				}
			}
		}
		fence(g, 0, -r, 0, r, true);
		fence(g, 0, r, 2 * r, r, false);
		fence(g, 2 * r, r, 2 * r, -r, false);
		fence(g, 2 * r, -r, 0, -r, false);
		// The terminal cabinet in the fence gap: cables plug in here; its door faces out of the yard.
		box(g, -0.45, 0.08, -0.45, 0.45, 1.7, 0.45, CABINET, STEEL, STEEL);
		panel(g, -0.47, 0.2, -0.36, -0.45, 1.45, 0.36, WEST, CABINET_DOOR, CABINET);
		panel(g, 0.45, 0.9, -0.2, 0.47, 1.3, 0.2, EAST, WARNING_LABEL, CABINET);
	}

	private static void gantry(Geometry g, double x, double halfSpan, double height) {
		for (double z : new double[] {-halfSpan, halfSpan}) {
			box(g, x - 0.2, 0.08, z - 0.2, x + 0.2, height, z + 0.2, GALVANIZED);
			box(g, x - 0.5, 0.08, z - 0.5, x + 0.5, 0.35, z + 0.5, FOUNDATION);
			beam(g, x, 0.35, z, x + 1.4, 0.12, z, 0.12, GALVANIZED);                 // back stay
		}
		box(g, x - 0.2, height - 0.4, -halfSpan - 0.2, x + 0.2, height, halfSpan + 0.2, GALVANIZED);
	}

	/** A bushing on top of a piece of equipment: brown porcelain with a steel cap the jumper clamps to. */
	private static void bushing(Geometry g, double x, double y0, double y1, double z, double core, double shed) {
		box(g, x - shed, y0, z - shed, x + shed, y0 + 0.08, z + shed, STEEL);
		insulator(g, new Vec3(x, y0 + 0.08, z), new Vec3(x, y1 - 0.08, z), core, shed, 0.18, GLAZE_BROWN);
		box(g, x - core - 0.03, y1 - 0.1, z - core - 0.03, x + core + 0.03, y1, z + core + 0.03, STEEL);
	}

	private static void substation(Geometry g, State s) {
		yard(g, 5);
		gantry(g, 1.5, 3, 10.8);
		for (int k = 0; k < 3; k++) {
			double z = (k - 1) * 2;
			double hv = -0.9 + 0.55 * k;   // where this phase's bushing stands on the transformer
			// The line lands on a string under the gantry beam; a jumper drops to the breaker, then on to the transformer.
			box(g, 1.38, 10.3, z - 0.12, 1.62, 10.45, z + 0.12, STEEL);
			insulator(g, new Vec3(1.5, 10.3, z), new Vec3(1.5, 8.15, z), 0.08, 0.18, 0.18, GLAZE_TEAL);
			box(g, 1.36, 7.9, z - 0.14, 1.64, 8.15, z + 0.14, STEEL);
			tube(g, List.of(new Vec3(1.5, 7.9, z), new Vec3(2.2, 5.6, z), new Vec3(3.1, 3.7, z), new Vec3(3.5, 3.3, z)), 0.04, 4, CONDUCTOR);
			box(g, 3.2, 0.08, z - 0.3, 3.8, 1.6, z + 0.3, BREAKER, STEEL, STEEL);
			panel(g, 3.19, 0.3, z - 0.2, 3.2, 0.7, z + 0.2, WEST, WARNING_LABEL, BREAKER);
			bushing(g, 3.5, 1.6, 3.3, z, 0.1, 0.2);
			tube(g, List.of(new Vec3(3.5, 3.3, z), new Vec3(4.4, 3.95, (z + hv) / 2), new Vec3(5.6, 3.7, hv)), 0.04, 4, CONDUCTOR);
			bushing(g, 5.6, 2.75, 3.7, hv, 0.09, 0.19);
			bushing(g, 7.2, 2.75, 3.25, -0.6 + 0.6 * k, 0.06, 0.12);
		}
		// The transformer: tank, radiators, conservator and bushings.
		box(g, 5.0, 0.08, -1.6, 7.5, 2.6, 1.6, TRANSFORMER_TANK, STEEL_DARK, STEEL_DARK);
		box(g, 4.9, 2.6, -1.7, 7.6, 2.75, 1.7, STEEL_DARK);
		for (int side : new int[] {-1, 1}) {
			double zIn = side * 1.6, zOut = side * 1.98;
			box(g, 5.2, 0.3, Math.min(zIn, zOut), 7.3, 2.3, Math.max(zIn, zOut), RADIATOR, STEEL_DARK, STEEL_DARK);
		}
		// The conservator tank along the far edge of the lid, on two saddles.
		lathe(g, new Vec3(5.3, 3.2, 1.15), new Vec3(7.2, 3.2, 1.15), new double[][] {{0, 0.3}, {0.05, 0.34}, {1.85, 0.34}, {1.9, 0.3}}, 8, STEEL);
		box(g, 5.45, 2.75, 1.07, 5.6, 3.2, 1.23, STEEL);
		box(g, 6.9, 2.75, 1.07, 7.05, 3.2, 1.23, STEEL);
		panel(g, 5.9, 0.9, 1.98, 6.3, 1.3, 1.99, SOUTH, WARNING_LABEL, STEEL_DARK);
		if (s.cryoTermination) {
			// A superconducting line's cryogenic pipes end in two termination vessels by the front fence.
			for (double z : new double[] {-4, 4}) {
				box(g, 0.4, 0.08, z - 0.6, 1.6, 0.2, z + 0.6, PAD);
				lathe(g, new Vec3(1.0, 0.2, z), new Vec3(1.0, 3.4, z), new double[][] {{0, 0.5}, {0.1, 0.42}, {2.6, 0.42}, {2.7, 0.5}, {2.95, 0.5},
						{3.0, 0.38}, {3.25, 0.38}}, 8, STEEL);
				g.light = LightCoordsUtil.FULL_BRIGHT;
				lathe(g, new Vec3(1.0, 1.0, z), new Vec3(1.0, 1.4, z), new double[][] {{0, 0.43}, {0.4, 0.43}}, 8, CRYO);
				g.light = s.bodyLight;
				tube(g, List.of(new Vec3(1.0, 1.2, z), new Vec3(2.2, 1.2, z), new Vec3(2.2, 0.0, z)), 0.1, 6, CRYO_JACKET);   // nitrogen return
			}
		}
	}

	private static void converterStation(Geometry g) {
		yard(g, 7);
		gantry(g, 1.5, 5, 12.8);
		for (double z : new double[] {-4, 4}) {
			box(g, 1.36, 12.25, z - 0.14, 1.64, 12.45, z + 0.14, STEEL);
			insulator(g, new Vec3(1.5, 12.25, z), new Vec3(1.5, 12.0, z), 0.09, 0.2, 0.2, GLAZE_TEAL);
			box(g, 1.34, 11.8, z - 0.16, 1.66, 12.05, z + 0.16, STEEL);
			// The jumper into a wall bushing through the valve hall wall.
			tube(g, List.of(new Vec3(1.5, 11.8, z), new Vec3(1.7, 9.8, z), new Vec3(1.95, 9.0, z)), 0.06, 6, CONDUCTOR);
			box(g, 1.85, 8.85, z - 0.16, 2.0, 9.15, z + 0.16, STEEL);
			insulator(g, new Vec3(2.0, 9.0, z), new Vec3(2.6, 9.0, z), 0.1, 0.22, 0.2, GLAZE_BROWN);
		}
		box(g, 2.6, 0.08, -4.4, 11.4, 11.6, 4.4, VALVE_HALL, STEEL, STEEL_DARK);
		box(g, 2.5, 11.6, -4.5, 11.5, 11.9, 4.5, STEEL);
		panel(g, 6.0, 0.08, 4.4, 7.4, 2.6, 4.42, SOUTH, CABINET_DOOR, VALVE_HALL);
		panel(g, 6.4, 2.8, 4.4, 7.0, 3.4, 4.41, SOUTH, WARNING_LABEL, VALVE_HALL);
		for (double x = 3.5; x < 11; x += 2.5) {
			panel(g, x, 9.5, -4.41, x + 1.2, 10.3, -4.4, NORTH, VENT, VALVE_HALL);              // ventilation louvres
		}
	}

	// ------------------------------------------------------------------ wires

	private static void drawWire(Geometry g, float[] w, int load) {
		Vec3 a = new Vec3(w[0], w[1], w[2]);
		Vec3 b = new Vec3(w[3], w[4], w[5]);
		LineWires.Wire wire = new LineWires.Wire(a, b, w[6]);
		int bundle = (int) w[7];
		boolean carbon = w[8] > 0;
		boolean supercon = w[10] > 0;
		// Across-the-line and up directions, for the sub-conductors of a bundle.
		double dx = b.x - a.x, dz = b.z - a.z;
		double len = Math.max(1e-6, Math.hypot(dx, dz));
		double px = -dz / len, pz = dx / len;
		double[][] offsets = bundleOffsets(bundle);
		int segments = Math.max(8, Math.min(48, (int) (a.distanceTo(b) / 2)));
		float[] uv = supercon ? CRYO_JACKET : carbon ? CARBON_CONDUCTOR : CONDUCTOR;
		double radius = supercon ? 0.3 : bundle > 1 ? 0.035 : 0.045;
		// A loaded line warms: a faint warm sheen grows with the load.
		g.color = 0xFF000000 | 255 << 16 | (255 - load * 12 / 255) << 8 | (255 - load * 30 / 255);
		for (double[] o : offsets) {
			double ox = px * o[0], oy = o[1], oz = pz * o[0];
			List<Vec3> points = new ArrayList<>();
			for (int i = 0; i <= segments; i++) {
				points.add(wire.at(i / (double) segments).add(ox, oy, oz));
			}
			tube(g, points, radius, supercon ? 8 : 4, uv);
		}
		if (bundle > 1) {
			// Spacers hold the sub-conductors apart every few metres.
			double r = Math.abs(offsets[0][0]) + 0.06;
			for (int i = 1; i < 6; i++) {
				Vec3 p = wire.at(i / 6.0);
				box(g, p.x - 0.04 - Math.abs(px) * r, p.y - r, p.z - 0.04 - Math.abs(pz) * r, p.x + 0.04 + Math.abs(px) * r, p.y + r,
						p.z + 0.04 + Math.abs(pz) * r, STEEL);
			}
		}
		if (supercon) {
			// The liquid nitrogen inside glows through the inspection stripe while the line is cold and carrying power.
			g.light = LightCoordsUtil.FULL_BRIGHT;
			List<Vec3> points = new ArrayList<>();
			for (int i = 0; i <= segments; i++) {
				points.add(wire.at(i / (double) segments).add(0, 0.27, 0));
			}
			tube(g, points, 0.06, 4, load > 0 ? CRYO : CRYO_JACKET);
		}
		g.color = 0xFFFFFFFF;
	}

	/** Sub-conductor positions in a bundle: {across the line, up}, in blocks. */
	private static double[][] bundleOffsets(int bundle) {
		if (bundle <= 1) {
			return new double[][] {{0, 0}};
		}
		double radius = bundle == 4 ? 0.23 : bundle == 6 ? 0.3 : 0.36;
		double[][] out = new double[bundle][];
		for (int i = 0; i < bundle; i++) {
			double angle = Math.PI * 2 * i / bundle + (bundle == 4 ? Math.PI / 4 : 0);
			out[i] = new double[] {Math.cos(angle) * radius, Math.sin(angle) * radius};
		}
		return out;
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
