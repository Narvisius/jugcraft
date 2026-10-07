package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.NuclearRenderer.*;

import io.github.jimbozoomer.jugcraft.client.NuclearRenderer.Frame;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import net.minecraft.world.phys.Vec3;

/**
 * The pre-made plants' building equipment (docs/features/nuclear.md, "Pre-made plants"): pieces that only have to look
 * right, drawn in 3D like the working equipment. Each is written in its own frame (x to the right, y up, z away, in
 * blocks from the anchor's corner) with its front at z = 0, toward whoever placed it. {@link #draw} is the body,
 * {@link #glow} the parts drawn at full brightness (screens, lamps, the water over spent fuel) and {@link #blink} the
 * few lamps that change with time.
 */
final class PlantDecor {
	private PlantDecor() {
	}

	static void draw(EquipmentKind kind, Frame f) {
		switch (kind) {
			case POLAR_CRANE -> bridgeCrane(f, 40, 5, 5, 26);
			case OVERHEAD_CRANE -> bridgeCrane(f, 40, 5, 4, 9);
			case REFUELING_MACHINE -> refuelingMachine(f);
			case FUEL_HANDLING_MACHINE -> fuelHandlingMachine(f);
			case MOISTURE_SEPARATOR_REHEATER -> moistureSeparatorReheater(f);
			case FEEDWATER_HEATER -> feedwaterHeater(f);
			case EMERGENCY_DIESEL_GENERATOR -> dieselGenerator(f);
			case STORAGE_TANK -> storageTank(f);
			case HEAT_EXCHANGER -> heatExchanger(f);
			case PUMP_SKID -> pumpSkid(f);
			case SPENT_FUEL_RACK -> fuelRack(f);
			case DRY_STORAGE_CASK -> dryCask(f);
			case TRAVELLING_WATER_SCREEN -> waterScreen(f);
			case PLANT_DISPLAY_WALL -> displayWall(f);
			case MAIN_CONTROL_BOARD -> controlBoard(f);
			case SWITCHGEAR_LINEUP -> switchgear(f);
			case AIR_HANDLING_UNIT -> airHandler(f);
			default -> {
			}
		}
	}

	static void glow(EquipmentKind kind, Frame f) {
		switch (kind) {
			case SPENT_FUEL_RACK -> {
				// The assemblies' tops seen through glowing water, and a haze of blue over them.
				f.quad(v(0.1, 3.86, 3.9), v(0.1, 3.86, 0.1), v(3.9, 3.86, 0.1), v(3.9, 3.86, 3.9), v(0, 1, 0),
						FUEL_RACK[0], FUEL_RACK[1], FUEL_RACK[2], FUEL_RACK[3]);
				if (f.detail) {
					for (int side = 0; side < 4; side++) {
						haze(f, side);
					}
				}
			}
			case PLANT_DISPLAY_WALL -> {
				// Three by two screens in their bezels.
				for (int i = 0; i < 3; i++) {
					for (int j = 0; j < 2; j++) {
						double x0 = 0.35 + i * 2.8, y0 = 0.55 + j * 1.6;
						front(f, x0, y0, x0 + 2.7, y0 + 1.5, 0.26, i == 1 && j == 1 ? DISPLAY : (i + j) % 2 == 0 ? SCREEN : DISPLAY);
					}
				}
			}
			case MAIN_CONTROL_BOARD -> {
				// Two monitors on the board, the lit indicator rows of the mimic, the desk's push buttons.
				front(f, 0.6, 1.95, 1.9, 2.75, 1.17, SCREEN);
				front(f, 4.1, 1.95, 5.4, 2.75, 1.17, DISPLAY);
				for (int k = 0; k < 6; k++) {
					front(f, 0.4 + k * 0.9, 2.82, 0.75 + k * 0.9, 2.92, 1.17, k % 3 == 2 ? LAMP_RED : LAMP_GREEN);
				}
			}
			case SWITCHGEAR_LINEUP -> {
				for (int k = 0; k < 4; k++) {
					double x = k * 2 + 1.0;
					front(f, x - 0.35, 2.3, x - 0.15, 2.45, 0.06, LAMP_GREEN);
					front(f, x + 0.15, 2.3, x + 0.35, 2.45, 0.06, k == 2 ? LAMP_RED : LAMP_GREEN);
				}
			}
			// The local panel's screen (its lamps are live).
			case EMERGENCY_DIESEL_GENERATOR -> front(f, 0.4, 1.7, 1.1, 2.2, 0.195, SCREEN);
			case AIR_HANDLING_UNIT -> front(f, 3.0, 2.42, 3.3, 2.55, 0.18, LAMP_GREEN);
			case REFUELING_MACHINE, FUEL_HANDLING_MACHINE -> {
				// The operator's screen on the trolley.
				double x = kind == EquipmentKind.REFUELING_MACHINE ? 7.5 : 7.0, top = kind == EquipmentKind.REFUELING_MACHINE ? 7.5 : 6.5;
				front(f, x - 0.92, top + 0.85, x - 0.18, top + 1.25, 0.61, SCREEN);
			}
			default -> {
			}
		}
	}

	/** Lamps that change: alarms that flash now and then, a display's alarm tile, a running engine's light. */
	static void blink(EquipmentKind kind, Frame f, float time) {
		int tick = (int) (time / 8);
		switch (kind) {
			case MAIN_CONTROL_BOARD -> {
				// An annunciator window flashes for a while every so often.
				if ((tick / 40) % 3 == 0 && tick % 2 == 0) {
					front(f, 2.4, 2.75, 3.6, 2.92, 1.16, LAMP_RED);
				}
			}
			case PLANT_DISPLAY_WALL -> {
				if (tick % 6 < 3) {
					front(f, 8.35, 3.55, 8.85, 3.75, 0.25, LAMP_RED);
				}
			}
			case SWITCHGEAR_LINEUP -> {
				if (tick % 10 < 5) {
					front(f, 7.55, 2.6, 7.75, 2.72, 0.05, LAMP_RED);
				}
			}
			default -> {
			}
		}
	}

	// ------------------------------------------------------------------ helpers

	/** A face on the front (seen from -z) at depth z: a screen, a lamp. */
	static void front(Frame f, double x0, double y0, double x1, double y1, double z, float[] uv) {
		f.quad(v(x0, y1, z), v(x0, y0, z), v(x1, y0, z), v(x1, y1, z), v(0, 0, -1), uv[0], uv[1], uv[2], uv[3]);
	}

	/** A vertical sheet of blue light rising off one side of a fuel rack (translucent texels are cut, so it reads as glow). */
	private static void haze(Frame f, int side) {
		float[] u = CHERENKOV;
		double y0 = 3.86, y1 = 4.0;
		switch (side) {
			case 0 -> f.quad(v(0.1, y1, 0.1), v(0.1, y0, 0.1), v(3.9, y0, 0.1), v(3.9, y1, 0.1), v(0, 0, -1), u[0], u[1], u[2], u[3]);
			case 1 -> f.quad(v(3.9, y1, 3.9), v(3.9, y0, 3.9), v(0.1, y0, 3.9), v(0.1, y1, 3.9), v(0, 0, 1), u[0], u[1], u[2], u[3]);
			case 2 -> f.quad(v(0.1, y1, 3.9), v(0.1, y0, 3.9), v(0.1, y0, 0.1), v(0.1, y1, 0.1), v(-1, 0, 0), u[0], u[1], u[2], u[3]);
			default -> f.quad(v(3.9, y1, 0.1), v(3.9, y0, 0.1), v(3.9, y0, 3.9), v(3.9, y1, 3.9), v(1, 0, 0), u[0], u[1], u[2], u[3]);
		}
	}

	/** A railing along x at depth z, from x0 to x1, standing on y. */
	static void railX(Frame f, double x0, double x1, double y, double z) {
		if (!f.detail) return;
		f.box(x0, y, z - 0.03, x1, y + 1.0, z + 0.03, HANDRAIL);
	}

	/** A railing along z at x. */
	static void railZ(Frame f, double z0, double z1, double y, double x) {
		if (!f.detail) return;
		f.box(x - 0.03, y, z0, x + 0.03, y + 1.0, z1, HANDRAIL);
	}

	/** A horizontal vessel along z: a shell between two dished heads, on saddles. */
	static void horizontalVessel(Frame f, double cx, double cy, double z0, double z1, double r, float[] shell, double[] saddles) {
		double head = r * 0.45;
		f.revolve(v(cx, cy, z0), v(cx, cy, z1), new double[][] {{0, 0}, {head * 0.25, r * 0.66}, {head * 0.6, r * 0.92}, {head, r},
				{z1 - z0 - head, r}, {z1 - z0 - head * 0.6, r * 0.92}, {z1 - z0 - head * 0.25, r * 0.66}, {z1 - z0, 0}}, f.sides(24), shell, false, false);
		if (f.detail) {
			for (double z = z0 + head + 1.5; z < z1 - head - 0.5; z += 3) {
				f.revolve(v(cx, cy, z), v(cx, cy, z + 1), new double[][] {{0, r + 0.03}, {0.12, r + 0.03}}, 24, WELD_BAND, true, true);
			}
		}
		for (double z : saddles) {
			f.box(cx - r * 0.85, 0, z - 0.25, cx + r * 0.85, cy - r * 0.55, z + 0.25, CARBON_STEEL);
			f.box(cx - r * 0.95, 0, z - 0.35, cx + r * 0.95, 0.15, z + 0.35, CARBON_STEEL);
		}
	}

