package io.github.jimbozoomer.jugcraft.tower;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Closed cargo housing and its fitted socket, sliced and rotated once. */
final class CargoExchangeShapes {
	private CargoExchangeShapes() { }
	// Matches tools/tower_cargo_exchange.py COLLISION and exported panel metadata.
	private static final double[][] SOLIDS = {
		{0, 0, 0, 80, 4, 64},
		{2, 4, .4, 78, 46.4, 62.6},
		{31.8, 17.2, 0, 48.2, 33, 2.2},
	};
	private static final Map<Direction, VoxelShape[]> SHAPES = build();
	private static Map<Direction, VoxelShape[]> build() {
		var shapes = new EnumMap<Direction, VoxelShape[]>(Direction.class);
		var cells = ExchangeStationBlock.CARGO_FOOTPRINT.offsets();
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			VoxelShape[] parts = new VoxelShape[cells.size()];
			for (int part = 0; part < cells.size(); part++) {
				var cell = cells.get(part);
				double x = cell.getX() * 16, y = cell.getY() * 16, z = cell.getZ() * 16;
				VoxelShape shape = Shapes.empty();
				for (double[] solid : SOLIDS) {
					double x0 = Math.max(x, solid[0]), y0 = Math.max(y, solid[1]), z0 = Math.max(z, solid[2]);
					double x1 = Math.min(x + 16, solid[3]), y1 = Math.min(y + 16, solid[4]), z1 = Math.min(z + 16, solid[5]);
					if (x0 < x1 && y0 < y1 && z0 < z1) shape = Shapes.or(shape,
							FurnitureBlock.turned(new double[] {x0 - x, y0 - y, z0 - z, x1 - x, y1 - y, z1 - z}, facing));
				}
				parts[part] = shape.optimize();
			}
			shapes.put(facing, parts);
		}
		return shapes;
	}
	static VoxelShape shape(int part, Direction facing) { return SHAPES.get(facing)[part]; }
}
