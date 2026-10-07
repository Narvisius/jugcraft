package io.github.jimbozoomer.jugcraft.world;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;

/**
 * One Retro Game Shop in every new village. A village is built by the game's jigsaw placer, which fills each house
 * slot from a shuffled list of the houses pool. While a village has no shop yet, the shop goes to the front of that
 * list, so the first house slot it fits becomes the shop; once it is placed it leaves the list, so there is never a
 * second.
 *
 * <p>Village houses are built inside their street's own plot (the game's "expansion hack" for villages), and many
 * plots are too small for the 9 by 8 shop: in a desert village only 12 of the 38 house slots can hold it, and none of
 * them is at a meeting point. So now and then a village is laid out with no slot that has room. When the placer
 * finishes with the shop offered but not placed, the village is laid out again from its start piece
 * ({@link #relayout}), up to {@link #LAYOUTS} layouts in all. The new layout draws on the same seeded random source, so
 * a world seed still makes the same village.
 *
 * <p>State is per thread: a whole structure is laid out on one thread, inside JigsawPlacement's private
 * {@code addPieces} ({@code mixin/JigsawPlacementMixin} calls {@link #beginStructure}, {@link #relayout} after each
 * layout and {@link #endStructure}). The placer creates a piece for everything it places
 * ({@code mixin/PoolElementStructurePieceMixin} calls {@link #placed}). Outside a village, or with the
 * {@code retro_trader} switch off, nothing changes.
 */
public final class RetroShopPlacement {
	/** How many layouts a village may try for its shop; the last one stays, with or without a shop. */
	public static final int LAYOUTS = 32;

	/** For the structure being built on this thread. Null outside jigsaw placement. */
	private static final ThreadLocal<State> STATE = new ThreadLocal<>();
	/** New layouts made since the game started (for tests). */
	private static final AtomicInteger RELAYOUTS = new AtomicInteger();
	/** For tests: how many first layouts of each structure started on this thread are built without the shop. */
	private static final ThreadLocal<Integer> WITHHELD = ThreadLocal.withInitial(() -> 0);

	private RetroShopPlacement() {
	}

	private static final class State {
		/** The shop is placed. */
		boolean placed;
		/** A houses pool holding the shop was asked for candidates (never in zombie villages or other structures). */
		boolean offered;
		/** Layouts tried so far, this one included. */
		int layouts = 1;
		/** Layouts built without the shop (tests only; see {@link #withholdShopForTests}). */
		int withheld;
	}

	/** A jigsaw structure (a village, or anything else) starts being laid out on this thread. */
	public static void beginStructure() {
		State state = new State();
		state.withheld = WITHHELD.get();
		STATE.set(state);
	}

	/** The structure being laid out on this thread is finished. */
	public static void endStructure() {
		STATE.remove();
	}

	/**
	 * For tests: structures started on this thread build their first {@code layouts} layouts without the shop, so a
	 * village has to be laid out again to get one. 0 (the default) turns this off.
	 */
	public static void withholdShopForTests(int layouts) {
		WITHHELD.set(layouts);
	}

	/** The placer created a piece for this element. */
	public static void placed(StructurePoolElement element) {
		State state = STATE.get();
		if (state != null && element != null && element == RetroTrader.shop()) {
			state.placed = true;
		}
	}

	/**
	 * The candidates for one jigsaw slot, reordered: the shop first while the structure has none, and not at all once
	 * it has one. Returns null (keep the game's list) when the list does not hold the shop or no structure is being built.
	 */
	public static List<StructurePoolElement> reorder(List<StructurePoolElement> shuffled) {
		StructurePoolElement shop = RetroTrader.shop();
		State state = STATE.get();
		if (shop == null || state == null || shuffled.stream().noneMatch(element -> element == shop)) {
			return null;
		}
		state.offered = true;
		List<StructurePoolElement> out = new ArrayList<>(shuffled.size());
		if (!state.placed && state.layouts > state.withheld) {
			out.add(shop);
		}
		for (StructurePoolElement element : shuffled) {
			if (element != shop) {
				out.add(element);
			}
		}
		return out;
	}

	/**
	 * A layout is finished. If this structure was offered the shop but has none, and has layouts left, clear it for a
	 * new one: every piece but the start piece is dropped and the start piece forgets its junctions. Returns whether
	 * the caller should lay the structure out again.
	 */
	public static boolean relayout(PoolElementStructurePiece start, List<PoolElementStructurePiece> pieces) {
		State state = STATE.get();
		if (state == null || !state.offered || state.placed || state.layouts >= LAYOUTS || pieces.isEmpty() || pieces.get(0) != start) {
			return false;
		}
		state.layouts++;
		RELAYOUTS.incrementAndGet();
		pieces.removeIf(piece -> piece != start);
		start.getJunctions().clear();
		return true;
	}

	/** New village layouts made for a shop since the game started. */
	public static int relayouts() {
		return RELAYOUTS.get();
	}
}