	// ------------------------------------------------------------------ cranes and fuel machines

	/**
	 * A double-girder bridge crane: end trucks on rails at each end, two box girders with a walkway between, the
	 * trolley with its hoist drum and motor, the operator's cab under one end, and the hook block hanging
	 * {@code drop} blocks below on its ropes.
	 */
	static void bridgeCrane(Frame f, double span, double deep, double tall, double drop) {
		double top = tall - 0.6;
		// End trucks with their wheels and buffers.
		for (double x0 : new double[] {0, span - 2.2}) {
			f.box(x0, top - 1.8, 0, x0 + 2.2, top - 0.2, deep, PAINT_YELLOW);
			f.plate(x0 + 0.2, top - 1.6, -0.02, x0 + 2.0, top - 0.6, 0.0, 0, HAZARD, PAINT_YELLOW);
			for (double z : new double[] {0.7, deep - 0.7}) {
				f.cylinder(v(x0 - 0.02, top - 1.95, z), v(x0 + 2.22, top - 1.95, z), 0.32, f.sides(12), CARBON_STEEL, true);
			}
			f.box(x0 + 0.4, top - 2.3, 0.2, x0 + 1.8, top - 2.15, deep - 0.2, STAINLESS_DARK);
		}
		// Box girders, the walkway between them and the railings.
		for (double z0 : new double[] {0.35, deep - 1.45}) {
			f.box(2.2, top - 2.0, z0, span - 2.2, top, z0 + 1.1, PAINT_YELLOW);
			if (f.detail) {
				for (double x = 4; x < span - 3; x += 3) {
					f.box(x, top - 2.02, z0 - 0.02, x + 0.12, top + 0.02, z0 + 1.12, PAINT_YELLOW);
				}
			}
		}
		f.box(2.2, top - 0.08, 1.45, span - 2.2, top, deep - 1.45, GRATING);
		railX(f, 2.2, span - 2.2, top, 0.38);
		railX(f, 2.2, span - 2.2, top, deep - 0.38);
		// Trolley: frame on its wheels, the hoist drum with grooves, gearbox and motor, a small house over the controls.
		double tx = span / 2 - 3.2;
		f.box(tx, top, 0.3, tx + 6.4, top + 0.35, deep - 0.3, PAINT_YELLOW);
		f.cylinder(v(tx + 0.6, top + 0.95, deep / 2), v(tx + 4.2, top + 0.95, deep / 2), 0.62, f.sides(16), STAINLESS_DARK, true);
		if (f.detail) {
			for (double x = tx + 0.8; x < tx + 4.1; x += 0.25) {
				f.revolve(v(x, top + 0.95, deep / 2), v(x + 1, top + 0.95, deep / 2), new double[][] {{0, 0.64}, {0.06, 0.64}}, 16, CARBON_STEEL, true, true);
			}
		}
		f.box(tx + 4.3, top + 0.35, deep / 2 - 0.6, tx + 5.1, top + 1.3, deep / 2 + 0.6, PAINT_GREY);
		f.cylinder(v(tx + 5.1, top + 0.9, deep / 2), v(tx + 6.3, top + 0.9, deep / 2), 0.45, f.sides(12), PAINT_BLUE, true);
		// Ropes down to the hook block, its sheaves, the hook.
		double hy = top - drop;
		for (double x : new double[] {tx + 1.6, tx + 3.2}) {
			for (double z : new double[] {deep / 2 - 0.25, deep / 2 + 0.25}) {
				f.box(x - 0.04, hy + 1.2, z - 0.04, x + 0.04, top + 0.4, z + 0.04, STAINLESS_DARK);
			}
		}
		f.box(tx + 1.1, hy + 0.4, deep / 2 - 0.55, tx + 3.7, hy + 1.3, deep / 2 + 0.55, PAINT_YELLOW);
		f.plate(tx + 1.3, hy + 0.5, deep / 2 - 0.57, tx + 3.5, hy + 1.2, deep / 2 - 0.55, 0, HAZARD, PAINT_YELLOW);
		f.cylinder(v(tx + 2.4, hy + 0.4, deep / 2), v(tx + 2.4, hy - 0.2, deep / 2), 0.18, f.sides(10), CARBON_STEEL, true);
		f.box(tx + 2.25, hy - 0.95, deep / 2 - 0.12, tx + 2.55, hy - 0.2, deep / 2 + 0.12, CARBON_STEEL);
		f.box(tx + 2.25, hy - 1.2, deep / 2 - 0.12, tx + 3.0, hy - 0.95, deep / 2 + 0.12, CARBON_STEEL);
		f.box(tx + 2.75, hy - 0.95, deep / 2 - 0.12, tx + 3.0, hy - 0.7, deep / 2 + 0.12, CARBON_STEEL);
		// The operator's cab hanging under one end of the bridge.
		f.box(3.0, top - 4.2, 0.2, 5.4, top - 2.0, 2.0, PAINT_YELLOW);
		f.plate(3.1, top - 3.6, 0.18, 5.3, top - 2.4, 0.2, 0, GLASS, PAINT_YELLOW);
		f.plate(5.4, top - 3.6, 0.4, 5.42, top - 2.4, 1.8, 3, GLASS, PAINT_YELLOW);
	}

	/**
	 * The refuelling machine over the reactor cavity: a gantry bridge on two end trucks, its trolley carrying the
	 * operator's console and the fuel mast, which reaches down into the cavity with an assembly gripper.
	 */
	static void refuelingMachine(Frame f) {
		gantry(f, 15, 4, 7.5, 7.5, 10);
	}

	/** The fuel handling machine over the spent fuel pool: the same kind of gantry, lower and lighter. */
	static void fuelHandlingMachine(Frame f) {
		gantry(f, 14, 5, 6.5, 7.0, 9);
	}

	private static void gantry(Frame f, double span, double deep, double top, double tx, double mast) {
		for (double x0 : new double[] {0, span - 1.4}) {
			f.box(x0, 0.2, 0, x0 + 1.4, 1.0, deep, PAINT_YELLOW);
			for (double z : new double[] {0.5, deep - 0.5}) {
				f.cylinder(v(x0 - 0.02, 0.32, z), v(x0 + 1.42, 0.32, z), 0.3, f.sides(12), CARBON_STEEL, true);
			}
			// Legs up to the bridge, braced.
			for (double z : new double[] {0.2, deep - 0.8}) {
				f.box(x0 + 0.3, 1.0, z, x0 + 1.1, top - 1.2, z + 0.6, PAINT_YELLOW);
			}
			f.box(x0 + 0.4, top * 0.5, 0.5, x0 + 1.0, top * 0.5 + 0.3, deep - 0.5, PAINT_YELLOW);
		}
		for (double z0 : new double[] {0.1, deep - 1.0}) {
			f.box(0, top - 1.2, z0, span, top, z0 + 0.9, PAINT_YELLOW);
		}
		f.box(0, top - 0.1, 1.0, span, top, deep - 1.0, GRATING);
		railX(f, 0, span, top, 0.12);
		railX(f, 0, span, top, deep - 0.12);
		// Trolley with the console stand and the mast housing above it.
		f.box(tx - 1.6, top, 0.6, tx + 1.6, top + 0.3, deep - 0.6, PAINT_GREY);
		f.box(tx - 1.0, top + 0.3, 0.6, tx - 0.1, top + 0.8, 1.2, PAINT_GREY, DESK, PAINT_GREY);
		f.box(tx - 1.0, top + 0.8, 0.62, tx - 0.1, top + 1.3, 0.68, PAINT_GREY);
		double mz = deep / 2;
		f.box(tx + 0.1, top + 0.3, mz - 0.5, tx + 1.1, top + 2.2, mz + 0.5, STAINLESS);
		// The mast: an outer tube through the bridge and down, the inner tube with the gripper.
		f.box(tx + 0.3, top - mast, mz - 0.3, tx + 0.9, top + 0.3, mz + 0.3, STAINLESS);
		f.box(tx + 0.38, top - mast - 1.4, mz - 0.22, tx + 0.82, top - mast, mz + 0.22, STAINLESS_DARK);
		f.box(tx + 0.25, top - mast - 1.7, mz - 0.35, tx + 0.95, top - mast - 1.4, mz + 0.35, HAZARD);
		if (f.detail) {
			for (double y = top - mast + 0.5; y < top; y += 1.0) {
				f.box(tx + 0.27, y, mz - 0.33, tx + 0.93, y + 0.08, mz + 0.33, WELD_BAND);
			}
		}
	}

