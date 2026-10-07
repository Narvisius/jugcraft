package io.github.jimbozoomer.jugcraft.town;

import io.github.jimbozoomer.jugcraft.styx.StyxTownDistrict;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Where the town is: conversions between world and town coordinates and the questions everything else asks (is this
 * place inside the wall, is it protected). The town lives in the Overworld; it exists once {@link TownState} has an
 * origin.
 */
public final class Town {
	/** How far below the town's ground protection reaches (cellars and foundations). */
	public static final int PROTECT_BELOW = 12;
	/** How far above the town's ground protection reaches (above the spires). */
	public static final int PROTECT_ABOVE = 96;

	private Town() {
	}

	/** The town's origin when `level` is the Overworld and the town is placed, else null. */
	public static @Nullable BlockPos origin(Level level) {
		if (!(level instanceof ServerLevel server) || server != server.getServer().overworld()) {
			return null;
		}
		return TownState.get(server).origin();
	}

	/** Whether a block is in the town's protected area: inside the wall or within the margin outside it. */
	public static boolean isProtected(Level level, BlockPos pos) {
		BlockPos origin = origin(level);
		if (origin == null) {
			return false;
		}
		int dy = pos.getY() - origin.getY();
		if (dy < -PROTECT_BELOW || dy > PROTECT_ABOVE) {
			return false;
		}
		return TownData.get().protectedColumn(pos.getX() - origin.getX(), pos.getZ() - origin.getZ())
				|| districtColumn(level, origin, pos);
	}

	/** Whether a position is inside the town's wall (its streets, houses and wall), at any height near the ground. */
	public static boolean isInside(Level level, BlockPos pos) {
		BlockPos origin = origin(level);
		if (origin == null) {
			return false;
		}
		int dy = pos.getY() - origin.getY();
		return dy >= -PROTECT_BELOW && dy <= PROTECT_ABOVE
				&& (TownData.get().mask(pos.getX() - origin.getX(), pos.getZ() - origin.getZ()) == TownData.INSIDE
				|| districtColumn(level, origin, pos));
	}

	private static boolean districtColumn(Level level, BlockPos origin, BlockPos pos) {
		return level instanceof ServerLevel server && TownState.get(server).styxDistrict()
				&& StyxTownDistrict.contains(pos.getX() - origin.getX(), pos.getZ() - origin.getZ());
	}

	/** A town position in the world. */
	public static BlockPos world(BlockPos origin, int x, int y, int z) {
		return origin.offset(x, y, z);
	}

	public static BlockPos world(BlockPos origin, BlockPos local) {
		return origin.offset(local);
	}

	/** The town's middle at ground level, in the world. */
	public static BlockPos centre(BlockPos origin) {
		int half = TownData.get().size / 2;
		return origin.offset(half, 0, half);
	}
}
