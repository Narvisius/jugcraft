package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * Diesels make real power (Justin, 27 October 2026: "Safety loads need real power", "Storms and a test switch"; the
 * plan is docs/plans/diesels-real-power.md): a reactor's electrical plant, worked out by its vessel each tick before its
 * core ticks.
 *
 * <ul>
 * <li>The grid: its switchyard's (YardBlockEntity: lost for a few minutes when a strike in a thunderstorm trips the
 * lines). A plant with no switchyard has it (a plant with no grid still starts up).</li>
 * <li>The plant's own buses (the coolant, condensate and circulating water pumps, main feed): live while the generator
 * carries them, or the grid does through SAT-1. Losing the grid at power: the generator loses its load and the turbine
 * trips (above P-9 the reactor with it); the buses go dead, the coolant pumps' breakers open (they coast down and stay
 * off until started from the board), the condensate and circulating water pumps stop (they start again with their bus),
 * main feed is lost, and the aux feed pumps get their start signal.</li>
 * <li>Safety buses 1A and 1B: fed from the grid through the reserve auxiliary transformers' breakers (RES XFMR A, B on
 * the electrical board: the test switch), else by their own diesel. A bus that loses the grid sheds it (its RES breaker
 * opens) and its diesel, at AUTO, starts by itself and closes onto it five seconds later; its load sequencer then
 * starts the bus's loads a step every five seconds (DieselFuel.STEP): the RHR pump at step 3, service water at 5, the
 * motor-driven aux feed pump at 6 (the step its legend gives the containment spray pump, which nothing signals yet), the
 * battery chargers at 8. With the grid back, the plant's buses come back by themselves; a safety bus stays on its diesel
 * until its RES breaker is closed from the board, which takes the diesel off it (it runs on unloaded until stopped).</li>
 * </ul>
 *
 * What it works out goes into the core (ReactorCore.power and friends) for its tick and the boards; the pump houses'
 * pumps follow their buses (WaterWorksBlockEntity.power), the transformer yard feeds the load centres from the grid or
 * the diesels.
 */
public final class PlantPower {
	/** How far from the vessel its diesels are looked for (as the boards do: the two nearest). */
	public static final int REACH = 256;
	/** The load sequencer's steps for the loads it starts (DieselFuel.STEP apart, from its diesel's breaker closing). */
	public static final int STEP_RHR = 3, STEP_SW = 5, STEP_AFW = 6, STEP_CHARGERS = 8;

	private PlantPower() {
	}

	/** A vessel's electrical plant: what it has found (looked for again when equipment changes and every five seconds) and what it last saw. */
	public static final class State {
		final @Nullable BlockPos[] edg = new BlockPos[2];
		@Nullable BlockPos yard;
		final List<BlockPos> houses = new ArrayList<>();
		int version = -1;
		long found = Long.MIN_VALUE;
		/** Last tick: the plant's buses live; each safety bus's diesel started on its loss (so it isn't started again until the bus has been back). */
		boolean plantWas = true;
		final boolean[] started = new boolean[2];
		/** The diesel breaker each safety bus had last tick (to take a diesel off its bus when its RES breaker is closed). */
		final boolean[] resWasOpen = new boolean[2];
	}