	// ------------------------------------------------------------------ turbine building

	/** A moisture separator reheater: a long horizontal vessel on saddles with its steam nozzles on top. */
	static void moistureSeparatorReheater(Frame f) {
		horizontalVessel(f, 2.5, 2.4, 0.1, 19.9, 2.25, INSULATION, new double[] {3.5, 10, 16.5});
		for (double z : new double[] {5, 15}) {
			f.cylinder(v(2.5, 4.3, z), v(2.5, 4.97, z), 0.5, f.sides(14), INSULATION, true);
			f.cylinder(v(2.5, 4.85, z), v(2.5, 5.0, z), 0.65, f.sides(14), FLANGE, true);
		}
		f.cylinder(v(2.5, 0.6, 10), v(2.5, 0.15, 10), 0.3, f.sides(10), STAINLESS_DARK, true);
		f.plate(2.0, 2.0, 0.05, 3.0, 2.6, 0.08, 0, NAMEPLATE, PAINT_GREY);
		f.box(0.05, 0, 9.4, 0.55, 3.2, 9.9, CARBON_STEEL);
		railZ(f, 0.5, 19.5, 4.6, 0.6);
	}

	/** A feedwater heater: a horizontal shell with its channel head and tube-side nozzles at the front end. */
	static void feedwaterHeater(Frame f) {
		horizontalVessel(f, 1.5, 1.55, 1.2, 11.9, 1.3, PAINT_GREY, new double[] {3, 9.5});
		// The channel head: a flanged bonnet at the front.
		f.cylinder(v(1.5, 1.55, 1.0), v(1.5, 1.55, 1.3), 1.45, f.sides(20), FLANGE, true);
		f.revolve(v(1.5, 1.55, 1.0), v(1.5, 1.55, 0.0), new double[][] {{0, 1.3}, {0.5, 1.2}, {0.9, 0.6}, {1.0, 0}}, f.sides(20), PAINT_GREY, false, false);
		f.pipe(0.3, STAINLESS, v(1.0, 2.6, 0.6), v(1.0, 3.0, 0.6));
		f.pipe(0.3, STAINLESS, v(2.0, 2.6, 0.6), v(2.0, 3.0, 0.6));
		f.pipe(0.35, INSULATION, v(1.5, 2.7, 6), v(1.5, 3.0, 6));
		f.plate(1.0, 1.0, 1.6, 2.0, 1.4, 1.61, 0, NAMEPLATE, PAINT_GREY);
	}

	// ------------------------------------------------------------------ auxiliary and diesel buildings

	/**
	 * An emergency diesel generator set, as in a plant's diesel room: on a concrete inertia block and a steel skid,
	 * <ul>
	 * <li>the V16 engine: sump, crankcase with a bolted inspection door per cylinder each side, the two cylinder banks
	 * with a head and an aluminium rocker cover per cylinder, a fuel injection pump per cylinder on the outside of each
	 * bank with its high-pressure line up to the head, the yellow fuel header under them, the lagged exhaust manifold
	 * in the vee;</li>
	 * <li>at the free end (the front): the gear case with the gauge board between the jacket water and lube oil pumps,
	 * the governor above it, two turbochargers with their air filters and the charge air cooler between them, and the
	 * exhaust up from them through its expansion bellows and silencer to the roof (the stack above);</li>
	 * <li>at the drive end: the flywheel housing with the air start motors, the coupling in its yellow mesh guard, the
	 * generator on its feet with its louvred air intake, end bracket, outboard bearing, exciter and the cooling fan
	 * behind its guard at the far end, the terminal box on its side with the cable conduit down into the floor, the
	 * stencil;</li>
	 * <li>the jacket water (green), lube oil (brown), fuel (yellow) and starting air (blue) piping; the fuel supply in
	 * from the day tank through its gate valve (handwheel, yoke and rising stem) into the duplex filter;</li>
	 * <li>the local control panel at the front with its screen and the auto-start caution sign;</li>
	 * <li>a grated walkway along each side of the engine at head height, with toe boards and handrails, and steps up
	 * to each one beside the generator (half-block frames under them, so they walk like stairs).</li>
	 * </ul>
	 * What moves while it runs (the coupling, the cooling fan, the gauge needles, the lamps) is drawn by {@link #live}.
	 */
	static void dieselGenerator(Frame f) {
		// The inertia block and the skid.
		f.box(0.15, 0, 0.15, 6.85, 0.3, 13.85, CONCRETE);
		f.box(1.3, 0.3, 0.4, 1.8, 0.75, 13.6, CARBON_STEEL);
		f.box(5.2, 0.3, 0.4, 5.7, 0.75, 13.6, CARBON_STEEL);
		f.box(1.8, 0.3, 0.4, 5.2, 0.42, 13.6, CARBON_STEEL);
		if (f.detail) {
			for (double z = 0.9; z < 13.5; z += 2.1) {
				f.box(1.2, 0.3, z, 1.9, 0.36, z + 0.5, BLACK);
				f.box(5.1, 0.3, z, 5.8, 0.36, z + 0.5, BLACK);
			}
		}
		engine(f);
		drive(f);
		dieselPiping(f);
		// The local control panel at the front left, its caution sign (its screen and lamps light up: glow, live).
		f.box(0.25, 0.3, 0.2, 1.25, 2.6, 0.75, PAINT_GREY);
		f.box(0.2, 2.6, 0.15, 1.3, 2.7, 0.8, PAINT_GREY);
		f.plate(0.35, 0.85, 0.16, 1.15, 1.45, 0.2, 0, SIGN_AUTOSTART, PAINT_GREY);
		if (f.detail) {
			for (int k = 0; k < 3; k++) {
				double x = 0.45 + k * 0.25;
				f.cylinder(v(x, 1.55, 0.2), v(x, 1.55, 0.13), 0.06, 8, k == 0 ? RED_IRON : BLACK, true);
			}
		}
		walkways(f);
	}

	/** The shaft's height: flywheel, coupling, generator and exciter all turn about it. */
	static final double SHAFT_Y = 2.2;

