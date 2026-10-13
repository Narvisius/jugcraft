package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.concordance.ritual.StructurePattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The loaded Circle Anchors, so a change to a block can void the cached report of the circles it might belong to
 * (roadmap step 12: bounded checks, invalidated by relevant changes rather than repeated every tick). Anchors join the
 * index when their block entity loads and leave it when it unloads or is removed. A change at a position voids every
 * anchor within {@link StructurePattern#MAX_REACH} blocks of it, which covers any structure a data pack can define.
 * <p>
 * The changes it hears about: a player breaking any block, a Ley Pylon or Warding Stone placed or removed, and a
 * pylon's charge changing. Anything else (an explosion, a piston, fluid flowing into a clearance) is caught by the
 * anchor's own checks: every ritual step checks the whole circle again, and an idle report is kept at most
 * {@link CircleAnchorBlockEntity#CACHE_TICKS} ticks. Server thread only.
 * <p>
 * The index is kept by chunk (roadmap step 30), so a change looks only at the anchors in the few chunks within reach of
 * it, however many circles a world holds, and an anchor loaded again is indexed once, never twice.
 */
public final class Rituals {
	private static final Map<ResourceKey<Level>, Map<Long, Set<BlockPos>>> ANCHORS = new HashMap<>();
	private static final List<Completed> COMPLETED = new CopyOnWriteArrayList<>();

	/**
	 * Something told when a ritual completes (roadmap step 25: a Concord Spire hears its Kindling): where its anchor
	 * stands, which ritual, and the participants present. Listeners only read: the ritual has already made its result.
	 */
	@FunctionalInterface
	public interface Completed {
		void completed(ServerLevel level, BlockPos anchor, String ritual, List<ServerPlayer> present);
	}

	private Rituals() {
	}

	static void register() {
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, level) -> {
			if (blockEntity instanceof CircleAnchorBlockEntity) {
				BlockPos pos = blockEntity.getBlockPos().immutable();
				ANCHORS.computeIfAbsent(level.dimension(), unused -> new HashMap<>())
						.computeIfAbsent(ChunkPos.containing(pos).pack(), unused -> new HashSet<>()).add(pos);
			}
		});
		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, level) -> {
			if (blockEntity instanceof CircleAnchorBlockEntity) {
				Map<Long, Set<BlockPos>> chunks = ANCHORS.get(level.dimension());
				BlockPos pos = blockEntity.getBlockPos();
				long chunk = ChunkPos.containing(pos).pack();
				Set<BlockPos> anchors = chunks == null ? null : chunks.get(chunk);
				if (anchors != null && anchors.remove(pos) && anchors.isEmpty()) {
					chunks.remove(chunk);
				}
			}
		});
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel server) {
				changed(server, pos, player.getUUID());
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ANCHORS.clear());
	}

	/** A block that could be part of a circle changed at {@code pos}: anchors in reach check again when next asked. */
	public static void changed(ServerLevel level, BlockPos pos) {
		changed(level, pos, null);
	}

	/**
	 * As {@link #changed(ServerLevel, BlockPos)}, by {@code player} if a player broke it: an anchor in reach remembers
	 * them, so a containment that fails because of it is their doing (roadmap step 28).
	 */
	public static void changed(ServerLevel level, BlockPos pos, @Nullable UUID player) {
		for (BlockPos anchor : near(level, pos)) {
			if (level.getBlockEntity(anchor) instanceof CircleAnchorBlockEntity found) {
				found.invalidate();
				found.disturbedBy(player, level.getGameTime());
			}
		}
	}

	/** The loaded anchors within {@link StructurePattern#MAX_REACH} of {@code pos}, from the chunks in reach only. */
	public static List<BlockPos> near(ServerLevel level, BlockPos pos) {
		Map<Long, Set<BlockPos>> chunks = ANCHORS.get(level.dimension());
		if (chunks == null || chunks.isEmpty()) {
			return List.of();
		}
		int reach = StructurePattern.MAX_REACH;
		List<BlockPos> found = new ArrayList<>();
		for (int cx = (pos.getX() - reach) >> 4; cx <= (pos.getX() + reach) >> 4; cx++) {
			for (int cz = (pos.getZ() - reach) >> 4; cz <= (pos.getZ() + reach) >> 4; cz++) {
				Set<BlockPos> anchors = chunks.get(new ChunkPos(cx, cz).pack());
				if (anchors == null) {
					continue;
				}
				for (BlockPos anchor : anchors) {
					if (Math.abs(anchor.getX() - pos.getX()) <= reach && Math.abs(anchor.getY() - pos.getY()) <= reach
							&& Math.abs(anchor.getZ() - pos.getZ()) <= reach) {
						found.add(anchor);
					}
				}
			}
		}
		return found;
	}

	/** Adds a listener told whenever a ritual completes (at registration). */
	public static void listen(Completed listener) {
		COMPLETED.add(listener);
	}

	/** A ritual completed at {@code anchor} with {@code present} taking part (the anchor calls this; tests too). */
	public static void completed(ServerLevel level, BlockPos anchor, String ritual, List<ServerPlayer> present) {
		for (Completed listener : COMPLETED) {
			listener.completed(level, anchor, ritual, List.copyOf(present));
		}
	}

	/** How many anchors are loaded in a level (tests). */
	public static int loaded(ServerLevel level) {
		Map<Long, Set<BlockPos>> chunks = ANCHORS.get(level.dimension());
		if (chunks == null) {
			return 0;
		}
		int count = 0;
		for (Set<BlockPos> anchors : chunks.values()) {
			count += anchors.size();
		}
		return count;
	}
}
