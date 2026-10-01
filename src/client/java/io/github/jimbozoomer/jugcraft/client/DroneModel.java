package io.github.jimbozoomer.jugcraft.client;

import net.minecraft.util.LightCoordsUtil;

/**
 * The drones' 3D shapes, built from boxes and rotor discs on one 256x256 texture
 * ({@code textures/entity/drone_depot.png}, drawn by tools/drone_textures.py). They follow the Drone Tower's
 * look: dark matte military hulls in the material of the tower tier that unlocks each drone, and dull red
 * lights only. Sizes match the dock slots: small drones fit a quarter of a pad, medium ones half a pad,
 * large ones a whole 5x5 pad (and each fits its hangar in the tower).
 *
 * <ul>
 * <li>1 Courier Quad (small): gunmetal body, four arms in an X.
 * <li>2 Survey Hexacopter (small): dark steel with a hazard band, six arms, a sensor dome underneath.
 * <li>3 Lifter Octocopter (small): steel armour plate, eight arms.
 * <li>4 Ducted-Fan Runner (medium): low aluminium wedge hull, four shrouded fans, skids, chin sensor.
 * <li>5 Tiltrotor Carrier (medium): tungsten-steel fuselage, high wing, nacelles that tilt forward to
 * cruise, V-tail, cargo door.
 * <li>6 Tandem Freighter (medium): carbon-composite cargo hull, two 4-blade rotors fore and aft, rear ramp.
 * <li>7 Hybrid Aerostat (large): an armoured silicon-carbide lifting body, X tail, four vectored fans,
 * gondola.
 * <li>8 Ion-Wind Glider (large): a depleted-uranium stealth flying wing with red ion emitter edges and
 * flush lift-fan grilles.
 * <li>9 Superconducting Ring Lifter (large): a hexagonal graphene ring hull with six lift fans and a glowing
 * coil band, and an armoured cargo pod slung in the middle.
 * </ul>
 * The geometry matches the concept renders in tools/drone_concepts.py and tools/t9_options.py.
 */
final class DroneModel {
	/** One texel of the 256x256 sheet. */
	private static final float PX = 1 / 256f;

	/** A 16x16 region of the old part of the sheet, by column and row. */
	private static float[] region(int col, int row) {
		return new float[] {col * 16 * PX, row * 16 * PX, (col + 1) * 16 * PX, (row + 1) * 16 * PX};
	}

	/** A 32x32 fleet region at pixel (x, y) (tools/drone_textures.py FLEET_ORDER and fleet_slot). */
	private static float[] fleet(int x, int y) {
		return new float[] {x * PX, y * PX, (x + 32) * PX, (y + 32) * PX};
	}

	static final float[] ROTOR = region(1, 1);
	static final float[] CRATE = region(2, 1);
	static final float[] HATCH = region(3, 1);
	static final float[] LIFT = region(4, 1);
	static final float[] ARM = region(5, 1);
	static final float[] SHAFT = region(7, 1);
	static final float[] WHITE = region(0, 2);

	// The fleet regions (names as in tools/drone_textures.py).
	static final float[] ALU = fleet(128, 0);
	static final float[] ALU_DARK = fleet(160, 0);
	static final float[] TUNGSTEN = fleet(192, 0);
	static final float[] TUNGSTEN_DARK = fleet(224, 0);
	static final float[] CARBON = fleet(128, 32);
	static final float[] CARBON_DARK = fleet(160, 32);
	static final float[] SIC = fleet(192, 32);
	static final float[] SIC_DARK = fleet(224, 32);
	static final float[] DU = fleet(128, 64);
	static final float[] DU_DARK = fleet(160, 64);
	static final float[] GRAPHENE = fleet(192, 64);
	static final float[] GRAPHENE_DARK = fleet(224, 64);
	static final float[] STEEL = fleet(128, 96);
	static final float[] BLACK = fleet(160, 96);
	static final float[] GLASS = fleet(192, 96);
	static final float[] SENSOR = fleet(224, 96);
	static final float[] VENT = fleet(128, 128);
	static final float[] HAZARD = fleet(160, 128);
	static final float[] GLOW = fleet(192, 128);
	static final float[] CONDUIT = fleet(224, 128);
	static final float[] GUNMETAL = fleet(128, 160);
	static final float[] GUNMETAL_DARK = fleet(160, 160);
	static final float[] ARMOR_PLATE = fleet(192, 160);
	static final float[] FRAME = fleet(224, 160);
	static final float[] BLADES2 = fleet(128, 192);
	static final float[] BLADES3 = fleet(160, 192);
	static final float[] BLADES4 = fleet(192, 192);
	static final float[] BLADES5 = fleet(224, 192);
	static final float[] BLADES6 = fleet(128, 224);

