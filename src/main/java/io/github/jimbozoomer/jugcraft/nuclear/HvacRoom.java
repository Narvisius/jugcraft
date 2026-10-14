package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

/**
 * The Control Room HVAC Equipment Room (round 26, #11; Justin: "Control Room HVAC Equipment Room", all four kinds of
 * machine, "Switch to emergency mode", "Real ducts with dampers"): its HVAC control panel runs the room.
 *
 * <p>Normally train A runs (its air handler, chiller and chilled water pump; its normal outside air damper CR-D-1A open)
 * and train B stands by, its dampers shut. In emergency both trains go to filtered air: the normal outside air dampers
 * shut, the emergency intake dampers (CR-D-2) and filtration unit outlet dampers (CR-D-3) open, the filtration units'
 * booster fans start and every air handler, chiller and pump runs. The panel's selector is NORMAL, AUTO or EMERGENCY;
 * on AUTO it goes to emergency on a reactor trip, the reactor's high temperature (or damaged fuel), or high radiation at
 * either intake (its radiation monitors: the dose rate at the monitor, and the plume from damaged fuel).
 *
 * <p>Once a second the panel reads the nearest reactor (as a relay room cabinet does), its radiation monitors, then sets
 * the dampers (each one's variant: 2 x label + open, tools/hvac_room.py DAMPERS) and the machines (running, by their
 * anchor's train) in its room, and sends the players what it found: the mimic (each damper or machine lit red open or
 * running, green shut or stopped, dark if missing) and its annunciator. client/EdgTelemetry.java hvac draws the panel and
 * the monitors from that; client/NuclearRenderer.java the machines' gauges, lamps and the chiller's display.
 */
public final class HvacRoom {
	public static final String[] KEYS = {"hv_control_panel", "hv_rad_monitor", "hv_tox_monitor", "hv_refrig_monitor"};
	public static final int CONTROL = 0, RAD = 1, TOX = 2, REFRIG = 3;
	/** The mode selector's positions (left, up, right): its pointer's hv_sel&lt;k&gt; (tools/hvac_room.py). */
	public static final int NORMAL = 0, AUTO = 1, EMERGENCY = 2;
	public static final String[] MODES = {"NORMAL", "AUTO", "EMERGENCY"};
	/** The panel's room: this many blocks across either way, below and above it. */
	public static final int REACH = 32, BELOW = 3, ABOVE = 10;
	/** The dampers by label (tools/hvac_room.py DAMPERS). */
	public static final String[] DAMPER_TAGS = {"CR-D-1A", "CR-D-1B", "CR-D-2A", "CR-D-2B", "CR-D-3A", "CR-D-3B"};
	public static final String[] DAMPER_DUTIES = {"NORMAL OA ISOLATION", "EMERGENCY INTAKE", "CREFS OUTLET"};
	/** The mimic's parts along each train (tools/hvac_room.py HP_MAIN, HP_BRANCH: hv_&lt;part&gt;_&lt;train&gt;). */
	public static final String[] PARTS = {"d1", "ahu", "d2", "efu", "d3", "ch", "pump"};
	public static final int D1 = 0, AHU = 1, D2 = 2, EFU = 3, D3 = 4, CH = 5, PUMP = 6;
	/** The mimic's bits: running (open) at train * 7 + part, there at PRESENT + train * 7 + part. */
	public static final int PRESENT = 14;
	/**
	 * The annunciator's windows (tools/hvac_room.py HP_ANN, row by row): REACTOR TRIP, RX HIGH TEMP, INTAKE HIGH RAD,
	 * TOXIC GAS, EMERG MODE, CREFS LOW FLOW, CHILLER TRIP, CR LOW DIFF PRESS; and the room in emergency.
	 */
	public static final int A_RX_TRIP = 1, A_RX_HIGH_TEMP = 2, A_HIGH_RAD = 4, A_TOXIC = 8, A_EMERGENCY = 16, A_LOW_FLOW = 32, A_CHILLER = 64,
			A_LOW_DP = 128, EMERGENCY_ON = 256;
	/**
	 * The radiation monitors (counts a minute): background, what a mSv/h of dose rate at the detector reads (a GM
	 * detector's 3,300 cpm a mR/h), the ALERT and HIGH setpoints, and the plume from a reactor's damaged fuel in the
	 * intake air.
	 */
	public static final double RAD_BACKGROUND = 40, CPM_PER_MSV = 3.3e5, RAD_ALERT = 1e3, RAD_HIGH = 1e4, PLUME = 2e5;
	/** A filtration unit's filtered air (CFM), each. */
	public static final double EFU_CFM = 1_000;
	/** How often a panel looks for its dampers again (game ticks). */
	static final int RESCAN = 200;

