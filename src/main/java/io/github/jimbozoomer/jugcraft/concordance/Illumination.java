package io.github.jimbozoomer.jugcraft.concordance;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The Concordance's one way to change world light: every invocation, item and (later) ritual that lights the world
 * places its light through here, so each obeys the same permission rules, the same "open air only" rule and the same
 * self-ending light block. Effects reach it through the shared effect boundary ({@link ConcordanceEffects}, the
 * illumination operation); a carried lantern's trail light calls {@link #trail} directly. Light placed on behalf of a
 * player goes only where that player could change a block themselves ({@link Authority}): inside the world and its
 * border, not in spawn protection or a protected town, not where they may not build, and not where a protection mod
 * refuses them. Light with no player behind it (a shrine or spire whose owner is away) goes only where a server that
 * lets absent owners' devices act allows it.
 */
public final class Illumination {
	private Illumination() {
	}

	public enum Result {
		PLACED, REFRESHED, NO_SPACE, NOT_ALLOWED;

		public boolean lit() {
			return this == PLACED || this == REFRESHED;
		}
	}

	/**
	 * Whether {@code player} may change the block at {@code pos}: what breaking it by hand would face ({@link Authority},
	 * roadmap step 28). Nobody (null) may change nothing: a device asks as whoever answers for its owner.
	 */
	public static boolean mayChange(ServerLevel level, @Nullable Player player, BlockPos pos) {
		return Authority.mayChange(level, player, pos);
	}

	/** Whether Kindled light can go at {@code pos}: open air, or a Kindled mote already there. */
	public static boolean open(BlockState state) {
		return state.isAir() || state.is(JugcraftConcordance.LUMEN_MOTE);
	}

	/**
	 * Sets a Kindled mote at {@code pos} lasting {@code steps} steps, or lengthens one already there. Only open air is
	 * lit: nothing is ever replaced.
	 */
	public static Result kindle(ServerLevel level, @Nullable Player source, BlockPos pos, int steps) {
		BlockState state = level.getBlockState(pos);
		if (!open(state)) {
			return Result.NO_SPACE;
		}
		if (!mayChange(level, source, pos)) {
			return Result.NOT_ALLOWED;
		}
		int age = Math.clamp(steps, 0, LumenMoteBlock.MAX_STEPS);
		if (state.is(JugcraftConcordance.LUMEN_MOTE) && !state.getValue(LumenMoteBlock.TRAIL)) {
			if (state.getValue(LumenMoteBlock.AGE) < age) {
				level.setBlock(pos, state.setValue(LumenMoteBlock.AGE, age), Block.UPDATE_CLIENTS);
			}
			return Result.REFRESHED;
		}
		// A lantern's trail light here becomes a Kindled mote (and the light rises from 12 to 14).
		level.setBlock(pos, JugcraftConcordance.LUMEN_MOTE.defaultBlockState().setValue(LumenMoteBlock.AGE, age), Block.UPDATE_ALL);
		return Result.PLACED;
	}

	/**
	 * Keeps a lantern's light beside its carrier: placed in the open air at their head, or at their feet when the head
	 * is not open. An existing light there is left alone (it checks for its lantern by itself).
	 */
	public static Result trail(ServerLevel level, Player holder, BlockPos feet) {
		for (BlockPos pos : new BlockPos[] {feet.above(), feet}) {
			BlockState state = level.getBlockState(pos);
			if (state.is(JugcraftConcordance.LUMEN_MOTE)) {
				return Result.REFRESHED;
			}
			if (state.isAir()) {
				if (!mayChange(level, holder, pos)) {
					return Result.NOT_ALLOWED;
				}
				level.setBlock(pos, JugcraftConcordance.LUMEN_MOTE.defaultBlockState().setValue(LumenMoteBlock.TRAIL, true),
						Block.UPDATE_ALL);
				return Result.PLACED;
			}
		}
		return Result.NO_SPACE;
	}
}
