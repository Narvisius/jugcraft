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
			case EMERGENCY_DIESEL_GENERATOR -> front(f, 0.9, 1.4, 1.6, 1.9, 0.08, SCREEN);
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
		f.box(tx + 2.25, hy - 1.0, deep / 2 - 0.12, tx + 2.55, hy - 0.2, deep / 2 + 0.12, CARBON_STEEL);
		f.box(tx + 2.25, hy - 1.2, deep / 2 - 0.12, tx + 3.0, hy - 0.95, deep / 2 + 0.12, CARBON_STEEL);
		f.box(tx + 2.75, hy - 1.2, deep / 2 - 0.12, tx + 3.0, hy - 0.7, deep / 2 + 0.12, CARBON_STEEL);
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
			f.cylinder(v(2.5, 4.3, z), v(2.5, 5.0, z), 0.5, f.sides(14), INSULATION, true);
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
	 * An emergency diesel generator set on its skid: the V engine block with its rocker covers, turbochargers and
	 * exhaust manifolds, the flywheel housing, the generator with its end bell and terminal box, and the local panel.
	 */
	static void dieselGenerator(Frame f) {
		f.box(0.1, 0, 0.2, 4.9, 0.43, 13.8, CARBON_STEEL);
		f.box(0.12, 0.43, 0.22, 4.88, 0.45, 13.78, GRATING);
		// Engine block and the two banks of cylinder heads.
		f.box(0.9, 0.46, 1.2, 4.1, 2.6, 8.8, PAINT_GREEN);
		f.box(1.2, 0.46, 1.0, 3.8, 1.2, 9.0, STAINLESS_DARK);
		for (int i = 0; i < 8; i++) {
			double z = 1.4 + i * 0.92;
			f.box(0.95, 2.6, z, 2.15, 3.15, z + 0.8, PAINT_GREEN);
			f.box(2.85, 2.6, z, 4.05, 3.15, z + 0.8, PAINT_GREEN);
			if (f.detail) {
				f.box(1.1, 3.15, z + 0.1, 2.0, 3.35, z + 0.7, STAINLESS_DARK);
				f.box(3.0, 3.15, z + 0.1, 3.9, 3.35, z + 0.7, STAINLESS_DARK);
			}
		}
		// Exhaust manifolds in the vee, the turbochargers at the front, the stack pipe going up.
		f.cylinder(v(2.5, 3.2, 1.2), v(2.5, 3.2, 8.6), 0.32, f.sides(12), INSULATION, true);
		for (double x : new double[] {1.4, 3.6}) {
			f.revolve(v(x, 3.0, 0.3), v(x, 3.0, 1.2), new double[][] {{0, 0.3}, {0.2, 0.55}, {0.7, 0.55}, {0.9, 0.35}}, f.sides(14), STAINLESS, true, true);
		}
		f.pipe(0.35, INSULATION, v(2.5, 3.2, 1.0), v(2.5, 4.95, 1.0));
		// Flywheel housing, coupling, the generator and its end bell, the terminal box.
		f.revolve(v(2.5, 1.9, 8.8), v(2.5, 1.9, 9.6), new double[][] {{0, 1.5}, {0.8, 1.5}}, f.sides(20), PAINT_GREEN, true, true);
		f.cylinder(v(2.5, 2.2, 9.6), v(2.5, 2.2, 13.2), 1.85, f.sides(24), GENERATOR, true);
		f.revolve(v(2.5, 2.2, 13.2), v(2.5, 2.2, 13.8), new double[][] {{0, 1.6}, {0.5, 1.2}, {0.6, 0}}, f.sides(20), GENERATOR, false, false);
		f.box(0.35, 4.0, 10.3, 2.0, 4.9, 11.8, PAINT_GREY);
		f.box(2.0, 0.46, 9.8, 3.0, 0.9, 13.0, CARBON_STEEL);
		// The local control panel at the front left.
		f.box(0.3, 0.46, 0.1, 2.2, 2.4, 0.9, PAINT_GREY);
		f.plate(1.7, 1.5, 0.08, 2.1, 1.9, 0.1, 0, NAMEPLATE, PAINT_GREY);
		f.plate(3.0, 0.6, 0.15, 4.4, 1.6, 0.2, 0, HAZARD, CARBON_STEEL);
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
		f.cylinder(v(5.8, 8.2, 1.0), v(6.0, 8.2, 1.0), 0.35, f.sides(12), PAINT_GREY, true);
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
			f.box(0.15, 2.08, 0.27, 8.85, 2.12, 0.3, RUBBER);
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
		f.box(0.2, 2.85, 0.5, 7.8, 3.0, 1.5, GRATING);
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
