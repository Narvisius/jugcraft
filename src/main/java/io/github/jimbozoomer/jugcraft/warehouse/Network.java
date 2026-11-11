package io.github.jimbozoomer.jugcraft.warehouse;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A warehouse as its terminal finds it: every warehouse block joined to the terminal (block to block, at faces and
 * edges, within reach), sorted into what the drones need: the lane cells, the docks, the pallet places with their
 * aisle cells and codes, the intakes and stations with the cells a drone works them from.
 */
public final class Network {
	public final LongSet members = new LongOpenHashSet();
	public final LongSet cells = new LongOpenHashSet();
	public final List<BlockPos> docks = new ArrayList<>();
	public final List<Place> places = new ArrayList<>();
	public final Map<BlockPos, Place> placeAt = new HashMap<>();
	public final List<Intake> intakes = new ArrayList<>();
	public final List<Station> stations = new ArrayList<>();
	public final List<BlockPos> panels = new ArrayList<>();
	/** Part W2: the Ready Storages (their hatches' beds aren't lane cells) and the Goods Out panels. */
	public final List<Vlm> vlms = new ArrayList<>();
	public final List<BlockPos> outPanels = new ArrayList<>();
	/** The other terminals joined to the same warehouse (only the first, by position, runs it). */
	public final List<BlockPos> terminals = new ArrayList<>();
	public boolean truncated;
	/**
	 * Part W3: the Pallet Lifts' landings (each with its bed out front, as Ready Storage's), the floors (the heights of
	 * their lane cells, lowest first: floor 1, 2, ...), the drone crossings' cells and the beacons beside them, the
	 * aisle signs.
	 */
	public final List<Landing> landings = new ArrayList<>();
	public final List<Integer> floors = new ArrayList<>();
	public final LongSet crossings = new LongOpenHashSet();
	public final List<BlockPos> beacons = new ArrayList<>();
	public final List<BlockPos> signs = new ArrayList<>();
	/**
	 * Part W4: the Drum Filling Stations and Drum Emptying Stations (as stations: their origin the fill bay's or the
	 * platform's block, the cell in front where a pallet drone works them from and the one behind that).
	 */
	public final List<Station> fillers = new ArrayList<>();
	public final List<Station> emptiers = new ArrayList<>();

	/** A lift landing: its origin, facing (its user looks it), the bed's cell, the cell a drone works the bed from, the one behind. */
	public record Landing(BlockPos origin, Direction facing, long bed, long access, long behind) {
	}

	/** The floor (0 the lowest) a cell or a block at height y is on: the highest floor's cells at or under it. */
	public int floor(int y) {
		int best = 0;
		for (int i = 0; i < floors.size(); i++) {
			if (floors.get(i) <= y) {
				best = i;
			}
		}
		return best;
	}

	public int floorOfCell(long cell) {
		return floor(BlockPos.getY(cell));
	}

	/** A pallet place: its block, the way into it from the aisle, its level (0 the ground place), the aisle cell, its code. */
	public record Place(BlockPos pos, Direction facing, int level, long access, String label) {
	}

	/** An intake: its origin, facing, the cell a stacker picks from (in front of the conveyor's stop end). */
	public record Intake(BlockPos origin, Direction facing, long access) {
	}

	/**
	 * A palletizing station: its origin (the build spot's block), facing, the cell in front of the build spot (a stacker
	 * works from it) and the one behind that (a pallet drone stands there to drive its forks in).
	 */
	public record Station(BlockPos origin, Direction facing, long access, long behind) {
	}

	/**
	 * A Ready Storage: its origin (the front row's middle block, at the bottom), facing (the way its user looks: toward its
	 * back, against the outer wall), the cell its hatch's roller bed stands in (in front of it), the cell a pallet drone
	 * works the bed from (in front of that, facing the hatch; it stands a little back into the next cell, behind).
	 */
	public record Vlm(BlockPos origin, Direction facing, long bed, long access, long behind) {
	}

