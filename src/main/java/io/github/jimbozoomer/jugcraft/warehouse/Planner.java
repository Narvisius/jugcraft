package io.github.jimbozoomer.jugcraft.warehouse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * The fleet's jobs (part W1, inbound): a stacker carries a case from an intake's conveyor to a station's pallet, or
 * wraps a finished pallet; a pallet drone takes a finished pallet from its station into a free pallet place; a drone
 * with nothing to do goes home to its dock and charges. Each job is one leg, worked out whole when it starts: the
 * paths (Router, booked so no two drones meet), the arm's and forks' moves, the events at their ticks.
 */
final class Planner {
	/** Events (Leg.Event.type). */
	static final int PICK_CASE = 1, PLACE_CASE = 2, WRAP = 3, PICK_PALLET = 4, PUT_PALLET = 5, DOCK = 6, UNDOCK = 7;
	/** Part W2's events: an order's pallet out of its place, onto a Ready Storage's bed, off it, back into a place, an empty one to a station. */
	static final int TAKE_PLACE = 8, PUT_BED = 9, TAKE_BED = 10, PUT_BACK = 11, PUT_STATION = 12;
	/** Part W3's: a pallet onto a Pallet Lift landing's bed (bound for another floor), off one. */
	static final int PUT_LIFT = 13, TAKE_LIFT = 14;
	/** Part W4's: an order's pallet of drums onto a Drum Emptying Station's platform, off it, emptied drums onto a Drum
	 * Filling Station's bay. (A filling station's strapped pallet is taken like a palletizing station's: PICK_PALLET.) */
	static final int PUT_EMPTIER = 15, TAKE_EMPTIER = 16, PUT_FILLER = 17;
	/**
	 * Part W5's: consolidation (a part pallet out of its place for it, onto a station's build spot; the stacker takes a
	 * case off the pallet a pallet drone holds out to it (arg: that drone); the station's pallet done), a cycle count's
	 * scan of a place's label.
	 */
	static final int CONSOL_TAKE = 18, CONSOL_SET = 19, CONSOL_PICK = 20, CONSOL_DONE = 21, COUNT = 22;
	/** Who keeps the second pallet of a consolidation till its drone comes for it. */
	static final int CONSOL_HOLD = Integer.MIN_VALUE + 2;
	/** How long the fleet has had nothing else to do before housekeeping starts (ticks). */
	static final int IDLE_FOR = 600;
	/** The pallet drone's scanner beam (its parts' fourth value, 0 off, 1 on). */
	static final int SCAN = 3;
	/** Who keeps a place a pallet is on its way to through a lift. */
	static final int LIFT_HOLD = Integer.MIN_VALUE + 1;
	/** The forks' heights (px): a pallet resting on a Ready Storage's rollers, carried while driving. */
	private static final float BED_SET = 2.35F, CARRY = 3.0F;
	/** A stacker's look while it wraps (the film runs to the pallet). */
	static final int WRAPPING = 100;
	/** How long a station waits for more cases before its pallet is finished as it is (ticks). */
	static final int CLOSE_AFTER = 600;
	static final int WRAP_TICKS = 120;
	/** The battery: what a tick working takes, what a tick charging gives; below LOW it goes home, it works again at READY. */
	static final float DRAIN = 0.00004F, CHARGE = 0.0006F, LOW = 0.2F, READY = 0.9F;
	/** The forks' and arm's speeds (px a tick for the lift and traverse). */
	private static final float LIFT_SPEED = 1.5F, TRAVERSE_SPEED = 1.6F;

	private final TerminalBlockEntity terminal;
	private final ServerLevel level;
	private final Network net;
	private final Router router;

	Planner(TerminalBlockEntity terminal, ServerLevel level) {
		this.terminal = terminal;
		this.level = level;
		this.net = terminal.network();
		this.router = terminal.router();
	}

	// ------------------------------------------------------------------ choosing jobs

	void assign(long now) {
		List<Drone> drones = terminal.drones();
		boolean held = terminal.fleetHeld();
		// Part W5: a consolidation's held place no station or drone waits for any more (timed out, the station gone): free.
		java.util.Set<BlockPos> waiting = new java.util.HashSet<>();
		for (Network.Station st : net.stations) {
			if (level.getBlockEntity(st.origin()) instanceof StationBlockEntity s && s.heldFor() != null) {
				waiting.add(s.heldFor());
			}
		}
		for (Drone d : drones) {
			if (d.job == Drone.CONSOLIDATE && d.source != null) {
				waiting.add(d.source);
			}
		}
		terminal.releaseHolds(CONSOL_HOLD, waiting);
		for (Drone d : drones) {
			if (d.busy(now) || now < d.leg.end()) {
				continue;
			}
			if (d.job != Drone.IDLE && d.job != Drone.HOME) {
				d.job = Drone.IDLE;
				d.source = d.target = null;
			}
			if (d.job == Drone.HOME && d.docked) {
				d.job = Drone.IDLE;
			}
			// Part W5: SEND HOME; paused (here or the whole fleet: it stays where its move ended); out of service (home, and stays).
			if (d.sendHome) {
				d.sendHome = false;
				if (!d.docked) {
					home(d, now);
				}
				continue;
			}
			if (held || d.paused) {
				continue;
			}
			if (d.outOfService) {
				if (!d.docked && now >= d.retry) {
					home(d, now);
				}
				continue;
			}
			if (d.battery < (d.docked ? READY : LOW)) {
				// Charging (or too low to work): home first.
				if (!d.docked && now >= d.retry) {
					home(d, now);
				}
				continue;
			}
			boolean planned = d.kind == DroneKind.STACKER ? stackerJob(d, now) : palletJob(d, now);
			if (!planned && !d.docked && d.job != Drone.HOME && now >= d.retry) {
				home(d, now);
			}
		}
		// Part W5: the CONTROL page's and the Status Board's housekeeping line, cleared once nothing of it is going on.
		if (drones.stream().noneMatch(x -> x.job == Drone.COUNT || x.job == Drone.CONSOLIDATE) && waiting.isEmpty()) {
			terminal.setHouse("", "");
		}
	}

	private boolean stackerJob(Drone d, long now) {
		// A case waiting at an intake's stop, to a station building a pallet (one stacker at an intake or a station at a time).
		List<Network.Intake> intakes = new ArrayList<>(net.intakes);
		intakes.sort(Comparator.comparingInt(i -> distance(d.cell, i.access())));
		for (Network.Intake in : intakes) {
			if (claimed(in.origin(), d) || !(level.getBlockEntity(in.origin()) instanceof IntakeBlockEntity intake)) {
				continue;
			}
			CaseLoad head = intake.ready(now);
			if (head == null) {
				continue;
			}
			Network.Station st = station(head, d);
			if (st != null && stack(d, in, intake, st, now)) {
				return true;
			}
		}
		// A pallet to finish: full, or waited long enough for more.
		for (Network.Station st : net.stations) {
			if (claimed(st.origin(), d) || !(level.getBlockEntity(st.origin()) instanceof StationBlockEntity station)) {
				continue;
			}
			if (station.wantsWrap(now) && terminal.powered(WarehousePower.Circuit.STATIONS) && wrap(d, st, now)) {
				return true;
			}
		}
		return false;
	}

	/** The station a case should go to: one building a pallet of the same item, else an empty pallet, else any with room. */
	private Network.@Nullable Station station(CaseLoad c, Drone d) {
		Network.Station same = null, empty = null, any = null;
		for (Network.Station st : net.stations) {
			if (claimed(st.origin(), d) || !(level.getBlockEntity(st.origin()) instanceof StationBlockEntity station) || !station.accepts()
					|| station.consolidating()) {
				continue;
			}
			PalletLoad load = station.build();
			if (load == null) {
				continue;
			}
			if (load.isEmpty()) {
				empty = empty == null ? st : empty;
			} else if (load.startsWith(c.item())) {
				same = same == null ? st : same;
			} else {
				any = any == null ? st : any;
			}
		}
		return same != null ? same : empty != null ? empty : any;
	}

	private boolean palletJob(Drone d, long now) {
		// Part W3: a pallet come up (or down) a lift onto this floor: on to where it goes.
		if (liftOutJob(d, now)) {
			return true;
		}
		// A pallet waiting on a dispatch hatch's bed: back to its place (an empty one to a station).
		for (Network.Vlm v : net.vlms) {
			if (claimed(v.origin(), d) || !(level.getBlockEntity(v.origin()) instanceof ReadyStorageBlockEntity rs) || !rs.waiting()) {
				continue;
			}
			if (returnJob(d, v, rs, now)) {
				return true;
			}
		}
		// Part W4: a pallet waiting on a Drum Emptying Station's platform: back to its place (emptied: to a filling station).
		for (Network.Station e : net.emptiers) {
			if (claimed(e.origin(), d) || !(level.getBlockEntity(e.origin()) instanceof EmptierBlockEntity em) || !em.waiting()) {
				continue;
			}
			if (emptierReturnJob(d, e, em, now)) {
				return true;
			}
		}
		// Part W5: a consolidation under way: its second pallet, held out to the stacker at the station.
		if (consolidateSecond(d, now)) {
			return true;
		}
		// RUSH orders ahead of everything; then the job groups in the order the CONTROL page has them.
		if (fetchJob(d, now, RUSH)) {
			return true;
		}
		for (int group : terminal.priorities()) {
			boolean planned = switch (group) {
				case ControlPayload.ORDERS -> fetchJob(d, now, ITEMS);
				case ControlPayload.PUTAWAY -> putawayJob(d, now, false);
				case ControlPayload.LIQUIDS -> fetchJob(d, now, LIQUID) || putawayJob(d, now, true);
				case ControlPayload.HOUSEKEEPING -> housekeeping(d, now);
				default -> false;
			};
			if (planned) {
				return true;
			}
		}
		return false;
	}

	/** Which orders fetchJob looks at: RUSH ones (either kind), items, liquids. */
	private static final int RUSH = 0, ITEMS = 1, LIQUID = 2;

