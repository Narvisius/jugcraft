package io.github.jimbozoomer.jugcraft.tower;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Cached cell-local collision for the 9-wide, 6-deep, 5-tall station models. */
final class LargeExchangeShapes {
	private LargeExchangeShapes() { }
	// Matches tools/tower_large_exchanges.py and its exported panel metadata.
	private static final double[][] CARGO = {
		{0, 0, 0, 144, 4, 96}, {2, 4, .4, 142, 78.4, 94.6}, {63.8, 17.2, 0, 80.2, 33, 3.1},
	};
	private static final double[][] POWER = {
		{0, 0, 0, 144, 4, 96}, {4, 4, .15, 140, 78, 22}, {33, 6, 24, 111, 73, 91},
		{6, 8, 27, 31, 70, 92}, {113, 8, 27, 138, 70, 92}, {65.5, 17.5, 0, 78.5, 30.5, 2.4},
	};
	private static final Map<Direction, VoxelShape[]> CARGO_SHAPES = build(CARGO);
	private static final Map<Direction, VoxelShape[]> POWER_SHAPES = build(POWER);
	private static Map<Direction, VoxelShape[]> build(double[][] solids) {
		var result = new EnumMap<Direction, VoxelShape[]>(Direction.class);
		var cells = ExchangeStationBlock.LARGE_FOOTPRINT.offsets();
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			VoxelShape[] parts = new VoxelShape[cells.size()];
			for (int part = 0; part < cells.size(); part++) {
				var cell = cells.get(part);
				double x = cell.getX() * 16, y = cell.getY() * 16, z = cell.getZ() * 16;
				VoxelShape shape = Shapes.empty();
				for (double[] solid : solids) {
					double x0 = Math.max(x, solid[0]), y0 = Math.max(y, solid[1]), z0 = Math.max(z, solid[2]);
					double x1 = Math.min(x + 16, solid[3]), y1 = Math.min(y + 16, solid[4]), z1 = Math.min(z + 16, solid[5]);
					if (x0 < x1 && y0 < y1 && z0 < z1) shape = Shapes.or(shape,
							FurnitureBlock.turned(new double[] {x0 - x, y0 - y, z0 - z, x1 - x, y1 - y, z1 - z}, facing));
				}
				parts[part] = shape.optimize();
			}
			result.put(facing, parts);
		}
		return result;
	}
	static VoxelShape shape(boolean energy, int part, Direction facing) {
		return (energy ? POWER_SHAPES : CARGO_SHAPES).get(facing)[part];
	}
}