	/** The engine block, heads, injection, front end, turbochargers and exhaust (the diesel generator's frame). */
	private static void engine(Frame f) {
		f.box(2.0, 0.42, 1.2, 5.0, 1.0, 8.4, ENGINE_GREEN);
		f.box(1.7, 1.0, 1.0, 5.3, 2.1, 8.6, PAINT_GREEN);
		f.box(1.5, 2.1, 1.1, 3.3, 2.95, 8.5, PAINT_GREEN);
		f.box(3.7, 2.1, 1.1, 5.5, 2.95, 8.5, PAINT_GREEN);
		f.box(3.3, 2.1, 1.1, 3.7, 2.4, 8.5, ENGINE_GREEN);
		for (int i = 0; i < 8; i++) {
			double z = 1.2 + i * 0.9;
			// The crankcase doors either side.
			f.plate(1.62, 1.15, z, 1.7, 1.9, z + 0.72, 2, CRANKCASE_DOOR, PAINT_GREEN);
			f.plate(5.3, 1.15, z, 5.38, 1.9, z + 0.72, 3, CRANKCASE_DOOR, PAINT_GREEN);
			// The heads and their rocker covers.
			f.box(1.45, 2.95, z, 3.2, 3.3, z + 0.8, ENGINE_GREEN);
			f.box(3.8, 2.95, z, 5.55, 3.3, z + 0.8, ENGINE_GREEN);
			f.box(1.6, 3.3, z + 0.06, 3.05, 3.55, z + 0.74, ROCKER);
			f.box(3.95, 3.3, z + 0.06, 5.4, 3.55, z + 0.74, ROCKER);
			if (f.detail) {
				// The injection pumps outside each bank, their lines up to the heads, the exhaust branches into the vee.
				f.box(1.15, 2.2, z + 0.2, 1.5, 2.75, z + 0.6, GUNMETAL);
				f.box(5.5, 2.2, z + 0.2, 5.85, 2.75, z + 0.6, GUNMETAL);
				f.cylinder(v(1.32, 2.75, z + 0.4), v(1.62, 3.12, z + 0.4), 0.035, 6, STAINLESS, false);
				f.cylinder(v(5.68, 2.75, z + 0.4), v(5.38, 3.12, z + 0.4), 0.035, 6, STAINLESS, false);
				f.box(3.12, 2.98, z + 0.25, 3.3, 3.2, z + 0.55, INSULATION);
				f.box(3.7, 2.98, z + 0.25, 3.88, 3.2, z + 0.55, INSULATION);
			}
		}
		// The lagged exhaust manifold in the vee.
		f.cylinder(v(3.5, 3.18, 0.95), v(3.5, 3.18, 8.45), 0.3, f.sides(12), INSULATION, true);
		// The free end: the gear case, the jacket water and lube oil pumps low on it with the gauge board between and
		// above them, the governor (a drum with its linkage to the fuel racks) over that.
		f.box(1.8, 0.9, 0.7, 5.2, 2.9, 1.0, ENGINE_GREEN);
		f.plate(2.95, 1.45, 0.62, 4.05, 2.2, 0.7, 0, GAUGE_BOARD, BLACK);
		f.cylinder(v(2.25, 1.15, 0.25), v(2.25, 1.15, 0.7), 0.32, f.sides(14), ENGINE_GREEN, true);
		f.cylinder(v(4.75, 1.15, 0.28), v(4.75, 1.15, 0.7), 0.28, f.sides(14), ENGINE_GREEN, true);
		f.cylinder(v(3.5, 2.55, 0.35), v(3.5, 2.55, 0.7), 0.22, f.sides(12), GUNMETAL, true);
		f.box(3.36, 2.77, 0.42, 3.64, 2.86, 0.62, STAINLESS_DARK);
		if (f.detail) {
			f.cylinder(v(3.5, 2.8, 0.52), v(1.32, 2.8, 0.9), 0.03, 6, STAINLESS, true);
			f.cylinder(v(3.5, 2.8, 0.52), v(5.68, 2.8, 0.9), 0.03, 6, STAINLESS, true);
		}
		// Two turbochargers above it (turbine behind, compressor in front with its air filter), the charge air cooler.
		for (double x : new double[] {2.3, 4.7}) {
			f.revolve(v(x, 3.6, 1.35), v(x, 3.6, 0.6), new double[][] {{0, 0.3}, {0.15, 0.52}, {0.6, 0.52}, {0.75, 0.32}}, f.sides(14), TURBO, true, true);
			f.revolve(v(x, 3.6, 0.6), v(x, 3.6, 0.1), new double[][] {{0, 0.46}, {0.35, 0.42}, {0.5, 0.22}}, f.sides(14), STAINLESS, true, true);
			f.column(x, 0.35, 3.95, 4.55, 0.4, f.sides(14), GUNMETAL);
			f.cylinder(v(x, 4.55, 0.35), v(x, 4.62, 0.35), 0.48, f.sides(14), STAINLESS_DARK, true);
			f.cylinder(v(x, 3.85, 0.35), v(x, 3.97, 0.35), 0.2, 8, STAINLESS, false);
		}
		f.box(2.85, 3.3, 0.5, 4.15, 3.95, 1.15, ENGINE_GREEN);
		// The exhaust: up from each turbine, together over the middle, then the expansion bellows (its convolutions on
		// the pipe), the silencer and the pipe on up through the roof (its thimble at the ceiling) to the stack.
		f.pipe(0.2, INSULATION, v(2.3, 3.95, 1.0), v(2.3, 4.45, 1.0), v(3.5, 4.45, 1.0));
		f.pipe(0.2, INSULATION, v(4.7, 3.95, 1.0), v(4.7, 4.45, 1.0), v(3.5, 4.45, 1.0));
		f.cylinder(v(3.5, 4.45, 0.95), v(3.5, 6.0, 0.95), 0.3, f.sides(12), INSULATION, false);
		f.cylinder(v(3.5, 4.88, 0.95), v(3.5, 4.94, 0.95), 0.42, f.sides(12), GUNMETAL, true);
		f.cylinder(v(3.5, 5.36, 0.95), v(3.5, 5.42, 0.95), 0.42, f.sides(12), GUNMETAL, true);
		for (double y = 4.97; y < 5.33; y += 0.09) {
			f.cylinder(v(3.5, y, 0.95), v(3.5, y + 0.05, 0.95), 0.37, f.sides(12), STAINLESS_DARK, true);
		}
		f.revolve(v(3.5, 6.0, 0.95), v(3.5, 9.0, 0.95), new double[][] {{0, 0.3}, {0.35, 0.72}, {2.65, 0.72}, {3.0, 0.3}}, f.sides(16), INSULATION, false, false);
		if (f.detail) {
			for (double y = 6.5; y < 8.7; y += 0.7) {
				f.band(3.5, 0.95, y, 0.74, 0.06, GUNMETAL);
			}
		}
		f.cylinder(v(3.5, 9.0, 0.95), v(3.5, 11.0, 0.95), 0.3, f.sides(12), INSULATION, false);
		f.cylinder(v(3.5, 10.9, 0.95), v(3.5, 11.0, 0.95), 0.62, f.sides(12), GUNMETAL, true);
	}

	/** The flywheel, coupling guard, generator and exciter, terminal box and stencil (the diesel generator's frame). */
	private static void drive(Frame f) {
		double a = SHAFT_Y;
		f.revolve(v(3.5, a, 8.5), v(3.5, a, 9.25), new double[][] {{0, 1.25}, {0.15, 1.55}, {0.75, 1.55}}, f.sides(20), PAINT_GREEN, false, true);
		// The air start motors bolted to the flywheel housing either side, on their pads.
		for (double side : new double[] {-1, 1}) {
			double x = 3.5 + side * 1.62;
			f.box(Math.min(x, 3.5 + side * 1.4), a - 0.62, 8.62, Math.max(x, 3.5 + side * 1.4), a - 0.3, 9.14, PAINT_GREEN);
			f.cylinder(v(x, a - 0.46, 8.55), v(x, a - 0.46, 9.3), 0.2, f.sides(10), GUNMETAL, true);
		}
		// The coupling's guard: a yellow angle frame (an edge bar along each corner, a ring at each end) with mesh panels
		// on the top and both sides, the turning coupling inside (live) showing through the mesh.
		double g0 = 9.25, g1 = 10.0, gx0 = 2.15, gx1 = 4.85, gy0 = 0.75, gy1 = 3.25, t = 0.08;
		for (double x : new double[] {gx0, gx1 - t}) {
			for (double y : new double[] {gy0, gy1 - t}) {
				f.box(x, y, g0, x + t, y + t, g1, PAINT_YELLOW);
			}
		}
		for (double z : new double[] {g0, g1 - t}) {
			f.box(gx0 + t, gy0, z, gx1 - t, gy0 + t, z + t, PAINT_YELLOW);
			f.box(gx0 + t, gy1 - t, z, gx1 - t, gy1, z + t, PAINT_YELLOW);
			f.box(gx0, gy0 + t, z, gx0 + t, gy1 - t, z + t, PAINT_YELLOW);
			f.box(gx1 - t, gy0 + t, z, gx1, gy1 - t, z + t, PAINT_YELLOW);
		}
		if (f.detail) {
			double m0 = g0 + t, m1 = g1 - t, y = gy1 - t / 2;
			f.quad(v(gx0 + t, y, m1), v(gx0 + t, y, m0), v(gx1 - t, y, m0), v(gx1 - t, y, m1), v(0, 1, 0),
					GRATING[0], GRATING[1], GRATING[2], GRATING[3]);
			f.quad(v(gx0 + t / 2, gy1 - t, m1), v(gx0 + t / 2, gy0 + t, m1), v(gx0 + t / 2, gy0 + t, m0), v(gx0 + t / 2, gy1 - t, m0), v(-1, 0, 0),
					GRATING[0], GRATING[1], GRATING[2], GRATING[3]);
			f.quad(v(gx1 - t / 2, gy1 - t, m0), v(gx1 - t / 2, gy0 + t, m0), v(gx1 - t / 2, gy0 + t, m1), v(gx1 - t / 2, gy1 - t, m1), v(1, 0, 0),
					GRATING[0], GRATING[1], GRATING[2], GRATING[3]);
		}
		f.cylinder(v(3.5, a, 9.2), v(3.5, a, 10.05), 0.22, f.sides(10), STAINLESS_DARK, false);
		// The generator, its feet, air intake, end bracket, outboard bearing, exciter, and the cooling fan's cowl and
		// guard at the far end (the fan behind it: live).
		f.cylinder(v(3.5, a, 10.0), v(3.5, a, 12.9), 1.55, f.sides(24), GENERATOR, true);
		f.box(1.75, 0.42, 10.2, 2.45, 1.2, 12.7, GENERATOR);
		f.box(4.55, 0.42, 10.2, 5.25, 1.2, 12.7, GENERATOR);
		airIntake(f, a);
		f.revolve(v(3.5, a, 12.9), v(3.5, a, 13.25), new double[][] {{0, 1.55}, {0.2, 1.3}, {0.35, 0.9}}, f.sides(20), GENERATOR, false, true);
		f.box(3.0, 0.42, 12.9, 4.0, a - 0.4, 13.35, GENERATOR);
		f.cylinder(v(3.5, a, 13.2), v(3.5, a, 13.55), 0.75, f.sides(16), GENERATOR, true);
		f.revolve(v(3.5, a, 13.55), v(3.5, a, 13.85), new double[][] {{0, 0.75}, {0.3, 0.75}}, f.sides(16), GENERATOR, false, false);
		if (f.detail) {
			f.disc(v(3.5, a, 13.84), v(0, 0, 1), 0.74, 16, GRATING);
		}
		f.plate(1.9, a - 0.4, 10.9, 1.98, a + 0.5, 12.0, 2, STENCIL_EDG, GENERATOR);
		f.plate(5.02, a - 0.4, 11.8, 5.1, a + 0.5, 12.8, 3, STENCIL_EDG, GENERATOR);
		// The terminal box on the right (bolted to the frame at the shaft's height), its conduit down into the floor.
		f.box(4.9, a - 0.6, 10.4, 5.5, a + 0.6, 11.6, PAINT_GREY);
		f.cylinder(v(5.2, a - 0.6, 11.0), v(5.2, 0.3, 11.0), 0.12, 8, STAINLESS_DARK, false);
		if (f.detail) {
			f.box(5.5, a - 0.45, 10.55, 5.54, a + 0.45, 11.45, STAINLESS_DARK);
		}
	}

