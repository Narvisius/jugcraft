package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * A diesel generator's starting air (round 31; Justin: "Starting air system ... Yes, it works", in psig). Its
 * receivers hold the air the engine starts on, its compressors refill them, its dryer dries what goes in: each piece
 * joins the nearest diesel generator within {@link #REACH} blocks. Full is {@link #FULL} psig; each start takes
 * {@link #PER_START} psig shared over the receivers (two: 20 psig a start, five starts from full); a start that would
 * leave less than {@link #MIN} barely turns it over, and it fails to start (FAIL TO START). The lead compressor cuts in below {@link #LEAD_ON}, the lag
 * below {@link #LAG_ON}; each running one puts back {@link #RATE} psig a second until the receivers are full.
 *
 * <p>A generator with no receivers near (one built by hand without them) starts as it always did, on air it is taken
 * to have.
 */
public final class StartingAir {
	public static final double FULL = 250, LEAD_ON = 225, LAG_ON = 200, MIN = 150, PER_START = 40, RATE = 2;
	/** How far (blocks) from a generator's anchor its starting air pieces may stand. */
	public static final int REACH = 40;
	/** How often (ticks) a generator looks for its pieces again even when nothing in the plant changed. */
	static final int RESCAN = 200;

	private StartingAir() {
	}

	/** Whether the generator has starting air pieces (receivers): its air is kept, used and refilled. */
	public static boolean kept(EquipmentBlockEntity edg) {
		return edg.airReceivers > 0;
	}

	/** psig one start takes from the generator's receivers. */
	public static double perStart(EquipmentBlockEntity edg) {
		return PER_START / Math.max(1, edg.airReceivers);
	}

	/** Whether the generator has the air for a start (a generator with no receivers always has). */
	public static boolean enough(EquipmentBlockEntity edg) {
		return enough(edg.air, edg.airReceivers);
	}

	/** Whether {@code air} psig in {@code receivers} receivers (none: its air isn't kept) is enough for a start. */
	public static boolean enough(double air, int receivers) {
		return receivers <= 0 || air - PER_START / receivers >= MIN - 1e-6;
	}

	/**
	 * A start: with too little air it barely turns over (wasting some) and won't start (false); else the air it takes
	 * is used (true). A generator with no receivers always has its air.
	 */
	public static boolean start(EquipmentBlockEntity edg) {
		boolean ok = enough(edg);
		edg.air = afterStart(edg.air, edg.airReceivers);
		return ok;
	}

	/** The air left after a start (or a failed one: half a start's worth wasted turning it over). */
	public static double afterStart(double air, int receivers) {
		if (receivers <= 0) {
			return air;
		}
		double take = PER_START / receivers;
		return Math.max(0, air - (enough(air, receivers) ? take : take / 2));
	}

	/**
	 * A second of a generator's compressors (the lead first in {@code running}, which is updated): the lead runs below
	 * {@link #LEAD_ON}, the others below {@link #LAG_ON}, each on until {@link #FULL}; the air after it. Its air isn't
	 * kept ({@code kept} false): all stopped, full.
	 */
	public static double second(double air, boolean kept, boolean[] running) {
		int on = 0;
		for (int k = 0; k < running.length; k++) {
			running[k] = kept && (air < (k == 0 ? LEAD_ON : LAG_ON) || running[k] && air < FULL);
			if (running[k]) {
				on++;
			}
		}
		return kept ? Math.min(FULL, air + RATE * on) : FULL;
	}

	/**
	 * Each second (spread by position): the generator finds its pieces again when the plant changed (or now and then),
	 * runs its compressors (the lead below {@link #LEAD_ON}, the lag below {@link #LAG_ON}, both on until {@link #FULL})
	 * and refills its receivers; the pieces get its air and their own state, sent to the players when it changes.
	 */
	public static void tick(ServerLevel level, EquipmentBlockEntity edg, long now) {
		if (Math.floorMod(now + edg.getBlockPos().asLong(), 20) != 0) {
			return;
		}
		EquipmentRegistry registry = EquipmentRegistry.of(level);
		if (edg.airVersion != registry.version() || now - edg.airScanned >= RESCAN) {
			edg.airVersion = registry.version();
			edg.airScanned = now;
			edg.airPieces = find(registry, edg.getBlockPos());
		}
		List<EquipmentBlockEntity> receivers = new ArrayList<>(), compressors = new ArrayList<>(), dryers = new ArrayList<>();
		for (BlockPos at : edg.airPieces) {
			if (level.isLoaded(at) && level.getBlockEntity(at) instanceof EquipmentBlockEntity piece && piece.kind().startingAir()) {
				(switch (piece.kind()) {
					case EDG_AIR_RECEIVER -> receivers;
					case EDG_AIR_COMPRESSOR -> compressors;
					default -> dryers;
				}).add(piece);
			}
		}
		boolean dirty = false;
		if (receivers.size() != edg.airReceivers) {
			edg.airReceivers = receivers.size();
			dirty = true;
		}
		// The lead first: compressor A (an even label) before B, then by where they stand.
		compressors.sort(Comparator.comparingInt(StartingAir::label).thenComparingLong(c -> c.getBlockPos().asLong()));
		boolean[] running = new boolean[compressors.size()];
		for (int k = 0; k < running.length; k++) {
			running[k] = compressors.get(k).running;
		}
		edg.air = second(edg.air, kept(edg), running);
		for (int k = 0; k < running.length; k++) {
			compressors.get(k).setRunning(now, running[k]);
			compressors.get(k).airLead = k == 0;
		}
		if (Math.abs(edg.air - edg.airSent) >= 1 || edg.air == FULL && edg.airSent != FULL) {
			edg.airSent = edg.air;
			dirty = true;
		}
		if (dirty) {
			edg.changed();
		}
		for (List<EquipmentBlockEntity> group : List.of(receivers, compressors, dryers)) {
			for (EquipmentBlockEntity piece : group) {
				join(piece, edg, piece.kind() == EquipmentKind.EDG_AIR_COMPRESSOR ? (piece.running ? edg.air + 5 : 0) : edg.air);
			}
		}
	}

	/** A piece takes its generator and the pressure it shows (sent to the players when either changes). */
	private static void join(EquipmentBlockEntity piece, EquipmentBlockEntity edg, double shows) {
		boolean dirty = false;
		if (!edg.getBlockPos().equals(piece.linked)) {
			piece.linked = edg.getBlockPos().immutable();
			dirty = true;
		}
		piece.air = shows;
		if (Math.abs(piece.air - piece.airSent) >= 1 || (piece.air == 0) != (piece.airSent == 0)) {
			piece.airSent = piece.air;
			dirty = true;
		}
		if (dirty) {
			piece.changed();
		}
	}

	/** The piece's label: 0 or 2 an A, 1 or 3 a B (its anchor's variant). */
	static int label(EquipmentBlockEntity piece) {
		return piece.getBlockState().hasProperty(EquipmentAnchorBlock.VARIANT) ? piece.getBlockState().getValue(EquipmentAnchorBlock.VARIANT) % 2 : 0;
	}

	/** The starting air pieces within {@link #REACH} of a generator that have no other generator nearer. */
	static List<BlockPos> find(EquipmentRegistry registry, BlockPos at) {
		Map<BlockPos, EquipmentRegistry.Entry> anchors = registry.anchors();
		List<BlockPos> edgs = new ArrayList<>();
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : anchors.entrySet()) {
			if (e.getValue().kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR) {
				edgs.add(e.getKey());
			}
		}
		List<BlockPos> out = new ArrayList<>();
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : anchors.entrySet()) {
			if (!e.getValue().kind().startingAir()) {
				continue;
			}
			double d = e.getKey().distSqr(at);
			if (d > (double) REACH * REACH) {
				continue;
			}
			BlockPos piece = e.getKey();
			if (edgs.stream().noneMatch(o -> !o.equals(at) && o.distSqr(piece) < d)) {
				out.add(piece.immutable());
			}
		}
		return out;
	}

	/**
	 * A piece's own check (each five seconds): its generator still there and within reach, or it stands alone (its
	 * compressor stopped, its gauge at nothing, its dryer's lamp out).
	 */
	public static void tickPiece(ServerLevel level, EquipmentBlockEntity piece, long now) {
		if (piece.linked == null || Math.floorMod(now + piece.getBlockPos().asLong(), 100) != 0 || !level.isLoaded(piece.linked)) {
			return;
		}
		// (A generator that hasn't looked for its pieces since it loaded keeps them until it has.)
		if (!(level.getBlockEntity(piece.linked) instanceof EquipmentBlockEntity edg) || edg.kind() != EquipmentKind.EMERGENCY_DIESEL_GENERATOR
				|| edg.airVersion != -1 && !edg.airPieces.contains(piece.getBlockPos())) {
			piece.linked = null;
			piece.air = 0;
			piece.airSent = 0;
			piece.setRunning(now, false);
			piece.changed();
		}
	}

	/** Whether a piece's generator is there for it (its dryer's POWER lamp, the click's message). */
	public static boolean joined(EquipmentBlockEntity piece) {
		return piece.linked != null;
	}

	/** Blocks from a piece to its generator (for its message), or -1. */
	public static int distance(EquipmentBlockEntity piece) {
		return piece.linked == null ? -1 : (int) Math.round(Math.sqrt(piece.linked.distSqr(piece.getBlockPos())));
	}
}
