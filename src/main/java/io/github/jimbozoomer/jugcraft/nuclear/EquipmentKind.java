package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The working equipment of a reactor unit (docs/features/nuclear.md), each one placed whole from its item and drawn by
 * the client. Sizes are blocks across (to the placer's right), deep (away from the placer) and tall, from the
 * approved equipment sheet. Same ids and order as EQUIPMENT in tools/nuclear.py (checked by tools/check_mod_data.py).
 */
public enum EquipmentKind {
	//                         id                        wide deep tall
	REACTOR_VESSEL("reactor_pressure_vessel", 7, 7, 20),
	STEAM_GENERATOR("reactor_steam_generator", 7, 7, 21),
	COOLANT_PUMP("reactor_coolant_pump", 5, 3, 10),
	PRESSURIZER("pressurizer", 5, 3, 18),
	TURBINE_GENERATOR("turbine_generator", 11, 50, 10),
	CONDENSER("condenser", 12, 6, 6),
	FEEDWATER_PUMP("feedwater_pump", 3, 2, 3),
	MAIN_TRANSFORMER("main_transformer", 5, 5, 8),
	CONTROL_CONSOLE("reactor_control_console", 3, 2, 2);

	public final String id;
	public final int wide, deep, tall;
	private List<BlockPos> solid;

	EquipmentKind(String id, int wide, int deep, int tall) {
		this.id = id;
		this.wide = wide;
		this.deep = deep;
		this.tall = tall;
	}

	/** The pipe kinds that join to this equipment. */
	public boolean takes(PipeKind pipe) {
		return switch (pipe) {
			case COOLANT -> this == REACTOR_VESSEL || this == STEAM_GENERATOR || this == COOLANT_PUMP || this == PRESSURIZER;
			case STEAM -> this == STEAM_GENERATOR || this == TURBINE_GENERATOR || this == FEEDWATER_PUMP || this == CONDENSER;
			case WATER -> this == CONDENSER;
		};
	}

	/**
	 * Whether a block cell (local coordinates: x to the right, y up, z away from the placer) is part of the
	 * equipment's body: the cells that get a frame block. Round vessels leave their corners free.
	 */
	public boolean solid(int x, int y, int z) {
		double cx = x + 0.5 - wide / 2.0, cz = z + 0.5 - deep / 2.0;
		double r2 = cx * cx + cz * cz;
		return switch (this) {
			// The vessel on its support skirt; the drive housings on top are narrower.
			case REACTOR_VESSEL -> y < 14 ? r2 <= 3.3 * 3.3 : r2 <= 2.4 * 2.4;
			// The narrow tube bundle below, the wide steam drum above (a cone between).
			case STEAM_GENERATOR -> y < 10 ? r2 <= 2.5 * 2.5 : r2 <= 3.3 * 3.3;
			// The pump casing at the front, the tall motor stand behind it.
			case COOLANT_PUMP -> y < 4 || (y < 9 && z >= 1);
			case PRESSURIZER -> (cx * cx) <= 1.6 * 1.6;
			// The turbine deck all the way along, the casings above along the middle.
			case TURBINE_GENERATOR -> y < 3 || (x >= 2 && x <= 8 && y < 8);
			case MAIN_TRANSFORMER -> y < 5 && x < 5 && z < 5;
			default -> true;
		};
	}

	/** Every solid cell, local coordinates (the anchor's own cell included if solid). */
	public List<BlockPos> solidCells() {
		if (solid == null) {
			List<BlockPos> cells = new ArrayList<>();
			for (int y = 0; y < tall; y++) {
				for (int z = 0; z < deep; z++) {
					for (int x = 0; x < wide; x++) {
						if (solid(x, y, z)) {
							cells.add(new BlockPos(x, y, z));
						}
					}
				}
			}
			solid = List.copyOf(cells);
		}
		return solid;
	}

	/** The cells that get frame blocks: every solid cell but the anchor's own. */
	public List<BlockPos> frames(Direction facing) {
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos local : solidCells()) {
			if (!local.equals(BlockPos.ZERO)) {
				out.add(toWorld(local, facing));
			}
		}
		return out;
	}

	/** A local cell as an offset from the anchor in the world, for equipment placed facing {@code facing}. */
	public static BlockPos toWorld(BlockPos local, Direction facing) {
		Direction right = facing.getClockWise();
		return new BlockPos(right.getStepX() * local.getX() + facing.getStepX() * local.getZ(), local.getY(),
				right.getStepZ() * local.getX() + facing.getStepZ() * local.getZ());
	}

	/** An offset from the anchor back to local coordinates (the inverse of {@link #toWorld}). */
	public static BlockPos toLocal(BlockPos offset, Direction facing) {
		Direction right = facing.getClockWise();
		int x = right.getStepX() * offset.getX() + right.getStepZ() * offset.getZ();
		int z = facing.getStepX() * offset.getX() + facing.getStepZ() * offset.getZ();
		return new BlockPos(x, offset.getY(), z);
	}

	public static EquipmentKind byId(String id) {
		for (EquipmentKind kind : values()) {
			if (kind.id.equals(id)) {
				return kind;
			}
		}
		return null;
	}
}