	/** The tower's dull red (navigation and status lights, emitters, coils) and its brighter warning red. */
	static final int DULL_RED = 0xFF96261E;
	static final int WARN_RED = 0xFFEB3E2C;

	/** Crate edge per tier (bigger drones carry bigger crates). */
	private static final float[] CRATE_SIZES = {0.34f, 0.4f, 0.5f, 0.5f, 0.7f, 0.9f, 1.2f, 1.2f, 1.5f};
	static final float CRATE_SIZE = CRATE_SIZES[0];

	private DroneModel() {
	}

	/** Heading of a docked drone: the Tandem Freighter lies along its half-pad, the rest face south. */
	static float dockYaw(int tier) {
		return tier == 6 ? (float) (Math.PI / 2) : 0;
	}

	/**
	 * One drone. {@code (x, y, z)} is the middle of its underside, {@code yaw} its heading, {@code cable}
	 * the length of the winch cable with a crate on it (negative: no crate), {@code spin} the rotor angle
	 * (0 when resting), {@code cruising} 0 to 1 how far into forward flight it is.
	 */
	static void draw(DroneDepotRenderer.Geometry g, float x, float y, float z, float yaw, int tier, float cable, float spin, int light,
			float cruising) {
		g.reset();
		g.light = light;
		g.color = 0xFFFFFFFF;
		g.translate(x, y, z).yaw(yaw);
		switch (tier) {
			case 2 -> multirotor(g, 6, 1f, GUNMETAL_DARK, GUNMETAL, HAZARD, true, spin);
			case 3 -> multirotor(g, 8, 1.15f, ARMOR_PLATE, GUNMETAL_DARK, GUNMETAL_DARK, false, spin);
			case 4 -> runner(g, spin);
			case 5 -> tiltrotor(g, spin, cruising);
			case 6 -> tandem(g, spin);
			case 7 -> aerostat(g, spin);
			case 8 -> glider(g, spin);
			case 9 -> ringLifter(g, spin);
			case 10 -> multirotor(g, 4, 1f, HAZARD, GLOW, GLOW, false, spin); // the Creative Drone: a gold quad
			default -> multirotor(g, 4, 1f, GUNMETAL, GUNMETAL_DARK, null, false, spin);
		}
		if (cable > 0) {
			float size = CRATE_SIZES[Math.max(0, Math.min(CRATE_SIZES.length - 1, tier - 1))];
			float top = -cable;
			g.color = 0xFF202226;
			g.box(-0.012, top, -0.012, 0.012, 0.05, 0.012, WHITE);
			g.color = 0xFFFFFFFF;
			g.box(-size / 2, top - size, -size / 2, size / 2, top, size / 2, CRATE);
		}
		g.reset();
	}

	private static void rotor(DroneDepotRenderer.Geometry g, double x, double y, double z, double radius, double angle, float[] uv) {
		g.push();
		g.translate(x, y, z).yaw(angle);
		g.disc(0, 0, 0, radius, uv);
		g.pop();
	}

