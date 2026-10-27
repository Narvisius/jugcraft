package io.github.jimbozoomer.jugcraft.concordance.spirits;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ComposeText;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWillLedger;
import io.github.jimbozoomer.jugcraft.concordance.worker.Agreement;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Prediction;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, unit 1 (D-01, D-02): what a block that keeps an agreement shares, extracted from the Spirit
 * Anchor (critique 27) for the anchor now and D-04's pact perch. It keeps who placed the block, the agreement's terms as
 * sealed here and the presence serving it; the world's Bound Will is the one authority for the rest (D-S9): whether it
 * stands, where it is kept, whether it is held and why, its service terms and day's count, its upkeep and its escrow. A
 * block counts its terms only while the record keeps them for the same holder at this place: a copy elsewhere is inert,
 * says CONFLICTING_AGREEMENT and releases nothing; a record from before Chunk D is claimed only by the block its terms
 * were sealed at. Sealing asks the owner's side and Authority, never a stand-in (D-S8), is refused with one reason before
 * anything is recorded, spent or called (D-S11), spends its cost only once sealed (D-S13), and notes the presence on its
 * holder's roster at once (G5). A lost presence is called again under the same record at no cost (D-S18). Only its
 * living holder, in person and with the Concordance on, ends it, once; its escrow goes back into their inventory (the
 * rest at their feet) and the departure is kept (D-S6); breaking the block with the Concordance on releases it (D-S16).
 * Upkeep is looked at only while the Concordance is on and its holder is present near the place (D-S3, D-S16). No item
 * lives here: the escrow is the record's. Its actions are not rate-gated themselves: the anchor's use asks RateGate, and
 * the pact screens of D-04 and D-07 ask it before they end, suspend, pay in or call again (D-P8). Breaking its block
 * releases the agreement (the anchor) or leaves it without its place (the pact perch, D-04), both only with the
 * Concordance on; a pact without its place, or stranded at a place that no longer keeps it, is seated again where its
 * holder may seal.
 */