	/**
	 * The generator's cooling air intake on top of it: a louvred hood. Its corner posts and dark bird screen start down
	 * inside the generator's round body (so it meets the curve with no gaps), real slats on all four sides recessed in
	 * the frame, a band round the top and an overhanging rain cap.
	 */
	private static void airIntake(Frame f, double a) {
		double x0 = 2.55, x1 = 4.45, z0 = 10.45, z1 = 12.45, y0 = a + 1.05, band = a + 2.09, top = a + 2.17, p = 0.08;
		// The screen behind the slats.
		f.box(x0 + 0.06, y0, z0 + 0.06, x1 - 0.06, band, z1 - 0.06, BLACK);
		// Corner posts, the band under the cap.
		for (double x : new double[] {x0, x1 - p}) {
			for (double z : new double[] {z0, z1 - p}) {
				f.box(x, y0, z, x + p, top, z + p, ENGINE_GREEN);
			}
		}
		f.box(x0, band, z0 + p, x0 + p, top, z1 - p, ENGINE_GREEN);
		f.box(x1 - p, band, z0 + p, x1, top, z1 - p, ENGINE_GREEN);
		f.box(x0 + p, band, z0, x1 - p, top, z0 + p, ENGINE_GREEN);
		f.box(x0 + p, band, z1 - p, x1 - p, top, z1, ENGINE_GREEN);
		// The slats, a little back from the frame's face, dark gaps between.
		for (double y = y0 + 0.05; y + 0.04 < band - 0.05; y += 0.12) {
			f.box(x0 + 0.02, y, z0 + p, x0 + 0.06, y + 0.04, z1 - p, ENGINE_GREEN);
			f.box(x1 - 0.06, y, z0 + p, x1 - 0.02, y + 0.04, z1 - p, ENGINE_GREEN);
			f.box(x0 + p, y, z0 + 0.02, x1 - p, y + 0.04, z0 + 0.06, ENGINE_GREEN);
			f.box(x0 + p, y, z1 - 0.06, x1 - p, y + 0.04, z1 - 0.02, ENGINE_GREEN);
		}
		// The rain cap, overhanging all round.
		f.box(x0 - 0.08, top, z0 - 0.08, x1 + 0.08, top + 0.08, z1 + 0.08, GENERATOR);
	}

	/** The jacket water, lube oil, fuel and starting air piping, and the fuel supply in from the day tank. */
	private static void dieselPiping(Frame f) {
		// Fuel headers along both banks (yellow), fed from the duplex filter on the near side.
		f.cylinder(v(1.25, 2.12, 1.0), v(1.25, 2.12, 8.6), 0.07, 8, PIPE_YELLOW, true);
		f.cylinder(v(5.75, 2.12, 1.0), v(5.75, 2.12, 8.6), 0.07, 8, PIPE_YELLOW, true);
		f.cylinder(v(1.25, 2.12, 8.6), v(5.75, 2.12, 8.6), 0.07, 8, PIPE_YELLOW, false);
		// The supply from the day tank comes in under the walkway: the room's steel pipe meets this one beside the skid
		// (its end at x -0.38, the steel pipe's side), then a flanged gate valve with its yoke, rising stem and red
		// handwheel, then the pipe into the duplex filter's inlet manifold.
		double y = 0.5, z = 5.5;
		f.cylinder(v(-0.38, y, z), v(0.12, y, z), 0.125, 8, STAINLESS_DARK, false);
		gateValve(f, 0.12, 0.5, y, z);
		f.cylinder(v(0.5, y, z), v(0.56, y, z), 0.09, 8, PIPE_YELLOW, false);
		// The duplex filter: two canisters on a manifold top and bottom, the change-over lever on top.
		f.box(0.55, 0.3, 5.0, 0.95, 0.66, 6.0, GUNMETAL);
		f.column(0.75, 5.2, 0.66, 1.35, 0.17, f.sides(10), GUNMETAL);
		f.column(0.75, 5.8, 0.66, 1.35, 0.17, f.sides(10), GUNMETAL);
		f.box(0.6, 1.35, 5.05, 0.9, 1.45, 5.95, GUNMETAL);
		f.box(0.7, 1.45, 5.45, 0.8, 1.62, 5.55, RED_IRON);
		f.box(0.7, 1.62, 5.2, 0.8, 1.7, 5.8, RED_IRON);
		f.pipe(0.06, PIPE_YELLOW, v(0.75, 1.45, 5.5), v(0.75, 1.75, 5.5), v(1.25, 1.75, 5.5), v(1.25, 2.05, 5.5));
		// Jacket water: the outlet manifolds over each bank's heads, forward and down to the pumps, out into the floor.
		for (double x : new double[] {1.5, 5.5}) {
			double pump = x < 3 ? 2.25 : 4.75;
			f.pipe(0.08, PIPE_GREEN, v(x, 3.45, 8.4), v(x, 3.45, 0.4), v(x, 1.15, 0.4), v(pump, 1.15, 0.4));
			f.pipe(0.1, PIPE_GREEN, v(pump, 0.83, 0.32), v(pump, 0.3, 0.32));
		}
		// Lube oil: the filter and cooler under the right-hand walkway, piped to the front.
		f.box(5.6, 0.75, 3.0, 6.35, 1.7, 4.4, ENGINE_GREEN);
		f.cylinder(v(5.97, 1.7, 3.7), v(5.97, 1.8, 3.7), 0.3, 10, STAINLESS_DARK, true);
		f.pipe(0.07, PIPE_BROWN, v(5.97, 1.1, 3.0), v(5.97, 1.1, 1.2), v(5.3, 1.1, 1.2));
		f.pipe(0.07, PIPE_BROWN, v(5.6, 1.4, 4.4), v(5.45, 1.4, 4.4));
		// Starting air: up from the floor to the air start motors.
		for (double side : new double[] {-1, 1}) {
			double x = 3.5 + side * 1.62, out = x < 3 ? 1.55 : 5.45;
			f.pipe(0.06, PIPE_LBLUE, v(x, SHAFT_Y - 0.46, 9.3), v(x, SHAFT_Y - 0.46, 9.45), v(out, SHAFT_Y - 0.46, 9.45), v(out, 0.3, 9.45));
		}
	}

	/**
	 * A flanged gate valve in a pipe along x from x0 to x1 at (y, z): the body with its flanges, the bonnet, the yoke
	 * and the rising stem through it, the red handwheel (rim, spokes and hub) on top.
	 */
	private static void gateValve(Frame f, double x0, double x1, double y, double z) {
		double c = (x0 + x1) / 2;
		f.cylinder(v(x0, y, z), v(x0 + 0.05, y, z), 0.2, 10, GUNMETAL, true);
		f.cylinder(v(x1 - 0.05, y, z), v(x1, y, z), 0.2, 10, GUNMETAL, true);
		f.revolve(v(x0 + 0.05, y, z), v(x1 - 0.05, y, z), new double[][] {{0, 0.12}, {0.06, 0.17}, {x1 - x0 - 0.16, 0.17}, {x1 - x0 - 0.1, 0.12}},
				10, GUNMETAL, false, false);
		f.box(c - 0.1, y, z - 0.1, c + 0.1, y + 0.28, z + 0.1, GUNMETAL);
		f.box(c - 0.12, y + 0.28, z - 0.12, c + 0.12, y + 0.32, z + 0.12, GUNMETAL);
		if (!f.detail) {
			return;
		}
		// The yoke's two arms and its top, the stem rising through it.
		f.box(c - 0.1, y + 0.32, z - 0.025, c - 0.07, y + 0.52, z + 0.025, GUNMETAL);
		f.box(c + 0.07, y + 0.32, z - 0.025, c + 0.1, y + 0.52, z + 0.025, GUNMETAL);
		f.box(c - 0.1, y + 0.52, z - 0.04, c + 0.1, y + 0.56, z + 0.04, GUNMETAL);
		f.cylinder(v(c, y + 0.32, z), v(c, y + 0.68, z), 0.018, 6, STAINLESS, true);
		// The handwheel: a rim of eight short bars, four spokes, the hub.
		double wy = y + 0.6, r = 0.17;
		for (int k = 0; k < 8; k++) {
			double a0 = Math.PI * 2 * k / 8, a1 = Math.PI * 2 * (k + 1) / 8;
			f.cylinder(v(c + r * Math.cos(a0), wy, z + r * Math.sin(a0)), v(c + r * Math.cos(a1), wy, z + r * Math.sin(a1)), 0.018, 6, RED_IRON, true);
		}
		f.cylinder(v(c - r, wy, z), v(c + r, wy, z), 0.012, 6, RED_IRON, false);
		f.cylinder(v(c, wy, z - r), v(c, wy, z + r), 0.012, 6, RED_IRON, false);
		f.cylinder(v(c, wy - 0.025, z), v(c, wy + 0.025, z), 0.035, 8, STAINLESS, true);
	}

