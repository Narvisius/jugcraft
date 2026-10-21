package io.github.jimbozoomer.jugcraft.finance;

import io.github.jimbozoomer.jugcraft.town.Town;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Whether a chunk belongs to the walled town, whose land the land office never sells. */
final class TownLand {
	private TownLand() {
	}

	/** True when any part of the chunk is in the town's protected area (checked on every column at its ground). */
	static boolean touchesTown(Level level, long key) {
		BlockPos origin = Town.origin(level);
		if (origin == null) {
			return false;
		}
		int baseX = LandRegistry.keyX(key) << 4;
		int baseZ = LandRegistry.keyZ(key) << 4;
		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				if (Town.isProtected(level, new BlockPos(baseX + Math.min(15, dx), origin.getY(), baseZ + Math.min(15, dz)))) {
					return true;
				}
			}
		}
		return false;
	}
}
