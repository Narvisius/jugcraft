package io.github.jimbozoomer.jugcraft.machine;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

/**
 * The blocks a machine occupies, as offsets from its master block, written for a machine whose
 * front faces north (x = east, y = up, z = south). Part 0 is always the master at {@code (0, 0, 0)}.
 * Offsets turn with the machine the same way its block model does, so a part that sits to the
 * viewer's right stays on the right whichever way the machine faces.
 */
public record Footprint(List<Vec3i> offsets) {
	public static final Footprint SINGLE = new Footprint(List.of(Vec3i.ZERO));

	public static Footprint of(Vec3i... offsets) {
		return new Footprint(List.of(offsets));
	}

	/** A straight column: the master plus {@code height - 1} blocks above it. */
	public static Footprint tall(int height) {
		Vec3i[] offsets = new Vec3i[height];
		for (int i = 0; i < height; i++) {
			offsets[i] = new Vec3i(0, i, 0);
		}
		return of(offsets);
	}

	/**
	 * A box {@code width} blocks wide (extending to the machine's right, seen from the front, so -x),
	 * {@code height} tall and {@code depth} deep (extending back, +z). Parts are numbered layer by layer
	 * from the bottom, row by row from the front, right to left; the master is the front left bottom
	 * block. Keep in sync with {@code cuboid()} in tools/large_machines.py.
	 */
	public static Footprint cuboid(int width, int height, int depth) {
		Vec3i[] offsets = new Vec3i[width * height * depth];
		int index = 0;
		for (int y = 0; y < height; y++) {
			for (int z = 0; z < depth; z++) {
				for (int x = 0; x < width; x++) {
					offsets[index++] = new Vec3i(-x, y, z);
				}
			}
		}
		return of(offsets);
	}

	public int size() {
		return offsets.size();
	}

	public BlockPos partPos(BlockPos master, Direction facing, int part) {
		return master.offset(rotate(offsets.get(part), facing));
	}

	public BlockPos masterPos(BlockPos partPos, Direction facing, int part) {
		return partPos.subtract(rotate(offsets.get(part), facing));
	}

	/** Turns a north-facing side clockwise (seen from above) to match {@code facing}; up and down stay. */
	static Direction rotate(Direction side, Direction facing) {
		if (side.getAxis() == Direction.Axis.Y) {
			return side;
		}
		Direction turned = side;
		for (Direction step = Direction.NORTH; step != facing; step = step.getClockWise()) {
			turned = turned.getClockWise();
		}
		return turned;
	}

	/** Turns a north-facing offset clockwise (seen from above) to match {@code facing}. */
	static Vec3i rotate(Vec3i offset, Direction facing) {
		int turns = switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		int x = offset.getX();
		int z = offset.getZ();
		for (int i = 0; i < turns; i++) {
			int oldX = x;
			x = -z;
			z = oldX;
		}
		return new Vec3i(x, offset.getY(), z);
	}
}
