package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds nothing to save: on each client a shining {@link BlackLightBlock} records, every tick, where it is and when, in
 * a small map per level. Glow Paint looks through that map (only the lights that are on) for one in range; a light
 * that has gone out or been broken stops being recorded and is dropped from the map.
 */
public class BlackLightBlockEntity extends BlockEntity {
	/** How many ticks since it last said so a light still counts as shining. */
	public static final int FRESH_TICKS = 2;
	private static final Map<Level, Map<BlockPos, Long>> SHINING = new WeakHashMap<>();

	public BlackLightBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.BLACK_LIGHT_ENTITY, pos, state);
	}

	static void shine(Level level, BlockPos pos) {
		Map<BlockPos, Long> lights = SHINING.computeIfAbsent(level, l -> new HashMap<>());
		long time = level.getGameTime();
		lights.put(pos.immutable(), time);
		lights.values().removeIf(seen -> time - seen > FRESH_TICKS * 20L);
	}

	/** How brightly Glow Paint at {@code pos} glows under the nearest shining black light (0 if none is in range). */
	public static float glowAt(Level level, BlockPos pos) {
		Map<BlockPos, Long> lights = SHINING.get(level);
		if (lights == null) {
			return 0.0F;
		}
		long time = level.getGameTime();
		float best = 0.0F;
		for (Map.Entry<BlockPos, Long> light : lights.entrySet()) {
			if (time - light.getValue() <= FRESH_TICKS) {
				best = Math.max(best, BlackLightBlock.glow(Math.sqrt(light.getKey().distSqr(pos))));
			}
		}
		return best;
	}
}
