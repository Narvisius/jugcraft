package io.github.jimbozoomer.jugcraft.raiders;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * A member of the raider faction (batch 57): infantry, walkers and blimps. Each may belong to a raid
 * ({@link RaiderRaids}), which gives it an objective to march on; one that does not (summoned, or spawned from an egg)
 * just fights what it finds. Raiders' bombs and grenades spare other raiders.
 */
public interface Raider {
	/** The raid this raider belongs to, or null. */
	@Nullable
	UUID raid();

	/** Where its raid is headed (the base or town under attack), or null. */
	@Nullable
	BlockPos objective();

	/** Joins a raid headed for {@code objective}. */
	void joinRaid(UUID raid, BlockPos objective);
}
