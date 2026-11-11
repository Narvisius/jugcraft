package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Tri-Pillar's night lighting (Justin's night lighting sheet, approved 5 November 2026): at 20:00, once the last of
 * the off-shift crew have gone off, the core switches the ceiling lights on its two crew floors to their night modes (the
 * layout's {@code night_lights}, tools/tower_night_lights.py: night in the halls, low where the crew sleep, off in rooms
 * nobody is in; the lamps round the night watch's posts stay on); at 05:00, as they get up, back on. A few dozen a tick:
 * two block changes a day and nothing running between ({@link TowerCoreBlockEntity#serverTick}).
 */
public final class TowerNightLights {
	/** The night's lighting, ticks of the day (06:00 = 0): from 20:00 to 05:00. */
	public static final int DIM_AT = 14000, UP_AT = 23000;
	/** Lamps switched a tick. */
	public static final int PER_TICK = 48;
	/** How often (ticks) the core looks at the clock. */
	public static final int CHECK_TICKS = 20;

	private TowerNightLights() {
	}

	/** Whether the tower's lights are at their night modes at {@code dayTime} (the world's clock). */
	public static boolean night(long dayTime) {
		int t = (int) Math.floorMod(dayTime, 24000L);
		return t >= DIM_AT && t < UP_AT;
	}

	/** Puts the lamp at {@code pos} (if one of the tower's lamps is there) in {@code mode}; true if it changed. */
	public static boolean set(ServerLevel level, BlockPos pos, TowerLightBlock.Mode mode) {
		if (!level.isLoaded(pos)) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof TowerLightBlock) || state.getValue(TowerLightBlock.MODE) == mode) {
			return false;
		}
		level.setBlock(pos, state.setValue(TowerLightBlock.MODE, mode), Block.UPDATE_CLIENTS);
		return true;
	}
}