	/** The grated walkways along the engine at head height, their toe boards and handrails, and a flight of steps down
	 * from each beside the generator. */
	private static void walkways(Frame f) {
		// The decks are walked at 2 (on the frames under them); the steps beside the generator go down a block-wide
		// tread at a time, half a block each (the half-block frames under them), to the floor at z 12. No two faces
		// share a plane (z-fighting): the posts stand a hair proud of the rails and toe boards they carry, the treads
		// stop short of the stringers, each tread's nosing is its own strip at the tread's front.
		double deck = 2.0, z0 = 0.9, z1 = 9.0;
		for (double[] side : new double[][] {{0.05, 1.4, 0.0}, {5.6, 6.95, 6.95}}) {
			double a = side[0], b = side[1], edge = side[2];
			f.box(a, deck - 0.1, z0, b, deck, z1, GRATING);
			// Its posts down to the plinth, toe board and rails along the outer edge.
			for (double z : new double[] {z0, 3.6, 6.3, z1 - 0.15}) {
				f.box(edge - 0.01, 0.3, z - 0.01, edge + 0.09, deck + 1.08, z + 0.16, PAINT_YELLOW);
			}
			f.box(edge - 0.02, deck, z0, edge + 0.07, deck + 0.15, z1, PAINT_YELLOW);
			f.box(edge, deck + 0.98, z0, edge + 0.08, deck + 1.06, z1, PAINT_YELLOW);
			f.box(edge, deck + 0.5, z0, edge + 0.08, deck + 0.56, z1, PAINT_YELLOW);
			// Across the front end (from the side rail, not into it).
			double r0 = edge < a ? edge + 0.08 : a, r1 = edge < a ? b : edge;
			f.box(r0, deck + 0.98, z0, r1, deck + 1.06, z0 + 0.08, PAINT_YELLOW);
			f.box(r0, deck + 0.5, z0, r1, deck + 0.56, z0 + 0.08, PAINT_YELLOW);
			// The flight down beside the generator: grated treads, yellow nosings, a stringer and a handrail each side
			// sloping down it (half a block a block), a post at the foot of each handrail.
			boolean left = a < 3;
			double t0 = left ? a + 0.03 : a + 0.06, t1 = left ? b - 0.06 : b - 0.03, zEnd = z1 + 3;
			for (int k = 0; k < 3; k++) {
				double top = 1.5 - k * 0.5, za = z1 + k;
				f.box(t0, top - 0.08, za, t1, top, za + 0.94, GRATING);
				f.box(t0, top - 0.1, za + 0.94, t1, top, za + 1.0, PAINT_YELLOW);
			}
			for (double x : new double[] {t0 - 0.04, t1 + 0.04}) {
				// The stringer: a steel plate (with thickness: a face drawn both ways would fight with itself).
				slopedPlate(f, x, 0.015, new double[][] {{deck + 0.12, z1}, {deck - 0.45, z1}, {0.0, zEnd - 0.5}, {0.62, zEnd}}, PAINT_YELLOW);
				f.cylinder(v(x, deck + 1.0, z1), v(x, 1.5, zEnd), 0.04, 6, PAINT_YELLOW, true);
				f.box(x - 0.03, 0.0, zEnd - 0.03, x + 0.03, 1.5, zEnd + 0.03, PAINT_YELLOW);
			}
		}
	}

	/**
	 * A flat plate standing in the plane x (half-thickness {@code h} either side), its outline the four (y, z) corners
	 * in order round it: both sides and the four edges, so it has real thickness.
	 */
	private static void slopedPlate(Frame f, double x, double h, double[][] c, float[] uv) {
		Vec3[] lo = new Vec3[4], hi = new Vec3[4];
		for (int i = 0; i < 4; i++) {
			lo[i] = v(x - h, c[i][0], c[i][1]);
			hi[i] = v(x + h, c[i][0], c[i][1]);
		}
		f.quad(lo[0], lo[1], lo[2], lo[3], v(-1, 0, 0), uv[0], uv[1], uv[2], uv[3]);
		f.quad(hi[3], hi[2], hi[1], hi[0], v(1, 0, 0), uv[0], uv[1], uv[2], uv[3]);
		for (int i = 0; i < 4; i++) {
			int j = (i + 1) % 4;
			// The edge's outward normal in the (y, z) plane: the outline runs round, so it is the edge turned a quarter.
			double dy = c[j][0] - c[i][0], dz = c[j][1] - c[i][1], len = Math.hypot(dy, dz);
			Vec3 n = v(0, dz / len, -dy / len);
			Vec3 centre = v(x, (c[0][0] + c[2][0]) / 2, (c[0][1] + c[2][1]) / 2), mid = v(x, (c[i][0] + c[j][0]) / 2, (c[i][1] + c[j][1]) / 2);
			if (n.dot(mid.subtract(centre)) < 0) {
				n = n.scale(-1);
			}
			f.quad(hi[i], lo[i], lo[j], hi[j], n, uv[0], uv[1], uv[2], uv[3]);
		}
	}

	/**
	 * What moves while the diesel generator runs (drawn every frame, at full detail only): the coupling turning in its
	 * guard and the cooling fan behind its guard at the far end (both at {@code angle} degrees), the gauge needles
	 * (at zero stopped, at their running readings as it comes up to speed, trembling a little). {@code speed} is 0
	 * stopped .. 1 at full speed.
	 */
	static void live(EquipmentKind kind, Frame f, float time, float speed, float angle, boolean running) {
		if (kind != EquipmentKind.EMERGENCY_DIESEL_GENERATOR) {
			return;
		}
		double a = SHAFT_Y, turn = Math.toRadians(angle);
		// The coupling: two flanged hubs bolted together (eight bolts round them), a white mark on one to show it turn.
		f.cylinder(v(3.5, a, 9.35), v(3.5, a, 9.5), 0.72, 16, STAINLESS_DARK, true);
		f.cylinder(v(3.5, a, 9.75), v(3.5, a, 9.9), 0.72, 16, STAINLESS_DARK, true);
		f.cylinder(v(3.5, a, 9.5), v(3.5, a, 9.75), 0.42, 12, GUNMETAL, true);
		for (int k = 0; k < 8; k++) {
			double b = turn + Math.PI * 2 * k / 8, x = 3.5 + 0.58 * Math.cos(b), y = a + 0.58 * Math.sin(b);
			f.cylinder(v(x, y, 9.3), v(x, y, 9.95), 0.05, 6, STAINLESS, true);
		}
		double mx = 3.5 + 0.72 * Math.cos(turn), my = a + 0.72 * Math.sin(turn);
		f.box(mx - 0.05, my - 0.05, 9.37, mx + 0.05, my + 0.05, 9.48, PAINT_WHITE);
		// The cooling fan: six blades behind the guard.
		for (int k = 0; k < 6; k++) {
			spoke(f, a, 13.72, turn * 1.0 + Math.PI * 2 * k / 6, 0.12, 0.68, 0.11, GUNMETAL);
		}
		f.cylinder(v(3.5, a, 13.66), v(3.5, a, 13.78), 0.14, 10, GUNMETAL, true);
		// The gauge needles (the dials are painted on the board: lube oil, jacket water, fuel, starting air).
		double[] reading = {0.68, 0.55, 0.47, 0.8};
		double[][] centre = {{0.25, 0.234}, {0.75, 0.234}, {0.25, 0.734}, {0.75, 0.734}};
		for (int k = 0; k < 4; k++) {
			double value = k == 3 ? 0.8 - 0.1 * Math.min(1, speed * 3) : reading[k] * speed;
			value += speed > 0.9 ? Math.sin(time * 0.9 + k * 1.7) * 0.012 : 0;
			double cx = 2.95 + centre[k][0] * 1.1, cy = 2.2 - centre[k][1] * 0.75;
			double deg = 140 + 260 * value, len = 0.13;
			double dx = Math.cos(Math.toRadians(deg)) * len, dy = -Math.sin(Math.toRadians(deg)) * len;
			double px = -dy / len * 0.012, py = dx / len * 0.012, zz = 0.612;
			f.quad(v(cx + px, cy + py, zz), v(cx - px, cy - py, zz), v(cx + dx - px * 0.3, cy + dy - py * 0.3, zz),
					v(cx + dx + px * 0.3, cy + dy + py * 0.3, zz), v(0, 0, -1), RED_IRON[0], RED_IRON[1], RED_IRON[2], RED_IRON[3]);
		}
	}