	private boolean putawayJob(Drone d, long now, boolean drums) {
		for (Network.Station st : drums ? List.<Network.Station>of() : net.stations) {
			if (claimed(st.origin(), d) || !(level.getBlockEntity(st.origin()) instanceof StationBlockEntity station) || !station.ready()) {
				continue;
			}
			if (net.floorOfCell(st.behind()) != net.floorOfCell(d.cell)) {
				continue;
			}
			int look = station.build() == null ? 0 : station.build().look();
			Network.Place place = freePlace(st);
			if (place != null) {
				if (putaway(d, st, look, STATION_TABLE, place, now)) {
					return true;
				}
			} else if (liftPutaway(d, st, look, STATION_TABLE, now)) {
				// This floor is full: up (or down) a lift to a floor with room.
				return true;
			}
		}
		// Part W4: a Drum Filling Station's strapped pallet of drums.
		for (Network.Station st : drums ? net.fillers : List.<Network.Station>of()) {
			if (claimed(st.origin(), d) || !(level.getBlockEntity(st.origin()) instanceof FillerBlockEntity filler) || !filler.ready()) {
				continue;
			}
			if (net.floorOfCell(st.behind()) != net.floorOfCell(d.cell)) {
				continue;
			}
			int look = filler.build() == null ? 0 : filler.build().look();
			Network.Place place = freePlace(st);
			if (place != null) {
				if (putaway(d, st, look, fillDeck(), place, now)) {
					return true;
				}
			} else if (liftPutaway(d, st, look, fillDeck(), now)) {
				return true;
			}
		}
		return false;
	}

	/** Where a pallet stands on a palletizing station's turntable, on a filling station's scale, an emptying station's platform. */
	static final float STATION_TABLE = 1.3F;

	private static float fillDeck() {
		return (float) WarehouseLayout.get().number("drum_filler", "fill_deck");
	}

	private static float emptyDeck() {
		return (float) WarehouseLayout.get().number("drum_emptier", "deck");
	}

	/** The nearest free pallet place to a station on its floor that a drone can reach, lower levels first. */
	private Network.@Nullable Place freePlace(Network.Station st) {
		return freePlaceOn(net.floorOfCell(st.behind()), st.behind());
	}

	/** The nearest free pallet place to a cell on a floor that a drone can reach, lower levels first. */
	private Network.@Nullable Place freePlaceOn(int floor, long near) {
		Network.Place best = null;
		int bestScore = Integer.MAX_VALUE;
		for (Network.Place p : net.places) {
			if (p.level() > 4 || terminal.reserved(p.pos()) || !router.isCell(p.access()) || net.floorOfCell(p.access()) != floor
					|| !(level.getBlockEntity(p.pos()) instanceof RackBlockEntity rack) || rack.load() != null) {
				continue;
			}
			int score = distance(near, p.access()) + p.level() * 6;
			if (score < bestScore) {
				best = p;
				bestScore = score;
			}
		}
		return best;
	}

	private boolean claimed(BlockPos pos, Drone self) {
		for (Drone o : terminal.drones()) {
			if (o != self && o.job != Drone.IDLE && o.job != Drone.HOME && (pos.equals(o.source) || pos.equals(o.target))) {
				return true;
			}
		}
		return false;
	}

	private static int distance(long a, long b) {
		BlockPos p = BlockPos.of(a), q = BlockPos.of(b);
		return Math.abs(p.getX() - q.getX()) + Math.abs(p.getZ() - q.getZ());
	}

	// ------------------------------------------------------------------ the legs

	/** A leg's start for a drone: its pose now, coming back to its cell's middle if a job left it off it. */
	private LegBuilder begin(Drone d, long now) {
		Leg.Key pose = d.leg.last();
		LegBuilder lb = new LegBuilder(now + 1, d.leg.y(), pose);
		BlockPos c = BlockPos.of(d.cell);
		float cx = c.getX() + 0.5F, cz = c.getZ() + 0.5F;
		if (Math.abs(pose.x() - cx) > 1e-3 || Math.abs(pose.z() - cz) > 1e-3) {
			lb.to(6, cx, cz, Router.yaw(d.heading), null);
		}
		if (d.docked && d.dock.asLong() == d.cell) {
			lb.event(UNDOCK, d.dock, 0);
		}
		return lb;
	}

	/** Plans and books the drive from where the builder is to a cell (facing a heading), to stay there stay ticks. */
	private boolean drive(Drone d, LegBuilder lb, long from, int heading, long to, int toHeading, int stay, long... extra) {
		List<Router.Step> path = router.path(d.id, from, heading, lb.now(), to, toHeading, stay, extra);
		if (path == null) {
			return false;
		}
		router.bookPath(d.id, path);
		lb.path(path);
		return true;
	}

	private void commit(Drone d, LegBuilder lb, long cell, int heading, int job, @Nullable BlockPos source, @Nullable BlockPos target) {
		Leg leg = lb.build();
		// It stands in its last cell from the leg's end until its next move.
		router.book(cell, lb.now(), Long.MAX_VALUE, d.id);
		d.leg = leg;
		d.done = 0;
		d.cell = cell;
		d.heading = heading;
		d.job = job;
		d.source = source;
		d.target = target;
		terminal.legStarted(d);
	}

	private boolean stack(Drone d, Network.Intake in, IntakeBlockEntity intake, Network.Station st, long now) {
		router.release(d.id);
		LegBuilder lb = begin(d, now);
		int hIn = Router.heading(in.facing()), hSt = Router.heading(st.facing());
		if (!drive(d, lb, d.cell, d.heading, in.access(), hIn, 60)) {
			rebook(d, now);
			return false;
		}
		// Pick: over the case, down onto it, take it (the conveyor's head case), up, and hold it high to drive.
		double[] top = intake.headTop();
		double[] on = local(lb, top[0], top[1], top[2]);
		double[] qOver = over(lb, top), qOn = Kinematics.reachLocal(on[0], on[1], on[2]);
		double[] qCarry = Kinematics.reachLocal(8, 22, 12);
		if (qOver == null || qOn == null) {
			rebook(d, now);
			return false;
		}
		CaseLoad head = intake.ready(now);
		lb.to(12, null, null, null, arm(qOver, lb)).to(6, null, null, null, arm(qOn, lb));
		lb.event(PICK_CASE, in.origin(), 0).look(head == null ? CaseKind.CARTON.ordinal() : head.kind().ordinal());
		lb.to(6, null, null, null, arm(qOver, lb)).to(12, null, null, null, arm(qCarry, lb));
		router.book(in.access(), lb.start, lb.now(), d.id);
		if (!drive(d, lb, in.access(), hIn, st.access(), hSt, 50)) {
			router.release(d.id);
			rebook(d, now);
			return false;
		}
		// Place: over its spot on the pallet, down, let go, up, fold.
		StationBlockEntity station = (StationBlockEntity) level.getBlockEntity(st.origin());
		double[] slot = station.slotTop(station.build() == null ? 0 : station.build().size());
		double[] at = local(lb, slot[0], slot[1], slot[2]);
		double[] qAbove = over(lb, slot), qAt = Kinematics.reachLocal(at[0], at[1], at[2]);
		if (qAbove == null || qAt == null) {
			router.release(d.id);
			rebook(d, now);
			return false;
		}
		long arrived = lb.now();
		lb.to(12, null, null, null, arm(qAbove, lb)).to(6, null, null, null, arm(qAt, lb));
		lb.event(PLACE_CASE, st.origin(), 0).look(-1);
		lb.to(6, null, null, null, arm(qAbove, lb)).to(12, null, null, null, Kinematics.PARKED);
		router.book(st.access(), arrived, lb.now(), d.id);
		commit(d, lb, st.access(), hSt, Drone.STACK, in.origin(), st.origin());
		return true;
	}

	private boolean wrap(Drone d, Network.Station st, long now) {
		router.release(d.id);
		LegBuilder lb = begin(d, now);
		int hSt = Router.heading(st.facing());
		if (!drive(d, lb, d.cell, d.heading, st.access(), hSt, WRAP_TICKS + 20)) {
			rebook(d, now);
			return false;
		}
		long arrived = lb.now();
		// The film carriage runs up and down the mast twice while the turntable turns the pallet.
		lb.to(10, null, null, null, Kinematics.PARKED).event(WRAP, st.origin(), WRAP_TICKS).look(WRAPPING);
		int quarter = WRAP_TICKS / 4;
		float top = (float) (Kinematics.FILM_TOP);
		lb.part(quarter, 4, top).part(quarter, 4, 0).part(quarter, 4, top).part(WRAP_TICKS - 3 * quarter, 4, 0);
		lb.look(-1).hold(4);
		router.book(st.access(), arrived, lb.now(), d.id);
		commit(d, lb, st.access(), hSt, Drone.WRAP, null, st.origin());
		return true;
	}

	private boolean putaway(Drone d, Network.Station st, int look, float table, Network.Place place, long now) {
		router.release(d.id);
		LegBuilder lb = begin(d, now);
		int hSt = Router.heading(st.facing());
		Direction toStation = st.facing();
		if (!router.isCell(st.behind()) || !drive(d, lb, d.cell, d.heading, st.behind(), hSt, 80, st.access())) {
			rebook(d, now);
			return false;
		}
		long arrived = lb.now();
		// Forks down to the pallet's pockets, in, up (the pallet lifts off the turntable), back out.
		BlockPos spot = st.origin();
		float spotX = spot.getX() + 0.5F, spotZ = spot.getZ() + 0.5F;
		float inX = spotX - toStation.getStepX() * 19.6F / 16, inZ = spotZ - toStation.getStepZ() * 19.6F / 16;
		BlockPos behind = BlockPos.of(st.behind());
		lb.partAt(LIFT_SPEED, 0, table + 0.1F).to(12, inX, inZ, null, null).partAt(LIFT_SPEED, 0, table + 1.7F);
		lb.event(PICK_PALLET, st.origin(), 0).look(look);
		lb.to(12, behind.getX() + 0.5F, behind.getZ() + 0.5F, null, null);
		router.book(st.access(), arrived, lb.now(), d.id);
		router.book(st.behind(), arrived, lb.now(), d.id);
		// To the place's aisle cell, along the aisle, standing back so the pallet is in line with the place.
		Direction f = place.facing();
		int h = alongAisle(place, f);
		if (h < 0) {
			router.release(d.id);
			rebook(d, now);
			return false;
		}
		Direction heading = Router.HEADINGS[h];
		long back = BlockPos.of(place.access()).relative(heading.getOpposite()).asLong();
		if (!drive(d, lb, st.behind(), hSt, place.access(), h, 200, back)) {
			router.release(d.id);
			rebook(d, now);
			return false;
		}
		long atPlace = lb.now();
		BlockPos a = BlockPos.of(place.access());
		float workX = a.getX() + 0.5F - heading.getStepX() * 11.3F / 16, workZ = a.getZ() + 0.5F - heading.getStepZ() * 11.3F / 16;
		// Which side the place is on, in the drone's own frame (its x to its left): the head turns to it, the forks go across to it.
		double yaw = Math.toRadians(Router.yaw(h));
		double localX = f.getStepX() * Math.cos(yaw) - f.getStepZ() * Math.sin(yaw);
		float side = localX >= 0 ? 1 : -1;
		float set = place.level() == 0 ? 0.2F : (float) (place.level() * 16 + Kinematics.BEAM_TOP + 0.2);
		float approach = set + 1.5F;
		lb.to(6, workX, workZ, null, null);
		lb.partAt(TRAVERSE_SPEED, 1, -8.3F * side).part(10, 2, 90 * side);
		lb.partAt(LIFT_SPEED, 0, approach).partAt(TRAVERSE_SPEED, 1, 7.7F * side).partAt(0.5F, 0, set);
		lb.event(PUT_PALLET, place.pos(), 0).look(-1);
		lb.partAt(TRAVERSE_SPEED, 1, -8.3F * side).part(10, 2, 0).partAt(TRAVERSE_SPEED, 1, 0).partAt(LIFT_SPEED, 0, 0);
		lb.to(6, a.getX() + 0.5F, a.getZ() + 0.5F, null, null);
		router.book(place.access(), atPlace, lb.now(), d.id);
		router.book(back, atPlace, lb.now(), d.id);
		terminal.reserve(place.pos(), d.id);
		commit(d, lb, place.access(), h, Drone.PUTAWAY, st.origin(), place.pos());
		return true;
	}

