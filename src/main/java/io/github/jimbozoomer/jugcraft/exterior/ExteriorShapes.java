package io.github.jimbozoomer.jugcraft.exterior;

import io.github.jimbozoomer.jugcraft.warehouse.WarehouseLayout;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The exterior kit's hitboxes (tools/exterior_kit.py draws the pieces): boxes in the piece's own pixels, turned as its
 * blockstate turns its model. An outer piece is drawn with its outer face to +z (facing south: no turn); a stair
 * climbs toward -z (facing north: no turn).
 */
final class ExteriorShapes {
	private ExteriorShapes() {
	}

	/** Boxes (px, x0 y0 z0 x1 y1 z1 each) of a piece drawn with its outer face to +z, turned to face {@code facing}. */
	static VoxelShape outer(Direction facing, double[]... boxes) {
		return turned(facing.getOpposite(), boxes);
	}

	/** Boxes of a piece drawn facing north (a stair climbing north), turned to {@code facing}. */
	static VoxelShape turned(Direction facing, double[]... boxes) {
		VoxelShape shape = Shapes.empty();
		for (double[] b : boxes) {
			shape = Shapes.or(shape, WarehouseLayout.turned(b, facing));
		}
		return shape.optimize();
	}

	/** One shape for each horizontal facing. */
	static Map<Direction, VoxelShape> byFacing(Function<Direction, VoxelShape> make) {
		Map<Direction, VoxelShape> out = new EnumMap<>(Direction.class);
		for (Direction d : Direction.Plane.HORIZONTAL) {
			out.put(d, make.apply(d));
		}
		return out;
	}

	static void shapeMessage(Player player, String name) {
		player.sendOverlayMessage(Component.translatable("message.jugcraft.exterior.shape", name.replace('_', ' ')));
	}
}
