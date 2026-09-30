package io.github.jimbozoomer.jugcraft.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * The arc furnace multiblock: a solid 3x3x3 cube of arc furnace casing with the
 * controller in the middle of one face, facing outward. Power enters through the
 * controller's front.
 */
public final class ArcFurnaceStructure {
	private ArcFurnaceStructure() {
	}

	public static boolean isFormed(Level level, BlockPos controller, Direction facing, Block casing) {
		BlockPos center = controller.relative(facing.getOpposite());
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos pos = center.offset(dx, dy, dz);
					if (!pos.equals(controller) && !level.getBlockState(pos).is(casing)) {
						return false;
					}
				}
			}
		}
		return true;
	}
}
