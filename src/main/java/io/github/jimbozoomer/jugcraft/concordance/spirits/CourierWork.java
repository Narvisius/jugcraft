package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Logistics;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Progress;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Request;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A worker's courier decision for its Courier Post (roadmap step 18), extracted from the Clockwork Porter (Concordance
 * Chunk D, D-08) so that the porter and the workshop hand make the same one and neither copies it: take up stranded cargo
 * at the post, take cargo back, deliver, pick up what it reserved, or find a source and reserve there; with no live claim,
 * claim the post's next request it serves. Each decision makes at most one ledger step besides a claim, and the items
 * move in the same Transfer API transaction ({@link CourierLedger}); the worker holds only its claim's id, never the items.
 * It works as its worker's owner (a porter's keeper, a hand's holder), only where they and the request's requester could
 * reach by hand (roadmap step 28), and only while the requester answers. What differs between couriers is their
 * {@link Terms}. It loads nothing: only loaded chunks are read, and an unloaded place is waited for.
 */
final class CourierWork {
	/** What one kind of courier adds to the shared decision. */
	interface Terms {
		/** Why it may take no new claim now (a porter's body; a hand's hold, hours and day's quota), or null. Never asked while it holds a live claim. */
		@Nullable Status ready();

		/**
		 * Why it does not serve {@code request} (a hand: IDLE, not its beneficiaries', more aboard than its carry or to be taken
		 * back to a source beyond its area; OUTSIDE_AGREEMENT, bound beyond its area), or null.
		 */
		@Nullable Status refuses(Request request);

		/** Whether it may take from, or take cargo back to, {@code pos} (a hand: inside its area). */
		boolean reaches(BlockPos pos);

		/** The most it reserves at a source a trip (a porter: no cap of its own). */
		int load();

		/** Standing at its post (a porter fuels at a pylon there). */
		void atPost(ServerLevel level, BlockPos post);

		/** A delivery put something in; {@code finished}: all it carried is delivered and its claim is over. */
		void delivered(ServerLevel level, @Nullable ServerPlayer keeper, boolean finished, long now);

		/** Its post is no longer there (a porter forgets it and is idle; a hand's anchor keeps it, and the hand waits outside its agreement): why it waits. */
		Status postGone();
	}

	private final WorkerEntity<?> worker;
	private final double reach;
	/** The request it holds a claim on (0: none): the one thing a courier saves. */
	private long task;

	CourierWork(WorkerEntity<?> worker, double reach) {
		this.worker = worker;
		this.reach = reach;
	}

	long task() {
		return task;
	}

	/** Read back from a save; the claim is checked against the ledger at its next decision. */
	void restore(long saved) {
		task = saved;
	}

	/** Forgets its claim without a ledger step (it lapses by itself). */
	void forget() {
		task = 0L;
	}

	/** Gives up its claim, if any: the ledger keeps whatever was in transit, stranded at the post, never dropped. */
	void release(ServerLevel level, String why) {
		if (task != 0L) {
			CourierLedger.of(level.getServer()).release(task, worker.getUUID(), why, false, level.getGameTime());
		}
		task = 0L;
	}

	/** One decision as a courier of {@code post}: its post in this level, loaded and still one its owner may use; then its claim's next step, or a new claim of the next request it serves. */
	Status serve(ServerLevel level, GlobalPos post, Terms terms, @Nullable ServerPlayer keeper, long now) {
		if (!level.dimension().equals(post.dimension())) {
			worker.stop();
			return Status.OTHER_DIMENSION;
		}
		BlockPos at = post.pos();
		if (!level.isLoaded(at)) {
			worker.stop();
			return Status.DESTINATION_UNLOADED;
		}
		if (!(level.getBlockEntity(at) instanceof CourierPostBlockEntity postEntity)) {
			// Its post is gone: its claim lapses by itself and the ledger keeps any cargo; the courier says why it waits.
			task = 0L;
			Status gone = terms.postGone();
			worker.stop();
			return gone;
		}
		UUID owner = worker.owner();
		if (owner == null || !postEntity.mayUse(owner)) {
			worker.stop();
			return Status.BLOCKED_BY_ACCESS;
		}
		CourierLedger ledger = CourierLedger.of(level.getServer());
		Vec3 home = WorkerEntity.centre(at);
		if (worker.near(home, reach + 0.5)) {
			terms.atPost(level, at);
		}
		Request request = task == 0L ? null : ledger.ledger().get(task);
		if (request == null || !request.claimedBy(worker.getUUID(), now)) {
			task = 0L;
			Status ready = terms.ready();
			if (ready != null) {
				return homeward(home, now, ready);
			}
			Place place = Couriers.place(level, at);
			Request first = ledger.ledger().next(place, now);
			Status refused = first == null ? null : terms.refuses(first);
			Request next = refused == null ? first : ledger.ledger().next(place, now, candidate -> terms.refuses(candidate) == null);
			// A request is served only while its requester could reach the sources themselves (step 28): it waits for them.
			if (next != null && Authority.answering(level, next.ticket().requester()) == null) {
				return homeward(home, now, Status.OWNER_OFFLINE);
			}
			if (next == null || ledger.claim(next.id(), worker.getUUID(), now) != Logistics.Outcome.DONE) {
				return homeward(home, now, next == null && refused != null ? refused : Status.IDLE);
			}
			task = next.id();
			request = ledger.ledger().get(task);
		}
		ledger.renew(task, worker.getUUID(), now);
		Status status = step(level, ledger, request, postEntity, home, terms, keeper, now);
		// A request done, cancelled or handed back is no longer its task.
		Request after = task == 0L ? null : ledger.ledger().get(task);
		if (after == null || !after.claimedBy(worker.getUUID(), now)) {
			task = 0L;
		}
		return status;
	}

