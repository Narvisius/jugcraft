package io.github.jimbozoomer.jugcraft.drone;

import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The build-job interface between things that need blocks placed (the Blueprint System, #23, and
 * later other builders) and things that place them (drone depots). Neither side reaches into the
 * other's internals.
 *
 * <p>A job source offers open positions. A depot reserves some, flies the materials there, and on
 * arrival asks the source to fill each position. The source decides what counts as filled and does
 * the block change, so hand-built blocks and drone-delivered blocks are treated the same way.
 */
public final class BuildJobs {
	private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();

	private BuildJobs() {
	}

	/**
	 * One position a source wants filled.
	 *
	 * @param jobId      the job it belongs to (for example one placed blueprint)
	 * @param owner      who placed the job
	 * @param mode       PERSONAL: only the owner's systems may build it; PARTY: party systems may too
	 * @param pos        where the block goes
	 * @param state      the exact block state wanted, or the default for "any material" slots
	 * @param anyOf      for "any material" slots, the blocks allowed; null means exactly {@code state}
	 * @param supplied   the source has already paid for the materials (the Drone Tower's modules): the depot
	 *                   takes nothing from its packager and nothing goes back to it
	 */
	public record Target(Source source, UUID jobId, UUID owner, UseMode mode, BlockPos pos, BlockState state,
			@Nullable TagKey<Block> anyOf, boolean supplied) {
		public Target(Source source, UUID jobId, UUID owner, UseMode mode, BlockPos pos, BlockState state, @Nullable TagKey<Block> anyOf) {
			this(source, jobId, owner, mode, pos, state, anyOf, false);
		}
	}

	/** Something that needs blocks placed. Implemented by the Blueprint System; tests use a simple one. */
	public interface Source {
		/**
		 * May work drones can't reach be done on the spot by the job's owner? True only for the Drone Tower
		 * (already paid for, and a stalled tier can't be finished any other way).
		 */
		default boolean crewMayFinish() {
			return false;
		}

		/**
		 * Up to {@code max} open positions within {@code radius} blocks of {@code center}, bottom layers
		 * first, skipping positions reserved by other depots, and never a position directly above one
		 * that is still waiting (drones deliver from above, so a block placed first would cover it). Only jobs {@code depotOwner}'s depot may
		 * serve are returned; sources should use {@link io.github.jimbozoomer.jugcraft.party.JugcraftParties#mayServe}.
		 */
		List<Target> openTargets(ServerLevel level, BlockPos center, int radius, UUID depotOwner, UseMode depotMode, int max);

		/** Reserves a position for one depot until {@code untilTick}; false if someone else holds it. */
		boolean reserve(ServerLevel level, Target target, UUID depot, long untilTick);

		/** Releases a reservation (delivery done, failed or cancelled). */
		void release(ServerLevel level, Target target, UUID depot);

		/** Checked again on arrival: is this position still wanted, and may this depot still serve it? */
		boolean stillWanted(ServerLevel level, Target target, UUID depotOwner, UseMode depotMode);

		/** Fills the position with {@code state}. Returns false if it could not (the item then returns to storage). */
		boolean fill(ServerLevel level, Target target, BlockState state);
	}

	public static void register(Source source) {
		SOURCES.add(source);
	}

	public static void unregister(Source source) {
		SOURCES.remove(source);
	}

	public static List<Source> sources() {
		return SOURCES;
	}
}
