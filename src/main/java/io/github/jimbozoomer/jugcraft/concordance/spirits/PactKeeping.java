package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Pattern;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, D-04 and D-05 (D-S3, D-S6, D-S7, D-S16): keeping a pact at its perch, for the perch's ticker and
 * menu. Once a second the perch looks: with the Concordance on, at its pact's upkeep (AgreementHolder.look: only its
 * holder, present near it, ever owes; never a stand-in) and, its holder near, calls its lost presence back at no cost (a
 * lost presence never ends a pact). Its holder pays offerings into the escrow by kind, server-side (the menu has no
 * slots); suspends and resumes it (idempotent, so a stale screen never toggles the wrong way); ends it (the escrow back
 * once into their inventory, the rest at their feet; Stolas departs; the reason told and kept), here or, for a pact
 * without its perch, from any perch that keeps nothing; and reads its page. Nothing here is rate-gated: the
 * menu asks RateGate before every action (D-P8). Work a second: one ledger lookup, one player lookup.
 */
public final class PactKeeping {
	/** The most offering kinds a deposit offers (tools/concordance_pacts.py SESSION). */
	public static final int MAX_OFFERINGS = 8;

	private PactKeeping() {
	}

	/** The perch's look at game time {@code now} (its ticker, once a second; tests call it with their own times). */
	public static void tick(ServerLevel level, AgreementHolder holder, BlockPos at, long now) {
		if (!Workers.enabled() || holder.agreement() == null) {
			return;
		}
		holder.look(level, now);
		BoundWill record = holder.record(level);
		ServerPlayer present = record == null ? null : Authority.presentNear(level, record.holder(), at, Pacts.REACH);
		if (present != null && holder.spiritLost(level) && holder.recall(level, present, now) == AgreementHolder.Recall.RECALLED) {
			present.sendSystemMessage(Component.translatable("message.jugcraft.concordance.pact.returned", name(record.counterpart())));
		}
	}

	/** A counterpart's name: its entity type's (entity.<ns>.<path>). */
	public static Component name(String counterpart) {
		Identifier id = Identifier.tryParse(Pacts.counterpart(counterpart));
		return id == null ? Component.literal(counterpart) : Component.translatable("entity." + id.getNamespace() + "." + id.getPath());
	}