	private HvacRoom() {
	}

	/** Which kind a block is ({@link #CONTROL} to {@link #REFRIG}), or -1 for anything else. */
	public static int kind(BlockState state) {
		for (int k = 0; k < KEYS.length; k++) {
			if (state.getBlock() == PlantInterior.BLOCKS.get(KEYS[k])) {
				return k;
			}
		}
		return -1;
	}

	/** The mimic part a machine is, or -1. */
	public static int part(EquipmentKind kind) {
		return switch (kind) {
			case CR_FILTRATION_UNIT -> EFU;
			case CR_AIR_HANDLER -> AHU;
			case CR_CHILLER -> CH;
			case CHW_PUMP -> PUMP;
			default -> -1;
		};
	}

	/** A machine's or damper's tag by part and train: CRF-1A, AHU-1B, CH-1A, CHP-1B, CR-D-2A. */
	public static String tag(int part, int train) {
		String t = train == 0 ? "A" : "B";
		return switch (part) {
			case EFU -> "CRF-1" + t;
			case AHU -> "AHU-1" + t;
			case CH -> "CH-1" + t;
			case PUMP -> "CHP-1" + t;
			case D1 -> "CR-D-1" + t;
			case D2 -> "CR-D-2" + t;
			default -> "CR-D-3" + t;
		};
	}

	/** A damper's mimic part by its label (0 to 5: CR-D-1A, 1B, 2A, 2B, 3A, 3B). */
	public static int damperPart(int label) {
		return switch (Math.floorMod(label, 6) / 2) {
			case 0 -> D1;
			case 1 -> D2;
			default -> D3;
		};
	}

	/** The room in emergency: the selector at EMERGENCY, or AUTO with a trip, a hot reactor, damaged fuel or a high intake. */
	public static boolean emergency(int mode, RelayRoom.Reactor r, boolean highRad) {
		if (mode == EMERGENCY) {
			return true;
		}
		return mode == AUTO && (r.status() == 3 || r.present() && r.temperature() >= ReactorCore.ALARM_TEMP
				|| (r.alarms() & ReactorCore.A_FUEL_DAMAGED) != 0 || highRad);
	}

	/**
	 * Whether a train's part is open or running: train A's air handler, chiller and pump always, train B's in emergency;
	 * the normal outside air damper while its train runs normally; the filtration units and their dampers in emergency.
	 */
	public static boolean wants(int part, int train, boolean emergency) {
		return switch (part) {
			case D1 -> train == 0 && !emergency;
			case D2, EFU, D3 -> emergency;
			default -> train == 0 || emergency;
		};
	}

	/** A radiation monitor's count rate (cpm): the dose rate at it, the plume from its reactor's damaged fuel. */
	public static double countRate(double mSvPerHour, RelayRoom.Reactor r) {
		return RAD_BACKGROUND + Math.max(0, mSvPerHour) * CPM_PER_MSV + ((r.alarms() & ReactorCore.A_FUEL_DAMAGED) != 0 ? PLUME : 0);
	}

	/** Once a second: an HVAC panel or monitor does its part. */
	public static void tick(LivePanelBlockEntity panel, ServerLevel level, int kind) {
		switch (kind) {
			case CONTROL -> control(panel, level);
			case RAD -> radiation(panel, level);
			default -> {
				// The toxic gas analysers and the refrigerant monitor read clean air (no gas or leak is modelled): the
				// client draws their zeros, nothing to send.
			}
		}
	}