	/** The diesel generator's panel lamps (drawn at full brightness): READY standing by, RUNNING (flashing amber while
	 * it comes up to speed). */
	static void liveLamps(EquipmentKind kind, Frame f, float time, float speed, boolean running) {
		if (kind != EquipmentKind.EMERGENCY_DIESEL_GENERATOR) {
			return;
		}
		if (!running) {
			front(f, 0.42, 2.32, 0.54, 2.44, 0.195, LAMP_GREEN);
		} else if (speed < 0.98) {
			if ((int) (time / 4) % 2 == 0) {
				front(f, 0.62, 2.32, 0.74, 2.44, 0.195, PAINT_YELLOW);
			}
		} else {
			front(f, 0.62, 2.32, 0.74, 2.44, 0.195, LAMP_GREEN);
		}
	}

	/**
	 * A flat blade turned about the shaft (along z at height {@code a}, x 3.5): from radius r0 to r1, {@code width}
	 * wide, at angle {@code b}, standing at depth z (drawn from both sides).
	 */
	private static void spoke(Frame f, double a, double z, double b, double r0, double r1, double width, float[] uv) {
		double cx = Math.cos(b), cy = Math.sin(b), wx = -cy * width / 2, wy = cx * width / 2;
		Vec3 p0 = v(3.5 + cx * r0 - wx, a + cy * r0 - wy, z), p1 = v(3.5 + cx * r1 - wx, a + cy * r1 - wy, z);
		Vec3 p2 = v(3.5 + cx * r1 + wx, a + cy * r1 + wy, z), p3 = v(3.5 + cx * r0 + wx, a + cy * r0 + wy, z);
		// One face: nothing culls back faces, so it shows from both sides (a second face the other way would fight it).
		f.quad(p0, p1, p2, p3, v(0, 0, -1), uv[0], uv[1], uv[2], uv[3]);
	}

	/** A vertical storage tank: a welded shell with stiffening rings, a domed roof with a vent, a ladder and a railing. */
	static void storageTank(Frame f) {
		double c = 2.5, r = 2.35;
		f.revolve(v(c, 0, c), v(c, 1, c), new double[][] {{0, r + 0.1}, {0.3, r + 0.1}}, f.sides(32), CONCRETE, true, true);
		f.cylinder(v(c, 0.3, c), v(c, 8.2, c), r, f.sides(32), PAINT_WHITE, false);
		if (f.detail) {
			for (double y = 2.2; y < 8; y += 2) {
				f.band(c, c, y, r + 0.03, 0.1, WELD_BAND);
			}
		}
		f.dome(c, c, 8.2, r, 0.75, PAINT_WHITE);
		f.cylinder(v(c, 8.85, c), v(c, 9.0, c), 0.3, f.sides(10), STAINLESS_DARK, true);
		// Ladder up the front, a railing round the top.
		f.box(c - 0.35, 0.3, c - r - 0.35, c - 0.3, 8.9, c - r - 0.3, STAINLESS_DARK);
		f.box(c + 0.3, 0.3, c - r - 0.35, c + 0.35, 8.9, c - r - 0.3, STAINLESS_DARK);
		if (f.detail) {
			for (double y = 0.6; y < 8.8; y += 0.3) {
				f.box(c - 0.32, y, c - r - 0.34, c + 0.32, y + 0.04, c - r - 0.31, STAINLESS_DARK);
			}
			f.revolve(v(c, 8.2, c), v(c, 9.2, c), new double[][] {{0, r - 0.05}, {0.8, r - 0.05}}, 24, HANDRAIL, false, false);
		}
		f.pipe(0.25, STAINLESS, v(c + r - 0.1, 0.8, c), v(c + r + 0.3, 0.8, c), v(c + r + 0.3, 0.3, c));
		f.plate(c + 0.8, 3.5, c - r - 0.02, c + 1.6, 4.1, c - r + 0.2, 0, NAMEPLATE, PAINT_WHITE);
	}

	/** A shell-and-tube heat exchanger (component cooling): horizontal on saddles, channel heads at both ends. */
	static void heatExchanger(Frame f) {
		horizontalVessel(f, 1.5, 1.4, 0.8, 8.2, 1.15, PAINT_GREEN, new double[] {2.3, 6.7});
		for (double z : new double[] {0.5, 8.5}) {
			f.cylinder(v(1.5, 1.4, z - 0.25), v(1.5, 1.4, z + 0.25), 1.3, f.sides(20), FLANGE, true);
		}
		f.pipe(0.28, PIPE_RED, v(1.5, 2.5, 2.0), v(1.5, 3.0, 2.0));
		f.pipe(0.28, PAINT_BLUE, v(1.5, 2.5, 7.0), v(1.5, 3.0, 7.0));
		f.pipe(0.22, STAINLESS, v(1.5, 2.6, 0.5), v(1.5, 3.0, 0.5));
		f.plate(1.0, 1.0, 0.24, 2.0, 1.6, 0.25, 0, NAMEPLATE, FLANGE);
	}

	/** A horizontal centrifugal pump and motor on a baseplate, with its suction and discharge piping. */
	static void pumpSkid(Frame f) {
		f.box(0.2, 0, 0.2, 2.8, 0.3, 4.8, CARBON_STEEL);
		f.revolve(v(1.5, 1.0, 0.4), v(1.5, 1.0, 1.6), new double[][] {{0, 0.5}, {0.15, 0.85}, {0.9, 0.85}, {1.2, 0.4}}, f.sides(18), PAINT_BLUE, true, true);
		f.box(1.0, 0.3, 0.6, 2.0, 0.5, 1.4, CARBON_STEEL);
		f.box(1.15, 0.65, 1.6, 1.85, 1.35, 2.2, HAZARD);
		f.cylinder(v(1.5, 1.0, 2.2), v(1.5, 1.0, 4.6), 0.8, f.sides(18), PAINT_GREY, true);
		if (f.detail) {
			for (double z = 2.4; z < 4.5; z += 0.25) {
				f.revolve(v(1.5, 1.0, z), v(1.5, 1.0, z + 1), new double[][] {{0, 0.86}, {0.06, 0.86}}, 16, FINS, true, true);
			}
		}
		f.box(1.0, 0.3, 2.6, 2.0, 0.4, 4.2, CARBON_STEEL);
		f.box(2.2, 1.0, 3.0, 2.6, 1.6, 3.6, PAINT_GREY);
		f.pipe(0.3, STAINLESS, v(1.5, 1.0, 0.0), v(1.5, 1.0, 0.4));
		f.pipe(0.25, STAINLESS, v(1.5, 1.8, 1.0), v(1.5, 3.0, 1.0));
	}

	// ------------------------------------------------------------------ fuel building

	/** A spent fuel rack: a stainless cell block standing on its feet in the pool (its top glows: {@link #glow}). */
	static void fuelRack(Frame f) {
		f.box(0.1, 0.3, 0.1, 3.9, 3.85, 3.9, STAINLESS);
		for (double x : new double[] {0.3, 3.4}) {
			for (double z : new double[] {0.3, 3.4}) {
				f.box(x, 0, z, x + 0.3, 0.3, z + 0.3, STAINLESS_DARK);
			}
		}
		if (f.detail) {
			for (double x = 1.05; x < 3.9; x += 0.95) {
				f.box(x - 0.03, 0.3, 0.07, x + 0.03, 3.85, 0.1, STAINLESS_DARK);
				f.box(x - 0.03, 0.3, 3.9, x + 0.03, 3.85, 3.93, STAINLESS_DARK);
			}
		}
	}