	private Status step(ServerLevel level, CourierLedger ledger, Request request, CourierPostBlockEntity postEntity, Vec3 home, Terms terms,
			@Nullable ServerPlayer keeper, long now) {
		Progress progress = request.progress();
		if (progress.carried() > 0 && !progress.aboard()) {
			if (!worker.near(home, reach + 0.5)) {
				return walkOrGiveUp(ledger, home, now);
			}
			worker.arrived();
			ledger.takeUp(task, worker.getUUID(), now);
			return Status.WORKING;
		}
		if (progress.returning()) {
			return takeBack(level, ledger, request, postEntity, terms, now);
		}
		if (progress.carried() > 0) {
			return deliver(level, ledger, request, terms, keeper, now);
		}
		if (progress.reserved() > 0 && progress.source() != null) {
			return pickUp(level, ledger, request, now);
		}
		return reserve(level, ledger, request, postEntity.getBlockPos(), terms, now);
	}

	private Status homeward(Vec3 home, long now, Status status) {
		if (!worker.near(home, reach + 1.5)) {
			worker.walkTo(home, 1.0F, 1, now);
		} else {
			worker.arrived();
		}
		return status;
	}

	/** Walks on, or, having given up, releases its claim (any cargo stays in the ledger, stranded at the post). */
	private Status walkOrGiveUp(CourierLedger ledger, Vec3 target, long now) {
		if (worker.walkTo(target, 1.0F, 1, now)) {
			return Status.TRAVELLING;
		}
		ledger.release(task, worker.getUUID(), "cannot_navigate", true, now);
		task = 0L;
		return Status.CANNOT_NAVIGATE;
	}

	private Status deliver(ServerLevel level, CourierLedger ledger, Request request, Terms terms, @Nullable ServerPlayer keeper, long now) {
		BlockPos to = Couriers.pos(request.ticket().destination());
		if (!level.isLoaded(to)) {
			worker.stop();
			return Status.DESTINATION_UNLOADED;
		}
		Storage<ItemVariant> destination = level.getBlockEntity(to) instanceof Container ? ItemStorage.SIDED.find(level, to, null) : null;
		if (destination == null) {
			ledger.destinationGone(request.ticket().destination(), now);
			return Status.RETURNING;
		}
		if (!worker.near(WorkerEntity.centre(to), reach + 0.5)) {
			return walkOrGiveUp(ledger, WorkerEntity.centre(to), now);
		}
		worker.arrived();
		Logistics.Outcome outcome = ledger.deliver(task, worker.getUUID(), destination, now);
		if (outcome == Logistics.Outcome.NOTHING_FREE) {
			return Status.FULL;
		}
		if (outcome == Logistics.Outcome.DONE) {
			Request after = ledger.ledger().get(task);
			terms.delivered(level, keeper, after == null || !after.claimedBy(worker.getUUID(), now), now);
		}
		return Status.WORKING;
	}

	private Status pickUp(ServerLevel level, CourierLedger ledger, Request request, long now) {
		BlockPos from = Couriers.pos(request.progress().source());
		if (!level.isLoaded(from)) {
			ledger.release(task, worker.getUUID(), "source_unloaded", true, now);
			task = 0L;
			return Status.DESTINATION_UNLOADED;
		}
		Storage<ItemVariant> source = store(level, from);
		if (source == null || !mayTake(level, request, from)) {
			ledger.release(task, worker.getUUID(), source == null ? "source_gone" : "blocked", true, now);
			task = 0L;
			return source == null ? Status.WAITING_FOR_RESOURCES : Status.BLOCKED_BY_ACCESS;
		}
		if (!worker.near(WorkerEntity.centre(from), reach + 0.5)) {
			return walkOrGiveUp(ledger, WorkerEntity.centre(from), now);
		}
		worker.arrived();
		return ledger.pickUp(task, worker.getUUID(), source, now) == Logistics.Outcome.DONE ? Status.WORKING : Status.WAITING_FOR_RESOURCES;
	}

