package io.github.jimbozoomer.jugcraft.agriculture;

/** Leaves whose appearance follows the shared calendar without replacing their other saved state. */
public interface SeasonalFoliage {
	SeasonalLeavesBlock.Schedule schedule();
}