public final class AgreementHolder {
	/** The terms as sealed here (moved unchanged from SpiritAnchorBlockEntity, so an anchor's save reads as before). */
	public static final Codec<Agreement> AGREEMENT_CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("record").forGetter(Agreement::record), UUIDUtil.CODEC.fieldOf("holder").forGetter(Agreement::holder),
			Codec.STRING.fieldOf("work").forGetter(Agreement::work), Codec.STRING.fieldOf("dimension").forGetter(Agreement::dimension),
			Codec.INT.fieldOf("x").forGetter(Agreement::x), Codec.INT.fieldOf("y").forGetter(Agreement::y), Codec.INT.fieldOf("z").forGetter(Agreement::z),
			Codec.INT.fieldOf("radius").forGetter(Agreement::radius), Codec.INT.fieldOf("from").forGetter(Agreement::from),
			Codec.INT.fieldOf("to").forGetter(Agreement::to), Codec.INT.fieldOf("quota").forGetter(Agreement::quota),
			Codec.LONG.fieldOf("window").forGetter(Agreement::window), Codec.INT.fieldOf("done_today").forGetter(Agreement::doneToday),
			Codec.BOOL.fieldOf("suspended").forGetter(Agreement::suspended)).apply(i, Agreement::new));

	/** How a block's terms stand against the world's Bound Wills (D-S9). */
	public enum Standing {
		/** It keeps no terms (or its record ended elsewhere, and it has just forgotten them, D-P7). */
		NONE,
		/** The record keeps them, for the same holder, at this place. */
		VALID,
		/** The record is kept elsewhere or for someone else: this is a copy, inert. */
		ELSEWHERE,
		/** The record is saved in a form this version cannot read: kept as written; the agreement waits. */
		UNREADABLE
	}

	public enum Recall {
		RECALLED, NOTHING, NOT_HOLDER, NOT_LOST, CONFLICTING, ROSTER_FULL, UNAVAILABLE, DISABLED
	}

	public enum Toggle {
		SUSPENDED, RESUMED, HELD_FOR_UPKEEP, NOTHING, NOT_HOLDER, CONFLICTING, DISABLED
	}

	public enum Ended {
		ENDED, NOTHING, NOT_HOLDER, NOT_PERMITTED, CONFLICTING, DISABLED
	}

	/** What breaking its block does to the agreement kept there (Concordance Chunk D, D-04, D-S6). */
	public enum OnBreak {
		/** The Spirit Anchor: released once, its escrow dropped, its presence leaving its load (unit 1). */
		RELEASE,
		/** The pact perch: never ended; the record, its escrow and terms stay, without its place; its presence leaves the world. */
		UNSEAT
	}

	/** How seating a pact at a place went (D-04, D-05; D-S6). Worded as message.jugcraft.concordance.workers.seat_<id> (DISABLED as the switch's). */
	public enum Seat {
		SEATED("seated"),
		DISABLED("disabled"),
		NOT_HOLDER("not_holder"),
		NOTHING("nothing"),
		NOT_PERMITTED("not_permitted"),
		TAKEN("taken"),
		ROSTER_FULL("roster_full"),
		UNAVAILABLE("unavailable");

		public final String id;

		Seat(String id) {
			this.id = id;
		}
	}

	/** A block entity that keeps an agreement through a holder. */
	public interface Holds {
		AgreementHolder holder();

		/**
		 * Calls this place's presence for {@code who}: the counterpart {@code counterpart} (namespaced), added to the level and
		 * on no roster yet; null if none came, or if this place does not call that counterpart.
		 */
		@Nullable WorkerEntity<?> call(ServerLevel level, UUID who, String counterpart);
	}

	/**
	 * A presence serving an agreement at a place. Only {@link WorkerEntity} subclasses implement it, so every presence has
	 * a roster entry its holder's roster can strike (critique 8).
	 */
	public interface Presence {
		@Nullable BlockPos seat();

		/** Its agreement is over or answered by another: it leaves what it carried at {@code home} (else at {@code at}), once, and departs. */
		void leave(ServerLevel level, @Nullable Holds home, BlockPos at);
	}

	/** A spirit definition's service terms, as a pact copies them at sealing (D-S23). */
	public static BoundWill.Service service(WorkerDefinition.Spirit terms) {
		return new BoundWill.Service(terms.work(), terms.radius(), terms.from(), terms.to(), terms.quota());
	}

	/**
	 * What a block seals: its agreement and counterpart ids; its service terms; its upkeep (or none); who benefits; whether a
	 * holder keeps one with this counterpart at most; whether the owner's party may seal here as well as the owner (the
	 * anchor: yes; D-04's perch: no, D-S8); and its cost, if any: asked before anything is called, paid in the same call once
	 * sealed (D-S13).
	 */
	public record Sealing(String agreement, String counterpart, BoundWill.Service service, BoundWill.@Nullable Upkeep upkeep, UseMode beneficiaries,
			boolean onePerHolder, boolean partySeals, @Nullable Predicate<ServerPlayer> affords, @Nullable Consumer<ServerPlayer> pay) {
	}

	public record Sealed(Pacts.Outcome outcome, @Nullable WorkerEntity<?> presence) {
	}

	private final BlockEntity block;
	private final Holds holds;
	private final OnBreak onBreak;
	private final boolean ownerRequired;
	private @Nullable UUID owner;
	private @Nullable Agreement agreement;
	private @Nullable UUID spirit;

	public <B extends BlockEntity & Holds> AgreementHolder(B block) {
		this(block, OnBreak.RELEASE, false);
	}

	/**
	 * A holder for {@code block}: {@code onBreak} says what breaking it does; with {@code ownerRequired}, a block that records
	 * no owner seals and seats for nobody (D-04's perch, placed only by a player in person: one a machine set, by a blueprint
	 * stake or a drone, records nobody; the unit review), where the anchor keeps the ownerless legacy rule (D-S8).
	 */
	public <B extends BlockEntity & Holds> AgreementHolder(B block, OnBreak onBreak, boolean ownerRequired) {
		this.block = block;
		this.holds = block;
		this.onBreak = onBreak;
		this.ownerRequired = ownerRequired;
	}

	public @Nullable UUID owner() {
		return owner;
	}

	/** Who placed the block (D-S8): they or their party seal here. Null for a block placed before owners were recorded. */
	public void setOwner(@Nullable UUID next) {
		owner = next;
		block.setChanged();
	}

	public @Nullable Agreement agreement() {
		return agreement;
	}

	public @Nullable UUID spirit() {
		return spirit;
	}

	public Place place(ServerLevel level) {
		return Couriers.place(level, block.getBlockPos());
	}

	/** The Bound Will its terms name (whether or not they count here), or null. */
	public @Nullable BoundWill record(ServerLevel level) {
		return agreement == null ? null : BoundWills.of(level.getServer()).ledger().get(agreement.record());
	}

	/** The block forgets its terms (its record is untouched). */
	public void clear() {
		agreement = null;
		spirit = null;
		block.setChanged();
	}

	/**
	 * The presence's own record of a finished task, while its terms count here: the day's count is the Bound Will's (D-S1,
	 * so a pact seated again keeps its day; critique 4), and the block keeps a copy its presence and its reading use.
	 */
	public void taskDone(ServerLevel level, long now) {
		if (verify(level, now) != Standing.VALID) {
			return;
		}
		agreement = agreement.didTask(now);
		block.setChanged();
		BoundWills wills = BoundWills.of(level.getServer());
		BoundWill record = wills.ledger().get(agreement.record());
		wills.update(record.with(Pacts.didTask(record.pact(), now)));
	}

	/**
	 * Whether {@code player} may seal here (D-S8): never a stand-in, whoever's identity it carries; only where they could
	 * change the block (Authority.mayChange: the world, spawn protection, towns, claims); and only its owner, or their party
	 * where {@code partySeals} (the Courier Post's rule, CourierPostBlockEntity.mayUse). A block that records no owner lets
	 * anyone who may change it seal (an anchor placed before Chunk D), unless this holder requires an owner (the perch: then
	 * nobody). Seating asks the same.
	 */
	public boolean maySeal(ServerLevel level, ServerPlayer player, boolean partySeals) {
		if (Authority.isStandIn(player) || !Authority.mayChange(level, player, block.getBlockPos())) {
			return false;
		}
		if (owner == null) {
			return !ownerRequired;
		}
		return owner.equals(player.getUUID()) || partySeals && JugcraftParties.sameParty(owner, player.getUUID());
	}

	/**
	 * Seals {@code sealing} for {@code player}: DONE, or the one reason it was refused. A refusal records, spends and calls
	 * nothing; the roster and the cost are asked before anything is called (D-S11, D-S13), and the cost is paid only once
	 * the Bound Will is recorded. A record still kept at this place by a block that no longer knows it is left without its
	 * place (it keeps its holder and escrow, D-P3).
	 */
	public Sealed seal(ServerLevel level, ServerPlayer player, Sealing sealing, long now) {
		if (!Workers.enabled()) {
			return new Sealed(Pacts.Outcome.DISABLED, null);
		}
		verify(level, now);
		MinecraftServer server = level.getServer();
		BoundWills wills = BoundWills.of(server);
		UUID holder = player.getUUID();
		Pacts.Outcome refused = Pacts.refusal(maySeal(level, player, sealing.partySeals()), agreement != null,
				sealing.onePerHolder() && Pacts.holds(wills.ledger(), holder, sealing.counterpart()), wills.ledger().heldBy(holder).size(),
				WorkerRoster.of(server).full(holder));
		if (refused != Pacts.Outcome.DONE) {
			return new Sealed(refused, null);
		}
		if (sealing.affords() != null && !sealing.affords().test(player)) {
			return new Sealed(Pacts.Outcome.CANNOT_AFFORD, null);
		}
		WorkerEntity<?> presence = holds.call(level, holder, Pacts.counterpart(sealing.counterpart()));
		if (presence == null) {
			return new Sealed(Pacts.Outcome.UNAVAILABLE, null);
		}
		Place here = place(level);
		BoundWill orphan = Pacts.keptAt(wills.ledger(), here);
		BoundWill record = new BoundWill(UUID.randomUUID(), sealing.agreement(), sealing.counterpart(), holder, now, false,
				Pacts.sealed(here, sealing.beneficiaries().id(), presence.getUUID(), sealing.service(), sealing.upkeep(), now));
		if (wills.seal(record).outcome() != BoundWillLedger.Outcome.DONE) {
			presence.discard();
			return new Sealed(Pacts.Outcome.UNAVAILABLE, null);
		}
		if (orphan != null) {
			wills.update(Pacts.unseat(orphan));
		}
		if (sealing.pay() != null) {
			sealing.pay().accept(player);
		}
		BlockPos pos = block.getBlockPos();
		BoundWill.Service service = sealing.service();
		agreement = new Agreement(record.id(), holder, service.work(), here.dimension(), pos.getX(), pos.getY(), pos.getZ(), service.radius(),
				service.from(), service.to(), service.quota(), BoundWill.Pact.NO_WINDOW, 0, false);
		spirit = presence.getUUID();
		block.setChanged();
		note(level, presence);
		return new Sealed(Pacts.Outcome.DONE, presence);
	}

	/** A presence noted on its holder's roster at once (G5). */
	private static void note(ServerLevel level, WorkerEntity<?> presence) {
		UUID holder = presence.owner();
		if (holder != null) {
			WorkerRoster.of(level.getServer()).note(holder, presence.getUUID(), presence.kind(), presence.status(),
					level.dimension().identifier().toString(), presence.blockPosition());
		}
	}

	/**
	 * Whether this block's terms count (D-S9): its Bound Will kept for the same holder at this place. A record gone forgets
	 * the terms (it was ended elsewhere, D-P7); an unreadable one waits. A record from before Chunk D is claimed here once,
	 * only if its terms were sealed here and its spirit, if loaded, is seated here (D-P3): its place, presence, service terms
	 * and day's count written, a suspension the block kept moved onto it (the block's flag written false, D-P4), and a record
	 * still kept here left without its place.
	 */
	public Standing verify(ServerLevel level, long now) {
		if (agreement == null) {
			return Standing.NONE;
		}
		BoundWills wills = BoundWills.of(level.getServer());
		BoundWill record = wills.ledger().get(agreement.record());
		if (record == null) {
			if (wills.unreadable(agreement.record())) {
				return Standing.UNREADABLE;
			}
			clear();
			return Standing.NONE;
		}
		if (!record.holder().equals(agreement.holder())) {
			return Standing.ELSEWHERE;
		}
		Place here = place(level);
		BoundWill.Pact pact = record.pact();
		if (pact == null) {
			boolean sealedHere = here.equals(new Place(agreement.dimension(), agreement.x(), agreement.y(), agreement.z()));
			Entity found = spirit == null ? null : level.getEntity(spirit);
			if (!sealedHere || found instanceof Presence presence && !block.getBlockPos().equals(presence.seat())) {
				return Standing.ELSEWHERE;
			}
			BoundWill orphan = Pacts.keptAt(wills.ledger(), here);
			if (orphan != null) {
				wills.update(Pacts.unseat(orphan));
			}
			wills.update(Pacts.claim(record, here, spirit, agreement, now));
			if (agreement.suspended()) {
				agreement = agreement.suspended(false);
				block.setChanged();
			}
			return Standing.VALID;
		}
		if (!here.equals(pact.place())) {
			return Standing.ELSEWHERE;
		}
		if (agreement.suspended()) {
			agreement = agreement.suspended(false);
			block.setChanged();
		}
		if (pact.presence() != null && !pact.presence().equals(spirit)) {
			spirit = pact.presence();
			block.setChanged();
		}
		return Standing.VALID;
	}

	/** Why its service is held now (SUSPENDED or UPKEEP_HELD), or null. */
	public @Nullable Status held(ServerLevel level) {
		BoundWill record = record(level);
		return record == null || !Pacts.held(record) ? null : Pacts.status(record);
	}

	/** Its state for a reading: held, or an offering due (UPKEEP_DUE), or null when simply active. */
	public @Nullable Status state(ServerLevel level) {
		BoundWill record = record(level);
		return record == null ? null : Pacts.status(record);
	}

	/**
	 * Concordance Chunk D, D-06: how its terms stand now, read only (Jade's readings): verify's answer without its writes,
	 * so nothing is claimed, forgotten, moved or marked changed. A record from before Chunk D not yet claimed counts here if
	 * its terms were sealed here (verify would claim it).
	 */
	public Standing peek(ServerLevel level) {
		if (agreement == null) {
			return Standing.NONE;
		}
		BoundWills wills = BoundWills.of(level.getServer());
		BoundWill record = wills.ledger().get(agreement.record());
		if (record == null) {
			return wills.unreadable(agreement.record()) ? Standing.UNREADABLE : Standing.NONE;
		}
		if (!record.holder().equals(agreement.holder())) {
			return Standing.ELSEWHERE;
		}
		BoundWill.Pact pact = record.pact();
		Place kept = pact == null ? new Place(agreement.dimension(), agreement.x(), agreement.y(), agreement.z()) : pact.place();
		return place(level).equals(kept) ? Standing.VALID : Standing.ELSEWHERE;
	}

	/**
	 * Whether its presence is lost (D-S18): not loaded and not removed here, and struck off its holder's roster (a killed
	 * shade strikes itself off). One that is only unloaded keeps its roster entry and is not lost.
	 */
	public boolean spiritLost(ServerLevel level) {
		if (agreement == null) {
			return false;
		}
		if (spirit == null) {
			return true;
		}
		if (level.getEntity(spirit) instanceof WorkerEntity<?> worker && !worker.isRemoved()) {
			return false;
		}
		return !WorkerRoster.of(level.getServer()).workers(agreement.holder()).containsKey(spirit);
	}

	/**
	 * Its holder calls another presence under the same record, at no cost, when theirs is lost (D-S18); refused when their
	 * roster is full. The Bound Will names its new presence.
	 */
	public Recall recall(ServerLevel level, ServerPlayer actor, long now) {
		if (!Workers.enabled()) {
			return Recall.DISABLED;
		}
		Standing standing = verify(level, now);
		if (standing == Standing.NONE) {
			return Recall.NOTHING;
		}
		if (standing != Standing.VALID) {
			return Recall.CONFLICTING;
		}
		if (Authority.isStandIn(actor) || !agreement.holder().equals(actor.getUUID())) {
			return Recall.NOT_HOLDER;
		}
		if (!spiritLost(level)) {
			return Recall.NOT_LOST;
		}
		if (WorkerRoster.of(level.getServer()).full(actor.getUUID())) {
			return Recall.ROSTER_FULL;
		}
		BoundWills wills = BoundWills.of(level.getServer());
		BoundWill record = wills.ledger().get(agreement.record());
		WorkerEntity<?> presence = holds.call(level, actor.getUUID(), Pacts.counterpart(record.counterpart()));
		if (presence == null) {
			return Recall.UNAVAILABLE;
		}
		spirit = presence.getUUID();
		block.setChanged();
		wills.update(record.with(record.pact().withPresence(spirit)));
		note(level, presence);
		return Recall.RECALLED;
	}

	/** Its holder suspends or resumes the agreement kept here, on its Bound Will (D-S9); a hold for upkeep lifts only by paying. */
	public Toggle toggle(ServerLevel level, ServerPlayer player, long now) {
		if (!Workers.enabled()) {
			return Toggle.DISABLED;
		}
		Standing standing = verify(level, now);
		if (standing == Standing.NONE) {
			return Toggle.NOTHING;
		}
		if (standing != Standing.VALID) {
			return Toggle.CONFLICTING;
		}
		return toggle(level, player, agreement.record(), now);
	}

	/**
	 * Its holder suspends or resumes record {@code id} by itself, its place loaded or not (D-07's roster; critique 9): only
	 * with the Concordance on, never a stand-in, only its holder; a hold for upkeep lifts only by paying.
	 */
	public static Toggle toggle(ServerLevel level, ServerPlayer player, UUID id, long now) {
		if (!Workers.enabled()) {
			return Toggle.DISABLED;
		}
		BoundWills wills = BoundWills.of(level.getServer());
		BoundWill record = wills.ledger().get(id);
		if (record == null || record.pact() == null) {
			return Toggle.NOTHING;
		}
		if (Authority.isStandIn(player) || !record.holder().equals(player.getUUID())) {
			return Toggle.NOT_HOLDER;
		}
		BoundWill next = Pacts.suspend(record, now);
		if (next != null) {
			wills.update(next);
			return Toggle.SUSPENDED;
		}
		next = Pacts.resume(record, now);
		if (next != null) {
			wills.update(next);
			return Toggle.RESUMED;
		}
		return Toggle.HELD_FOR_UPKEEP;
	}

	/**
	 * One look at its upkeep at {@code now} (D-S3, D-S16): only with the Concordance on, this block's terms counting, and its
	 * holder present near it; at most one period a look. D-04's perch ticker calls this; the anchor's definition has no upkeep
	 * (D-P9).
	 */
	public Pacts.Event look(ServerLevel level, long now) {
		if (!Workers.enabled() || verify(level, now) != Standing.VALID) {
			return Pacts.Event.NONE;
		}
		BoundWills wills = BoundWills.of(level.getServer());
		BoundWill record = wills.ledger().get(agreement.record());
		BoundWill.Upkeep upkeep = record.pact().upkeep();
		if (upkeep == null) {
			return Pacts.Event.NONE;
		}
		ServerPlayer present = Authority.presentNear(level, record.holder(), block.getBlockPos(), Pacts.REACH);
		Predicate<ItemStack> offering = offering(upkeep.offering());
		Pacts.Due due = Pacts.due(record, present != null, wills.periods(record.id(), offering, upkeep.count()), now);
		if (!due.changed() || due.take() && !wills.take(record.id(), offering, upkeep.count())) {
			return Pacts.Event.NONE;
		}
		wills.update(due.next());
		if (present != null && due.event() != Pacts.Event.NONE) {
			present.sendSystemMessage(said(due.event(), due.next(), wills.periods(record.id(), offering, upkeep.count()), now));
		}
		return due.event();
	}

	/**
	 * Its holder, in person and near it, pays offerings into its escrow (what matches, up to its cap); paid while held for
	 * upkeep, it resumes at once. Returns how many were taken from {@code stack}. D-04's menu calls this (server-side action, D-S6).
	 */
	public int deposit(ServerLevel level, ServerPlayer player, ItemStack stack, long now) {
		if (!Workers.enabled() || Authority.isStandIn(player) || verify(level, now) != Standing.VALID) {
			return 0;
		}
		BoundWills wills = BoundWills.of(level.getServer());
		BoundWill record = wills.ledger().get(agreement.record());
		BoundWill.Upkeep upkeep = record.pact().upkeep();
		if (upkeep == null || !record.holder().equals(player.getUUID())
				|| Authority.presentNear(level, player.getUUID(), block.getBlockPos(), Pacts.REACH) != player) {
			return 0;
		}
		int before = stack.getCount();
		wills.deposit(record.id(), stack, offering(upkeep.offering()), upkeep.cap() * upkeep.count());
		int taken = before - stack.getCount();
		if (taken > 0) {
			look(level, now);
		}
		return taken;
	}

	/** Its holder ends the agreement kept here, without breaking the block (G6). */
	public Ended end(ServerLevel level, ServerPlayer player, long now) {
		if (!Workers.enabled()) {
			return Ended.DISABLED;
		}
		Standing standing = verify(level, now);
		if (standing == Standing.NONE) {
			return Ended.NOTHING;
		}
		if (standing != Standing.VALID) {
			return Ended.CONFLICTING;
		}
		return end(level, holds, player, agreement.record(), Pacts.ENDED, now);
	}

	/**
	 * Its holder ends record {@code id} (here, or from D-07's roster with {@code home} null): only with the Concordance on,
	 * never a stand-in, only a living holder; the record goes once; its escrow back into their inventory (overflow at their
	 * feet), never to a dead player (D-S6); its presence leaves; the departure is kept. A second end finds nothing.
	 */
	public static Ended end(ServerLevel level, @Nullable Holds home, ServerPlayer player, UUID id, String reason, long now) {
		if (!Workers.enabled()) {
			return Ended.DISABLED;
		}
		if (Authority.isStandIn(player) || !player.isAlive()) {
			return Ended.NOT_PERMITTED;
		}
		BoundWills.Released released = BoundWills.of(level.getServer()).release(id, player.getUUID());
		if (released.outcome() != BoundWillLedger.Outcome.DONE) {
			return released.outcome() == BoundWillLedger.Outcome.NOT_HOLDER ? Ended.NOT_HOLDER : Ended.NOTHING;
		}
		for (ItemStack stack : released.escrow()) {
			player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
		}
		departed(level, home, released.record(), reason, now);
		return Ended.ENDED;
	}

	/**
	 * Its owner, in person, seats here their own pact with {@code counterpart} that no holder block keeps (its place broken,
	 * D-S6; or broken while the Concordance was off, its place, loaded, keeping nothing of it): the same record, terms, day's
	 * count, upkeep clock and escrow; its presence called again at no cost. Only with the Concordance on; never a stand-in;
	 * only this block's owner where they may build (as the perch seals: no party); only where no agreement is kept; refused
	 * with one reason before anything is written or called. The first look after it follows the absence rule (Pacts.due).
	 */
	public Seat seat(ServerLevel level, ServerPlayer player, String counterpart, long now) {
		if (!Workers.enabled()) {
			return Seat.DISABLED;
		}
		if (Authority.isStandIn(player)) {
			return Seat.NOT_HOLDER;
		}
		verify(level, now);
		MinecraftServer server = level.getServer();
		BoundWills wills = BoundWills.of(server);
		BoundWill record = Pacts.pactWith(wills.ledger(), player.getUUID(), counterpart);
		if (record == null || record.pact().service() == null || !stranded(server, record)) {
			return Seat.NOTHING;
		}
		if (!maySeal(level, player, false)) {
			return Seat.NOT_PERMITTED;
		}
		if (agreement != null) {
			return Seat.TAKEN;
		}
		WorkerRoster roster = WorkerRoster.of(server);
		UUID old = record.pact().presence();
		if (roster.full(player.getUUID()) && (old == null || !roster.workers(player.getUUID()).containsKey(old))) {
			return Seat.ROSTER_FULL;
		}
		WorkerEntity<?> presence = holds.call(level, player.getUUID(), Pacts.counterpart(record.counterpart()));
		if (presence == null) {
			return Seat.UNAVAILABLE;
		}
		if (old != null) {
			roster.remove(record.holder(), old);
		}
		Place here = place(level);
		BoundWill orphan = Pacts.keptAt(wills.ledger(), here);
		if (orphan != null && !orphan.id().equals(record.id())) {
			wills.update(Pacts.unseat(orphan));
		}
		BoundWill.Pact pact = record.pact().at(here).withPresence(presence.getUUID());
		wills.update(record.with(pact));
		BoundWill.Service service = pact.service();
		BlockPos pos = block.getBlockPos();
		agreement = new Agreement(record.id(), record.holder(), service.work(), here.dimension(), pos.getX(), pos.getY(), pos.getZ(),
				service.radius(), service.from(), service.to(), service.quota(), pact.window(), pact.doneToday(), false);
		spirit = presence.getUUID();
		block.setChanged();
		note(level, presence);
		return Seat.SEATED;
	}

	/**
	 * Whether {@code record}'s pact is kept by no holder block: it has no place; or its place's level is gone; or its place is
	 * loaded and the block there keeps other terms or none (broken while switched off). An unloaded place may still keep it,
	 * so it is not stranded.
	 */
	public static boolean stranded(MinecraftServer server, BoundWill record) {
		BoundWill.Pact pact = record.pact();
		if (pact == null) {
			return false;
		}
		if (pact.place() == null) {
			return true;
		}
		ServerLevel where = levelOf(server, pact.place().dimension());
		if (where == null) {
			return true;
		}
		BlockPos at = Couriers.pos(pact.place());
		if (!where.isLoaded(at)) {
			return false;
		}
		Agreement kept = where.getBlockEntity(at) instanceof Holds found ? found.holder().agreement() : null;
		return kept == null || !kept.record().equals(record.id());
	}

	/**
	 * Its block is broken or blown up (preRemoveSideEffects): with the Concordance on (D-S16), an agreement whose terms count
	 * here is, for UNSEAT (the pact perch, D-04, D-S6), left without its place, never ended: its record, escrow, hold and
	 * terms stay its holder's and its presence leaves the world; for RELEASE (the anchor), released once, its escrow dropped
	 * where the block stood (whatever the drop rules: it was paid, not mined) and its presence leaving its load here. One
	 * that does not count (a copy releases and unseats nothing), or one broken while switched off, changes nothing: the
	 * record stays, kept at its vanished place (stranded, to be seated again), and the next seal there leaves it without its
	 * place (critique 5).
	 */
	public void broken(ServerLevel level, long now) {
		if (agreement != null && Workers.enabled() && verify(level, now) == Standing.VALID) {
			if (onBreak == OnBreak.UNSEAT) {
				unseat(level);
			} else {
				BoundWills.Released released = BoundWills.of(level.getServer()).release(agreement.record(), agreement.holder());
				if (released.outcome() == BoundWillLedger.Outcome.DONE) {
					BlockPos pos = block.getBlockPos();
					for (ItemStack stack : released.escrow()) {
						Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
					}
					departed(level, holds, released.record(), Pacts.BROKEN, now);
				}
			}
		}
		agreement = null;
		spirit = null;
	}

	/**
	 * D-04 (D-S6): its place is gone: the record stays its holder's (escrow, hold, upkeep, terms), without its place; its
	 * presence leaves the world, struck off the roster.
	 */
	private void unseat(ServerLevel level) {
		BoundWills wills = BoundWills.of(level.getServer());
		BoundWill record = wills.ledger().get(agreement.record());
		UUID presence = record.pact() == null ? null : record.pact().presence();
		wills.update(Pacts.unseat(record));
		if (presence != null) {
			WorkerRoster.of(level.getServer()).remove(record.holder(), presence);
			if (level.getEntity(presence) instanceof Presence leaving) {
				leaving.leave(level, null, block.getBlockPos());
			}
		}
	}

	/**
	 * Record {@code record} was released: its presence is struck off its holder's roster and leaves its load at the block
	 * keeping it ({@code home}, or the loaded block at its place), which forgets it; the departure is kept (D-S6).
	 */
	private static void departed(ServerLevel near, @Nullable Holds home, BoundWill record, String reason, long now) {
		MinecraftServer server = near.getServer();
		BoundWill.Pact pact = record.pact();
		Place place = pact == null ? null : pact.place();
		ServerLevel where = home != null || place == null ? near : levelOf(server, place.dimension());
		BlockPos seat = home != null ? home.holder().block.getBlockPos() : place == null ? null : Couriers.pos(place);
		Holds kept = home != null ? home
				: where != null && seat != null && where.isLoaded(seat) && where.getBlockEntity(seat) instanceof Holds found ? found : null;
		boolean keeps = kept != null && kept.holder().agreement != null && record.id().equals(kept.holder().agreement.record());
		UUID presence = pact != null && pact.presence() != null ? pact.presence() : keeps ? kept.holder().spirit : null;
		if (presence != null) {
			WorkerRoster.of(server).remove(record.holder(), presence);
			Entity entity = where == null ? null : where.getEntity(presence);
			if (entity instanceof Presence leaving) {
				leaving.leave(where, kept, seat != null && where.isLoaded(seat) ? seat : entity.blockPosition());
			}
		}
		if (keeps) {
			kept.holder().clear();
		}
		PactHistory.of(server).add(record.holder(), new Pacts.Departure(record.id(), record.agreement(), record.counterpart(), reason, now, place));
	}

	private static @Nullable ServerLevel levelOf(MinecraftServer server, String dimension) {
		Identifier id = Identifier.tryParse(dimension);
		return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
	}

	private static Component conflicting() {
		return ComposeText.show(Diagnostic.of(Code.CONFLICTING_AGREEMENT).text());
	}

	/** What a seal tells its player, a refusal its one reason (G4). */
	public static Component told(Pacts.Outcome outcome) {
		return switch (outcome) {
			case DONE -> Component.translatable("message.jugcraft.concordance.workers.seal_done");
			case DISABLED -> Component.translatable("message.jugcraft.concordance.disabled");
			case NOT_PERMITTED -> Component.translatable("message.jugcraft.concordance.workers.seal_not_permitted");
			case TAKEN -> Component.translatable("message.jugcraft.concordance.workers.seal_taken");
			case ALREADY_HELD -> Component.translatable("message.jugcraft.concordance.workers.seal_already_held");
			case HOLDER_FULL -> Component.translatable("message.jugcraft.concordance.workers.seal_holder_full", BoundWillLedger.MAX_PER_HOLDER);
			case ROSTER_FULL -> Component.translatable("message.jugcraft.concordance.workers.seal_roster_full", WorkerRoster.MAX_PER_PLAYER);
			case UNAVAILABLE -> Component.translatable("message.jugcraft.concordance.workers.seal_unavailable");
			case CANNOT_AFFORD -> Component.translatable("message.jugcraft.concordance.workers.seal_cannot_afford");
		};
	}

	public static Component told(Recall recall) {
		return switch (recall) {
			case RECALLED -> Component.translatable("message.jugcraft.concordance.workers.spirit_recalled");
			case NOTHING -> Component.translatable("message.jugcraft.concordance.workers.no_agreement");
			case NOT_HOLDER -> Component.translatable("message.jugcraft.concordance.workers.not_holder");
			case NOT_LOST -> Component.translatable("message.jugcraft.concordance.workers.spirit_away");
			case CONFLICTING -> conflicting();
			case ROSTER_FULL -> Component.translatable("message.jugcraft.concordance.workers.recall_roster_full", WorkerRoster.MAX_PER_PLAYER);
			case UNAVAILABLE -> Component.translatable("message.jugcraft.concordance.workers.seal_unavailable");
			case DISABLED -> Component.translatable("message.jugcraft.concordance.disabled");
		};
	}

	public static Component told(Toggle toggle) {
		return switch (toggle) {
			case SUSPENDED -> Component.translatable("message.jugcraft.concordance.workers.suspended");
			case RESUMED -> Component.translatable("message.jugcraft.concordance.workers.resumed");
			case HELD_FOR_UPKEEP -> Component.translatable("message.jugcraft.concordance.workers.held_for_upkeep");
			case NOTHING -> Component.translatable("message.jugcraft.concordance.workers.no_agreement");
			case NOT_HOLDER -> Component.translatable("message.jugcraft.concordance.workers.not_holder");
			case CONFLICTING -> conflicting();
			case DISABLED -> Component.translatable("message.jugcraft.concordance.disabled");
		};
	}

	public static Component told(Ended ended) {
		return switch (ended) {
			case ENDED -> Component.translatable("message.jugcraft.concordance.workers.ended");
			case NOTHING -> Component.translatable("message.jugcraft.concordance.workers.no_agreement");
			case NOT_HOLDER -> Component.translatable("message.jugcraft.concordance.workers.not_holder");
			case NOT_PERMITTED -> Component.translatable("message.jugcraft.concordance.workers.end_not_permitted");
			case CONFLICTING -> conflicting();
			case DISABLED -> Component.translatable("message.jugcraft.concordance.disabled");
		};
	}

	public static Component told(Seat seat) {
		return switch (seat) {
			case SEATED -> Component.translatable("message.jugcraft.concordance.workers.seat_seated");
			case DISABLED -> Component.translatable("message.jugcraft.concordance.disabled");
			case NOT_HOLDER -> Component.translatable("message.jugcraft.concordance.workers.seat_not_holder");
			case NOTHING -> Component.translatable("message.jugcraft.concordance.workers.seat_nothing");
			case NOT_PERMITTED -> Component.translatable("message.jugcraft.concordance.workers.seat_not_permitted");
			case TAKEN -> Component.translatable("message.jugcraft.concordance.workers.seat_taken");
			case ROSTER_FULL -> Component.translatable("message.jugcraft.concordance.workers.seat_roster_full", WorkerRoster.MAX_PER_PLAYER);
			case UNAVAILABLE -> Component.translatable("message.jugcraft.concordance.workers.seat_unavailable");
		};
	}

	/** What one look tells its holder: an offering taken, a warning with its grace, a hold for upkeep, a resumption. */
	private static Component said(Pacts.Event event, BoundWill record, int periods, long now) {
		BoundWill.Upkeep upkeep = record.pact().upkeep();
		Component what = offeringName(upkeep.offering());
		return switch (event) {
			case PAID -> Component.translatable("message.jugcraft.concordance.workers.upkeep_paid", periods);
			case WARNED -> Component.translatable("message.jugcraft.concordance.workers.upkeep_warned", upkeep.count(), what,
					days(upkeep.graceUntil() - now));
			case SUSPENDED -> Component.translatable("message.jugcraft.concordance.workers.upkeep_held", upkeep.count(), what);
			case RESUMED -> Component.translatable("message.jugcraft.concordance.workers.upkeep_resumed");
			case NONE -> Component.empty();
		};
	}

	/** Whether a stack is an offering of item tag {@code tag} (none matches a tag id that cannot be read). */
	static Predicate<ItemStack> offering(String tag) {
		Identifier id = Identifier.tryParse(tag);
		if (id == null) {
			return stack -> false;
		}
		TagKey<Item> key = TagKey.create(Registries.ITEM, id);
		return stack -> !stack.isEmpty() && stack.is(key);
	}

	static Component offeringName(String tag) {
		return Component.translatableWithFallback("tag.item." + tag.replace(':', '.'), "#" + tag);
	}

	static long days(long ticks) {
		return Math.max(0L, (ticks + Pacts.DAY - 1) / Pacts.DAY);
	}

	public void load(ValueInput input) {
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		agreement = input.read("agreement", AGREEMENT_CODEC).orElse(null);
		spirit = input.read("spirit", UUIDUtil.CODEC).orElse(null);
	}

	public void save(ValueOutput output) {
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		if (agreement != null) {
			output.store("agreement", AGREEMENT_CODEC, agreement);
		}
		if (spirit != null) {
			output.store("spirit", UUIDUtil.CODEC, spirit);
		}
	}
}
