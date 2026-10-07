package io.github.jimbozoomer.jugcraft.town;

import io.github.jimbozoomer.jugcraft.styx.StyxTownDistrict;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Builds the town into the world a chunk at a time, as its chunks load: nothing is loaded to build it, and a chunk is
 * built once (TownState remembers which). Building a chunk levels the ground inside the wall to the town's height
 * (filling down to the land under it, clearing the land and trees above), blends the ground outside the wall back to the
 * land's own height over the town's blend ring (paths lead out of the gates), places the town's blocks, sets its decor
 * sites to the current theme and brings out the townsfolk who stand in it.
 *
 * <p>At most {@link #CHUNKS_PER_TICK} chunk is built per server tick. Blocks are set without neighbour updates, as a
 * structure template places them (their shapes are worked out in tools/town.py), and without drops.
 */
public final class TownBuilder {
	public static final int CHUNKS_PER_TICK = 1;
	/** How deep under the town's ground a valley or hollow is filled at most. */
	public static final int FILL_DEPTH = 40;
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
	private static final Deque<ChunkPos> QUEUE = new ArrayDeque<>();
	private static final Set<Long> QUEUED = new HashSet<>();

	private TownBuilder() {
	}

	public static void register() {
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
			if (level == level.getServer().overworld()) {
				queue(level, chunk.getPos());
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(TownBuilder::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			QUEUE.clear();
			QUEUED.clear();
		});
	}

	/** Queues a chunk if it overlaps the town and is not built yet. */
	public static void queue(ServerLevel level, ChunkPos pos) {
		BlockPos origin = TownState.get(level).origin();
		if (origin == null || !(overlaps(origin, pos)
				|| TownState.get(level).styxDistrict() && StyxTownDistrict.overlaps(origin, pos))) {
			return;
		}
		long key = pos.pack();
		if (!TownState.get(level).built(key) && QUEUED.add(key)) {
			QUEUE.add(pos);
		}
	}

	/** Queues every loaded chunk of the town (when the town is first placed). */
	public static void queueLoaded(ServerLevel level) {
		BlockPos origin = TownState.get(level).origin();
		if (origin == null) {
			return;
		}
		int size = TownData.get().size;
		int width = TownState.get(level).styxDistrict() ? StyxTownDistrict.MAX_X + 1 : size;
		for (int cx = origin.getX() >> 4; cx <= (origin.getX() + width - 1) >> 4; cx++) {
			for (int cz = origin.getZ() >> 4; cz <= (origin.getZ() + size - 1) >> 4; cz++) {
				if (level.getChunkSource().getChunkNow(cx, cz) != null) {
					queue(level, new ChunkPos(cx, cz));
				}
			}
		}
	}

	public static boolean overlaps(BlockPos origin, ChunkPos pos) {
		int size = TownData.get().size;
		int x0 = pos.x() << 4;
		int z0 = pos.z() << 4;
		return x0 + 15 >= origin.getX() && x0 < origin.getX() + size && z0 + 15 >= origin.getZ() && z0 < origin.getZ() + size;
	}

	/** Chunks waiting to be built (for tests and the town command). */
	public static int waiting() {
		return QUEUE.size();
	}

	private static void tick(MinecraftServer server) {
		if (QUEUE.isEmpty()) {
			return;
		}
		ServerLevel level = server.overworld();
		for (int n = 0; n < CHUNKS_PER_TICK && !QUEUE.isEmpty(); n++) {
			ChunkPos pos = QUEUE.poll();
			long key = pos.pack();
			QUEUED.remove(key);
			LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
			TownState state = TownState.get(level);
			if (chunk == null || state.built(key) || state.origin() == null) {
				continue;
			}
			build(level, chunk, state.origin());
			state.markBuilt(key);
			TownDecor.chunkBuilt(level, pos);
			TownsfolkCare.chunkBuilt(level, pos);
		}
	}

	/** Builds the town's part of one loaded chunk. */
	public static void build(ServerLevel level, LevelChunk chunk, BlockPos origin) {
		TownData data = TownData.get();
		ChunkPos pos = chunk.getPos();
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		int floor = origin.getY();
		for (int wx = pos.x() << 4; wx <= (pos.x() << 4) + 15; wx++) {
			for (int wz = pos.z() << 4; wz <= (pos.z() << 4) + 15; wz++) {
				int x = wx - origin.getX();
				int z = wz - origin.getZ();
				if (TownState.get(level).styxDistrict() && StyxTownDistrict.contains(x, z)) {
					StyxTownDistrict.buildColumn(level, chunk, origin, x, z);
					continue;
				}
				int mask = data.mask(x, z);
				if (mask == 0) {
					continue;
				}
				int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, wx & 15, wz & 15);
				int ground = ground(level, at, wx, top, wz);
				if (mask == TownData.INSIDE) {
					levelColumn(level, at, wx, wz, floor, ground, top, data, x, z);
				} else {
					blendColumn(level, at, wx, wz, floor, ground, top, mask, data, x, z);
				}
			}
		}
	}

	/** The column's ground: the highest block that is not air, water, a plant, a log or leaves. */
	static int ground(ServerLevel level, BlockPos.MutableBlockPos at, int x, int top, int z) {
		for (int y = top; y > level.getMinY(); y--) {
			BlockState state = level.getBlockState(at.set(x, y, z));
			if (!state.getCollisionShape(level, at).isEmpty() && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
				return y;
			}
		}
		return level.getMinY();
	}

	private static void levelColumn(ServerLevel level, BlockPos.MutableBlockPos at, int wx, int wz, int floor, int ground, int top,
			TownData data, int x, int z) {
		int dataTop = floor + data.yMin + data.height - 1;
		// Clear anything above the town's blocks (a hill or a tree taller than the town).
		for (int y = top; y > dataTop; y--) {
			set(level, at.set(wx, y, wz), Blocks.AIR.defaultBlockState());
		}
		// Fill under the ground down to the land (a valley or a cave mouth), at most FILL_DEPTH deep.
		for (int y = floor - 1; y >= Math.max(level.getMinY(), floor - FILL_DEPTH); y--) {
			BlockState state = level.getBlockState(at.set(wx, y, wz));
			if (!state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced()) {
				break;
			}
			set(level, at, y >= floor - 3 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState());
		}
		// The town's own blocks, ground included.
		for (int y = data.yMin; y < data.yMin + data.height; y++) {
			int index = data.index(x, y, z);
			if (index != TownData.KEEP) {
				set(level, at.set(wx, floor + y, wz), data.state(index));
			}
		}
	}

	private static void blendColumn(ServerLevel level, BlockPos.MutableBlockPos at, int wx, int wz, int floor, int ground, int top,
			int mask, TownData data, int x, int z) {
		double t = Math.min(1.0, mask / (double) data.blend);
		double s = t * t * (3 - 2 * t);
		int target = (int) Math.round(floor + (ground - floor) * s);
		BlockState surface = data.surface(x, z);
		if (target > ground) {
			for (int y = ground + 1; y < target; y++) {
				set(level, at.set(wx, y, wz), y >= target - 3 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState());
			}
			set(level, at.set(wx, target, wz), surface != null ? surface : Blocks.GRASS_BLOCK.defaultBlockState());
			// Plants and trees standing on the old ground are buried or cleared.
			for (int y = Math.max(target + 1, ground + 1); y <= top; y++) {
				set(level, at.set(wx, y, wz), Blocks.AIR.defaultBlockState());
			}
		} else if (target < ground) {
			for (int y = top; y > target; y--) {
				set(level, at.set(wx, y, wz), Blocks.AIR.defaultBlockState());
			}
			set(level, at.set(wx, target, wz), surface != null ? surface : Blocks.GRASS_BLOCK.defaultBlockState());
		} else if (surface != null) {
			for (int y = top; y > target; y--) {
				set(level, at.set(wx, y, wz), Blocks.AIR.defaultBlockState());
			}
			set(level, at.set(wx, target, wz), surface);
		}
		// Town blocks outside the wall (a gatehouse's steps, the wall's plinth) stand on the town's own ground.
		for (int y = 0; y < data.yMin + data.height; y++) {
			int index = data.index(x, y, z);
			if (index != TownData.KEEP && index != TownData.AIR) {
				set(level, at.set(wx, floor + y, wz), data.state(index));
			}
		}
	}

	/** Places town scenery without activating carved pumpkins' golem patterns. Those patterns
	 * read neighboring chunks, which may not be loaded while the town is being built.
	 * Decorative pumpkins must also remain scenery when placed above copper or iron.
	 */
	public static void set(ServerLevel level, BlockPos pos, BlockState state) {
		if (level.getBlockState(pos) != state) {
			level.setBlock(pos, state, FLAGS | (state.getBlock() instanceof CarvedPumpkinBlock ? Block.UPDATE_SKIP_ON_PLACE : 0));
		}
	}
}
