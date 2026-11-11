package io.github.jimbozoomer.jugcraft.building;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Jugcraft's roofs keep the rain out (Justin, 27 October 2026: "despite being inside, it rains"). Since 26.3 the game
 * no longer asks a block whether it is solid to know where rain stops (and where it is raining at all, where snow
 * settles, where the surface is): the motion-blocking heightmaps take the blocks in the tag
 * {@code #minecraft:blocks_motion}, and only vanilla's own blocks are in it. So every Jugcraft roof, floor and wall let
 * the rain through. Here a Jugcraft block that is solid by the rule the game used before (and still keeps as
 * {@link BlockState#isSolid()}) counts in those heightmaps as well (mixin/HeightmapCoverMixin), and a chunk saved with
 * the old heightmaps has them worked out again when it loads.
 */
public final class RainCover {
	/** The namespaces of the blocks this mod registers. */
	private static final Set<String> OURS = Set.of("jugcraft", "peepo", "peepo_companion");
	/** The heightmaps that go by the motion-blocking tag (what is shown to a client, and the ocean floor). */
	public static final Set<Heightmap.Types> TYPES = EnumSet.of(Heightmap.Types.MOTION_BLOCKING,
			Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR);
	@SuppressWarnings("unchecked")
	private static final Predicate<BlockState>[] WRAPPED = new Predicate[Heightmap.Types.values().length];

	private RainCover() {
	}

	/** Whether {@code state} is one of this mod's blocks that stops the rain: solid as the game used to judge it. */
	public static boolean covers(BlockState state) {
		return state.isSolid() && ours(state.getBlock());
	}

	/** {@link #covers}, less leaves (for the heightmap that looks through them). */
	public static boolean coversNoLeaves(BlockState state) {
		return covers(state) && !(state.getBlock() instanceof LeavesBlock) && !state.is(BlockTags.LEAVES);
	}

	private static boolean ours(Block block) {
		return OURS.contains(block.builtInRegistryHolder().key().identifier().getNamespace());
	}

	/** The test a heightmap of {@code type} uses: the game's own ({@code original}), and this mod's solid blocks. */
	public static Predicate<BlockState> wrap(Heightmap.Types type, Predicate<BlockState> original) {
		Predicate<BlockState> extra = switch (type) {
			case MOTION_BLOCKING, OCEAN_FLOOR, OCEAN_FLOOR_WG -> RainCover::covers;
			case MOTION_BLOCKING_NO_LEAVES -> RainCover::coversNoLeaves;
			default -> null;
		};
		if (extra == null) {
			return original;
		}
		Predicate<BlockState> out = WRAPPED[type.ordinal()];
		if (out == null) {
			// (each type's own test never changes, so one wrapper each; a race only makes a second, equal one)
			out = original.or(extra);
			WRAPPED[type.ordinal()] = out;
		}
		return out;
	}

	/** A chunk saved before this fix keeps heightmaps that ignore this mod's blocks: they are worked out again when it loads. */
	public static void register() {
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
			if (!generated && hasOurs(chunk)) {
				Heightmap.primeHeightmaps(chunk, TYPES);
			}
		});
	}

	private static boolean hasOurs(LevelChunk chunk) {
		for (LevelChunkSection section : chunk.getSections()) {
			if (!section.hasOnlyAir() && section.maybeHas(RainCover::covers)) {
				return true;
			}
		}
		return false;
	}
}