	/** The offering kinds {@code player} carries that {@code record}'s upkeep takes, by item id, at most MAX_OFFERINGS. */
	public static List<Item> offerings(ServerPlayer player, BoundWill record) {
		BoundWill.Upkeep upkeep = record.pact() == null ? null : record.pact().upkeep();
		if (upkeep == null) {
			return List.of();
		}
		Predicate<ItemStack> offering = AgreementHolder.offering(upkeep.offering());
		TreeMap<String, Item> kinds = new TreeMap<>();
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (offering.test(stack)) {
				kinds.put(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getItem());
			}
		}
		return kinds.values().stream().limit(MAX_OFFERINGS).toList();
	}

	/** Whether {@code record}'s escrow has room for another offering (fewer than its cap's worth held). */
	public static boolean room(ServerLevel level, BoundWill record) {
		BoundWill.Upkeep upkeep = record.pact() == null ? null : record.pact().upkeep();
		if (upkeep == null) {
			return false;
		}
		Predicate<ItemStack> offering = AgreementHolder.offering(upkeep.offering());
		int held = BoundWills.of(level.getServer()).escrow(record.id()).stream().filter(offering).mapToInt(ItemStack::getCount).sum();
		return held < upkeep.cap() * upkeep.count();
	}

	/**
	 * Its holder pays offerings of {@code kind} from their inventory, in slot order, into the escrow through
	 * AgreementHolder.deposit (its holder in person and near, switched on; what matches and fits, the cap counted after any
	 * period it takes). Returns how many were taken.
	 */
	public static int deposit(ServerLevel level, ServerPlayer player, AgreementHolder holder, Item kind, long now) {
		Inventory inventory = player.getInventory();
		int taken = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty() || !stack.is(kind)) {
				continue;
			}
			int moved = holder.deposit(level, player, stack, now);
			taken += moved;
			if (moved == 0 || !stack.isEmpty()) {
				break;
			}
		}
		if (taken > 0) {
			inventory.setChanged();
		}
		return taken;
	}

	/** What a deposit of {@code taken} {@code kind} tells its holder. */
	public static Component deposited(ServerLevel level, AgreementHolder holder, Item kind, int taken) {
		BoundWill record = holder.record(level);
		BoundWill.Upkeep upkeep = record == null || record.pact() == null ? null : record.pact().upkeep();
		if (taken <= 0 || upkeep == null) {
			return Component.translatable("message.jugcraft.concordance.pact.deposit_none");
		}
		int periods = BoundWills.of(level.getServer()).periods(record.id(), AgreementHolder.offering(upkeep.offering()), upkeep.count());
		return Component.translatable("message.jugcraft.concordance.pact.deposited", taken, kind.getDefaultInstance().getHoverName(), periods, upkeep.cap());
	}

	/** Its holder suspends the pact kept here; already held, it says how (nothing toggles back). */
	public static AgreementHolder.Toggle suspend(ServerLevel level, ServerPlayer player, AgreementHolder holder, long now) {
		BoundWill record = mine(level, player, holder, now);
		if (record != null && Pacts.held(record)) {
			return record.pact().hold() == BoundWill.Hold.HOLDER ? AgreementHolder.Toggle.SUSPENDED : AgreementHolder.Toggle.HELD_FOR_UPKEEP;
		}
		return holder.toggle(level, player, now);
	}

	/** Its holder resumes the pact kept here; not held, it says so; held for upkeep, only an offering lifts it. */
	public static AgreementHolder.Toggle resume(ServerLevel level, ServerPlayer player, AgreementHolder holder, long now) {
		BoundWill record = mine(level, player, holder, now);
		if (record != null && !Pacts.held(record)) {
			return AgreementHolder.Toggle.RESUMED;
		}
		return holder.toggle(level, player, now);
	}

	/** The record kept here if {@code player} is its holder, in person, with the Concordance on; otherwise null. */
	private static @Nullable BoundWill mine(ServerLevel level, ServerPlayer player, AgreementHolder holder, long now) {
		if (!Workers.enabled() || Authority.isStandIn(player) || holder.verify(level, now) != AgreementHolder.Standing.VALID) {
			return null;
		}
		BoundWill record = holder.record(level);
		return record != null && record.holder().equals(player.getUUID()) ? record : null;
	}

	/** {@code player}'s own pact with {@code counterpart} that no holder block keeps (AgreementHolder.stranded), or null. */
	public static @Nullable BoundWill strandedPact(ServerLevel level, ServerPlayer player, String counterpart) {
		if (!Workers.enabled() || Authority.isStandIn(player)) {
			return null;
		}
		BoundWill record = Pacts.pactWith(BoundWills.of(level.getServer()).ledger(), player.getUUID(), counterpart);
		return record != null && AgreementHolder.stranded(level.getServer(), record) ? record : null;
	}

	/** How an end went, and the words for it. */
	public record Ending(AgreementHolder.Ended ended, Component said) {
	}

	/**
	 * Its holder ends the pact kept here, or (at a perch keeping nothing) their own pact with {@code counterpart} without its
	 * perch (AgreementHolder.end): who departs, why and how many offerings came back; or the refusal.
	 */
	public static Ending end(ServerLevel level, ServerPlayer player, AgreementHolder holder, String counterpart, long now) {
		BoundWill record = mine(level, player, holder, now);
		boolean here = record != null;
		if (!here && holder.verify(level, now) == AgreementHolder.Standing.NONE) {
			record = strandedPact(level, player, counterpart);
		}
		int returned = record == null ? 0 : BoundWills.of(level.getServer()).escrow(record.id()).stream().mapToInt(ItemStack::getCount).sum();
		AgreementHolder.Ended ended = here || record == null ? holder.end(level, player, now)
				: AgreementHolder.end(level, null, player, record.id(), Pacts.ENDED, now);
		if (ended != AgreementHolder.Ended.ENDED || record == null) {
			return new Ending(ended, AgreementHolder.told(ended));
		}
		return new Ending(ended, Component.translatable("message.jugcraft.concordance.pact.departed", name(record.counterpart()),
				Component.translatable("message.jugcraft.concordance.workers.departed.ended"), returned));
	}

	/** The pact page: rows (at most 9), the patterns a reckoning offers (ids, D-05) and the offering kinds a deposit offers. */
	public record Page(List<Component> rows, List<String> patterns, List<Item> offerings) {
		public static final Page NONE = new Page(List.of(), List.of(), List.of());
	}

	/** What the pact kept here shows its holder at game time {@code now}; NONE for anyone else, a copy, or switched off. */
	public static Page page(ServerLevel level, ServerPlayer player, AgreementHolder holder, long now) {
		if (!Workers.enabled() || Authority.isStandIn(player) || holder.verify(level, now) != AgreementHolder.Standing.VALID) {
			return Page.NONE;
		}
		BoundWill record = holder.record(level);
		BoundWill.Pact pact = record.pact();
		BoundWill.Service service = pact.service();
		if (service == null || !record.holder().equals(player.getUUID())) {
			return Page.NONE;
		}
		MinecraftServer server = level.getServer();
		BoundWill.Upkeep upkeep = pact.upkeep();
		Component offering = upkeep == null ? Component.empty() : AgreementHolder.offeringName(upkeep.offering());
		List<Component> rows = new ArrayList<>();
		Status status = Pacts.status(record);
		if (status == Status.SUSPENDED) {
			rows.add(Component.translatable("message.jugcraft.concordance.pact.row.state.suspended"));
		} else if (status == Status.UPKEEP_HELD && upkeep != null) {
			rows.add(Component.translatable("message.jugcraft.concordance.pact.row.state.upkeep_held", upkeep.count(), offering));
		} else if (status == Status.UPKEEP_DUE && upkeep != null) {
			rows.add(Component.translatable("message.jugcraft.concordance.pact.row.state.upkeep_due", upkeep.count(), offering,
					AgreementHolder.days(upkeep.graceUntil() - now)));
		} else {
			rows.add(Component.translatable("message.jugcraft.concordance.pact.row.state.active"));
		}
		if (holder.spiritLost(level)) {
			rows.add(Component.translatable("message.jugcraft.concordance.pact.row.lost", name(record.counterpart()), WorkerRoster.MAX_PER_PLAYER));
		}
		rows.add(Component.translatable("message.jugcraft.concordance.pact.row.service", service.quota(),
				Math.max(0, service.quota() - Pacts.done(pact, now))));
		if (upkeep != null) {
			if (!Pacts.held(record)) {
				rows.add(Component.translatable("message.jugcraft.concordance.pact.row.next", upkeep.count(), offering, AgreementHolder.days(upkeep.due() - now)));
			}
			rows.add(Component.translatable("message.jugcraft.concordance.pact.row.escrow",
					BoundWills.of(server).periods(record.id(), AgreementHolder.offering(upkeep.offering()), upkeep.count()), upkeep.cap()));
		}
		rows.add(Component.translatable(StarReckoning.kept(player) ? "message.jugcraft.concordance.pact.row.obligation_kept"
				: "message.jugcraft.concordance.pact.row.obligation"));
		rows.add(service.from() == 0 && service.to() == Pacts.DAY
				? Component.translatable("message.jugcraft.concordance.pact.row.area_any", service.radius())
				: Component.translatable("message.jugcraft.concordance.pact.row.area", service.radius(), Workers.clock(service.from()), Workers.clock(service.to())));
		rows.add(Component.translatable("message.jugcraft.concordance.pact.row.end"));
		List<String> patterns = StarReckoning.choices(server, player.getUUID()).stream().map(Pattern::id).toList();
		return new Page(List.copyOf(rows), patterns, offerings(player, record));
	}
}