	/** A square duct ring of half-size {@code r} and height {@code h} round (x, y, z). */
	private static void duct(DroneDepotRenderer.Geometry g, double x, double y, double z, double r, double h, double wall, float[] uv) {
		g.box(x - r, y, z - r, x + r, y + h, z - r + wall, uv);
		g.box(x - r, y, z + r - wall, x + r, y + h, z + r, uv);
		g.box(x - r, y, z - r, x - r + wall, y + h, z + r, uv);
		g.box(x + r - wall, y, z - r, x + r, y + h, z + r, uv);
	}

	private static void glow(DroneDepotRenderer.Geometry g, int argb, Runnable part) {
		int light = g.light;
		g.light = LightCoordsUtil.FULL_BRIGHT;
		g.color = argb;
		part.run();
		g.color = 0xFFFFFFFF;
		g.light = light;
	}

	/** A small glowing light, centred on (x, y, z). */
	private static void light(DroneDepotRenderer.Geometry g, double x, double y, double z, double s, int argb) {
		glow(g, argb, () -> g.centred(x, y, z, s, s, s, GLOW));
	}

	// ------------------------------------------------------------------ small multirotors (tiers 1-3)

	private static void multirotor(DroneDepotRenderer.Geometry g, int arms, float s, float[] hull, float[] dark, float[] accent,
			boolean dome, float spin) {
		g.push();
		g.scale(s);
		g.centred(0, 0.115, 0, 0.30, 0.11, 0.38, hull);
		g.centred(0, 0.175, -0.02, 0.22, 0.02, 0.26, dark);
		if (accent != null) {
			g.centred(0, 0.115, 0, 0.305, 0.02, 0.385, accent);
		}
		g.box(-0.13, 0, -0.13, -0.10, 0.06, 0.13, FRAME);
		g.box(0.10, 0, -0.13, 0.13, 0.06, 0.13, FRAME);
		if (dome) {
			g.box(-0.06, 0.02, 0.02, 0.06, 0.07, 0.14, SENSOR);
		}
		g.centred(0, 0.09, 0.18, 0.07, 0.06, 0.05, SENSOR);
		glow(g, spin == 0 ? DULL_RED : WARN_RED, () -> g.box(-0.03, 0.12, 0.19, 0.03, 0.15, 0.205, GLOW));
		double reach = arms == 8 ? 0.36 : 0.34;
		double blade = arms == 8 ? 0.13 : 0.15;
		for (int k = 0; k < arms; k++) {
			double angle = 2 * Math.PI * k / arms + (arms == 4 ? Math.PI / 4 : 0);
			g.push();
			g.yaw(angle);
			g.box(-0.018, 0.118, 0, 0.018, 0.142, reach, FRAME);
			g.box(-0.03, 0.11, reach - 0.03, 0.03, 0.165, reach + 0.03, dark);
			rotor(g, 0, 0.17, reach, blade, spin * (k % 2 == 0 ? 1 : -1) + k, BLADES2);
			g.pop();
		}
		g.pop();
	}

	// ------------------------------------------------------------------ medium (tiers 4-6)

	/** Tier 4: about 1.9 blocks across; a low wedge hull and four shrouded fans on stub pylons. */
	private static void runner(DroneDepotRenderer.Geometry g, float spin) {
		g.centred(0, 0.30, 0, 0.62, 0.22, 1.0, ALU);
		g.centred(0, 0.44, -0.08, 0.46, 0.08, 0.7, ALU_DARK);
		g.centred(0, 0.27, 0.57, 0.42, 0.14, 0.16, ALU_DARK);
		g.centred(0, 0.24, 0.68, 0.24, 0.08, 0.08, ALU_DARK);
		g.centred(0, 0.14, 0.46, 0.13, 0.11, 0.13, SENSOR);
		g.centred(0, 0.49, -0.42, 0.03, 0.22, 0.03, STEEL);
		for (int sx = -1; sx <= 1; sx += 2) {
			g.box(sx * 0.26 - 0.025, 0, -0.38, sx * 0.26 + 0.025, 0.04, 0.38, BLACK);
			for (double sz : new double[] {-0.25, 0.25}) {
				g.centred(sx * 0.26, 0.12, sz, 0.035, 0.18, 0.035, BLACK);
			}
			for (int sz = -1; sz <= 1; sz += 2) {
				double fx = sx * 0.66, fz = sz * 0.5;
				g.box(Math.min(sx * 0.3, fx), 0.3, fz - 0.05, Math.max(sx * 0.3, fx), 0.36, fz + 0.05, ALU_DARK);
				duct(g, fx, 0.22, fz, 0.29, 0.2, 0.05, ALU_DARK);
				rotor(g, fx, 0.31, fz, 0.25, sx * sz * (0.6 + spin * 1.3), BLADES4);
				g.centred(fx, 0.31, fz, 0.08, 0.04, 0.08, STEEL);
			}
			light(g, sx * 0.95, 0.36, 0.5, 0.05, DULL_RED);
		}
		light(g, 0, 0.6, -0.42, 0.05, WARN_RED);
	}

