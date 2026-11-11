package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The main turbine-generator and its moisture separator reheaters at the turbine (turbine hall part 1; Justin:
 * "Turbine hall (Recommended)", "Local trip + turning gear"; tools/turbine_set.py, tools/turbine_hall.py).
 *
 * <p>Each half second a turbine finds the reactor whose unit it belongs to (the nearest within
 * {@link EquipmentBlockEntity#PANEL_RANGE} if none claims it) and copies what its front standard, valves and turning gear
 * show: its speed, load, latched or tripped, the turning gear in and its motor running (synced). A reheater joins the
 * nearest turbine within {@link #MSR_REACH} blocks (its gauges read that turbine's load).
 *
 * <p>At the turbine: the manual trip on the front standard (a first click lifts its guard for five seconds, a second
 * pulls the handle: the turbine trips as from the control room, the reactor following above P-9); the turning gear's
 * lever beside the last LP (in only at zero speed: its motor then rolls the shaft at 3 rpm; out at any time). A click
 * elsewhere says how the turbine stands.
 *
 * <p>Turbine hall part 2 (Justin: "Pumps you can switch, plant responds"; tools/feed_train.py): the condenser shells and
 * hotwells, the condensate pumps, the main feed pumps, the lube oil reservoir, the EHC unit, the seal oil unit and the
 * gas control panel each join the nearest turbine within {@link #FEED_REACH} blocks and show its unit's plant (synced in
 * the turbine's fields: its speed, the unit's load, the turbine's flags and the piece's own). A condensate or main feed
 * pump says each half second it is there (its unit's ReactorCore then limits main feed to what its pumps carry). At a
 * condensate pump's station START runs it and STOP stops it (it then stands by in AUTO; STOP again takes it out of
 * AUTO); at a main feed pump's station TRIP trips it and RESET resets it (it is started again from the secondary board).
 * Sneaking with an empty hand relabels a piece that has labels.
 */
public final class TurbineHall {
	/** Synced flags ({@link EquipmentBlockEntity#turbineFlags}). */
	public static final int PRESENT = 1, LATCHED = 2, TURNING_GEAR = 4, TG_MOTOR = 8, ONLINE = 16, HOT = 32;
	/**
	 * Turbine hall part 2's own: the unit's steam reaches its turbine; this pump running; standing by in AUTO; tripped;
	 * started by AUTO; the condensate header pressurised; feedwater isolated; and (two bits from {@link #PEERS}) how many
	 * pumps of its kind run.
	 */
	public static final int STEAM = 64, RUN = 128, AUTO = 256, TRIPPED = 512, AUTO_STARTED = 1024, CP_HDR = 2048, FEED_ISO = 4096, PEERS = 13;
	/** How far from a turbine the feed train and the oil plant may stand (blocks); ticks a pump's word lasts at its core. */
	public static final int FEED_REACH = 64, SEEN = 40;
	/** A condensate pump's station (its local frame, blocks: the buttons on its face toward -z at {@link #CP_FACE}). */
	static final double CP_START_X = 0.17, CP_STOP_X = 0.43, CP_BUTTON_Y = 1.12, CP_FACE = 0.02;
	/** A main feed pump's turbine control station: TRIP and RESET on its door at {@link #MFP_FACE}. */
	static final double MFP_TRIP_X = 3.1, MFP_RESET_X = 3.44, MFP_BUTTON_Y = 1.35, MFP_FACE = 0.06;
	/** A condensate pump stopped by hand stands by in AUTO after this long (ticks): another standby starts first. */
	static final int CP_HOLD = 200;
	/** How long the trip handle's guard stays up after it is lifted (ticks). */
	public static final int GUARD = 100;
	/** How far from a turbine its reheaters may stand (blocks). */
	public static final int MSR_REACH = 48;
	/** Where things are on the turbine (its local frame, blocks): the trip handle's housing on the front standard. */
	static final double TRIP_X0 = 5.05, TRIP_X1 = 5.95, TRIP_Y0 = 0.25, TRIP_Y1 = 0.9, TRIP_Z = 0.45;
	/** The turning gear's lever (on the pedestal's left between LP C and the generator) and its station beside it. */
	static final double LEVER_X = 3.4, LEVER_Z0 = 36.15, LEVER_Z1 = 37.2, LEVER_Y0 = 0.75, LEVER_Y1 = 1.85;

	private TurbineHall() {
	}

	/** Every half second (spread by position): a turbine copies its reactor's turbine, a reheater finds its turbine. */
	public static void tick(ServerLevel level, EquipmentBlockEntity e, long now) {
		if (Math.floorMod(now + e.getBlockPos().asLong(), 10) == 0) {
			refresh(level, e, now);
		}
	}

	/** Brings a turbine's or a reheater's synced state up to date now. */
	static void refresh(ServerLevel level, EquipmentBlockEntity e, long now) {
		EquipmentRegistry registry = EquipmentRegistry.of(level);
		if (e.kind() == EquipmentKind.TURBINE_GENERATOR) {
			EquipmentBlockEntity vessel = reactor(level, registry, e, now);
			int flags = 0;
			float rpm = 0, load = 0;
			if (vessel != null) {
				ReactorCore c = vessel.core;
				ReactorUnit unit = vessel.unit();
				boolean steam = unit != null && unit.steamToPower();
				boolean latched = steam && !c.turbineTripped && !c.scrammed;
				flags |= PRESENT;
				if (latched) flags |= LATCHED;
				if (c.turningGear && c.turbineSpeed <= ReactorCore.TURNING_GEAR * 3) {
					flags |= TURNING_GEAR;
					if (steam) flags |= TG_MOTOR;
				}
				if (c.electric > 0) flags |= ONLINE;
				if (c.temperature > 150) flags |= HOT;
				rpm = (float) (c.turbineSpeed * 1800);
				load = (float) Math.max(0, Math.min(1.2, c.electric / (double) Math.max(1, c.design.ratedJe())));
			}
			if (flags != e.turbineFlags || Math.abs(rpm - e.turbineRpm) > 0.4 || Math.abs(load - e.turbineLoad) > 0.004) {
				e.turbineFlags = flags;
				e.turbineRpm = rpm;
				e.turbineLoad = load;
				e.changed();
			}
		} else if (e.kind().feedTrain()) {
			refreshPiece(level, registry, e, now);
		} else if (e.kind() == EquipmentKind.MOISTURE_SEPARATOR_REHEATER) {
			if (e.linked == null || Math.floorMod(now + e.getBlockPos().asLong(), 100) == 0 || !level.isLoaded(e.linked)
					|| !(level.getBlockEntity(e.linked) instanceof EquipmentBlockEntity t) || t.kind() != EquipmentKind.TURBINE_GENERATOR) {
				BlockPos found = nearest(registry, e.middle(), EquipmentKind.TURBINE_GENERATOR, MSR_REACH);
				if (!java.util.Objects.equals(found, e.linked)) {
					e.linked = found;
					e.changed();
				}
			}
		}
	}

	/**
	 * The reactor this turbine belongs to: the one whose unit lists it, else the nearest (looked for again every five
	 * seconds and whenever the plant changes; {@link EquipmentBlockEntity#linked} keeps it).
	 */
	static @Nullable EquipmentBlockEntity reactor(ServerLevel level, EquipmentRegistry registry, EquipmentBlockEntity turbine, long now) {
		if (turbine.linked != null && Math.floorMod(now + turbine.getBlockPos().asLong(), 100) != 0 && turbine.reactorVersion == registry.version()
				&& level.isLoaded(turbine.linked) && level.getBlockEntity(turbine.linked) instanceof EquipmentBlockEntity v && v.kind().reactor()) {
			return v;
		}
		turbine.reactorVersion = registry.version();
		BlockPos found = null;
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> en : registry.anchors().entrySet()) {
			if (en.getValue().kind().reactor() && level.isLoaded(en.getKey()) && level.getBlockEntity(en.getKey()) instanceof EquipmentBlockEntity v
					&& v.unit() != null && v.unit().turbines.contains(turbine.getBlockPos())) {
				found = en.getKey().immutable();
				break;
			}
		}
		if (found == null) {
			found = turbine.nearestReactor(registry);
		}
		if (!java.util.Objects.equals(found, turbine.linked)) {
			turbine.linked = found;
			turbine.changed();
		}
		return found != null && level.isLoaded(found) && level.getBlockEntity(found) instanceof EquipmentBlockEntity v ? v : null;
	}

	/** A piece's label (its anchor's variant, within its labels). */
	public static int label(Level level, EquipmentBlockEntity e) {
		net.minecraft.world.level.block.state.BlockState state = level.getBlockState(e.getBlockPos());
		int v = state.hasProperty(EquipmentAnchorBlock.VARIANT) ? state.getValue(EquipmentAnchorBlock.VARIANT) : 0;
		return Math.max(0, Math.min(e.kind().labels() - 1, v));
	}

	/** The turbine a piece of the feed train has joined (looked for again every five seconds), or null. */
	static @Nullable EquipmentBlockEntity turbineOf(ServerLevel level, EquipmentRegistry registry, EquipmentBlockEntity e, long now) {
		if (e.linked == null || Math.floorMod(now + e.getBlockPos().asLong(), 100) == 0 || !level.isLoaded(e.linked)
				|| !(level.getBlockEntity(e.linked) instanceof EquipmentBlockEntity t) || t.kind() != EquipmentKind.TURBINE_GENERATOR) {
			BlockPos found = nearest(registry, e.middle(), EquipmentKind.TURBINE_GENERATOR, FEED_REACH);
			if (!java.util.Objects.equals(found, e.linked)) {
				e.linked = found;
				e.changed();
			}
		}
		return e.linked != null && level.isLoaded(e.linked) && level.getBlockEntity(e.linked) instanceof EquipmentBlockEntity t
				&& t.kind() == EquipmentKind.TURBINE_GENERATOR ? t : null;
	}

	/** The reactor a piece's turbine belongs to, or null. */
	static @Nullable EquipmentBlockEntity vesselOf(ServerLevel level, @Nullable EquipmentBlockEntity turbine) {
		return turbine != null && turbine.linked != null && level.isLoaded(turbine.linked) && level.getBlockEntity(turbine.linked) instanceof EquipmentBlockEntity v
				&& v.kind().reactor() ? v : null;
	}

	/**
	 * A piece of the feed train or the oil plant: it finds its turbine and that turbine's reactor, copies the turbine's
	 * speed, the unit's load and the turbine's flags, adds its own (a pump: running, standing by, tripped, started by
	 * AUTO; how many of its kind run) and, a condensate or main feed pump, says it is there.
	 */
	static void refreshPiece(ServerLevel level, EquipmentRegistry registry, EquipmentBlockEntity e, long now) {
		EquipmentBlockEntity turbine = turbineOf(level, registry, e, now);
		EquipmentBlockEntity vessel = vesselOf(level, turbine);
		int flags = 0;
		float rpm = 0, load = 0;
		if (turbine != null && vessel != null && (turbine.turbineFlags & PRESENT) != 0) {
			ReactorCore c = vessel.core;
			ReactorUnit unit = vessel.unit();
			flags = turbine.turbineFlags & 63;
			if (unit != null && unit.steamToPower()) flags |= STEAM;
			if (c.feedIsolated) flags |= FEED_ISO;
			if (!c.hasCps() || c.cpOn() > 0) flags |= CP_HDR;
			rpm = turbine.turbineRpm;
			load = turbine.turbineLoad;
			int k = label(level, e);
			if (e.kind() == EquipmentKind.CONDENSATE_PUMP) {
				c.cpSeen[k] = SEEN;
				if ((c.cpRunning & 1 << k) != 0) flags |= RUN;
				else if ((c.cpAuto & 1 << k) != 0) flags |= AUTO;
				if ((c.cpAutoStarted & c.cpRunning & 1 << k) != 0) flags |= AUTO_STARTED;
				flags |= Math.min(3, c.cpOn()) << PEERS;
				flags |= CP_HDR;
			} else if (e.kind() == EquipmentKind.MAIN_FEED_PUMP) {
				c.mfpSeen[k] = SEEN;
				boolean turning = (c.mfpRunning & 1 << k) != 0 && (c.mfpTripped & 1 << k) == 0 && (flags & STEAM) != 0 && c.temperature > ReactorCore.MFP_STEAM;
				if (turning) flags |= RUN;
				if ((c.mfpTripped & 1 << k) != 0) flags |= TRIPPED;
				int on = c.mfpOn();
				flags |= Math.min(3, on) << PEERS;
				// Its speed follows the feed it carries (its share of the feed against its 60%).
				double feed = 0;
				for (int g = 0; g < c.loops; g++) {
					feed = Math.max(feed, c.sgFeed[g]);
				}
				double share = on > 0 ? Math.min(1, feed / (on * ReactorCore.MFP_SHARE)) : 0;
				rpm = turning ? (float) (2700 + 2500 * share) : 0;
			}
		}
		if (flags != e.turbineFlags || Math.abs(rpm - e.turbineRpm) > 2 || Math.abs(load - e.turbineLoad) > 0.004) {
			e.turbineFlags = flags;
			e.turbineRpm = rpm;
			e.turbineLoad = load;
			e.changed();
		}
	}

	/** Where on a pump's station a click landed. */
	public enum Button { START, STOP, TRIP, RESET, NONE }

	public static Button button(EquipmentBlockEntity e, @Nullable Vec3 hit) {
		if (hit == null) {
			return Button.NONE;
		}
		Vec3 l = Switchgear.local(e, hit);
		if (e.kind() == EquipmentKind.CONDENSATE_PUMP && l.z <= CP_FACE + 0.06 && Math.abs(l.y - CP_BUTTON_Y) <= 0.08) {
			return Math.abs(l.x - CP_START_X) <= 0.08 ? Button.START : Math.abs(l.x - CP_STOP_X) <= 0.08 ? Button.STOP : Button.NONE;
		}
		if (e.kind() == EquipmentKind.MAIN_FEED_PUMP && l.z <= MFP_FACE + 0.06 && Math.abs(l.y - MFP_BUTTON_Y) <= 0.1) {
			return Math.abs(l.x - MFP_TRIP_X) <= 0.1 ? Button.TRIP : Math.abs(l.x - MFP_RESET_X) <= 0.08 ? Button.RESET : Button.NONE;
		}
		return Button.NONE;
	}

	/** What a pump's button did to its unit's feed train. */
	public enum Pressed { STARTED, ALREADY, STOPPED_AUTO, AUTO_OFF, AUTO_ON, TRIPPED, ALREADY_TRIPPED, RESET, NOT_TRIPPED }

	/** A condensate pump's START or STOP (k its label): STOP stops a running pump (it stands by in AUTO), toggles AUTO on a stopped one. */
	public static Pressed condensatePump(ReactorCore c, int k, boolean start) {
		int bit = 1 << k;
		c.cpAutoStarted &= ~bit;
		if (start) {
			if ((c.cpRunning & bit) != 0) {
				return Pressed.ALREADY;
			}
			c.cpRunning |= bit;
			c.cpAuto &= ~bit;
			return Pressed.STARTED;
		}
		if ((c.cpRunning & bit) != 0) {
			c.cpRunning &= ~bit;
			c.cpAuto |= bit;
			c.cpHold[k] = CP_HOLD;
			return Pressed.STOPPED_AUTO;
		}
		c.cpAuto ^= bit;
		c.cpHold[k] = 0;
		return (c.cpAuto & bit) != 0 ? Pressed.AUTO_ON : Pressed.AUTO_OFF;
	}

	/** A main feed pump's TRIP or RESET (k its label): TRIP trips it out of service; RESET clears the trip. */
	public static Pressed mainFeedPump(ReactorCore c, int k, boolean trip) {
		int bit = 1 << k;
		if (trip) {
			if ((c.mfpTripped & bit) != 0) {
				return Pressed.ALREADY_TRIPPED;
			}
			c.mfpTripped |= bit;
			c.mfpRunning &= ~bit;
			return Pressed.TRIPPED;
		}
		if ((c.mfpTripped & bit) == 0) {
			return Pressed.NOT_TRIPPED;
		}
		c.mfpTripped &= ~bit;
		return Pressed.RESET;
	}

	/** Using a piece of the feed train or the oil plant: its pump's buttons, a new label, or how it stands. */
	public static void usePiece(Level level, EquipmentBlockEntity e, Player player, @Nullable Vec3 hit, boolean relabel) {
		if (level.isClientSide() || !(level instanceof ServerLevel server)) {
			return;
		}
		long now = level.getGameTime();
		int k = label(level, e);
		String tag = "1" + (char) ('A' + k);
		if (relabel && e.kind().labels() > 1) {
			net.minecraft.world.level.block.state.BlockState state = level.getBlockState(e.getBlockPos());
			int next = (k + 1) % e.kind().labels();
			level.setBlock(e.getBlockPos(), state.setValue(EquipmentAnchorBlock.VARIANT, next), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.relay_relabel", e.kind() == EquipmentKind.LO_STORAGE_TANK
					? (next == 0 ? "T-401 CLEAN" : "T-402 DIRTY") : "1" + (char) ('A' + next)));
			return;
		}
		EquipmentRegistry registry = EquipmentRegistry.of(server);
		EquipmentBlockEntity turbine = turbineOf(server, registry, e, now);
		EquipmentBlockEntity vessel = vesselOf(server, turbine);
		Button b = button(e, hit);
		Vec3 at = hit != null ? hit : e.middle();
		if (b != Button.NONE) {
			if (vessel == null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.feed_idle", FEED_REACH));
				return;
			}
			ReactorCore c = vessel.core;
			Pressed done = b == Button.START || b == Button.STOP ? condensatePump(c, k, b == Button.START) : mainFeedPump(c, k, b == Button.TRIP);
			vessel.changed();
			refreshPiece(server, registry, e, now);
			level.playSound(null, at.x, at.y, at.z, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6F, b == Button.TRIP || b == Button.STOP ? 0.8F : 1.1F);
			String kind = e.kind() == EquipmentKind.CONDENSATE_PUMP ? "cp" : "mfp";
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear." + kind + "_" + done.name().toLowerCase(java.util.Locale.ROOT), tag));
			return;
		}
		refreshPiece(server, registry, e, now);
		player.sendOverlayMessage(pieceStatus(e, tag, vessel != null ? vessel.core : null));
	}

	/** What a piece of the feed train or the oil plant tells whoever uses it. */
	static Component pieceStatus(EquipmentBlockEntity e, String tag, @Nullable ReactorCore c) {
		int f = e.turbineFlags;
		if ((f & PRESENT) == 0 || c == null) {
			return Component.translatable("message.jugcraft.nuclear.feed_idle", FEED_REACH);
		}
		double rpm = e.turbineRpm, load = e.turbineLoad;
		String n = "message.jugcraft.nuclear.";
		return switch (e.kind()) {
			case CONDENSER, CONDENSER_SHELL, CONDENSER_HOTWELL -> Component.translatable(n + "condenser_status", tag, fmt(value("VACUUM", f, rpm, load), 1),
					fmt(value("HOTWELL", f, rpm, load), 0));
			case CONDENSATE_PUMP -> Component.translatable(n + "cp_status", tag,
					Component.translatable(n + ((f & RUN) != 0 ? "pump_running" : (f & AUTO) != 0 ? "pump_standby" : "pump_stopped")), f >> PEERS & 3,
					fmt(value("CP AMPS", f, rpm, load), 0));
			case MAIN_FEED_PUMP -> Component.translatable(n + "mfp_status", tag,
					Component.translatable(n + ((f & TRIPPED) != 0 ? "pump_tripped" : (f & RUN) != 0 ? "pump_running" : "pump_stopped")), fmt(rpm, 0),
					fmt(value("MFP DISCHARGE", f, rpm, load), 0));
			case LUBE_OIL_RESERVOIR -> Component.translatable(n + "lo_status", fmt(value("BRG OIL HDR", f, rpm, load), 0), fmt(value("OIL TEMP", f, rpm, load), 0),
					Component.translatable(n + (lamp("tg_oil", f, rpm) ? "lo_tg" : lamp("aux_oil", f, rpm) ? "lo_aux" : "lo_mop")));
			case EHC_UNIT -> Component.translatable(n + "ehc_status", fmt(value("EHC PRESS", f, rpm, load), 0), fmt(value("FLUID TEMP", f, rpm, load), 0));
			case SEAL_OIL_UNIT -> Component.translatable(n + "so_status", fmt(value("SEAL OIL", f, rpm, load), 0), fmt(value("SEAL DP", f, rpm, load), 0));
			case H2_GAS_PANEL -> Component.translatable(n + "h2_status", fmt(value("H2 PURITY", f, rpm, load), 1), fmt(value("CASING PRESS", f, rpm, load), 0));
			// Turbine hall part 3: the lube oil room's plant.
			case LO_CONDITIONER -> Component.translatable(n + "lo_cond_status", fmt(value("COND FILTER DP", f, rpm, load), 0),
					Component.translatable(n + (lamp("cond_heat", f, rpm) ? "lo_cond_heating" : lamp("cond_run", f, rpm) ? "lo_cond_running" : "pump_stopped")));
			case LO_STORAGE_TANK -> Component.translatable(n + "lo_tank_status", tag.equals("1A") ? "T-401 CLEAN" : "T-402 DIRTY",
					fmt(level("LO STORAGE", f) * 100, 0));
			case LO_TRANSFER_PUMPS -> Component.translatable(n + "lo_xfer_status");
			default -> Component.empty();
		};
	}

	private static String fmt(double v, int dp) {
		return String.format(java.util.Locale.ROOT, "%,." + dp + "f", v);
	}

	/**
	 * A gauge's reading on a piece of the feed train or the oil plant (its unit's: psig, in Hg, F, amps, inches), from
	 * its synced flags, speed (a main feed pump's own) and the unit's load. The board's readings agree (BoardPlant): 60
	 * psig hydrogen at 97.5% purity, 24 psig bearing oil, 1,600 psig EHC fluid.
	 */
	public static double value(String name, int f, double rpm, double load) {
		boolean on = (f & PRESENT) != 0, run = (f & RUN) != 0, hot = (f & HOT) != 0, latched = (f & LATCHED) != 0, hdr = on && (f & CP_HDR) != 0;
		int peers = f >> PEERS & 3;
		double vacuum = on && (hot || latched || rpm > 1) ? 27.5 + 0.3 * (1 - Math.min(1, load)) : 0;
		double suction = hdr ? 360 - 40 * Math.min(1, load) : on ? 12 : 0;
		return switch (name) {
			case "VACUUM", "MFP EXH PRESS" -> vacuum;
			case "HOTWELL" -> on ? 30 : 28;
			case "CP DISCH" -> run ? 360 - 30 * Math.min(1, load) : on ? 4 : 0;
			case "CP AMPS" -> run ? 110 + 80 * Math.min(1, load / Math.max(1, peers) / ReactorCore.CP_SHARE) : 0;
			case "MFP SUCTION" -> suction;
			case "MFP DISCHARGE" -> run ? 1100 + 80 * Math.min(1, load) : suction;
			case "MFP BAL LEAKOFF" -> run ? suction + 25 : suction;
			case "MFP SEAL WTR" -> hdr ? 410 : 0;
			case "MFP BRG OIL" -> on ? 18 : 0;
			case "MFP LO PRESS" -> on ? 55 : 0;
			case "MFP LO TEMP" -> run ? 125 : on ? 95 : 70;
			case "LO PUMP DISCH" -> lamp("aux_oil", f, rpm) || lamp("tg_oil", f, rpm) ? 95 : 0;
			case "BRG OIL HDR" -> on ? 24 : 0;
			case "OIL TEMP" -> on ? rpm > 100 ? 118 : 95 : 70;
			case "LO RES TEMP" -> on ? rpm > 100 ? 140 : 105 : 70;
			case "LO FILTER DP" -> on ? 6 : 0;
			case "EHC PRESS" -> on ? 1600 : 0;
			case "EHC PP A DISCH" -> on ? 1650 : 0;
			case "FLUID TEMP" -> on ? 120 : 70;
			case "SEAL OIL" -> on ? 70 : 0;
			case "SO H2 SIDE" -> on ? 69 : 0;
			case "H2 CASING", "CASING PRESS" -> on ? 60 : 0;
			case "SEAL DP" -> on ? 10 : 0;
			case "DEW POINT" -> on ? 20 : 45;
			case "H2 BOTTLE" -> 1800;
			case "H2 REG OUT" -> on ? 65 : 0;
			case "CO2 BOTTLE" -> 850;
			case "H2 PURITY" -> on ? 97.5 : 0;
			// Turbine hall part 3: the lube oil conditioner's coalescer filter (its pump runs whenever the turbine is
			// there); the transfer pumps stand by (they only run to move oil between the reservoir and the tanks).
			case "COND FILTER DP" -> on ? 6 : 0;
			case "LO XFER DISCH" -> 0;
			default -> 0;
		};
	}

	/** A part 2 gauge's dial: its bottom and top (tools/feed_train.py draws them so). */
	public static double[] dial(String name) {
		return switch (name) {
			case "VACUUM", "MFP EXH PRESS", "LO FILTER DP" -> new double[] {0, 30};
			case "HOTWELL", "MFP BRG OIL", "BRG OIL HDR" -> new double[] {0, 60};
			case "CP DISCH", "MFP SUCTION", "MFP BAL LEAKOFF", "MFP SEAL WTR" -> new double[] {0, 600};
			case "CP AMPS" -> new double[] {0, 300};
			case "MFP DISCHARGE" -> new double[] {0, 1600};
			case "MFP LO PRESS", "H2 CASING", "CASING PRESS" -> new double[] {0, 100};
			case "MFP LO TEMP", "OIL TEMP", "LO RES TEMP", "FLUID TEMP" -> new double[] {0, 200};
			case "LO PUMP DISCH", "SEAL OIL", "SO H2 SIDE", "H2 REG OUT", "CO2 REG OUT" -> new double[] {0, 150};
			case "EHC PRESS" -> new double[] {0, 2000};
			case "EHC PP A DISCH", "EHC PP B DISCH" -> new double[] {0, 2500};
			case "SEAL DP" -> new double[] {0, 20};
			case "DEW POINT" -> new double[] {-40, 80};
			case "H2 BOTTLE" -> new double[] {0, 3000};
			case "CO2 BOTTLE" -> new double[] {0, 1500};
			case "COND FILTER DP" -> new double[] {0, 30};
			case "LO XFER DISCH" -> new double[] {0, 100};
			default -> null;
		};
	}

	/**
	 * A part 2 lamp: a pump's RUNNING, STOP, AUTO and TRIPPED; the oil plant's pumps (the AC auxiliary oil pump below 90%
	 * speed, the turning gear oil pump on the gear, the lift oil pumps below 600 rpm, the DC emergency pump never: the AC
	 * ones have power); the EHC unit's pump A; the seal oil unit's air side and hydrogen side pumps.
	 */
	public static boolean lamp(String name, int f, double rpm) {
		boolean on = (f & PRESENT) != 0;
		return switch (name) {
			case "cp_run", "mfp_run" -> (f & RUN) != 0;
			case "cp_stop" -> (f & RUN) == 0;
			case "cp_auto" -> (f & AUTO) != 0;
			case "mfp_trip" -> (f & TRIPPED) != 0;
			case "aux_oil" -> on && rpm < 1800 * 0.9 && (f & TURNING_GEAR) == 0;
			case "tg_oil" -> on && (f & TURNING_GEAR) != 0;
			case "lift_oil" -> on && rpm < 600;
			case "ehc_a", "so_air", "so_h2" -> on;
			// The DC emergency pumps and EHC pump B stand by (the AC pumps have power); the hydrogen's purity and pressure
			// stay normal.
			case "emerg_oil", "ehc_b", "so_dc", "h2_pur_lo", "h2_press_lo" -> false;
			// Turbine hall part 3: the lube oil conditioner runs with the turbine there, its heater on while the oil is
			// cool (below speed); its high water alarm stays clear; the transfer pumps stand by.
			case "cond_run" -> on;
			case "cond_heat" -> on && rpm <= 100;
			case "cond_water", "lox_a", "lox_b" -> false;
			default -> false;
		};
	}

	/** A part 2 level (a fraction of its glass): the hotwell's, the oil plant's tanks'. */
	public static double level(String name, int f) {
		return switch (name) {
			case "HOTWELL" -> (f & PRESENT) != 0 ? 0.5 : 0.46;
			case "LO RESERVOIR" -> 0.7;
			case "EHC RESERVOIR" -> 0.65;
			case "SO DRAIN REG" -> 0.5;
			// Turbine hall part 3: the conditioner's settling tank, the storage tanks' level gauges.
			case "LO COND" -> 0.55;
			case "LO STORAGE" -> 0.62;
			default -> 0.5;
		};
	}

	/** The nearest anchor of a kind within reach of a point, or null. */
	static @Nullable BlockPos nearest(EquipmentRegistry registry, Vec3 here, EquipmentKind kind, int reach) {
		BlockPos best = null;
		double bestDist = (double) reach * reach;
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> en : registry.anchors().entrySet()) {
			if (en.getValue().kind() != kind) {
				continue;
			}
			EquipmentKind k = en.getValue().kind();
			BlockPos middle = en.getKey().offset(EquipmentKind.toWorld(new BlockPos(k.wide / 2, k.tall / 2, k.deep / 2), en.getValue().facing()));
			double d = Vec3.atCenterOf(middle).distanceToSqr(here);
			if (d < bestDist || d == bestDist && best != null && en.getKey().compareTo(best) < 0) {
				bestDist = d;
				best = en.getKey().immutable();
			}
		}
		return best;
	}

	/** Where on the turbine a click landed: the trip handle, the turning gear lever, or elsewhere. */
	public enum Spot { TRIP, LEVER, ELSEWHERE }

	public static Spot spot(EquipmentBlockEntity turbine, @Nullable Vec3 hit) {
		if (hit == null) {
			return Spot.ELSEWHERE;
		}
		Vec3 l = Switchgear.local(turbine, hit);
		if (l.z <= TRIP_Z && l.x >= TRIP_X0 && l.x <= TRIP_X1 && l.y >= TRIP_Y0 && l.y <= TRIP_Y1) {
			return Spot.TRIP;
		}
		if (l.x <= LEVER_X && l.z >= LEVER_Z0 && l.z <= LEVER_Z1 && l.y >= LEVER_Y0 && l.y <= LEVER_Y1) {
			return Spot.LEVER;
		}
		return Spot.ELSEWHERE;
	}

	/** Using the turbine: the trip handle, the turning gear lever, or what it shows. */
	public static void useTurbine(Level level, EquipmentBlockEntity turbine, Player player, @Nullable Vec3 hit) {
		if (level.isClientSide() || !(level instanceof ServerLevel server)) {
			return;
		}
		long now = level.getGameTime();
		EquipmentBlockEntity vessel = reactor(server, EquipmentRegistry.of(server), turbine, now);
		Spot spot = spot(turbine, hit);
		Vec3 at = hit != null ? hit : turbine.middle();
		if (vessel == null && spot != Spot.ELSEWHERE) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.turbine_no_reactor"));
			return;
		}
		switch (spot) {
			case TRIP -> {
				ReactorCore c = vessel.core;
				ReactorUnit unit = vessel.unit();
				if (now > turbine.tripGuardUntil) {
					turbine.tripGuardUntil = now + GUARD;
					turbine.changed();
					level.playSound(null, at.x, at.y, at.z, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.3F);
					player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.turbine_guard"));
					return;
				}
				if (!trip(c, unit != null && unit.steamToPower())) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.turbine_already_tripped"));
					return;
				}
				vessel.changed();
				refresh(server, turbine, now);
				level.playSound(null, at.x, at.y, at.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0F, 0.6F);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.turbine_tripped_local"));
			}
			case LEVER -> {
				ReactorCore c = vessel.core;
				switch (lever(c)) {
					case DISENGAGED -> {
						level.playSound(null, at.x, at.y, at.z, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, 0.8F);
						player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.tg_disengaged"));
					}
					case ROLLING -> {
						player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.tg_rolling", String.format("%.0f", c.turbineSpeed * 1800)));
						return;
					}
					case ENGAGED -> {
						level.playSound(null, at.x, at.y, at.z, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 0.8F);
						player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.tg_engaged"));
					}
				}
				vessel.changed();
				refresh(server, turbine, now);
			}
			default -> player.sendOverlayMessage(status(turbine));
		}
	}

	/**
	 * Pulling the front standard's trip handle (its guard already up): a latched turbine trips (its stop valves shut on
	 * the auto stop oil dumping) and its generator's breaker opens; false if it is already tripped, or has no steam.
	 */
	public static boolean trip(ReactorCore c, boolean steam) {
		if (c.turbineTripped || c.scrammed || !steam) {
			return false;
		}
		c.turbineTripped = true;
		c.genOpen = true;
		c.syncArmed = false;
		return true;
	}

	/** What throwing the turning gear lever did: put the gear in, took it out, or nothing (the shaft still rolling). */
	public enum Lever { ENGAGED, DISENGAGED, ROLLING }

	/** The turning gear lever: the gear comes out at any time; it goes in only with the shaft at rest. */
	public static Lever lever(ReactorCore c) {
		if (c.turningGear) {
			c.turningGear = false;
			return Lever.DISENGAGED;
		}
		if (c.turbineSpeed >= ReactorCore.ZERO_SPEED) {
			return Lever.ROLLING;
		}
		c.turningGear = true;
		return Lever.ENGAGED;
	}

	/** What the turbine tells whoever uses it. */
	static Component status(EquipmentBlockEntity t) {
		int f = t.turbineFlags;
		if ((f & PRESENT) == 0) {
			return Component.translatable("message.jugcraft.nuclear.turbine_no_reactor");
		}
		String rpm = String.format("%,.0f", t.turbineRpm);
		String state = (f & ONLINE) != 0 ? "online" : (f & LATCHED) != 0 ? "latched" : (f & TURNING_GEAR) != 0 ? "turning_gear"
				: t.turbineRpm < 1 ? "stopped" : "tripped";
		return Component.translatable("message.jugcraft.nuclear.turbine_status", rpm,
				Component.translatable("message.jugcraft.nuclear.turbine_" + state), String.format("%,.0f", t.turbineLoad * 1150));
	}

	/** Using a reheater: its label (sneaking with an empty hand: the other one's) and its turbine's steam. */
	public static void useReheater(Level level, EquipmentBlockEntity msr, Player player, boolean relabel) {
		if (level.isClientSide()) {
			return;
		}
		net.minecraft.world.level.block.state.BlockState state = level.getBlockState(msr.getBlockPos());
		int variant = state.getValue(EquipmentAnchorBlock.VARIANT);
		if (relabel) {
			variant = 1 - Math.min(1, variant);
			level.setBlock(msr.getBlockPos(), state.setValue(EquipmentAnchorBlock.VARIANT, variant), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.relay_relabel", variant == 1 ? "1B" : "1A"));
			return;
		}
		String tag = variant == 1 ? "1B" : "1A";
		if (msr.linked == null || !(level.getBlockEntity(msr.linked) instanceof EquipmentBlockEntity t) || (t.turbineFlags & PRESENT) == 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.msr_idle", tag));
			return;
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.msr_status", tag, String.format("%.0f", shellPsig(t.turbineLoad, t.turbineFlags)),
				String.format("%.0f", reheatF(t.turbineLoad, t.turbineFlags))));
	}

	/** A reheater's shell pressure (psig): the crossover's, following the load. */
	public static double shellPsig(double load, int flags) {
		return (flags & ONLINE) != 0 ? 12 + 155 * Math.min(1, load) : (flags & LATCHED) != 0 ? 8 : 0;
	}

	/** Its reheated steam's temperature (F): 505 at full load, falling with it; cooling off once tripped. */
	public static double reheatF(double load, int flags) {
		if ((flags & ONLINE) != 0) {
			return 330 + 175 * Math.min(1, load);
		}
		return (flags & HOT) != 0 ? 260 : 90;
	}
}
