package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tv.FlatTvBlock;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Blocks taken out of the mod that stay registered so worlds with one still load: no recipe, not in the creative menu
 * (JugcraftTower's tab skips them), and any in the world turns into what replaces it, when its chunk loads and when one
 * is put down (from an old stack, a command, a blueprint: RoomKitBlock.onPlace queues it). One call retires a block;
 * the work is done here for all of them, at the end of the level's tick. The first: the Lounge TV, which becomes a
 * working Flat Screen TV (Justin, 10 November 2026: "I don't want to use that shitty TV at all, anywhere. It should be
 * actual TVs"; tools/room_kit_recipes.RETIRED).
 */
public final class RetiredBlocks {
	/** Turns the retired block at {@code pos} (and whatever else is part of it) into its replacement. */
	public interface Converter {
		void convert(ServerLevel level, BlockPos pos, BlockState state);
	}

	private static final Map<Block, Converter> RETIRED = new IdentityHashMap<>();
	private static final Map<ServerLevel, Set<BlockPos>> QUEUE = new WeakHashMap<>();

	private RetiredBlocks() {
	}

	/** Retires {@code block}: the ones in the world become what {@code converter} makes of them. */
	public static void retire(Block block, Converter converter) {
		RETIRED.put(block, converter);
	}

	public static boolean retired(Block block) {
		return RETIRED.containsKey(block);
	}

	static void register() {
		RoomKitBlock tv = RoomKit.BLOCKS.get("lounge_tv");
		if (tv != null) {
			retire(tv, RetiredBlocks::toFlatScreen);
		}
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> scan(level, chunk));
		ServerTickEvents.END_LEVEL_TICK.register(RetiredBlocks::convertQueued);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			synchronized (QUEUE) {
				QUEUE.clear();
			}
		});
	}

	/** Puts the retired block at {@code pos} on the list for the end of this tick (nothing for a client's level). */
	public static void queue(Level level, BlockPos pos) {
		if (level instanceof ServerLevel server && retired(level.getBlockState(pos).getBlock())) {
			queued(server).add(pos.immutable());
		}
	}

	private static Set<BlockPos> queued(ServerLevel level) {
		synchronized (QUEUE) {
			return QUEUE.computeIfAbsent(level, l -> ConcurrentHashMap.newKeySet());
		}
	}

	/** Queues every retired block in {@code chunk} (its sections' palettes say which could have one); returns how many. */
	public static int scan(ServerLevel level, ChunkAccess chunk) {
		if (RETIRED.isEmpty()) {
			return 0;
		}
		int found = 0;
		LevelChunkSection[] sections = chunk.getSections();
		for (int i = 0; i < sections.length; i++) {
			LevelChunkSection section = sections[i];
			if (section == null || section.hasOnlyAir() || !section.maybeHas(s -> RETIRED.containsKey(s.getBlock()))) {
				continue;
			}
			int y0 = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
			int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						if (RETIRED.containsKey(section.getBlockState(x, y, z).getBlock())) {
							queued(level).add(new BlockPos(x0 + x, y0 + y, z0 + z));
							found++;
						}
					}
				}
			}
		}
		return found;
	}

	/** Converts what's queued for {@code level}; returns how many blocks were still retired ones. */
	public static int convertQueued(ServerLevel level) {
		Set<BlockPos> queue;
		synchronized (QUEUE) {
			queue = QUEUE.get(level);
		}
		if (queue == null || queue.isEmpty()) {
			return 0;
		}
		List<BlockPos> now = new ArrayList<>(queue);
		queue.removeAll(now);
		int done = 0;
		for (BlockPos pos : now) {
			if (!level.isLoaded(pos)) {
				continue;                                         // (back when its chunk loads again)
			}
			BlockState state = level.getBlockState(pos);
			Converter converter = RETIRED.get(state.getBlock());
			if (converter != null) {
				converter.convert(level, pos, state);
				done++;
			}
		}
		return done;
	}

	// ------------------------------------------------------------------ the Lounge TV

	/**
	 * A Lounge TV (three blocks wide, one high, its screen facing the way the piece faces) becomes a Flat Screen TV
	 * where it was, its back the other way: two blocks high where the row above it is clear, else one. It hangs on
	 * whatever is behind it, or stands on its neck and foot (FlatTvBlock.rejoin decides, and joins it into one screen).
	 */
	static void toFlatScreen(ServerLevel level, BlockPos pos, BlockState state) {
		Block flat = BuiltInRegistries.BLOCK.getValue(Jugcraft.id("flat_tv"));
		if (!(state.getBlock() instanceof RoomKitBlock kit) || !(flat instanceof FlatTvBlock)) {
			return;
		}
		Direction faces = kit.facingOf(state);
		BlockPos anchor = kit.anchor(pos, state);
		List<BlockPos> cells = new ArrayList<>();
		for (BlockPos cell : kit.cells(anchor, state)) {
			BlockState s = level.getBlockState(cell);
			if (s.is(kit) && kit.facingOf(s) == faces && kit.anchor(cell, s).equals(anchor)) {
				cells.add(cell);
			}
		}
		if (!cells.contains(pos)) {
			cells.add(pos);
		}
		boolean taller = true;
		for (BlockPos cell : cells) {
			BlockPos up = cell.above();
			taller &= !level.isOutsideBuildHeight(up) && level.getBlockState(up).isAir();
		}
		List<BlockPos> screen = new ArrayList<>(cells);
		if (taller) {
			for (BlockPos cell : cells) {
				screen.add(cell.above());
			}
		}
		BlockState tv = flat.defaultBlockState().setValue(FlatTvBlock.FACING, faces.getOpposite());
		for (BlockPos p : screen) {
			// (the first cell's going clears the rest of the piece, without drops: RoomKitBlock.clear)
			level.setBlock(p, tv, Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
		}
		FlatTvBlock.rejoin(level, screen.get(0), faces.getOpposite());
	}
}