	// ------------------------------------------------------------------ part W3: the Pallet Lifts

	/** The landing on a floor (with its lift's block entity) a drone there can set a pallet on now, nearest a cell. */
	private Network.@Nullable Landing freeLanding(Drone d, int floor, long near, long now) {
		Network.Landing best = null;
		int bestDist = Integer.MAX_VALUE;
		for (Network.Landing l : net.landings) {
			if (net.floorOfCell(l.access()) != floor || claimed(l.origin(), d) || !router.isCell(l.access())
					|| !(level.getBlockEntity(l.origin()) instanceof LiftBlockEntity lift) || !lift.bedFree(now)) {
				continue;
			}
			int dist = distance(near, l.access());
			if (dist < bestDist) {
				best = l;
				bestDist = dist;
			}
		}
		return best;
	}

	/** Whether a lift's landing at floor a also serves floor b (its column has landings on both). */
	private boolean serves(Network.Landing l, int floor) {
		if (!(level.getBlockEntity(l.origin()) instanceof LiftBlockEntity lift)) {
			return false;
		}
		LiftBlockEntity c = lift.controller();
		if (c == null) {
			return false;
		}
		for (int y : c.floors()) {
			if (net.floor(y) == floor) {
				return true;
			}
		}
		return false;
	}

	/** A station's finished pallet onto a lift's bed, bound for a free place on another floor (this one is full). */
	private boolean liftPutaway(Drone d, Network.Station st, int look, float table, long now) {
		int here = net.floorOfCell(st.behind());
		Network.Landing l = freeLanding(d, here, st.behind(), now);
		if (l == null) {
			return false;
		}
		Network.Place place = null;
		for (int k = 1; k < net.floors.size() && place == null; k++) {
			for (int fl : new int[] {here + k, here - k}) {
				if (place == null && fl >= 0 && fl < net.floors.size() && serves(l, fl)) {
					place = freePlaceOn(fl, l.access());
				}
			}
		}
		if (place == null) {
			return false;
		}
		router.release(d.id);
		LegBuilder lb = begin(d, now);
		int hSt = Router.heading(st.facing());
		if (!router.isCell(st.behind()) || !drive(d, lb, d.cell, d.heading, st.behind(), hSt, 80, st.access())) {
			rebook(d, now);
			return false;
		}
		long arrived = lb.now();
		Direction toStation = st.facing();
		BlockPos spot = st.origin();
		float inX = spot.getX() + 0.5F - toStation.getStepX() * 19.6F / 16, inZ = spot.getZ() + 0.5F - toStation.getStepZ() * 19.6F / 16;
		BlockPos behind = BlockPos.of(st.behind());
		lb.partAt(LIFT_SPEED, 0, table + 0.1F).to(12, inX, inZ, null, null).partAt(LIFT_SPEED, 0, table + 1.7F);
		lb.event(PICK_PALLET, st.origin(), 0).look(look);
		lb.to(12, behind.getX() + 0.5F, behind.getZ() + 0.5F, null, null);
		router.book(st.access(), arrived, lb.now(), d.id);
		router.book(st.behind(), arrived, lb.now(), d.id);
		if (!atLiftBed(d, lb, st.behind(), hSt, l, true, -1)) {
			router.release(d.id);
			rebook(d, now);
			return false;
		}
		terminal.reserve(place.pos(), LIFT_HOLD);
		terminal.liftBound(d.id, place.pos(), 0);
		((LiftBlockEntity) level.getBlockEntity(l.origin())).claimBed(lb.now() + 2);
		commit(d, lb, l.access(), Router.heading(l.facing()), Drone.TO_LIFT, st.origin(), l.origin());
		return true;
	}

	/**
	 * A pallet a lift brought onto this drone's floor: on to where it is bound (a place kept for it, a Ready Storage for
	 * an order), or (bound for nowhere: its plan was lost) the nearest free place.
	 */
	private boolean liftOutJob(Drone d, long now) {
		int here = net.floorOfCell(d.cell);
		for (Network.Landing l : net.landings) {
			if (net.floorOfCell(l.access()) != here || claimed(l.origin(), d) || !router.isCell(l.access())
					|| !(level.getBlockEntity(l.origin()) instanceof LiftBlockEntity lift) || !lift.bedReady(now)) {
				continue;
			}
			BlockPos dest = lift.bedDest();
			if (dest != null && net.floor(dest.getY()) != here) {
				continue;
			}
			PalletLoad load = lift.bed();
			int look = load == null ? 0 : load.look();
			int hl = Router.heading(l.facing());
			Network.Vlm v = null;
			for (Network.Vlm x : net.vlms) {
				if (x.origin().equals(dest)) {
					v = x;
				}
			}
			Network.Station em = null;
			for (Network.Station x : net.emptiers) {
				if (x.origin().equals(dest)) {
					em = x;
				}
			}
			if (em != null) {
				// Part W4: an order's pallet of drums: onto its Drum Emptying Station's platform when that is free.
				if (!(level.getBlockEntity(em.origin()) instanceof EmptierBlockEntity e) || !e.free() || claimed(em.origin(), d)) {
					continue;
				}
				router.release(d.id);
				LegBuilder lb = begin(d, now);
				if (!atLiftBed(d, lb, d.cell, d.heading, l, false, look) || !atEmptier(d, lb, l.access(), hl, em, true, lift.bedOrder(), -1)) {
					router.release(d.id);
					rebook(d, now);
					continue;
				}
				lift.claimBed(lb.now());
				commit(d, lb, em.access(), Router.heading(em.facing()), Drone.FROM_LIFT, l.origin(), em.origin());
				return true;
			}
			if (v != null) {
				// An order's pallet: onto its Ready Storage's bed when that is free.
				if (!(level.getBlockEntity(v.origin()) instanceof ReadyStorageBlockEntity rs) || !rs.free() || claimed(v.origin(), d)) {
					continue;
				}
				router.release(d.id);
				LegBuilder lb = begin(d, now);
				if (!atLiftBed(d, lb, d.cell, d.heading, l, false, look) || !atBed(d, lb, l.access(), hl, v, true, lift.bedOrder(), -1)) {
					router.release(d.id);
					rebook(d, now);
					continue;
				}
				lift.claimBed(lb.now());
				commit(d, lb, v.access(), Router.heading(v.facing()), Drone.FROM_LIFT, l.origin(), v.origin());
				return true;
			}
			Network.Place place = dest == null ? null : net.placeAt.get(dest);
			if (place == null || !(level.getBlockEntity(place.pos()) instanceof RackBlockEntity rack) || rack.load() != null
					|| terminal.reservedBy(place.pos()) != null && terminal.reservedBy(place.pos()) != LIFT_HOLD) {
				place = freePlaceOn(here, l.access());
			}
			if (place == null) {
				continue;
			}
			router.release(d.id);
			LegBuilder lb = begin(d, now);
			int h = -1;
			if (atLiftBed(d, lb, d.cell, d.heading, l, false, look)) {
				h = atPlace(d, lb, l.access(), hl, place, false, PUT_PALLET, 0, -1);
			}
			if (h < 0) {
				router.release(d.id);
				rebook(d, now);
				continue;
			}
			if (dest != null && !dest.equals(place.pos())) {
				terminal.unreserve(dest);
			}
			terminal.reserve(place.pos(), d.id);
			lift.claimBed(lb.now());
			commit(d, lb, place.access(), h, Drone.FROM_LIFT, l.origin(), place.pos());
			return true;
		}
		return false;
	}

	/**
	 * Drives to a lift landing's bed (its access cell, facing the landing) and sets the pallet it carries on the
	 * transfer conveyor (PUT_LIFT) or takes the one there (TAKE_LIFT, look its pallet), as at a Ready Storage's bed.
	 */
	private boolean atLiftBed(Drone d, LegBuilder lb, long from, int fromHeading, Network.Landing l, boolean put, int look) {
		int hl = Router.heading(l.facing());
		if (!router.isCell(l.access()) || !drive(d, lb, from, fromHeading, l.access(), hl, 80, l.behind())) {
			return false;
		}
		long arrived = lb.now();
		Direction f = l.facing();
		double[] bed = WarehousePiece.toWorld(WarehousePiece.LIFT_LANDING, f, l.origin(), 24.0, 0.0, 40.0);
		float inX = (float) (bed[0] - f.getStepX() * 19.6 / 16), inZ = (float) (bed[2] - f.getStepZ() * 19.6 / 16);
		BlockPos a = BlockPos.of(l.access());
		if (put) {
			lb.partAt(LIFT_SPEED, 0, BED_SET + 1.6F).to(10, inX, inZ, null, null).partAt(0.5F, 0, BED_SET);
			lb.event(PUT_LIFT, l.origin(), 0).look(-1);
			lb.partAt(LIFT_SPEED, 0, BED_SET - 1.2F).to(10, a.getX() + 0.5F, a.getZ() + 0.5F, null, null).partAt(LIFT_SPEED, 0, 0);
		} else {
			lb.partAt(LIFT_SPEED, 0, BED_SET - 1.0F).to(10, inX, inZ, null, null).partAt(0.5F, 0, BED_SET + 1.0F);
			lb.event(TAKE_LIFT, l.origin(), 0).look(look);
			lb.to(10, a.getX() + 0.5F, a.getZ() + 0.5F, null, null).partAt(LIFT_SPEED, 0, CARRY);
		}
		router.book(l.access(), arrived, lb.now(), d.id);
		router.book(l.behind(), arrived, lb.now(), d.id);
		return true;
	}