	/** Tier 5: tiltrotor transport, 2.3-block wing; the nacelles tilt forward as it cruises. */
	private static void tiltrotor(DroneDepotRenderer.Geometry g, float spin, float cruising) {
		g.centred(0, 0.36, 0, 0.44, 0.36, 1.6, TUNGSTEN);
		g.centred(0, 0.58, 0.05, 0.3, 0.1, 1.1, TUNGSTEN_DARK);
		g.centred(0, 0.33, 0.87, 0.34, 0.26, 0.16, TUNGSTEN);
		g.centred(0, 0.31, 1.0, 0.22, 0.18, 0.12, TUNGSTEN_DARK);
		g.centred(0, 0.17, 0.82, 0.12, 0.1, 0.12, SENSOR);
		g.centred(0, 0.18, -0.2, 0.3, 0.02, 0.6, HAZARD);
		g.centred(0, 0.4, -0.95, 0.3, 0.24, 0.32, TUNGSTEN);
		g.centred(0, 0.64, 0.12, 2.3, 0.06, 0.4, TUNGSTEN_DARK);
		for (int side = -1; side <= 1; side += 2) {
			g.push();
			g.translate(side * 0.12, 0.5, -0.98).roll(-side * 0.6);
			g.centred(0, 0.2, 0, 0.04, 0.42, 0.3, TUNGSTEN_DARK);
			g.pop();
			g.push();
			g.translate(side * 1.18, 0.66, 0.12).pitch(cruising * Math.PI / 2);
			g.centred(0, 0, 0, 0.2, 0.2, 0.55, TUNGSTEN);
			g.centred(0, 0, 0.3, 0.14, 0.14, 0.08, TUNGSTEN_DARK);
			rotor(g, 0, 0.16, 0.06, 0.6, side * (0.3 + spin * 1.4), BLADES3);
			g.centred(0, 0.14, 0.06, 0.08, 0.06, 0.08, STEEL);
			light(g, 0, 0, -0.29, 0.05, DULL_RED);
			g.pop();
			g.centred(side * 0.18, 0.08, 0.5, 0.05, 0.16, 0.05, BLACK);
		}
		g.centred(0, 0.08, -0.55, 0.05, 0.16, 0.05, BLACK);
		light(g, 0, 0.74, -1.08, 0.05, WARN_RED);
	}

