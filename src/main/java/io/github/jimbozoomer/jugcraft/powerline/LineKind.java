package io.github.jimbozoomer.jugcraft.powerline;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Every power line structure (docs/features/power-lines.md): the supports in their families and tiers, and the
 * grid-connection boxes where a line's wire turns into the mod's cables. Sizes are real (1 block = 1 m) and
 * capacities follow the mod's scale (1 JE/t is about 2.3 kW). IDs match tools/power_lines.py.
 */
public enum LineKind {
	// Supports: family, tier, height, reach, capacity.
	WOODEN_POLE("wooden_pole", Family.WOOD, 1, 12, 32, 4_096),
	DOUBLE_ARM_POLE("double_arm_pole", Family.WOOD, 2, 13, 32, 8_192),
	CONCRETE_POLE("spun_concrete_pole", Family.WOOD, 3, 14, 32, 16_384),
	STEEL_MONOPOLE("steel_monopole", Family.MONOPOLE, 1, 30, 64, 131_072),
	DOUBLE_CIRCUIT_MONOPOLE("double_circuit_monopole", Family.MONOPOLE, 2, 32, 64, 262_144),
	COMPACT_MONOPOLE("compact_monopole", Family.MONOPOLE, 3, 36, 64, 524_288),
	LATTICE_TOWER("lattice_tower", Family.LATTICE, 1, 56, 128, 2_097_152),
	RECONDUCTORED_LATTICE_TOWER("reconductored_lattice_tower", Family.LATTICE, 2, 56, 128, 3_145_728),
	MONITORED_LATTICE_TOWER("monitored_lattice_tower", Family.LATTICE, 3, 56, 128, 3_670_016),
	UHV_TOWER("uhv_tower", Family.UHV, 1, 65, 160, 4_194_304),
	UHVDC_TOWER("uhvdc_tower", Family.UHVDC, 1, 70, 160, 5_242_880),
	SUPERCONDUCTING_PYLON("superconducting_pylon", Family.SUPERCONDUCTING, 1, 20, 64, 16_777_216),
	// Boxes: what they take, capacity, footprint (x and z blocks) and height.
	SERVICE_PEDESTAL("service_pedestal", Family.BOX, 1, 2, 32, 16_384),
	PAD_MOUNT_TRANSFORMER("pad_mount_transformer", Family.BOX, 2, 2, 64, 524_288),
	UNIT_SUBSTATION("unit_substation", Family.BOX, 3, 11, 160, 16_777_216),
	CONVERTER_STATION("converter_station", Family.BOX, 4, 14, 160, 5_242_880);

	public enum Family {
		WOOD, MONOPOLE, LATTICE, UHV, UHVDC, SUPERCONDUCTING, BOX
	}

	public final String id;
	public final Family family;
	public final int tier;
	/** Height above the anchor block, in blocks. */
	public final int height;
	/** How far a wire can reach from it, in blocks. */
	public final int reach;
	/** JE per tick a line through it can carry (the slowest part of a line sets the line's rate). */
	public final long capacity;

	LineKind(String id, Family family, int tier, int height, int reach, long capacity) {
		this.id = id;
		this.family = family;
		this.tier = tier;
		this.height = height;
		this.reach = reach;
		this.capacity = capacity;
	}

	public boolean isBox() {
		return family == Family.BOX;
	}

	public static LineKind byId(String id) {
		for (LineKind kind : values()) {
			if (kind.id.equals(id)) {
				return kind;
			}
		}
		return null;
	}

	/** Half the width of the footprint in blocks (0: one block). */
	public int halfWidth() {
		return switch (this) {
			case LATTICE_TOWER, RECONDUCTORED_LATTICE_TOWER, MONITORED_LATTICE_TOWER -> 5;
			case UHV_TOWER, UHVDC_TOWER -> 6;
			case UNIT_SUBSTATION -> 5;
			case CONVERTER_STATION -> 7;
			default -> 0;
		};
	}

	/**
	 * Whether a wire may run between these two: supports join supports of their own family (any tier); a box takes
	 * the families it is built for.
	 */
	public static boolean compatible(LineKind a, LineKind b) {
		if (a.isBox() && b.isBox()) {
			return false;
		}
		if (a.isBox()) {
			return a.accepts(b.family);
		}
		if (b.isBox()) {
			return b.accepts(a.family);
		}
		return a.family == b.family;
	}

	/** For a box: the support families it takes. */
	public boolean accepts(Family support) {
		return switch (this) {
			case SERVICE_PEDESTAL -> support == Family.WOOD;
			case PAD_MOUNT_TRANSFORMER -> support == Family.WOOD || support == Family.MONOPOLE;
			case UNIT_SUBSTATION -> support != Family.UHVDC && support != Family.BOX;
			case CONVERTER_STATION -> support == Family.UHVDC;
			default -> false;
		};
	}

	/** The next tier in the same family (for upgrades in place), or null. */
	public LineKind upgradeOf() {
		for (LineKind kind : values()) {
			if (kind.family == family && kind.family != Family.BOX && kind.tier == tier - 1) {
				return kind;
			}
		}
		return null;
	}

	/**
	 * Where the wires from a structure of {@code partner}'s kind land. Usually {@link #attachPoints()}; a Unit
	 * Substation takes a superconducting line's cryogenic pipes on two termination vessels in its yard instead of
	 * on the gantry.
	 */
	public List<Vec3> attachPoints(LineKind partner) {
		if (this == UNIT_SUBSTATION && partner == SUPERCONDUCTING_PYLON) {
			return List.of(new Vec3(1.0, 3.4, -4), new Vec3(1.0, 3.4, 4));
		}
		return attachPoints();
	}