	// ------------------------------------------------------------------ part W2: orders out

	/**
	 * The first queued order of a kind (RUSH ones; else items or liquids, not rushed) a free Ready Storage (or emptying
	 * station) can take: its pallet fetched to the hatch.
	 */
	private boolean fetchJob(Drone d, long now, int which) {
		for (Order o : terminal.orders()) {
			boolean liquid = Drums.isLiquid(o.item);
			if (o.state != Order.QUEUED || (which == RUSH ? !o.rush : o.rush || liquid != (which == LIQUID))
					|| o.stream > 0 && ((liquid ? terminal.liquidMode() : terminal.portMode()) != TerminalBlockEntity.PORT_STREAMS || terminal.streamsOff())) {
				continue;
			}
			// Part W5: Ready Storage and the emptying stations start nothing new while their circuit is shed or off.
			if (!terminal.powered(liquid ? WarehousePower.Circuit.LIQUIDS : WarehousePower.Circuit.READY_STORAGE)) {
				continue;
			}
			if (liquid) {
				// Part W4: a liquid order's pallet of drums to a Drum Emptying Station.
				if (liquidFetch(d, o, now)) {
					return true;
				}
				continue;
			}
			Network.Vlm v = freeVlm(d);
			if (v == null) {
				continue;
			}
			// No room for it in Ready Storage (or the port's buffer): it waits till there is.
			long room = o.dest == Order.WINDOW ? terminal.pool().room(o.item, o.id) : terminal.port().room(o.item, o.id);
			if (room <= 0) {
				continue;
			}
			Network.Place place = palletFor(o);
			if (place == null) {
				// None of it left on any pallet that can be had: the order ends with what it got.
				if (!inFlight(o)) {
					o.state = o.filled > 0 ? (o.dest == Order.WINDOW ? Order.READY : Order.SENT) : Order.CANCELLED;
					o.readyAt = now;
				}
				continue;
			}
			// Part W3: the pallet on the Ready Storage's floor (fetched there), or on another (a drone there takes it to a lift).
			int pf = net.floorOfCell(place.access()), vf = net.floorOfCell(v.access()), df = net.floorOfCell(d.cell);
			if (pf == vf ? df == pf && fetch(d, o, place, v, now) : df == pf && liftFetch(d, o, place, v.origin(), v.access(), now)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * An order's pallet on another floor: out of its place onto a lift's bed, bound for the floor of where it is going
	 * (a Ready Storage, a Drum Emptying Station: its origin and the cell a drone works it from).
	 */
	private boolean liftFetch(Drone d, Order o, Network.Place place, BlockPos dest, long destAccess, long now) {
		int pf = net.floorOfCell(place.access()), vf = net.floorOfCell(destAccess);
		Network.Landing l = freeLanding(d, pf, place.access(), now);
		if (l == null || !serves(l, vf) || !(level.getBlockEntity(place.pos()) instanceof RackBlockEntity rack) || rack.load() == null) {
			return false;
		}
		router.release(d.id);
		LegBuilder lb = begin(d, now);
		int h = atPlace(d, lb, d.cell, d.heading, place, true, TAKE_PLACE, o.id, rack.load().look());
		if (h < 0 || !atLiftBed(d, lb, place.access(), h, l, true, -1)) {
			router.release(d.id);
			rebook(d, now);
			return false;
		}
		terminal.reserve(place.pos(), -o.id);
		terminal.liftBound(d.id, dest, o.id);
		o.state = Order.FETCHING;
		o.pallet = place.pos();
		((LiftBlockEntity) level.getBlockEntity(l.origin())).claimBed(lb.now() + 2);
		commit(d, lb, l.access(), Router.heading(l.facing()), Drone.TO_LIFT, place.pos(), l.origin());
		return true;
	}

	/** Whether a pallet with an order's item is on its way somewhere (being put away, at a hatch). */
	private boolean inFlight(Order o) {
		for (Drone x : terminal.drones()) {
			// (A drone on its way to a station's finished pallet carries nothing yet: it may be bringing more.)
			if (x.carried != null && x.carried.cases().stream().anyMatch(c -> c.sameItem(o.item))
					|| x.job == Drone.PUTAWAY && x.busy(level.getGameTime())) {
				return true;
			}
		}
		for (Network.Vlm v : net.vlms) {
			if (level.getBlockEntity(v.origin()) instanceof ReadyStorageBlockEntity rs && rs.pallet() != null
					&& rs.pallet().cases().stream().anyMatch(c -> c.sameItem(o.item))) {
				return true;
			}
		}
		// (W5: a consolidation's pallet on a station's build spot, till it is put away again.)
		for (Network.Station st : net.stations) {
			if (level.getBlockEntity(st.origin()) instanceof StationBlockEntity s && s.restock()
					&& s.build().cases().stream().anyMatch(c -> c.sameItem(o.item))) {
				return true;
			}
		}
		// (W4: a pallet of drums on a Drum Emptying Station's platform: its rack place is empty while it is out.)
		for (Network.Station e : net.emptiers) {
			if (level.getBlockEntity(e.origin()) instanceof EmptierBlockEntity em && em.pallet() != null
					&& em.pallet().cases().stream().anyMatch(c -> c.count() > 0 && c.sameItem(o.item))) {
				return true;
			}
		}
		// A pallet of it kept for another order (out at a hatch, coming back).
		for (Network.Place p : net.places) {
			Integer by = terminal.reservedBy(p.pos());
			if (by != null && by < 0 && level.getBlockEntity(p.pos()) instanceof RackBlockEntity rack && rack.load() != null
					&& rack.load().cases().stream().anyMatch(c -> c.sameItem(o.item))) {
				return true;
			}
		}
		return false;
	}

	private Network.@Nullable Vlm freeVlm(Drone d) {
		for (Network.Vlm v : net.vlms) {
			if (!router.isCell(v.access()) || !(level.getBlockEntity(v.origin()) instanceof ReadyStorageBlockEntity rs) || !rs.free()) {
				continue;
			}
			boolean taken = false;
			for (Drone o : terminal.drones()) {
				if (o != d && o.job == Drone.FETCH && v.origin().equals(o.target)) {
					taken = true;
				}
			}
			if (!taken) {
				return v;
			}
		}
		return null;
	}

	/** The pallet to fetch for an order: the smallest holding all it still wants, else the one holding most of it. */
	private Network.@Nullable Place palletFor(Order o) {
		Network.Place best = null;
		long bestCount = 0;
		boolean bestCovers = false;
		for (Network.Place p : net.places) {
			if (terminal.reserved(p.pos()) || !router.isCell(p.access()) || alongAisle(p, p.facing()) < 0
					|| !(level.getBlockEntity(p.pos()) instanceof RackBlockEntity rack) || rack.load() == null) {
				continue;
			}
			long count = 0;
			for (CaseLoad c : rack.load().cases()) {
				if (c.sameItem(o.item)) {
					count += c.count();
				}
			}
			if (count == 0) {
				continue;
			}
			boolean covers = count >= o.remaining();
			if (best == null || covers && (!bestCovers || count < bestCount) || !covers && !bestCovers && count > bestCount) {
				best = p;
				bestCount = count;
				bestCovers = covers;
			}
		}
		return best;
	}

	private boolean fetch(Drone d, Order o, Network.Place place, Network.Vlm v, long now) {
		if (!(level.getBlockEntity(place.pos()) instanceof RackBlockEntity rack) || rack.load() == null) {
			return false;
		}
		router.release(d.id);
		LegBuilder lb = begin(d, now);
		int h = atPlace(d, lb, d.cell, d.heading, place, true, TAKE_PLACE, o.id, rack.load().look());
		if (h < 0 || !atBed(d, lb, place.access(), h, v, true, o.id, -1)) {
			router.release(d.id);
			rebook(d, now);
			return false;
		}
		terminal.reserve(place.pos(), -o.id);
		o.state = Order.FETCHING;
		o.pallet = place.pos();
		commit(d, lb, v.access(), Router.heading(v.facing()), Drone.FETCH, place.pos(), v.origin());
		return true;
	}

	/** A pallet on a hatch's bed back to where it came from (or a free place near), or, empty, to a station's build spot. */
	private boolean returnJob(Drone d, Network.Vlm v, ReadyStorageBlockEntity rs, long now) {
		PalletLoad load = rs.pallet();
		if (load == null) {
			return false;
		}
		int hv = Router.heading(v.facing());
		if (load.isEmpty()) {
			Network.Station st = null;
			for (Network.Station s : net.stations) {
				if (!claimed(s.origin(), d) && router.isCell(s.behind()) && level.getBlockEntity(s.origin()) instanceof StationBlockEntity se && se.takesBack()) {
					st = s;
					break;
				}
			}
			if (st == null) {
				return false;
			}
			router.release(d.id);
			LegBuilder lb = begin(d, now);
			if (!atBed(d, lb, d.cell, d.heading, v, false, 0, 0) || !atStation(d, lb, v.access(), hv, st)) {
				router.release(d.id);
				rebook(d, now);
				return false;
			}
			((StationBlockEntity) level.getBlockEntity(st.origin())).setIncoming(now + 1200);
			rs.claim(lb.now());
			terminal.unreserve(rs.from());
			commit(d, lb, st.behind(), Router.heading(st.facing()), Drone.RETURN, v.origin(), st.origin());
			return true;
		}
		Network.Place place = null;
		Integer by = rs.from() == null ? null : terminal.reservedBy(rs.from());
		if (rs.from() != null && net.placeAt.containsKey(rs.from()) && (by == null || by < 0)
				&& level.getBlockEntity(rs.from()) instanceof RackBlockEntity rack && rack.load() == null) {
			place = net.placeAt.get(rs.from());
		}
		int vf = net.floorOfCell(v.access());
		if (place == null) {
			place = freePlaceOn(vf, v.access());
		}
		if (place == null) {
			// This floor is full: the nearest floor with room, through a lift.
			for (int k = 1; k < net.floors.size() && place == null; k++) {
				for (int fl : new int[] {vf + k, vf - k}) {
					if (place == null && fl >= 0 && fl < net.floors.size()) {
						place = freePlaceOn(fl, v.access());
					}
				}
			}
		}
		if (place == null) {
			return false;
		}
		if (net.floorOfCell(place.access()) != vf) {
			// Part W3: back to a place on another floor: onto a lift's bed, bound for it.
			Network.Landing l = freeLanding(d, vf, v.access(), now);
			if (l == null || !serves(l, net.floorOfCell(place.access()))) {
				return false;
			}
			router.release(d.id);
			LegBuilder lb = begin(d, now);
			if (!atBed(d, lb, d.cell, d.heading, v, false, 0, load.look()) || !atLiftBed(d, lb, v.access(), hv, l, true, -1)) {
				router.release(d.id);
				rebook(d, now);
				return false;
			}
			if (!place.pos().equals(rs.from())) {
				terminal.unreserve(rs.from());
			}
			terminal.reserve(place.pos(), LIFT_HOLD);
			terminal.liftBound(d.id, place.pos(), 0);
			rs.claim(lb.now());
			((LiftBlockEntity) level.getBlockEntity(l.origin())).claimBed(lb.now() + 2);
			commit(d, lb, l.access(), Router.heading(l.facing()), Drone.TO_LIFT, v.origin(), l.origin());
			return true;
		}
		router.release(d.id);
		LegBuilder lb = begin(d, now);
		int h = -1;
		if (atBed(d, lb, d.cell, d.heading, v, false, 0, load.look())) {
			h = atPlace(d, lb, v.access(), hv, place, false, PUT_BACK, 0, -1);
		}
		if (h < 0) {
			router.release(d.id);
			rebook(d, now);
			return false;
		}
		if (!place.pos().equals(rs.from())) {
			terminal.unreserve(rs.from());
		}
		terminal.reserve(place.pos(), d.id);
		rs.claim(lb.now());
		commit(d, lb, place.access(), h, Drone.RETURN, v.origin(), place.pos());
		return true;
	}

	/**
	 * Drives to a pallet place's aisle cell (along the aisle, standing back so the pallet is in line with the place) and
	 * sets a pallet into it (event, arg) or takes one out (the forks into its pockets, then up: look its pallet). Returns
	 * the heading it ends with at the place's cell, or -1.
	 */
	private int atPlace(Drone d, LegBuilder lb, long from, int fromHeading, Network.Place place, boolean take, int event, int arg, int look) {
		Direction f = place.facing();
		int h = alongAisle(place, f);
		if (h < 0) {
			return -1;
		}
		Direction heading = Router.HEADINGS[h];
		long back = BlockPos.of(place.access()).relative(heading.getOpposite()).asLong();
		if (!drive(d, lb, from, fromHeading, place.access(), h, 200, back)) {
			return -1;
		}
		long atPlace = lb.now();
		BlockPos a = BlockPos.of(place.access());
		float workX = a.getX() + 0.5F - heading.getStepX() * 11.3F / 16, workZ = a.getZ() + 0.5F - heading.getStepZ() * 11.3F / 16;
		double yaw = Math.toRadians(Router.yaw(h));
		double localX = f.getStepX() * Math.cos(yaw) - f.getStepZ() * Math.sin(yaw);
		float side = localX >= 0 ? 1 : -1;
		float set = place.level() == 0 ? 0.2F : (float) (place.level() * 16 + Kinematics.BEAM_TOP + 0.2);
		lb.to(6, workX, workZ, null, null);
		lb.partAt(TRAVERSE_SPEED, 1, -8.3F * side).part(10, 2, 90 * side);
		if (take) {
			lb.partAt(LIFT_SPEED, 0, Math.max(0, set - 1.0F)).partAt(TRAVERSE_SPEED, 1, 7.7F * side).partAt(0.5F, 0, set + 1.0F);
			lb.event(event, place.pos(), arg).look(look);
			lb.partAt(TRAVERSE_SPEED, 1, -8.3F * side).part(10, 2, 0).partAt(TRAVERSE_SPEED, 1, 0).partAt(LIFT_SPEED, 0, CARRY);
		} else {
			lb.partAt(LIFT_SPEED, 0, set + 1.5F).partAt(TRAVERSE_SPEED, 1, 7.7F * side).partAt(0.5F, 0, set);
			lb.event(event, place.pos(), arg).look(-1);
			lb.partAt(TRAVERSE_SPEED, 1, -8.3F * side).part(10, 2, 0).partAt(TRAVERSE_SPEED, 1, 0).partAt(LIFT_SPEED, 0, 0);
		}
		lb.to(6, a.getX() + 0.5F, a.getZ() + 0.5F, null, null);
		router.book(place.access(), atPlace, lb.now(), d.id);
		router.book(back, atPlace, lb.now(), d.id);
		return h;
	}

	/**
	 * Drives to a Ready Storage's bed (its access cell, facing the hatch) and sets the pallet it carries on the rollers
	 * (PUT_BED, arg the order) or takes the one there (TAKE_BED, look its pallet). The pallet's middle is 19.6 px ahead of
	 * the drone's, so it works from a little back into the cell behind.
	 */
	private boolean atBed(Drone d, LegBuilder lb, long from, int fromHeading, Network.Vlm v, boolean put, int arg, int look) {
		int hv = Router.heading(v.facing());
		if (!router.isCell(v.access()) || !drive(d, lb, from, fromHeading, v.access(), hv, 80, v.behind())) {
			return false;
		}
		long arrived = lb.now();
		Direction f = v.facing();
		double[] bed = WarehousePiece.toWorld(WarehousePiece.READY_STORAGE, f, v.origin(), 24.0, 0.0, 40.0);
		float inX = (float) (bed[0] - f.getStepX() * 19.6 / 16), inZ = (float) (bed[2] - f.getStepZ() * 19.6 / 16);
		BlockPos a = BlockPos.of(v.access());
		if (put) {
			lb.partAt(LIFT_SPEED, 0, BED_SET + 1.6F).to(10, inX, inZ, null, null).partAt(0.5F, 0, BED_SET);
			lb.event(PUT_BED, v.origin(), arg).look(-1);
			lb.partAt(LIFT_SPEED, 0, BED_SET - 1.2F).to(10, a.getX() + 0.5F, a.getZ() + 0.5F, null, null).partAt(LIFT_SPEED, 0, 0);
		} else {
			lb.partAt(LIFT_SPEED, 0, BED_SET - 1.0F).to(10, inX, inZ, null, null).partAt(0.5F, 0, BED_SET + 1.0F);
			lb.event(TAKE_BED, v.origin(), 0).look(look);
			lb.to(10, a.getX() + 0.5F, a.getZ() + 0.5F, null, null).partAt(LIFT_SPEED, 0, CARRY);
		}
		router.book(v.access(), arrived, lb.now(), d.id);
		router.book(v.behind(), arrived, lb.now(), d.id);
		return true;
	}

	// ------------------------------------------------------------------ part W4: liquids

	/** A liquid order: the pallet of drums holding most of what it wants to a free Drum Emptying Station that takes it. */
	private boolean liquidFetch(Drone d, Order o, long now) {
		net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant liquid = Drums.liquid(o.item);
		Network.Station e = freeEmptier(d, liquid);
		if (e == null || liquid == null) {
			return false;
		}
		Network.Place place = palletFor(o);
		if (place == null) {
			if (!inFlight(o)) {
				o.state = o.filled > 0 ? (o.dest == Order.WINDOW ? Order.READY : Order.SENT) : Order.CANCELLED;
				o.readyAt = now;
			}
			return false;
		}
		int pf = net.floorOfCell(place.access()), ef = net.floorOfCell(e.access()), df = net.floorOfCell(d.cell);
		if (df != pf) {
			return false;
		}
		if (pf != ef) {
			return liftFetch(d, o, place, e.origin(), e.access(), now);
		}
		if (!(level.getBlockEntity(place.pos()) instanceof RackBlockEntity rack) || rack.load() == null) {
			return false;
		}
		router.release(d.id);
		LegBuilder lb = begin(d, now);
		int h = atPlace(d, lb, d.cell, d.heading, place, true, TAKE_PLACE, o.id, rack.load().look());
		if (h < 0 || !atEmptier(d, lb, place.access(), h, e, true, o.id, -1)) {
			router.release(d.id);
			rebook(d, now);
			return false;
		}
		terminal.reserve(place.pos(), -o.id);
		o.state = Order.FETCHING;
		o.pallet = place.pos();
		commit(d, lb, e.access(), Router.heading(e.facing()), Drone.FETCH, place.pos(), e.origin());
		return true;
	}

	/** A Drum Emptying Station free for a liquid (its tank empty or holding it, with room) no other drone is bringing a pallet to. */
	private Network.@Nullable Station freeEmptier(Drone d, net.fabricmc.fabric.api.transfer.v1.fluid.@Nullable FluidVariant liquid) {
		if (liquid == null) {
			return null;
		}
		for (Network.Station e : net.emptiers) {
			if (!router.isCell(e.access()) || !(level.getBlockEntity(e.origin()) instanceof EmptierBlockEntity em) || !em.takes(liquid)) {
				continue;
			}
			boolean taken = false;
			for (Drone o : terminal.drones()) {
				if (o != d && (o.job == Drone.FETCH || o.job == Drone.TO_LIFT || o.job == Drone.FROM_LIFT) && e.origin().equals(o.target)) {
					taken = true;
				}
			}
			if (!taken && !terminal.liftBoundFor(e.origin())) {
				return e;
			}
		}
		return null;
	}

	/**
	 * Drives to a Drum Emptying Station (its front cell, facing it) and sets the pallet it carries on the platform
	 * (PUT_EMPTIER, arg the order) or takes the one there (TAKE_EMPTIER, look its pallet), as at a Ready Storage's bed.
	 */
	private boolean atEmptier(Drone d, LegBuilder lb, long from, int fromHeading, Network.Station e, boolean put, int arg, int look) {
		int he = Router.heading(e.facing());
		if (!router.isCell(e.access()) || !drive(d, lb, from, fromHeading, e.access(), he, 80, e.behind())) {
			return false;
		}
		long arrived = lb.now();
		Direction f = e.facing();
		double[] bay = WarehousePiece.toWorld(WarehousePiece.DRUM_EMPTIER, f, e.origin(), 24.0, 0.0, 24.0);
		float inX = (float) (bay[0] - f.getStepX() * 19.6 / 16), inZ = (float) (bay[2] - f.getStepZ() * 19.6 / 16);
		BlockPos a = BlockPos.of(e.access());
		float set = emptyDeck() + 0.2F;
		if (put) {
			lb.partAt(LIFT_SPEED, 0, set + 1.6F).to(10, inX, inZ, null, null).partAt(0.5F, 0, set);
			lb.event(PUT_EMPTIER, e.origin(), arg).look(-1);
			lb.partAt(LIFT_SPEED, 0, set - 1.2F).to(10, a.getX() + 0.5F, a.getZ() + 0.5F, null, null).partAt(LIFT_SPEED, 0, 0);
		} else {
			lb.partAt(LIFT_SPEED, 0, set - 1.0F).to(10, inX, inZ, null, null).partAt(0.5F, 0, set + 1.0F);
			lb.event(TAKE_EMPTIER, e.origin(), 0).look(look);
			lb.to(10, a.getX() + 0.5F, a.getZ() + 0.5F, null, null).partAt(LIFT_SPEED, 0, CARRY);
		}
		router.book(e.access(), arrived, lb.now(), d.id);
		router.book(e.behind(), arrived, lb.now(), d.id);
		return true;
	}

	/** Drives to a Drum Filling Station (its front cell, facing it) and sets a pallet of emptied drums on its bay (PUT_FILLER). */
	private boolean atFiller(Drone d, LegBuilder lb, long from, int fromHeading, Network.Station st) {
		int hSt = Router.heading(st.facing());
		if (!router.isCell(st.behind()) || !drive(d, lb, from, fromHeading, st.behind(), hSt, 80, st.access())) {
			return false;
		}
		long arrived = lb.now();
		Direction toStation = st.facing();
		BlockPos spot = st.origin();
		float inX = spot.getX() + 0.5F - toStation.getStepX() * 19.6F / 16, inZ = spot.getZ() + 0.5F - toStation.getStepZ() * 19.6F / 16;
		BlockPos behind = BlockPos.of(st.behind());
		float set = fillDeck() + 0.2F;
		lb.partAt(LIFT_SPEED, 0, set + 1.6F).to(12, inX, inZ, null, null).partAt(0.5F, 0, set);
		lb.event(PUT_FILLER, st.origin(), 0).look(-1);
		lb.partAt(LIFT_SPEED, 0, set - 0.8F).to(12, behind.getX() + 0.5F, behind.getZ() + 0.5F, null, null).partAt(LIFT_SPEED, 0, 0);
		router.book(st.access(), arrived, lb.now(), d.id);
		router.book(st.behind(), arrived, lb.now(), d.id);
		return true;
	}

	/**
	 * A pallet waiting on a Drum Emptying Station's platform: its drums all emptied, to a Drum Filling Station that takes
	 * them back (on its floor); else back to its place (or a free place near, through a lift if its floor is full).
	 */
	private boolean emptierReturnJob(Drone d, Network.Station e, EmptierBlockEntity em, long now) {
		PalletLoad load = em.pallet();
		if (load == null || !router.isCell(e.access())) {
			return false;
		}
		int he = Router.heading(e.facing());
		int ef = net.floorOfCell(e.access());
		if (em.emptied()) {
			Network.Station fl = null;
			for (Network.Station f : net.fillers) {
				if (!claimed(f.origin(), d) && router.isCell(f.behind()) && net.floorOfCell(f.behind()) == ef
						&& level.getBlockEntity(f.origin()) instanceof FillerBlockEntity fe && fe.takesBack(load.size())) {
					fl = f;
					break;
				}
			}
			if (fl == null) {
				return false;
			}
			router.release(d.id);
			LegBuilder lb = begin(d, now);
			if (!atEmptier(d, lb, d.cell, d.heading, e, false, 0, load.look()) || !atFiller(d, lb, e.access(), he, fl)) {
				router.release(d.id);
				rebook(d, now);
				return false;
			}
			((FillerBlockEntity) level.getBlockEntity(fl.origin())).setIncoming(now + 1200);
			em.claim(lb.now());
			terminal.unreserve(em.from());
			commit(d, lb, fl.behind(), Router.heading(fl.facing()), Drone.RETURN, e.origin(), fl.origin());
			return true;
		}
		Network.Place place = null;
		Integer by = em.from() == null ? null : terminal.reservedBy(em.from());
		if (em.from() != null && net.placeAt.containsKey(em.from()) && (by == null || by < 0)
				&& level.getBlockEntity(em.from()) instanceof RackBlockEntity rack && rack.load() == null) {
			place = net.placeAt.get(em.from());
		}
		if (place == null) {
			place = freePlaceOn(ef, e.access());
		}
		for (int k = 1; place == null && k < net.floors.size(); k++) {
			for (int fl : new int[] {ef + k, ef - k}) {
				if (place == null && fl >= 0 && fl < net.floors.size()) {
					place = freePlaceOn(fl, e.access());
				}
			}
		}
		if (place == null) {
			return false;
		}
		router.release(d.id);
		LegBuilder lb = begin(d, now);
		if (net.floorOfCell(place.access()) != ef) {
			Network.Landing l = freeLanding(d, ef, e.access(), now);
			if (l == null || !serves(l, net.floorOfCell(place.access())) || !atEmptier(d, lb, d.cell, d.heading, e, false, 0, load.look())
					|| !atLiftBed(d, lb, e.access(), he, l, true, -1)) {
				router.release(d.id);
				rebook(d, now);
				return false;
			}
			if (!place.pos().equals(em.from())) {
				terminal.unreserve(em.from());
			}
			terminal.reserve(place.pos(), LIFT_HOLD);
			terminal.liftBound(d.id, place.pos(), 0);
			em.claim(lb.now());
			((LiftBlockEntity) level.getBlockEntity(l.origin())).claimBed(lb.now() + 2);
			commit(d, lb, l.access(), Router.heading(l.facing()), Drone.TO_LIFT, e.origin(), l.origin());
			return true;
		}
		int h = -1;
		if (atEmptier(d, lb, d.cell, d.heading, e, false, 0, load.look())) {
			h = atPlace(d, lb, e.access(), he, place, false, PUT_BACK, 0, -1);
		}
		if (h < 0) {
			router.release(d.id);
			rebook(d, now);
			return false;
		}
		if (!place.pos().equals(em.from())) {
			terminal.unreserve(em.from());
		}
		terminal.reserve(place.pos(), d.id);
		em.claim(lb.now());
		commit(d, lb, place.access(), h, Drone.RETURN, e.origin(), place.pos());
		return true;
	}

	/** Drives to a station (behind its build spot) and sets an empty pallet on its turntable (PUT_STATION). */
	private boolean atStation(Drone d, LegBuilder lb, long from, int fromHeading, Network.Station st) {
		return atStation(d, lb, from, fromHeading, st, PUT_STATION);
	}

	/** Drives to a station (behind its build spot) and sets the pallet it carries on its turntable (event: PUT_STATION, CONSOL_SET). */
	private boolean atStation(Drone d, LegBuilder lb, long from, int fromHeading, Network.Station st, int event) {
		int hSt = Router.heading(st.facing());
		if (!router.isCell(st.behind()) || !drive(d, lb, from, fromHeading, st.behind(), hSt, 80, st.access())) {
			return false;
		}
		long arrived = lb.now();
		Direction toStation = st.facing();
		BlockPos spot = st.origin();
		float inX = spot.getX() + 0.5F - toStation.getStepX() * 19.6F / 16, inZ = spot.getZ() + 0.5F - toStation.getStepZ() * 19.6F / 16;
		BlockPos behind = BlockPos.of(st.behind());
		lb.partAt(LIFT_SPEED, 0, 3.0F).to(12, inX, inZ, null, null).partAt(0.5F, 0, 1.5F);
		lb.event(event, st.origin(), 0).look(-1);
		lb.partAt(LIFT_SPEED, 0, 0.8F).to(12, behind.getX() + 0.5F, behind.getZ() + 0.5F, null, null).partAt(LIFT_SPEED, 0, 0);
		router.book(st.access(), arrived, lb.now(), d.id);
		router.book(st.behind(), arrived, lb.now(), d.id);
		return true;
	}

	/** The way along the aisle at a place's cell with a cell behind it to stand back into, or -1. */
	private int alongAisle(Network.Place place, Direction f) {
		for (Direction dir : new Direction[] {f.getClockWise(), f.getCounterClockWise()}) {
			long back = BlockPos.of(place.access()).relative(dir.getOpposite()).asLong();
			if (router.isCell(back)) {
				return Router.heading(dir);
			}
		}
		return -1;
	}

	void home(Drone d, long now) {
		router.release(d.id);
		LegBuilder lb = begin(d, now);
		BlockPos dock = d.dock;
		if (!(level.getBlockState(dock).getBlock() instanceof DockBlock)) {
			rebook(d, now);
			d.retry = now + 100;
			return;
		}
		int out = Router.heading(level.getBlockState(dock).getValue(DockBlock.FACING).getOpposite());
		if (!drive(d, lb, d.cell, d.heading, dock.asLong(), out, 0)) {
			rebook(d, now);
			d.retry = now + 60;
			return;
		}
		if (d.kind == DroneKind.STACKER) {
			lb.to(10, null, null, null, Kinematics.PARKED);
		}
		lb.event(DOCK, dock, 0).hold(2);
		commit(d, lb, dock.asLong(), out, Drone.HOME, null, null);
	}

	/** Books a drone's cell again after a plan that came to nothing (its own bookings were let go first). */
	private void rebook(Drone d, long now) {
		router.release(d.id);
		router.book(d.cell, now, Long.MAX_VALUE, d.id);
	}

	// ------------------------------------------------------------------ part W5: housekeeping (consolidation, cycle counts)

	/** A consolidation's geometry: Kinematics.CONSOL_STRAFE, CONSOL_LIFT. */
	static final float CONSOL_STRAFE = (float) Kinematics.CONSOL_STRAFE, CONSOL_LIFT = (float) Kinematics.CONSOL_LIFT;
	/** A consolidation's case, from A to B (ticks). */
	static final int PICK_TICKS = 58;
	/** The scanner sits this far over the label it reads (px; the head's scanner is 7.5 px over the forks). */
	static final float SCAN_OVER = 6.0F, SCANNER_Y = 7.5F;

	/** Housekeeping, once the fleet has had nothing else to do for a while: a consolidation, else a cycle count. */
	private boolean housekeeping(Drone d, long now) {
		boolean idle = now - terminal.lastWork() >= IDLE_FOR;
		if ((idle && terminal.consolidateMode() == ControlPayload.WHEN_IDLE || terminal.consolidateNow()) && consolidateFirst(d, now)) {
			return true;
		}
		return idle && terminal.countMode() != ControlPayload.COUNT_OFF && countJob(d, now);
	}

	/** A part pallet the stackers can consolidate: cases (cartons or crates) of one item, not full. */
	private static boolean consolidatable(@Nullable PalletLoad load) {
		if (load == null || load.isEmpty() || load.full() || Drums.isLiquid(load)) {
			return false;
		}
		int kind = load.kind();
		if (kind != CaseKind.CARTON.ordinal() && kind != CaseKind.CARTON_W.ordinal() && kind != CaseKind.CRATE.ordinal()) {
			return false;
		}
		CaseLoad first = load.cases().get(0);
		for (CaseLoad c : load.cases()) {
			if (!c.sameItem(first.item())) {
				return false;
			}
		}
		return true;
	}

	/** The lane cells a consolidation at a station needs: the one left of the stacker's (A over it), the two behind that. */
	private long @Nullable [] consolSide(Network.Station st) {
		Direction f = st.facing();
		BlockPos s = BlockPos.of(st.access()).relative(f.getCounterClockWise());
		long s0 = s.asLong(), s1 = s.relative(f.getOpposite()).asLong(), s2 = s.relative(f.getOpposite(), 2).asLong();
		if (!router.isCell(st.access()) || !router.isCell(s0) || !router.isCell(s1) || !router.isCell(s2)) {
			return null;
		}
		return new long[] {s0, s1, s2};
	}

	/** A stacker on a floor that may work now (for a consolidation's second leg), nearest a cell, or null. */
	private @Nullable Drone freeStacker(int floor, long near, long now) {
		Drone best = null;
		int bestDist = Integer.MAX_VALUE;
		for (Drone o : terminal.drones()) {
			if (o.kind != DroneKind.STACKER || o.busy(now) || now < o.leg.end() || o.paused || o.outOfService || o.sendHome
					|| o.battery < (o.docked ? READY : LOW) || net.floorOfCell(o.cell) != floor) {
				continue;
			}
			int dist = distance(o.cell, near);
			if (dist < bestDist) {
				best = o;
				bestDist = dist;
			}
		}
		return best;
	}

	/** Whether a floor has a stacker that can work at all (not out of service). */
	private boolean anyStacker(int floor) {
		for (Drone o : terminal.drones()) {
			if (o.kind == DroneKind.STACKER && !o.outOfService && net.floorOfCell(o.dock.asLong()) == floor) {
				return true;
			}
		}
		return false;
	}

	/** A station on a floor (not this one) a drone may bring an empty pallet back to, or null. */
	private Network.@Nullable Station takeBack(Drone d, int floor, Network.@Nullable Station not) {
		for (Network.Station s : net.stations) {
			if (s != not && net.floorOfCell(s.behind()) == floor && !claimed(s.origin(), d) && router.isCell(s.behind())
					&& level.getBlockEntity(s.origin()) instanceof StationBlockEntity se && se.takesBack()) {
				return s;
			}
		}
		return null;
	}

	private int stationNumber(Network.Station st) {
		return net.stations.indexOf(st) + 1;
	}

	/**
	 * Consolidation, its first leg: two part pallets of one item that fit on one; the bigger (B) out of its place onto a
	 * station's build spot (CONSOL_SET; the station keeps the other's place, A, which is held for it). The second leg
	 * (consolidateSecond) brings A and has the stacker move its cases onto B.
	 */
	private boolean consolidateFirst(Drone d, long now) {
		if (now < terminal.consolPauseUntil()) {
			return false;
		}
		for (Drone o : terminal.drones()) {
			if (o != d && o.job == Drone.CONSOLIDATE) {
				return false;
			}
		}
		int floor = net.floorOfCell(d.cell);
		Network.Station st = null;
		StationBlockEntity station = null;
		int stDist = Integer.MAX_VALUE;
		int onFloor = 0;
		for (Network.Station s : net.stations) {
			if (net.floorOfCell(s.behind()) != floor || !(level.getBlockEntity(s.origin()) instanceof StationBlockEntity se)) {
				continue;
			}
			onFloor++;
			if (se.consolidating()) {
				return false;
			}
			if (claimed(s.origin(), d) || !se.canClear() || !router.isCell(s.behind()) || consolSide(s) == null) {
				continue;
			}
			int dist = distance(d.cell, s.behind());
			if (dist < stDist) {
				st = s;
				station = se;
				stDist = dist;
			}
		}
		// (The emptied pallet goes back to another station, and a stacker moves the cases: both must be there now, or the
		// first pallet would only wait on the station.)
		if (st == null || onFloor < 2 || !anyStacker(floor) || freeStacker(floor, st.access(), now) == null || takeBack(d, floor, st) == null) {
			return false;
		}
		java.util.Map<ItemVariant, List<Network.Place>> parts = new java.util.HashMap<>();
		for (Network.Place p : net.places) {
			if (p.level() > 4 || terminal.reserved(p.pos()) || !router.isCell(p.access()) || net.floorOfCell(p.access()) != floor
					|| alongAisle(p, p.facing()) < 0 || !(level.getBlockEntity(p.pos()) instanceof RackBlockEntity rack) || !consolidatable(rack.load())) {
				continue;
			}
			parts.computeIfAbsent(ItemVariant.of(rack.load().cases().get(0).item()), k -> new ArrayList<>()).add(p);
		}
		Network.Place a = null, b = null;
		int bestScore = Integer.MAX_VALUE;
		for (List<Network.Place> list : parts.values()) {
			if (list.size() < 2) {
				continue;
			}
			list.sort(Comparator.comparingInt(p -> ((RackBlockEntity) level.getBlockEntity(p.pos())).load().size()));
			Network.Place small = list.get(0);
			int n = ((RackBlockEntity) level.getBlockEntity(small.pos())).load().size();
			for (int i = list.size() - 1; i >= 1; i--) {
				Network.Place big = list.get(i);
				int m = ((RackBlockEntity) level.getBlockEntity(big.pos())).load().size();
				if (n + m > PalletLoad.MAX_CASES) {
					continue;
				}
				int score = n * 40 + distance(small.access(), st.behind()) + distance(big.access(), st.behind());
				if (score < bestScore) {
					a = small;
					b = big;
					bestScore = score;
				}
				break;
			}
		}
		if (a == null) {
			return false;
		}
		PalletLoad bLoad = ((RackBlockEntity) level.getBlockEntity(b.pos())).load();
		router.release(d.id);
		LegBuilder lb = begin(d, now);
		int h = atPlace(d, lb, d.cell, d.heading, b, true, CONSOL_TAKE, 0, bLoad.look());
		if (h < 0 || !atStation(d, lb, b.access(), h, st, CONSOL_SET)) {
			router.release(d.id);
			rebook(d, now);
			return false;
		}
		terminal.reserve(b.pos(), d.id);
		terminal.reserve(a.pos(), CONSOL_HOLD);
		station.setConsolPending(a.pos(), lb.now() + 400);
		int nA = ((RackBlockEntity) level.getBlockEntity(a.pos())).load().size();
		terminal.setHouse("CONSOLIDATING " + a.label() + " ONTO " + b.label(), "STATION " + stationNumber(st) + ": " + nA + (nA == 1 ? " CASE" : " CASES")
				+ " TO MOVE");
		commit(d, lb, st.behind(), Router.heading(st.facing()), Drone.CONSOLIDATE, b.pos(), st.origin());
		return true;
	}

	private static void waitUntil(LegBuilder lb, long t) {
		if (t > lb.now()) {
			lb.hold((int) (t - lb.now()));
		}
	}

	/**
	 * Consolidation, its second leg (two drones' legs at once): a pallet drone brings A out of its place and holds it
	 * over the lane left of the station's stacker cell; the stacker moves its cases one by one onto B (CONSOL_PICK off A,
	 * PLACE_CASE onto B), then B is done (CONSOL_DONE: wrapped and put away as any finished pallet); the emptied A goes
	 * back to another station's magazine.
	 */
	private boolean consolidateSecond(Drone d, long now) {
		for (Network.Station st : net.stations) {
			if (!(level.getBlockEntity(st.origin()) instanceof StationBlockEntity station) || station.consolFrom() == null) {
				continue;
			}
			BlockPos from = station.consolFrom();
			Network.Place a = net.placeAt.get(from);
			PalletLoad b = station.build();
			long[] side = consolSide(st);
			PalletLoad aLoad = level.getBlockEntity(from) instanceof RackBlockEntity rack ? rack.load() : null;
			Integer by = terminal.reservedBy(from);
			if (a == null || b == null || b.isEmpty() || side == null || !consolidatable(aLoad) || aLoad.size() + b.size() > PalletLoad.MAX_CASES
					|| !aLoad.cases().get(0).sameItem(b.cases().get(0).item()) || by != null && by != CONSOL_HOLD) {
				// (It can't be done any more: what is on the station is wrapped and put away as it is.)
				terminal.unreserveIf(from, CONSOL_HOLD);
				station.consolidated(now);
				continue;
			}
			int floor = net.floorOfCell(st.behind());
			if (net.floorOfCell(d.cell) != floor || terminal.fleetHeld()) {
				continue;
			}
			Drone sd = freeStacker(floor, st.access(), now);
			Network.Station back = takeBack(d, floor, st);
			if (sd == null || back == null) {
				continue;
			}
			int n = aLoad.size(), kind = aLoad.kind();
			Direction f = st.facing(), left = f.getCounterClockWise();
			int hF = Router.heading(f);
			router.release(d.id);
			router.release(sd.id);
			// The pallet drone: A out of its place, to the second cell behind the side cell, up clear of the stacker, in.
			LegBuilder lp = begin(d, now);
			int h = atPlace(d, lp, d.cell, d.heading, a, true, CONSOL_TAKE, 0, aLoad.look());
			if (h < 0 || !drive(d, lp, a.access(), h, side[2], hF, 200 + n * PICK_TICKS, side[1], side[0])) {
				router.release(d.id);
				rebook(d, now);
				rebook(sd, now);
				return false;
			}
			long pArrived = lp.now();
			BlockPos s0 = BlockPos.of(side[0]), s2 = BlockPos.of(side[2]);
			float hx = s0.getX() + 0.5F - f.getStepX() * 19.6F / 16, hz = s0.getZ() + 0.5F - f.getStepZ() * 19.6F / 16;
			lp.partAt(LIFT_SPEED, 0, CONSOL_LIFT).to(14, hx, hz, null, null);
			long pReady = lp.now();
			// The side cells are the pallet drone's while it holds A (booked before the stacker's way is found).
			int hold = 200 + n * PICK_TICKS;
			for (long c : side) {
				router.book(c, pArrived, pArrived + hold, d.id);
			}
			// The stacker: to its cell at the station.
			LegBuilder ls = begin(sd, now);
			if (!drive(sd, ls, sd.cell, sd.heading, st.access(), hF, 200 + n * PICK_TICKS)) {
				router.release(d.id);
				router.release(sd.id);
				rebook(d, now);
				rebook(sd, now);
				return false;
			}
			long sArrived = ls.now();
			long start = Math.max(pReady, ls.now()) + 2;
			if (start - Math.min(pReady, sArrived) > 150) {
				// (One would wait too long for the other: past what their ways were booked for. Later.)
				router.release(d.id);
				router.release(sd.id);
				rebook(d, now);
				rebook(sd, now);
				return false;
			}
			waitUntil(lp, start);
			waitUntil(ls, start);
			BlockPos acc = BlockPos.of(st.access());
			float cx = acc.getX() + 0.5F, cz = acc.getZ() + 0.5F;
			float sx = cx + left.getStepX() * CONSOL_STRAFE / 16, sz = cz + left.getStepZ() * CONSOL_STRAFE / 16;
			long lastPick = start;
			boolean ok = true;
			for (int j = 0; j < n && ok; j++) {
				int i = n - 1 - j;
				double[] top = Kinematics.heldTop(hx, hz, ls.y, Router.yaw(Router.heading(f)), CONSOL_LIFT, i);
				ls.to(6, sx, sz, null, null);
				double[] on = local(ls, top[0], top[1], top[2]);
				double[] qOver = over(ls, top), qOn = Kinematics.reachLocal(on[0], on[1], on[2]);
				if (qOver == null || qOn == null) {
					ok = false;
					break;
				}
				ls.to(10, null, null, null, arm(qOver, ls)).to(6, null, null, null, arm(qOn, ls));
				ls.event(CONSOL_PICK, st.origin(), d.id).look(aLoad.cases().get(i).kind().ordinal());
				lastPick = ls.now();
				waitUntil(lp, lastPick);
				lp.look(i == 0 ? 0 : PalletLoad.look(kind, i));
				ls.to(6, null, null, null, arm(qOver, ls)).to(6, cx, cz, null, null);
				double[] slot = station.slotTop(b.size() + j);
				double[] at = local(ls, slot[0], slot[1], slot[2]);
				double[] qAbove = over(ls, slot), qAt = Kinematics.reachLocal(at[0], at[1], at[2]);
				if (qAbove == null || qAt == null) {
					ok = false;
					break;
				}
				ls.to(12, null, null, null, arm(qAbove, ls)).to(6, null, null, null, arm(qAt, ls));
				ls.event(PLACE_CASE, st.origin(), 0).look(-1);
				ls.to(6, null, null, null, arm(qAbove, ls));
			}
			if (!ok) {
				router.release(d.id);
				router.release(sd.id);
				rebook(d, now);
				rebook(sd, now);
				terminal.unreserveIf(from, CONSOL_HOLD);
				station.consolidated(now);
				return false;
			}
			ls.event(CONSOL_DONE, st.origin(), 0).to(12, null, null, null, Kinematics.PARKED);
			router.book(st.access(), sArrived, ls.now(), sd.id);
			// The emptied pallet: back out, down, to the other station's magazine.
			waitUntil(lp, lastPick + 8);
			lp.to(14, s2.getX() + 0.5F, s2.getZ() + 0.5F, null, null).partAt(LIFT_SPEED, 0, CARRY);
			for (long c : side) {
				router.book(c, pArrived, lp.now(), d.id);
			}
			if (!atStation(d, lp, side[2], hF, back)) {
				router.release(d.id);
				router.release(sd.id);
				rebook(d, now);
				rebook(sd, now);
				return false;
			}
			((StationBlockEntity) level.getBlockEntity(back.origin())).setIncoming(lp.now() + 200);
			station.takeConsolFrom();
			terminal.reserve(from, d.id);
			terminal.setHouse("CONSOLIDATING " + a.label() + " AT STATION " + stationNumber(st), n + (n == 1 ? " CASE" : " CASES") + " ONTO "
					+ (b.size() + n) + " / " + PalletLoad.MAX_CASES);
			commit(sd, ls, st.access(), hF, Drone.CONSOLIDATE, from, st.origin());
			commit(d, lp, back.behind(), Router.heading(back.facing()), Drone.CONSOLIDATE, from, back.origin());
			return true;
		}
		return false;
	}

	/** Where a place's label is (its middle, world blocks): on its beam's plate; the ground place's on the first beam's. */
	static double[] labelPoint(Network.Place p) {
		boolean ground = p.level() == 0;
		BlockPos block = ground ? p.pos().above() : p.pos();
		double bx = ground ? 5.5 : p.level() == 1 ? 10.5 : 8.0, bz = 15.7;
		double wx, wz;
		switch (p.facing()) {
			case EAST -> {
				wx = 16 - bz;
				wz = bx;
			}
			case SOUTH -> {
				wx = 16 - bx;
				wz = 16 - bz;
			}
			case WEST -> {
				wx = bz;
				wz = 16 - bx;
			}
			default -> {
				wx = bx;
				wz = bz;
			}
		}
		return new double[] {block.getX() + wx / 16, block.getY() + 0.8 / 16, block.getZ() + wz / 16};
	}

	/**
	 * A cycle count: a pallet drone drives to the rack column whose count is oldest (daily: not counted today; nonstop:
	 * any not counted in the last minute), turns its head to it and reads each place's label with its scanner (COUNT:
	 * what is there is checked against the books), the lowest first.
	 */
	private boolean countJob(Drone d, long now) {
		int floor = net.floorOfCell(d.cell);
		boolean daily = terminal.countMode() == ControlPayload.COUNT_DAILY;
		java.util.Map<Long, List<Network.Place>> columns = new java.util.LinkedHashMap<>();
		for (Network.Place p : net.places) {
			if (p.level() > 4 || !router.isCell(p.access()) || net.floorOfCell(p.access()) != floor || alongAisle(p, p.facing()) < 0) {
				continue;
			}
			columns.computeIfAbsent(BlockPos.asLong(p.pos().getX(), 0, p.pos().getZ()), k -> new ArrayList<>()).add(p);
		}
		List<Network.Place> best = null;
		long bestAt = Long.MAX_VALUE;
		int bestDist = Integer.MAX_VALUE;
		for (List<Network.Place> col : columns.values()) {
			col.sort(Comparator.comparingInt(Network.Place::level));
			if (claimed(col.get(0).pos(), d)) {
				continue;
			}
			long oldest = Long.MAX_VALUE;
			for (Network.Place p : col) {
				if (!terminal.reserved(p.pos())) {
					oldest = Math.min(oldest, terminal.counted(p.pos()));
				}
			}
			if (oldest == Long.MAX_VALUE || (daily ? oldest > 0 && now - oldest < 24000 : now - oldest < 1200)) {
				continue;
			}
			int dist = distance(d.cell, col.get(0).access());
			if (oldest < bestAt || oldest == bestAt && dist < bestDist) {
				best = col;
				bestAt = oldest;
				bestDist = dist;
			}
		}
		if (best == null) {
			return false;
		}
		Network.Place p0 = best.get(0);
		Direction f = p0.facing();
		int h = alongAisle(p0, f);
		Direction heading = Router.HEADINGS[h];
		long back = BlockPos.of(p0.access()).relative(heading.getOpposite()).asLong();
		router.release(d.id);
		LegBuilder lb = begin(d, now);
		if (!drive(d, lb, d.cell, d.heading, p0.access(), h, 80 + 40 * best.size(), back)) {
			rebook(d, now);
			return false;
		}
		long at = lb.now();
		BlockPos a = BlockPos.of(p0.access());
		double yaw = Math.toRadians(Router.yaw(h));
		double localX = f.getStepX() * Math.cos(yaw) - f.getStepZ() * Math.sin(yaw);
		float side = localX >= 0 ? 1 : -1;
		lb.partAt(TRAVERSE_SPEED, 1, -8.3F * side).part(10, 2, 90 * side);
		int read = 0;
		for (Network.Place p : best) {
			if (terminal.reserved(p.pos())) {
				continue;
			}
			double[] label = labelPoint(p);
			// Along the aisle so the head's pivot (11.3 px ahead of its middle) is in line with the label; the scanner over it.
			double along = (label[0] - (a.getX() + 0.5)) * heading.getStepX() + (label[2] - (a.getZ() + 0.5)) * heading.getStepZ();
			float bx = (float) (a.getX() + 0.5 + heading.getStepX() * (along - 11.3 / 16)), bz = (float) (a.getZ() + 0.5 + heading.getStepZ() * (along - 11.3 / 16));
			float lift = (float) Math.max(0, Math.min(Kinematics.PD_LIFT, (label[1] - lb.y) * 16 + SCAN_OVER - SCANNER_Y));
			lb.to(8, bx, bz, null, null).partAt(LIFT_SPEED, 0, lift);
			lb.part(3, SCAN, 1).hold(14);
			lb.event(COUNT, p.pos(), 0).hold(6).part(3, SCAN, 0);
			read++;
		}
		lb.part(10, 2, 0).partAt(TRAVERSE_SPEED, 1, 0).partAt(LIFT_SPEED, 0, 0);
		lb.to(6, a.getX() + 0.5F, a.getZ() + 0.5F, null, null);
		router.book(p0.access(), at, lb.now(), d.id);
		router.book(back, at, lb.now(), d.id);
		String label = p0.label().length() > 2 ? p0.label().substring(0, p0.label().lastIndexOf('-')) : p0.label();
		terminal.setHouse("CYCLE COUNT " + label + " (" + read + (read == 1 ? " PLACE)" : " PLACES)"),
				daily ? "DAILY: EVERY PLACE ONCE A DAY" : "NONSTOP: WHILE THE FLEET IS IDLE");
		commit(d, lb, p0.access(), h, Drone.COUNT, null, p0.pos());
		return true;
	}

	// ------------------------------------------------------------------ geometry

	/** A world point (blocks) in the drone's own frame (px) where the builder has it now. */
	private static double[] local(LegBuilder lb, double wx, double wy, double wz) {
		double dx = (wx - lb.x) * 16, dz = (wz - lb.z) * 16;
		double a = Math.toRadians(-lb.yaw);
		double lx = dx * Math.cos(a) + dz * Math.sin(a), lz = -dx * Math.sin(a) + dz * Math.cos(a);
		return new double[] {lx + 8, (wy - lb.y) * 16, lz + 8};
	}

	/** The arm over a point (world blocks): 3 px over it, or as far over as it reaches. */
	private static double @Nullable [] over(LegBuilder lb, double[] w) {
		for (double up : new double[] {3, 2, 1.2, 0.6}) {
			double[] p = local(lb, w[0], w[1] + up / 16.0, w[2]);
			double[] q = Kinematics.reachLocal(p[0], p[1], p[2]);
			if (q != null) {
				return q;
			}
		}
		return null;
	}

	private static List<Float> arm(double[] q, LegBuilder lb) {
		float film = lb.p.size() > 4 ? lb.p.get(4) : 0;
		return List.of((float) q[0], (float) q[1], (float) q[2], (float) q[3], film);
	}
}
