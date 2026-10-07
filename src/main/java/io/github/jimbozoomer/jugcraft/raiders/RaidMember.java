package io.github.jimbozoomer.jugcraft.raiders;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** What every raider keeps about its raid: which raid, and where it is headed. Saved with the raider. */
final class RaidMember {
	/** Ticks between a raider's checks that its raid still stands. */
	private static final int CHECK_TICKS = 100;

	@Nullable
	UUID raid;
	@Nullable
	BlockPos objective;

	void join(UUID raid, BlockPos objective) {
		this.raid = raid;
		this.objective = objective.immutable();
	}

	/**
	 * Called each server tick: a raider whose raid has ended (beaten, or withdrawn while it was out of loaded chunks)
	 * withdraws too. Returns whether it has gone.
	 */
	boolean checkRaid(ServerLevel level, Mob mob) {
		if (raid == null || mob.tickCount % CHECK_TICKS != 0 || RaiderRaids.active(level, raid)) {
			return false;
		}
		withdraw(level, mob);
		return true;
	}

	static void withdraw(ServerLevel level, Mob mob) {
		level.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 12,
				mob.getBbWidth() / 2, mob.getBbHeight() / 3, mob.getBbWidth() / 2, 0.02);
		mob.discard();
	}

	/** A raider in a raid stays loaded with the world rather than despawning. */
	boolean persistent() {
		return raid != null;
	}

	void save(ValueOutput output) {
		if (raid != null) {
			output.store("raid", UUIDUtil.CODEC, raid);
		}
		if (objective != null) {
			output.store("objective", BlockPos.CODEC, objective);
		}
	}

	void load(ValueInput input) {
		raid = input.read("raid", UUIDUtil.CODEC).orElse(null);
		objective = input.read("objective", BlockPos.CODEC).orElse(null);
	}
}