	/** Tier 6: a 2.5-block carbon-composite freighter with two 4-blade rotors, front low and rear high. */
	private static void tandem(DroneDepotRenderer.Geometry g, float spin) {
		g.centred(0, 0.5, 0, 0.72, 0.6, 2.2, CARBON);
		g.centred(0, 0.86, -0.05, 0.44, 0.12, 1.7, CARBON_DARK);
		g.centred(0, 0.44, 1.16, 0.62, 0.46, 0.14, CARBON);
		g.centred(0, 0.4, 1.26, 0.46, 0.32, 0.08, CARBON_DARK);
		g.centred(0, 0.2, 1.08, 0.14, 0.12, 0.14, SENSOR);
		g.centred(0, 0.32, -1.13, 0.62, 0.36, 0.06, CARBON_DARK);
		g.centred(0, 0.15, -1.14, 0.62, 0.04, 0.07, HAZARD);
		g.centred(0, 0.98, 0.92, 0.32, 0.26, 0.36, CARBON_DARK);
		g.centred(0, 1.1, -0.92, 0.42, 0.52, 0.5, CARBON);
		g.centred(0, 1.0, -0.6, 0.3, 0.2, 0.3, VENT);
		for (int sx = -1; sx <= 1; sx += 2) {
			g.centred(sx * 0.42, 0.3, 0.15, 0.14, 0.24, 1.1, CARBON_DARK);
			for (double wz : new double[] {0.75, -0.7}) {
				g.centred(sx * 0.42, 0.08, wz, 0.08, 0.16, 0.16, BLACK);
			}
			light(g, sx * 0.5, 0.42, 0.72, 0.05, DULL_RED);
		}
		rotor(g, 0, 1.13, 0.92, 0.84, 0.2 + spin * 1.2, BLADES4);
		rotor(g, 0, 1.38, -0.92, 0.84, 0.6 - spin * 1.2, BLADES4);
		g.centred(0, 1.12, 0.92, 0.1, 0.06, 0.1, STEEL);
		g.centred(0, 1.37, -0.92, 0.1, 0.06, 0.1, STEEL);
		light(g, 0, 1.4, -0.6, 0.06, WARN_RED);
	}

	// ------------------------------------------------------------------ large (tiers 7-9), one whole pad