	/**
	 * Where the conductors hang, relative to the centre of the anchor block's bottom (blocks; x across the line, so a
	 * wire runs roughly along z). Which point joins which at the other end is {@link LineWires}' job.
	 */
	public List<Vec3> attachPoints() {
		List<Vec3> p = new ArrayList<>();
		switch (this) {
			case WOODEN_POLE -> {
				for (double x : new double[] {-1, 0, 1}) {
					p.add(new Vec3(x, 12.5, 0));
				}
			}
			case DOUBLE_ARM_POLE -> {
				p.add(new Vec3(-1.5, 13.0, 0)); p.add(new Vec3(0, 13.5, 0)); p.add(new Vec3(1.5, 13.0, 0));
			}
			case CONCRETE_POLE -> {
				p.add(new Vec3(-2, 13.5, 0)); p.add(new Vec3(2, 12.0, 0)); p.add(new Vec3(-2, 10.5, 0));
			}
			case STEEL_MONOPOLE -> {
				p.add(new Vec3(-3, 25, 0)); p.add(new Vec3(3, 21, 0)); p.add(new Vec3(-3, 17, 0));
			}
			case DOUBLE_CIRCUIT_MONOPOLE -> {
				for (double y : new double[] {27, 23, 19}) {
					p.add(new Vec3(-3, y, 0)); p.add(new Vec3(3, y, 0));
				}
			}
			case COMPACT_MONOPOLE -> {
				for (double y : new double[] {33, 28.5, 24}) {
					p.add(new Vec3(-3.5, y, 0)); p.add(new Vec3(3.5, y, 0));
				}
			}
			case LATTICE_TOWER, RECONDUCTORED_LATTICE_TOWER, MONITORED_LATTICE_TOWER -> {
				p.add(new Vec3(-6.5, 45.5, 0)); p.add(new Vec3(6.5, 45.5, 0)); p.add(new Vec3(-8, 38.5, 0));
				p.add(new Vec3(8, 38.5, 0)); p.add(new Vec3(-6.5, 31, 0)); p.add(new Vec3(6.5, 31, 0));
			}
			case UHV_TOWER -> {
				p.add(new Vec3(-11, 51.5, 0)); p.add(new Vec3(0, 51.5, 0)); p.add(new Vec3(11, 51.5, 0));
			}
			case UHVDC_TOWER -> {
				p.add(new Vec3(-10, 45, 0)); p.add(new Vec3(10, 45, 0));
			}
			case SUPERCONDUCTING_PYLON -> {
				p.add(new Vec3(-2.5, 21, 0)); p.add(new Vec3(2.5, 21, 0));
			}
			case SERVICE_PEDESTAL -> p.add(new Vec3(0, 2.05, 0));
			case PAD_MOUNT_TRANSFORMER -> p.add(new Vec3(0.5, 0.2, 0.5));
			case UNIT_SUBSTATION -> {
				for (double z : new double[] {-2, 0, 2}) {
					p.add(new Vec3(1.5, 8, z));                     // the gantry just inside the front fence
				}
			}
			case CONVERTER_STATION -> {
				p.add(new Vec3(1.5, 12, -4)); p.add(new Vec3(1.5, 12, 4));
			}
		}
		return p;
	}

	/**
	 * The invisible collision blocks the structure fills besides its anchor, relative to the anchor: the pole or the
	 * legs, so it can be walked around, hit and broken, and so nothing else can be built inside it.
	 */
	public List<BlockPos> frames() {
		List<BlockPos> out = new ArrayList<>();
		switch (family) {
			case WOOD, MONOPOLE, SUPERCONDUCTING -> {
				for (int y = 1; y < height; y++) {
					out.add(new BlockPos(0, y, 0));
				}
			}
			case LATTICE, UHV, UHVDC -> {
				int base = halfWidth();
				double waist = 1.0;
				int body = (int) (height * 0.66);
				for (int y = 0; y < height; y++) {
					double half = base - (base - waist) * Math.min(1.0, y / (double) body);
					int h = (int) Math.round(half);
					for (int sx : new int[] {-1, 1}) {
						for (int sz : new int[] {-1, 1}) {
							BlockPos leg = new BlockPos(sx * h, y, sz * h);
							if (!leg.equals(BlockPos.ZERO) && !out.contains(leg)) {
								out.add(leg);
							}
						}
					}
				}
			}
			case BOX -> {
				switch (this) {
					case SERVICE_PEDESTAL -> out.add(new BlockPos(0, 1, 0));
					case PAD_MOUNT_TRANSFORMER -> {
						for (int x = 0; x < 2; x++) {
							for (int z = 0; z < 2; z++) {
								for (int y = 0; y < 2; y++) {
									if (x + y + z > 0) {
										out.add(new BlockPos(x, y, z));
									}
								}
							}
						}
					}
					case UNIT_SUBSTATION, CONVERTER_STATION -> {
						// The anchor is the terminal cabinet in the middle of the front fence (x = 0), where cables plug in;
						// the yard runs back from it to x = 2r: a one-block fence ring and the equipment in the middle.
						int r = halfWidth();
						for (int x = 0; x <= 2 * r; x++) {
							for (int z = -r; z <= r; z++) {
								if ((x == 0 || x == 2 * r || Math.abs(z) == r) && (x != 0 || z != 0)) {
									out.add(new BlockPos(x, 0, z));
								}
							}
						}
						int core = this == CONVERTER_STATION ? 4 : 2;
						int tall = this == CONVERTER_STATION ? 12 : 3;
						for (int x = r - core; x <= r + core; x++) {
							for (int z = -core; z <= core; z++) {
								for (int y = 0; y < tall; y++) {
									out.add(new BlockPos(x, y, z));
								}
							}
						}
					}
					default -> {
					}
				}
			}
		}
		return out;
	}
}