	/** A row of places (an aisle's side) and its name ("2B": floor 2's aisle B; "B" in a one-floor warehouse). */
	public record Aisle(String name, int floor, List<BlockPos> places) {
	}

	public final List<Aisle> aisles = new ArrayList<>();

	private static final int[][] NEAR;

	static {
		List<int[]> near = new ArrayList<>();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					int n = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
					if (n == 1 || n == 2) {
						near.add(new int[] {dx, dy, dz});
					}
				}
			}
		}
		NEAR = near.toArray(int[][]::new);
	}

	/** Finds the warehouse joined to a terminal (its piece's blocks at start). */
	public static Network scan(Level level, BlockPos terminal, List<BlockPos> start, int reach, int max) {
		Network n = new Network();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (BlockPos p : start) {
			if (n.members.add(p.asLong())) {
				queue.add(p);
			}
		}
		List<BlockPos> racks = new ArrayList<>();
		java.util.Set<BlockPos> origins = new java.util.HashSet<>();
		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();
			BlockState state = level.getBlockState(p);
			if (state.getBlock() instanceof LaneBlock || state.getBlock() instanceof CrossingBlock) {
				BlockPos cell = p.above();
				BlockState above = level.getBlockState(cell);
				if (above.isAir() || above.getCollisionShape(level, cell).isEmpty() && !(above.getBlock() instanceof WarehouseBlock)) {
					n.cells.add(cell.asLong());
					if (state.getBlock() instanceof CrossingBlock) {
						n.crossings.add(cell.asLong());
					}
				}
			} else if (state.getBlock() instanceof BeaconBlock) {
				n.beacons.add(p);
			} else if (state.getBlock() instanceof AisleSignBlock) {
				n.signs.add(p);
			} else if (state.getBlock() instanceof DockBlock) {
				n.docks.add(p);
			} else if (state.getBlock() instanceof RackBlock) {
				racks.add(p);
			} else if (state.getBlock() instanceof PieceBlock piece) {
				BlockPos origin = piece.origin(p, state);
				if (origins.add(origin)) {
					Direction facing = state.getValue(PieceBlock.FACING);
					WarehousePiece wp = piece.piece;
					switch (wp) {
						case INTAKE -> {
							BlockPos stop = origin.offset(wp.offset(facing, wp.part(wp.width - 1, 0, wp.depth - 1)));
							n.intakes.add(new Intake(origin, facing, stop.relative(facing.getOpposite()).asLong()));
						}
						case STATION -> {
							BlockPos front = origin.relative(facing.getOpposite());
							n.stations.add(new Station(origin, facing, front.asLong(), front.relative(facing.getOpposite()).asLong()));
						}
						case TERMINAL -> {
							if (!origin.equals(terminal)) {
								n.terminals.add(origin);
							}
						}
						case GOODS_IN -> n.panels.add(origin);
						case READY_STORAGE -> {
							BlockPos bed = origin.relative(facing.getOpposite()), access = bed.relative(facing.getOpposite());
							n.vlms.add(new Vlm(origin, facing, bed.asLong(), access.asLong(), access.relative(facing.getOpposite()).asLong()));
						}
						case READY_STORAGE_EXT -> {
						}
						case GOODS_OUT -> n.outPanels.add(origin);
						case LIFT_LANDING -> {
							BlockPos bed = origin.relative(facing.getOpposite()), access = bed.relative(facing.getOpposite());
							n.landings.add(new Landing(origin, facing, bed.asLong(), access.asLong(), access.relative(facing.getOpposite()).asLong()));
						}
						case LIFT_SHAFT, LIFT_DRIVE, LIQUIDS_IN, LIQUIDS_OUT, SWITCHBOARD, STATUS_BOARD, SIGNAL_BOX -> {
						}
						case DRUM_FILLER, DRUM_EMPTIER -> {
							BlockPos front = origin.relative(facing.getOpposite());
							(wp == WarehousePiece.DRUM_FILLER ? n.fillers : n.emptiers)
									.add(new Station(origin, facing, front.asLong(), front.relative(facing.getOpposite()).asLong()));
						}
					}
				}
			}
			for (int[] d : NEAR) {
				BlockPos q = p.offset(d[0], d[1], d[2]);
				// (Up and down as far as the world goes: a warehouse may be built up floor on floor to the build limit.)
				if (Math.abs(q.getX() - terminal.getX()) > reach || Math.abs(q.getZ() - terminal.getZ()) > reach
						|| level.isOutsideBuildHeight(q) || n.members.contains(q.asLong()) || !level.isLoaded(q)) {
					continue;
				}
				if (level.getBlockState(q).getBlock() instanceof WarehouseBlock) {
					if (n.members.size() >= max) {
						n.truncated = true;
						continue;
					}
					n.members.add(q.asLong());
					queue.add(q);
				}
			}
		}
		java.util.TreeSet<Integer> ys = new java.util.TreeSet<>();
		for (long c : n.cells) {
			ys.add(BlockPos.getY(c));
		}
		n.floors.addAll(ys);
		n.places(level, racks);
		for (Vlm v : n.vlms) {
			n.cells.remove(v.bed());
		}
		for (Landing l : n.landings) {
			n.cells.remove(l.bed());
		}
		return n;
	}

	/**
	 * The pallet places, with their codes: each row of racks (the places in line facing the same way) an aisle letter
	 * in order, the places along it numbered from its left, the level counted from its ground place.
	 */
	private void places(Level level, List<BlockPos> racks) {
		// Rows: the same facing, the same line across it.
		Map<String, List<BlockPos>> rows = new TreeMap<>();
		for (BlockPos p : racks) {
			Direction f = level.getBlockState(p).getValue(RackBlock.FACING);
			int line = f.getAxis().choose(p.getX(), p.getY(), p.getZ());
			rows.computeIfAbsent(String.format(java.util.Locale.ROOT, "%03d%s%+08d%s", floor(p.getY()), f.getAxis(), line, f.getSerializedName()),
					k -> new ArrayList<>()).add(p);
		}
		int letter = 0, lastFloor = -1;
		boolean several = floors.size() > 1;
		for (List<BlockPos> row : rows.values()) {
			int fl = floor(row.get(0).getY());
			if (fl != lastFloor) {
				letter = 0;
				lastFloor = fl;
			}
			BlockPos first = row.get(0);
			Direction f = level.getBlockState(first).getValue(RackBlock.FACING);
			Direction right = f.getClockWise();
			Comparator<BlockPos> along = Comparator.comparingInt(p -> right.getAxis().choose(p.getX(), p.getY(), p.getZ()) * right.getAxisDirection().getStep());
			List<Integer> columns = row.stream().map(p -> right.getAxis().choose(p.getX(), p.getY(), p.getZ()) * right.getAxisDirection().getStep())
					.distinct().sorted().toList();
			String aisle = (several ? String.valueOf(fl + 1) : "") + (letter < 26 ? String.valueOf((char) ('A' + letter)) : "A" + (char) ('A' + letter - 26));
			letter++;
			aisles.add(new Aisle(aisle, fl, row));
			row.sort(along.thenComparingInt(BlockPos::getY));
			for (BlockPos p : row) {
				// The column's ground place: the lowest place under this one that is still the same rack.
				BlockPos ground = p;
				while (level.getBlockState(ground.below()).getBlock() instanceof RackBlock
						&& level.getBlockState(ground.below()).getValue(RackBlock.FACING) == f) {
					ground = ground.below();
				}
				int lv = p.getY() - ground.getY();
				int col = columns.indexOf(right.getAxis().choose(p.getX(), p.getY(), p.getZ()) * right.getAxisDirection().getStep()) + 1;
				String label = String.format(java.util.Locale.ROOT, "%s-%02d-%d", aisle, col, lv);
				BlockPos access = ground.relative(f.getOpposite());
				Place place = new Place(p, f, lv, access.asLong(), label);
				places.add(place);
				placeAt.put(p, place);
			}
		}
	}
}