	/** Tier 7: a 4.2-block armoured lifting-body airship with an X tail, gondola and four vectored fans. */
	private static void aerostat(DroneDepotRenderer.Geometry g, float spin) {
		g.centred(0, 1.45, 0, 2.3, 0.86, 3.7, SIC);
		g.centred(0, 1.45, 0, 1.7, 1.25, 4.0, SIC);
		g.centred(0, 1.5, 0.05, 1.1, 1.5, 4.2, SIC_DARK);
		for (double z : new double[] {-1.2, 0, 1.2}) {
			g.centred(0, 1.45, z, 2.34, 0.9, 0.12, SIC_DARK);
			g.centred(0, 1.45, z, 1.74, 1.29, 0.12, SIC_DARK);
		}
		for (int sx = -1; sx <= 1; sx += 2) {
			double side = sx;
			glow(g, DULL_RED, () -> g.centred(side * 1.16, 1.45, 0, 0.02, 0.05, 2.8, GLOW));
		}
		for (double roll : new double[] {0.75, -0.75, 2.39, -2.39}) {
			g.push();
			g.translate(0, 1.45, -1.75).roll(roll);
			g.centred(0, 0.9, 0, 0.07, 0.75, 0.62, SIC_DARK);
			g.pop();
		}
		g.centred(0, 0.62, 0.25, 0.62, 0.4, 1.4, SIC_DARK);
		g.centred(0, 0.36, 0.7, 0.18, 0.14, 0.18, SENSOR);
		g.centred(0, 0.62, 0.97, 0.46, 0.28, 0.06, GLASS);
		g.box(-0.04, 0.8, -0.3, 0.04, 1.05, 0.7, STEEL);
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				double px = sx * 1.55, pz = sz * 1.15;
				g.box(Math.min(sx * 1.0, px), 1.05, pz - 0.05, Math.max(sx * 1.0, px), 1.12, pz + 0.05, STEEL);
				duct(g, px, 0.92, pz, 0.32, 0.3, 0.06, SIC_DARK);
				rotor(g, px, 1.06, pz, 0.28, sx * sz * (0.5 + spin * 1.3), BLADES5);
				light(g, px + sx * 0.33, 1.07, pz, 0.05, DULL_RED);
			}
		}
		g.box(-0.3, 0, -0.3, 0.3, 0.42, 0.3, BLACK);
		light(g, 0, 2.27, 0.2, 0.08, WARN_RED);
	}

	/** Tier 8: a 4.6-block stealth flying wing with red ion emitter edges and flush lift-fan grilles. */
	private static void glider(DroneDepotRenderer.Geometry g, float spin) {
		g.centred(0, 0.42, 0.25, 1.0, 0.3, 2.0, DU);
		g.centred(0, 0.6, 0.35, 0.6, 0.1, 1.2, DU_DARK);
		g.centred(0, 0.4, 1.3, 0.6, 0.2, 0.14, DU);
		g.centred(0, 0.38, 1.42, 0.3, 0.14, 0.1, DU_DARK);
		g.centred(0, 0.22, 1.05, 0.12, 0.1, 0.12, SENSOR);
		int emitter = spin == 0 ? DULL_RED : WARN_RED;
		for (int side = -1; side <= 1; side += 2) {
			for (int i = 0; i < 4; i++) {
				double cx = side * (0.72 + i * 0.45);
				double cz = 0.2 - i * 0.24;
				double chord = 1.5 - i * 0.24;
				g.centred(cx, 0.44, cz, 0.47, 0.08, chord, DU);
				double lead = cz + chord / 2;
				glow(g, emitter, () -> g.centred(cx, 0.47, lead - 0.03, 0.46, 0.03, 0.04, GLOW));
				g.centred(cx, 0.44, cz - chord / 2 + 0.05, 0.47, 0.09, 0.1, DU_DARK);
			}
			g.centred(side * 1.0, 0.485, 0.1, 0.42, 0.01, 0.42, VENT);
			g.centred(side * 1.8, 0.485, -0.25, 0.42, 0.01, 0.42, VENT);
			g.push();
			g.translate(side * 2.2, 0.46, -0.55).roll(-side * 0.5);
			g.centred(0, 0.17, 0, 0.05, 0.34, 0.4, DU_DARK);
			g.pop();
			light(g, side * 2.22, 0.48, -0.32, 0.05, WARN_RED);
			g.box(side * 0.47 - 0.03, 0, 0.1, side * 0.47 + 0.03, 0.28, 0.16, BLACK);
		}
		g.box(-0.03, 0, 0.9, 0.03, 0.28, 0.96, BLACK);
	}

	/**
	 * Tier 9: a 4.6-block hexagonal ring hull of graphene with six lift fans set into it and a glowing YBCO
	 * coil band round it, and an armoured cargo pod slung in the open middle on three struts.
	 */
	private static void ringLifter(DroneDepotRenderer.Geometry g, float spin) {
		for (int k = 0; k < 6; k++) {
			g.push();
			g.yaw(k * Math.PI / 3);
			g.centred(0, 1.15, 1.75, 1.95, 0.42, 0.7, GRAPHENE);
			g.centred(0, 1.4, 1.75, 1.7, 0.08, 0.5, GRAPHENE_DARK);
			g.centred(0, 1.445, 1.75, 0.62, 0.01, 0.42, VENT);
			rotor(g, 0, 1.44, 1.75, 0.27, k + spin * (k % 2 == 0 ? 1.5 : -1.5), BLADES5);
			glow(g, DULL_RED, () -> g.centred(0, 1.05, 2.105, 1.9, 0.05, 0.02, CONDUIT));
			g.pop();
		}
		for (int k = 0; k < 3; k++) {
			g.push();
			g.yaw(k * 2 * Math.PI / 3 + 0.5);
			g.push();
			g.translate(0, 1.0, 0.9).pitch(-0.75);
			g.centred(0, 0, 0, 0.08, 0.08, 1.2, STEEL);
			g.pop();
			g.centred(0, 0.35, 1.65, 0.1, 0.7, 0.1, GRAPHENE_DARK);
			g.centred(0, 0.02, 1.65, 0.28, 0.04, 0.28, FRAME);
			g.pop();
		}
		g.centred(0, 0.62, 0, 0.9, 0.5, 0.9, GRAPHENE);
		g.centred(0, 0.9, 0, 0.6, 0.1, 0.6, GRAPHENE_DARK);
		g.centred(0, 0.32, 0, 0.6, 0.1, 0.6, HAZARD);
		light(g, 0, 1.0, 0, 0.08, WARN_RED);
	}
}