	/** A radiation monitor: the reactor (for its plume), the dose rate at the detector, sent when it has moved. */
	private static void radiation(LivePanelBlockEntity panel, ServerLevel level) {
		panel.readReactor(level);
		Vec3 at = Vec3.atCenterOf(panel.getBlockPos().above());
		double cpm = countRate(Radiation.rateAt(level, at), panel.reactor());
		double was = panel.hvValue;
		if (was <= 0 || Math.abs(cpm - was) > was * 0.08 || (cpm >= RAD_ALERT) != (was >= RAD_ALERT) || (cpm >= RAD_HIGH) != (was >= RAD_HIGH)) {
			panel.hvValue = cpm;
			panel.syncNow(level);
		}
	}

	/** The panel: reads the plant, sets its room's dampers and machines, shows what it found. */
	private static void control(LivePanelBlockEntity panel, ServerLevel level) {
		panel.readReactor(level);
		BlockPos here = panel.getBlockPos();
		boolean highRad = false;
		for (LivePanelBlockEntity monitor : panel.panelsAround(level, PlantInterior.BLOCKS.get(KEYS[RAD]))) {
			highRad |= monitor.hvValue >= RAD_HIGH;
		}
		RelayRoom.Reactor r = panel.reactor();
		boolean emergency = emergency(panel.mode, r, highRad);
		long now = level.getGameTime();
		List<LivePanelBlockEntity> others = panel.panelsAround(level, PlantInterior.BLOCKS.get(KEYS[CONTROL]));
		int state = 0;
		// The machines in its room (each to its nearest panel).
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : EquipmentRegistry.of(level).anchors().entrySet()) {
			int part = part(e.getValue().kind());
			BlockPos at = e.getKey();
			if (part < 0 || !inRoom(here, at) || nearerPanel(others, here, at) || !level.isLoaded(at)
					|| !(level.getBlockEntity(at) instanceof EquipmentBlockEntity machine)) {
				continue;
			}
			int train = train(machine.getBlockState());
			boolean run = wants(part, train, emergency);
			machine.setRunning(now, run);
			state |= 1 << PRESENT + train * 7 + part | (run ? 1 << train * 7 + part : 0);
		}
		// Its dampers, found again every ten seconds.
		if (panel.hvDampers == null || panel.hvRescan-- <= 0) {
			panel.hvDampers = findDampers(level, here);
			panel.hvRescan = RESCAN / 20;
		}
		Block damper = PlantInterior.BLOCKS.get("hv_damper");
		for (BlockPos at : panel.hvDampers) {
			BlockState s = level.getBlockState(at);
			if (s.getBlock() != damper || nearerPanel(others, here, at)) {
				continue;
			}
			int v = s.getValue(DamperBlock.VARIANT), label = v / 2, part = damperPart(label), train = label % 2;
			boolean open = wants(part, train, emergency);
			if ((v & 1) != (open ? 1 : 0)) {
				level.setBlock(at, s.setValue(DamperBlock.VARIANT, label * 2 + (open ? 1 : 0)), Block.UPDATE_CLIENTS);
				// The actuator drives the blades round.
				level.playSound(null, at, open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.5F, 0.7F);
			}
			state |= 1 << PRESENT + train * 7 + part | (open ? 1 << train * 7 + part : 0);
		}
		int alarms = alarms(r, highRad, emergency, state);
		if (state != panel.hvState || alarms != panel.hvAlarms) {
			if ((alarms & EMERGENCY_ON) != (panel.hvAlarms & EMERGENCY_ON)) {
				panel.hvSince = now;
			}
			panel.hvState = state;
			panel.hvAlarms = alarms;
			panel.syncNow(level);
		}
	}

	/** The annunciator's windows lit, and the room in emergency, from what the panel read and found. */
	public static int alarms(RelayRoom.Reactor r, boolean highRad, boolean emergency, int state) {
		int a = 0;
		if (r.status() == 3) a |= A_RX_TRIP;
		if (r.present() && r.temperature() >= ReactorCore.ALARM_TEMP) a |= A_RX_HIGH_TEMP;
		if (highRad) a |= A_HIGH_RAD;
		if (emergency) a |= A_EMERGENCY | EMERGENCY_ON;
		int efus = running(state, EFU), ahus = running(state, AHU);
		if (emergency && efus == 0) a |= A_LOW_FLOW;
		// A running air handler without its chiller running.
		for (int t = 0; t < 2; t++) {
			if (on(state, AHU, t) && !on(state, CH, t)) a |= A_CHILLER;
		}
		// The control room's pressure: held up by the air handlers' outside air, more by the filtration units in emergency.
		if (ahus == 0 || emergency && efus == 0) a |= A_LOW_DP;
		return a;
	}

	/** Whether a train's part is running (open), from the panel's mimic bits. */
	public static boolean on(int state, int part, int train) {
		return (state >> train * 7 + part & 1) != 0;
	}

	public static boolean present(int state, int part, int train) {
		return (state >> PRESENT + train * 7 + part & 1) != 0;
	}

	/** How many of a part are running (both trains). */
	public static int running(int state, int part) {
		return (on(state, part, 0) ? 1 : 0) + (on(state, part, 1) ? 1 : 0);
	}

	/** A machine's train: its anchor's variant (0 A, 1 B). */
	public static int train(BlockState anchor) {
		return anchor.hasProperty(EquipmentAnchorBlock.VARIANT) ? Math.min(1, anchor.getValue(EquipmentAnchorBlock.VARIANT)) : 0;
	}

	static boolean inRoom(BlockPos panel, BlockPos at) {
		int dy = at.getY() - panel.getY();
		return Math.abs(at.getX() - panel.getX()) <= REACH && Math.abs(at.getZ() - panel.getZ()) <= REACH && dy >= -BELOW && dy <= ABOVE;
	}

	/** Whether another HVAC panel is nearer to {@code at} (it runs that one). */
	private static boolean nearerPanel(List<LivePanelBlockEntity> others, BlockPos here, BlockPos at) {
		double d = at.distSqr(here);
		for (LivePanelBlockEntity o : others) {
			if (o.getBlockPos().distSqr(at) < d) {
				return true;
			}
		}
		return false;
	}

	/** The dampers in a panel's room (skipping the chunk sections that hold none). */
	static List<BlockPos> findDampers(ServerLevel level, BlockPos here) {
		Block damper = PlantInterior.BLOCKS.get("hv_damper");
		List<BlockPos> out = new ArrayList<>();
		int y0 = here.getY() - BELOW, y1 = here.getY() + ABOVE;
		for (int cx = (here.getX() - REACH) >> 4; cx <= (here.getX() + REACH) >> 4; cx++) {
			for (int cz = (here.getZ() - REACH) >> 4; cz <= (here.getZ() + REACH) >> 4; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				LevelChunkSection[] sections = chunk.getSections();
				for (int i = Math.max(0, level.getSectionIndex(y0)); i <= Math.min(sections.length - 1, level.getSectionIndex(y1)); i++) {
					LevelChunkSection section = sections[i];
					if (section == null || section.hasOnlyAir() || !section.maybeHas(s -> s.getBlock() == damper)) {
						continue;
					}
					int base = level.getSectionYFromSectionIndex(i) << 4;
					for (int y = 0; y < 16; y++) {
						for (int z = 0; z < 16; z++) {
							for (int x = 0; x < 16; x++) {
								if (section.getBlockState(x, y, z).getBlock() == damper) {
									BlockPos at = new BlockPos((cx << 4) + x, base + y, (cz << 4) + z);
									if (inRoom(here, at)) {
										out.add(at);
									}
								}
							}
						}
					}
				}
			}
		}
		return out;
	}

	/** The panel's use message: its mode, the room's state, its alarms. */
	public static String describe(LivePanelBlockEntity panel) {
		int a = panel.hvAlarms, s = panel.hvState;
		boolean emergency = (a & EMERGENCY_ON) != 0;
		String trains = emergency
				? String.format(Locale.ROOT, "EMERGENCY, %d filtration unit(s) running on filtered air", running(s, EFU))
				: running(s, AHU) > 0 ? "NORMAL, train A running, train B on standby" : "NORMAL, no air handler running";
		return String.format(Locale.ROOT, "%s: %s; %d alarm(s)", MODES[Math.floorMod(panel.mode, 3)], trains, Integer.bitCount(a & 0xFF));
	}

	/** A damper's tag, duty and position, for its use message. */
	public static String describeDamper(int variant) {
		int label = Math.floorMod(variant / 2, 6);
		return String.format(Locale.ROOT, "%s %s: %s", DAMPER_TAGS[label], DAMPER_DUTIES[label / 2], (variant & 1) != 0 ? "OPEN" : "SHUT");
	}
}