	/**
	 * A dry storage cask on the pad: a ventilated concrete overpack in a steel shell, the bolted lid, air inlets at the
	 * bottom and outlets near the top, lifting lugs and the trefoil plate.
	 */
	static void dryCask(Frame f) {
		double c = 1.5, r = 1.42;
		f.cylinder(v(c, 0, c), v(c, 5.3, c), r, f.sides(28), CONCRETE, false);
		if (f.detail) {
			f.band(c, c, 0.0, r + 0.03, 0.2, STAINLESS_DARK);
			f.band(c, c, 5.1, r + 0.03, 0.2, STAINLESS_DARK);
		}
		f.revolve(v(c, 5.3, c), v(c, 6.3, c), new double[][] {{0, r}, {0.3, r}, {0.45, 1.0}, {0.55, 1.0}}, f.sides(28), STAINLESS_DARK, false, true);
		for (int i = 0; i < 4; i++) {
			double a = Math.PI / 4 + i * Math.PI / 2;
			double x = c + Math.cos(a) * (r - 0.05), z = c + Math.sin(a) * (r - 0.05);
			f.box(x - 0.2, 0.3, z - 0.2, x + 0.2, 0.75, z + 0.2, RUBBER);
			f.box(x - 0.18, 4.7, z - 0.18, x + 0.18, 5.05, z + 0.18, RUBBER);
			f.box(x - 0.12, 5.55, z - 0.12, x + 0.12, 5.95, z + 0.12, CARBON_STEEL);
		}
		f.plate(c - 0.35, 2.4, c - r - 0.02, c + 0.35, 3.1, c - r + 0.25, 0, TREFOIL_SIGN, CONCRETE);
	}

	// ------------------------------------------------------------------ pump house

	/**
	 * A travelling water screen: two steel side frames down into the intake bay, the mesh baskets on their chain,
	 * the head section with its drive motor and the spray header that washes the baskets into the trash trough.
	 */
	static void waterScreen(Frame f) {
		for (double x0 : new double[] {0, 5.5}) {
			f.box(x0, 0, 0.1, x0 + 0.5, 9.0, 1.9, PAINT_BLUE);
		}
		f.box(0.5, 0.2, 0.7, 5.5, 7.6, 0.85, GRATING);
		f.box(0.5, 0.2, 1.15, 5.5, 7.6, 1.3, GRATING);
		if (f.detail) {
			for (double y = 0.6; y < 7.6; y += 0.6) {
				f.box(0.5, y, 0.65, 5.5, y + 0.06, 1.35, STAINLESS_DARK);
			}
		}
		// Head section.
		f.box(0.2, 7.6, 0.0, 5.8, 8.8, 2.0, PAINT_BLUE);
		f.box(0.3, 7.3, -0.4, 5.7, 7.6, 0.2, STAINLESS_DARK);
		f.cylinder(v(5.8, 8.2, 1.0), v(6.1, 8.2, 1.0), 0.35, f.sides(12), PAINT_GREY, true);
		f.pipe(0.12, PAINT_BLUE, v(0.5, 8.0, -0.1), v(5.5, 8.0, -0.1));
		railX(f, 0.2, 5.8, 8.8, 1.95);
		f.plate(2.4, 8.1, -0.02, 3.6, 8.5, 0.0, 0, NAMEPLATE, PAINT_BLUE);
	}

	// ------------------------------------------------------------------ control building

	/** The main control room's large display: a steel frame and stand holding six screens ({@link #glow}). */
	static void displayWall(Frame f) {
		f.box(0, 0, 0.3, 9, 0.5, 1.0, PAINT_GREY);
		f.box(0.15, 0.5, 0.3, 8.85, 3.85, 1.0, STAINLESS_DARK);
		f.box(0, 3.85, 0.25, 9, 4.0, 1.0, PAINT_GREY);
		if (f.detail) {
			for (int i = 0; i < 4; i++) {
				double x = 0.3 + i * 2.8;
				f.box(x - 0.03, 0.5, 0.27, x + 0.07, 3.85, 0.3, RUBBER);
			}
			f.box(0.15, 2.08, 0.275, 8.85, 2.12, 0.3, RUBBER);
		}
	}

	/**
	 * A main control board: a benchboard with the sloped desk of switches at the front and the vertical mimic board
	 * behind with meters, recorders and the annunciator windows along the top ({@link #glow}).
	 */
	static void controlBoard(Frame f) {
		f.box(0.05, 0, 0.1, 5.95, 0.85, 1.2, PAINT_GREY);
		f.box(0.05, 0, 0.05, 5.95, 0.12, 0.1, RUBBER);
		Vec3 tl = v(0.05, 1.15, 1.2), bl = v(0.05, 0.85, 0.1), br = v(5.95, 0.85, 0.1), tr = v(5.95, 1.15, 1.2);
		f.tiled(tl, bl, br, tr, v(0, 1.1, -0.3).normalize(), DESK, 1.5);
		f.tri(v(0.05, 1.15, 1.2), v(0.05, 0.85, 0.1), v(0.05, 0.85, 1.2), v(-1, 0, 0), PAINT_GREY);
		f.tri(v(5.95, 1.15, 1.2), v(5.95, 0.85, 1.2), v(5.95, 0.85, 0.1), v(1, 0, 0), PAINT_GREY);
		f.box(0.05, 0.85, 0.08, 5.95, 0.9, 0.12, PAINT_GREY);
		// The vertical board, its mimic face toward the operators, a hood over the annunciators.
		f.box(0.05, 0, 1.2, 5.95, 3.0, 1.95, PAINT_GREY);
		f.tiled(v(0.05, 2.75, 1.18), v(0.05, 1.15, 1.18), v(5.95, 1.15, 1.18), v(5.95, 2.75, 1.18), v(0, 0, -1), MIMIC, 1.5);
		f.box(0.05, 2.95, 0.95, 5.95, 3.0, 1.2, PAINT_GREY);
		if (f.detail) {
			// Switch handles on the desk.
			for (int k = 0; k < 10; k++) {
				double x = 0.4 + k * 0.55, z = 0.55;
				f.box(x - 0.05, 0.98, z - 0.05, x + 0.05, 1.08, z + 0.05, k % 4 == 0 ? PIPE_RED : RUBBER);
			}
			f.plate(5.2, 1.2, 1.16, 5.8, 1.7, 1.18, 0, NAMEPLATE, PAINT_GREY);
		}
	}

	/** Medium-voltage switchgear: four cabinets in a line, cable trays on top ({@link #glow} lamps). */
	static void switchgear(Frame f) {
		for (int k = 0; k < 4; k++) {
			double x0 = k * 2 + 0.05, x1 = k * 2 + 1.95;
			f.box(x0, 0.1, 0.1, x1, 2.85, 1.9, PAINT_GREY);
			f.tiled(v(x0 + 0.05, 2.8, 0.08), v(x0 + 0.05, 0.15, 0.08), v(x1 - 0.05, 0.15, 0.08), v(x1 - 0.05, 2.8, 0.08), v(0, 0, -1), SWITCHGEAR, 1.4);
		}
		f.box(0, 0, 0.05, 8, 0.1, 1.95, CARBON_STEEL);
		f.box(0.2, 2.85, 0.51, 7.8, 3.0, 1.49, GRATING);
		f.box(0.2, 2.85, 0.45, 7.8, 3.0, 0.5, CARBON_STEEL);
		f.box(0.2, 2.85, 1.5, 7.8, 3.0, 1.55, CARBON_STEEL);
	}

	/**
	 * An air handling unit: a long panelled casing with the intake louvre at the front, filter and coil sections with
	 * their access doors, the fan section with its motor, and the supply duct turning up at the back.
	 */
	static void airHandler(Frame f) {
		f.box(0.1, 0, 0.1, 3.9, 0.25, 7.9, CARBON_STEEL);
		f.box(0.2, 0.25, 0.2, 3.8, 2.6, 7.8, PAINT_WHITE);
		f.plate(0.4, 0.45, 0.18, 3.6, 2.4, 0.2, 0, LOUVRE, PAINT_WHITE);
		// Access doors on both long sides.
		f.plate(3.8, 0.4, 0.8, 3.82, 2.45, 3.8, 3, FILTER_BANK, PAINT_WHITE);
		f.plate(3.8, 0.4, 4.2, 3.82, 2.45, 7.2, 3, FILTER_BANK, PAINT_WHITE);
		f.plate(0.18, 0.4, 0.8, 0.2, 2.45, 3.8, 2, FILTER_BANK, PAINT_WHITE);
		f.plate(0.18, 0.4, 4.2, 0.2, 2.45, 7.2, 2, FILTER_BANK, PAINT_WHITE);
		// The supply duct up from the fan section, the motor's box, coil piping.
		f.box(1.0, 2.6, 6.0, 3.0, 3.0, 8.0, STAINLESS_DARK);
		f.box(0.9, 2.6, 5.9, 3.1, 2.7, 8.1, CARBON_STEEL);
		f.box(0.8, 2.6, 2.0, 1.4, 2.95, 2.6, PAINT_GREY);
		f.pipe(0.12, PIPE_RED, v(3.8, 1.6, 3.6), v(3.95, 1.6, 3.6), v(3.95, -0.05, 3.6));
		f.pipe(0.12, PAINT_BLUE, v(3.8, 1.0, 4.4), v(3.95, 1.0, 4.4), v(3.95, -0.05, 4.4));
	}
}