	/** Finds the nearest plain container round the post that it reaches, holding the item asked for, and reserves there (at most its load). */
	private Status reserve(ServerLevel level, CourierLedger ledger, Request request, BlockPos at, Terms terms, long now) {
		BlockPos destination = Couriers.pos(request.ticket().destination());
		List<BlockPos> found = new ArrayList<>();
		int radius = Couriers.SOURCE_RADIUS;
		for (int cx = (at.getX() - radius) >> 4; cx <= (at.getX() + radius) >> 4; cx++) {
			for (int cz = (at.getZ() - radius) >> 4; cz <= (at.getZ() + radius) >> 4; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockPos pos : chunk.getBlockEntities().keySet()) {
					if (Math.abs(pos.getX() - at.getX()) <= radius && Math.abs(pos.getY() - at.getY()) <= radius
							&& Math.abs(pos.getZ() - at.getZ()) <= radius && !pos.equals(destination) && !pos.equals(at) && terms.reaches(pos)) {
						found.add(pos.immutable());
					}
				}
			}
		}
		found.sort(Comparator.comparingDouble((BlockPos pos) -> pos.distSqr(at)).thenComparingInt(BlockPos::getY)
				.thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
		for (BlockPos pos : found) {
			Storage<ItemVariant> source = store(level, pos);
			if (source != null && mayTake(level, request, pos) && ledger.available(task, source) > 0
					&& ledger.reserve(task, worker.getUUID(), Couriers.place(level, pos), source, terms.load(), now) == Logistics.Outcome.DONE) {
				return Status.TRAVELLING;
			}
		}
		ledger.release(task, worker.getUUID(), "no_source", true, now);
		task = 0L;
		return Status.WAITING_FOR_RESOURCES;
	}

	/** Takes cancelled cargo back: to its source if it reaches it and it has room, otherwise into the post; with room in neither, it leaves it stranded for its requester. */
	private Status takeBack(ServerLevel level, CourierLedger ledger, Request request, CourierPostBlockEntity postEntity, Terms terms, long now) {
		ItemVariant item = ledger.item(task);
		int carried = request.progress().carried();
		Place source = request.progress().source();
		BlockPos into = null;
		Storage<ItemVariant> storage = null;
		if (source != null && item != null) {
			BlockPos from = Couriers.pos(source);
			Storage<ItemVariant> back = level.isLoaded(from) && terms.reaches(from) ? store(level, from) : null;
			if (back != null && Authority.mayChangeFor(level, worker.owner(), from) && CourierLedger.room(back, item, carried) > 0) {
				into = from;
				storage = back;
			}
		}
		if (storage == null && item != null) {
			Storage<ItemVariant> post = ItemStorage.SIDED.find(level, postEntity.getBlockPos(), null);
			if (post != null && CourierLedger.room(post, item, carried) > 0) {
				into = postEntity.getBlockPos();
				storage = post;
			}
		}
		if (storage == null) {
			ledger.release(task, worker.getUUID(), "nowhere_to_return", false, now);
			task = 0L;
			return Status.FULL;
		}
		if (!worker.near(WorkerEntity.centre(into), reach + 0.5)) {
			return walkOrGiveUp(ledger, WorkerEntity.centre(into), now);
		}
		worker.arrived();
		ledger.giveBack(task, worker.getUUID(), storage, now);
		return Status.RETURNING;
	}

	/** Whether a courier may take from {@code pos} for {@code request}: both whoever works it and the request's requester could reach it by hand, now (roadmap step 28). */
	private boolean mayTake(ServerLevel level, Request request, BlockPos pos) {
		UUID keeper = worker.owner();
		return Authority.mayChangeFor(level, keeper, pos) && Authority.mayChangeFor(level, request.ticket().requester(), pos);
	}

	/** A plain container's items through the Transfer API, as Jugcraft's item pipes reach them (null for anything else). */
	static @Nullable Storage<ItemVariant> store(ServerLevel level, BlockPos pos) {
		return container(level, pos) == null ? null : ItemStorage.SIDED.find(level, pos, null);
	}

	/** A plain container at {@code pos} (not a furnace-like one, which takes items by side), or null. */
	static @Nullable Container container(ServerLevel level, BlockPos pos) {
		BlockEntity entity = level.getBlockEntity(pos);
		return entity instanceof Container container && !(entity instanceof WorldlyContainer) ? container : null;
	}
}