	/** Each tick, from the vessel before its core ticks. */
	public static void tick(ServerLevel level, EquipmentBlockEntity vessel, ReactorUnit unit) {
		ReactorCore core = vessel.core;
		long now = level.getGameTime();
		State st = vessel.power();
		EquipmentRegistry registry = EquipmentRegistry.of(level);
		if (st.version != registry.version() || now - st.found >= 100 || now < st.found) {
			find(level, vessel, unit, st, registry);
			st.version = registry.version();
			st.found = now;
		}
		YardBlockEntity yard = st.yard != null && level.isLoaded(st.yard) && level.getBlockEntity(st.yard) instanceof YardBlockEntity y ? y : null;
		YardBlockEntity sw = yard != null ? yard.switchyard() : null;
		boolean grid = sw == null || sw.gridLive(now);
		core.gridBack = grid || sw == null ? -1 : sw.gridBack();
		// The grid lost under a generator carrying it: it loses its load, the turbine trips.
		if (!grid && core.electric > 0) {
			core.turbineTripped = true;
			core.genOpen = true;
			core.syncArmed = false;
		}
		boolean generator = core.electric > 0 && !core.genOpen && !core.turbineTripped;
		boolean plant = generator || grid;
		int power = (grid ? ReactorCore.PW_GRID : 0) | (plant ? ReactorCore.PW_PLANT : 0);
		int afw = 0, rhr = 0;
		boolean[] swLive = new boolean[2];
		boolean dieselsFeed = false;
		for (int k = 0; k < 2; k++) {
			EquipmentBlockEntity edg = st.edg[k] != null && level.isLoaded(st.edg[k]) && level.getBlockEntity(st.edg[k]) instanceof EquipmentBlockEntity e
					&& e.kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR ? e : null;
			boolean resOpen = (core.resOpen & 1 << k) != 0;
			// Losing the grid sheds it: the RES breaker opens on the bus's undervoltage.
			if (!grid && !resOpen) {
				core.resOpen |= 1 << k;
				resOpen = true;
			}
			// The RES breaker closed onto a bus its diesel carries: the bus is back on the grid, the diesel taken off it.
			if (st.resWasOpen[k] && !resOpen && edg != null && edg.breakerClosed(now)) {
				edg.breakerAt = -1;
				edg.changed();
			}
			st.resWasOpen[k] = resOpen;
			boolean offsite = grid && !resOpen;
			boolean diesel = edg != null && edg.breakerClosed(now);
			if (offsite || diesel) {
				power |= k == 0 ? ReactorCore.PW_1A : ReactorCore.PW_1B;
			}
			int steps = offsite ? DieselFuel.STEP_KW.length : diesel ? steps(edg.breakerSeconds(now)) : 0;
			if (steps >= STEP_AFW) afw |= 1 << k;
			if (steps >= STEP_RHR) rhr |= 1 << k;
			swLive[k] = steps >= STEP_SW;
			dieselsFeed |= !offsite && diesel && steps >= STEP_CHARGERS;
			// Its bus dead: its diesel, at AUTO, starts by itself (once: a failed start waits for the operator).
			if (offsite) {
				st.started[k] = false;
			} else if (edg != null && !edg.running && !st.started[k] && DieselPanels.remoteReady(edg)) {
				st.started[k] = true;
				EquipmentPlacement.dieselSwitch(level, edg, null, true, DieselPanels.REMOTE);
			}
		}
		// The plant's buses dead: the coolant pumps' breakers open, the condensate pumps stop (they start again with
		// the bus), and the aux feed pumps get their start signal (the motor-driven ones run once their bus has power).
		boolean pumps = vessel.kind() != EquipmentKind.SMALL_MODULAR_REACTOR;
		if (!plant) {
			if (pumps) {
				core.rcpStopped |= 15;
			}
			core.cpShed |= core.cpRunning;
			core.cpRunning = 0;
			if (st.plantWas && core.temperature > 150) {
				core.afwRunning |= 7;
			}
		} else if (core.cpShed != 0) {
			core.cpRunning |= core.cpShed;
			core.cpShed = 0;
		}
		st.plantWas = plant;
		core.power = power;
		core.afwPower = afw;
		core.rhrPower = rhr;
		core.tdafwDc = DcRoom.feederLive(level, vessel.getBlockPos(), 0, DcRoom.PNL1) || DcRoom.feederLive(level, vessel.getBlockPos(), 1, DcRoom.PNL1);
		if (yard != null && dieselsFeed) {
			yard.dieselsFeeding(now);
		}
		// The pump houses' pumps follow their buses (each second).
		if (Math.floorMod(now + vessel.getBlockPos().asLong(), 20) == 0) {
			for (BlockPos h : st.houses) {
				if (level.isLoaded(h) && level.getBlockEntity(h) instanceof WaterWorksBlockEntity works) {
					works.power(level, plant, swLive[0], swLive[1]);
				}
			}
		}
	}

	/** The load sequencer's steps picked up {@code seconds} after its diesel's breaker closed (0 to 8; as BoardPlant.edgSteps). */
	public static int steps(double seconds) {
		if (seconds < 0) {
			return 0;
		}
		int n = 0;
		for (int j = 0; j < DieselFuel.STEP_KW.length; j++) {
			if (seconds >= DieselFuel.STEP * j) {
				n = j + 1;
			}
		}
		return n;
	}

	/** The vessel's two nearest diesels (A and B by where they stand, as the boards name them), its transformer yard, its pump houses. */
	private static void find(ServerLevel level, EquipmentBlockEntity vessel, ReactorUnit unit, State st, EquipmentRegistry registry) {
		BlockPos here = vessel.getBlockPos();
		List<BlockPos> edgs = new ArrayList<>();
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : registry.anchors().entrySet()) {
			if (e.getValue().kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR && e.getKey().distSqr(here) <= (double) REACH * REACH) {
				edgs.add(e.getKey().immutable());
			}
		}
		edgs.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(here)));
		List<BlockPos> two = new ArrayList<>(edgs.subList(0, Math.min(2, edgs.size())));
		two.sort(java.util.Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
		st.edg[0] = two.size() > 0 ? two.get(0) : null;
		st.edg[1] = two.size() > 1 ? two.get(1) : null;
		st.yard = null;
		for (BlockPos t : unit.transformers) {
			if (level.getBlockEntity(t) instanceof YardBlockEntity y && y.yard()) {
				st.yard = t.immutable();
				break;
			}
		}
		st.houses.clear();
		WaterWorksBlockEntity intake = WaterWorks.intake(level, here);
		if (intake != null) {
			st.houses.add(intake.getBlockPos().immutable());
		}
		for (BlockPos c : unit.condensers) {
			if (level.getBlockEntity(c) instanceof EquipmentBlockEntity condenser) {
				for (BlockPos h : condenser.cwHouses()) {
					if (!st.houses.contains(h)) {
						st.houses.add(h.immutable());
					}
				}
			}
		}
	}
}
