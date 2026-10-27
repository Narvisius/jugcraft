package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ComposeText;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Pattern;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstralClaims;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstrolabeItem;
import io.github.jimbozoomer.jugcraft.concordance.sky.Sky;
import io.github.jimbozoomer.jugcraft.concordance.worker.PactDefinition;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, D-04 and D-05 (D-S6, D-S8, D-S13, D-S14, D-S15): the pact perch's screen on the server. It has no
 * slots and no button ids: every request is a {@link PactRequestPayload}, answered with a {@link PactStatePayload}. The
 * server drops a request from a stand-in, for a menu that is not its player's open one, from a menu no longer valid (the
 * perch gone, out of reach, the conversation timed out), out of bounds, or not newer than the last answered; asks the
 * rate gate; answers "changed", sending the terms again, a request naming a terms revision other than the one in force;
 * and only then acts, every action through the shared agreement holder's own checks. The revision counts the offer, what
 * accepting binds its reader to: a reload that changes it moves the revision; the perch being taken meanwhile does not, so
 * a second acceptance at one perch is refused as taken (D-S8). A choice indexes the list this menu last sent, which the
 * server keeps; after each action the player's inventory is synced, since a slotless menu syncs nothing of its own.
 */
public class PactMenu extends AbstractContainerMenu {
	public static final int RATE_TICKS = 4;
	public static final int OPEN_TICKS = 10;
	static final String RATE_ACTION = "pact";
	static final String CHOOSE_ACTION = "pact_choose";
	// The reasons the menu itself answers with (tools/concordance_pacts.py ANSWERS); the others are the shared outcomes' ids.
	public static final String CHANGED = "changed";
	public static final String TOO_FAST = "too_fast";
	public static final String NOT_OFFERED = "not_offered";
	public static final String SEALED = "sealed";
	public static final String DECLINED = "declined";
	public static final String DISABLED = "disabled";
	public static final String NO_DEFINITION = "no_definition";
	public static final String MISSING_EVIDENCE = "missing_evidence";
	public static final String NO_OBSERVATION = "no_observation";
	public static final String DEPOSITED = "deposited";
	public static final String NOTHING_DEPOSITED = "nothing_deposited";

	/** An action's answer: its reason (a stable id), whether it was done, and the words for it. */
	public record Answered(String reason, boolean done, Component said) {
	}

	/** What the client is told when the screen opens: which perch. */
	public record Opening(BlockPos perch) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Opening> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC,
				Opening::perch, Opening::new);
	}

	private record View(int mode, List<Component> beats, List<PactStatePayload.Term> terms, List<Component> notes,
			List<PactStatePayload.Choice> patterns, List<String> patternIds, List<PactStatePayload.Choice> offerings, List<Item> offeringItems,
			List<ItemStack> compact, int resonance, int actions) {
		static View closed(List<Component> notes) {
			return new View(PactStatePayload.CLOSED, List.of(), List.of(), notes, List.of(), List.of(), List.of(), List.of(), List.of(), 0,
					PactStatePayload.mask(PactRequestPayload.CHOOSE));
		}
	}

	public final Opening opening;
	private final @Nullable PactPerchBlockEntity perch;
	private int sequence;
	private int revision;
	private List<PactStatePayload.Term> offer = List.of();
	private List<String> patternIds = List.of();
	private List<Item> offeringItems = List.of();
	private boolean concluded;
	private @Nullable PactStatePayload lastSent;

	/** Client side: everything else arrives as PactStatePayload. */
	public PactMenu(int containerId, Inventory inventory, Opening opening) {
		this(containerId, opening, null);
	}

	private PactMenu(int containerId, Opening opening, @Nullable PactPerchBlockEntity perch) {
		super(Workers.PERCH_MENU, containerId);
		this.opening = opening;
		this.perch = perch;
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(PactRequestPayload.TYPE, PactRequestPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PactStatePayload.TYPE, PactStatePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PactRequestPayload.TYPE, (payload, context) -> receive(context.player(), payload));
		// A reload may change Stolas's offer: every open perch screen is sent its state again (a revision change).
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> refreshAll(server));
		PactSessions.register();
	}

	/** The state last sent (server side; tests read it). */
	public @Nullable PactStatePayload lastSent() {
		return lastSent;
	}

	/** The last request sequence this menu took (server side; tests send the next). */
	public int lastSequence() {
		return sequence;
	}

	/** Opens the perch's screen for {@code player} (the block's use, after its rate gate; tests call it directly). */
	public static void open(ServerPlayer player, ServerLevel level, PactPerchBlockEntity perch) {
		if (Authority.isStandIn(player)) {
			return;
		}
		long now = level.getGameTime();
		AgreementHolder holder = perch.holder();
		// Its holder back at their perch: a lost presence is called again, at no cost (D-S7).
		if (holder.verify(level, now) == AgreementHolder.Standing.VALID && holder.agreement().holder().equals(player.getUUID())
				&& holder.spiritLost(level)) {
			holder.recall(level, player, now);
		}
		Opening opening = new Opening(perch.getBlockPos().immutable());
		player.openMenu(new ExtendedMenuProvider<Opening>() {
			@Override
			public Opening getScreenOpeningData(ServerPlayer p) {
				return opening;
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("container.jugcraft.pact_perch");
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
				return new PactMenu(containerId, opening, perch);
			}
		});
		if (player.containerMenu instanceof PactMenu menu && menu.perch == perch) {
			menu.begin(level, player, now);
		}
	}

	private void begin(ServerLevel level, ServerPlayer player, long now) {
		PactSessions.open(player, GlobalPos.of(level.dimension(), perch.getBlockPos()), containerId, now);
		if (invitable(level, player, PactPerchBlockEntity.definition(), now)) {
			perch.welcome(level);
		}
		// Concordance Chunk D, D-06: he attends while the conversation is open (his listening loop; its sound).
		perch.heard(level);
		answer(level, player, null, 0, now);
	}

	/** Switched on, his offer loaded, the perch keeping nothing, its owner (D-S8), Stolas's Regard understood, a star observed, no pact with him held. */
	private boolean invitable(ServerLevel level, ServerPlayer player, @Nullable PactDefinition definition, long now) {
		return Workers.enabled() && definition != null && perch.holder().verify(level, now) == AgreementHolder.Standing.NONE
				&& perch.holder().maySeal(level, player, false) && knows(player, definition) && observed(level, player)
				&& !Pacts.holds(BoundWills.of(level.getServer()).ledger(), player.getUUID(), definition.id());
	}

	static boolean knows(Player player, PactDefinition definition) {
		return ConcordanceProgress.knowledge(player).state(definition.research()).atLeast(ResearchState.UNDERSTOOD);
	}

	static boolean observed(ServerLevel level, Player player) {
		return !AstralClaims.of(level.getServer()).observed(player.getUUID()).isEmpty();
	}

	@Override
	public boolean stillValid(Player player) {
		if (perch == null) {
			return true;
		}
		return !perch.isRemoved() && Container.stillValidBlockEntity(perch, player)
				&& !PactSessions.expired(player.getUUID(), containerId, player.level().getGameTime());
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		return false;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if (perch != null) {
			PactSessions.close(player.getUUID(), containerId);
		}
	}

	/** A screen's request (the receiver calls this, and so do tests); null when it is dropped. */
	public static @Nullable PactStatePayload receive(ServerPlayer player, PactRequestPayload request) {
		if (Authority.isStandIn(player) || !(player.containerMenu instanceof PactMenu menu) || menu.containerId != request.containerId()
				|| menu.perch == null || !menu.stillValid(player)) {
			return null;
		}
		if (request.action() < 0 || request.action() >= PactRequestPayload.ACTIONS.size() || request.choice() < 0
				|| request.choice() >= PactStatePayload.MAX_CHOICES || request.revision() < 0 || request.sequence() <= menu.sequence) {
			return null;
		}
		menu.sequence = request.sequence();
		ServerLevel level = player.level();
		long now = level.getGameTime();
		PactSessions.touch(player.getUUID(), menu.containerId, now);
		String gate = request.action() == PactRequestPayload.CHOOSE ? CHOOSE_ACTION : RATE_ACTION;
		if (!RateGate.allow(player, gate, RATE_TICKS)) {
			return menu.answer(level, player, new Answered(TOO_FAST, false, line("answer.too_fast")), request.sequence(), now);
		}
		menu.revise();
		if (request.revision() != menu.revision) {
			return menu.answer(level, player, new Answered(CHANGED, false, line("answer.changed")), request.sequence(), now);
		}
		Answered done = menu.act(level, player, request.action(), request.choice(), now);
		if (done != null) {
			// Concordance Chunk D, D-06: Stolas answers the action with his gesture and its sound, fired here, never replayed.
			menu.perch.react(level, request.action(), done.reason(), done.done());
		}
		player.inventoryMenu.broadcastChanges();
		if (player.containerMenu != menu) {
			// Declined: the menu closed in this call and its client drops a state for a closed menu, so the answer is shown
			// on the action bar instead and the state only returned (the unit review).
			if (done != null) {
				player.sendOverlayMessage(done.said());
			}
			return menu.state(level, player, done, request.sequence(), now);
		}
		return menu.answer(level, player, done, request.sequence(), now);
	}

	/** The action's answer; null for CHOOSE (the holder asked to hear his terms: nothing is stored or spent). */
	private @Nullable Answered act(ServerLevel level, ServerPlayer player, int action, int choice, long now) {
		return switch (action) {
			case PactRequestPayload.CHOOSE -> null;
			case PactRequestPayload.ACCEPT -> accept(level, player, now);
			case PactRequestPayload.DECLINE -> decline(player);
			case PactRequestPayload.RECALL -> recall(level, player, choice, now);
			case PactRequestPayload.DEPOSIT -> deposit(level, player, choice, now);
			case PactRequestPayload.SUSPEND -> toggled(PactKeeping.suspend(level, player, perch.holder(), now));
			case PactRequestPayload.RESUME -> toggled(PactKeeping.resume(level, player, perch.holder(), now));
			case PactRequestPayload.END -> end(level, player, now);
			case PactRequestPayload.SEAT -> seat(level, player, now);
			default -> notOffered();
		};
	}

	/** Accepting: refused with its first reason before anything is called or spent; the compact paid by the seal itself (D-S13). */
	private Answered accept(ServerLevel level, ServerPlayer player, long now) {
		PactDefinition definition = PactPerchBlockEntity.definition();
		if (!Workers.enabled()) {
			return new Answered(DISABLED, false, Component.translatable("message.jugcraft.concordance.disabled"));
		}
		if (definition == null) {
			return new Answered(NO_DEFINITION, false, line("unavailable"));
		}
		if (!perch.holder().maySeal(level, player, false)) {
			return refused(Pacts.Outcome.NOT_PERMITTED);
		}
		if (!knows(player, definition)) {
			return new Answered(MISSING_EVIDENCE, false, line("answer.missing_evidence"));
		}
		if (!observed(level, player)) {
			return new Answered(NO_OBSERVATION, false, line("missing.observation"));
		}
		List<PactCompact.Need> needs = PactCompact.needs(definition);
		if (needs == null) {
			return new Answered(NO_DEFINITION, false, line("unavailable"));
		}
		AgreementHolder.Sealed sealed = perch.holder().seal(level, player, PactPerchBlockEntity.sealing(definition,
				payer -> PactCompact.affords(payer, needs, definition.resonance()), payer -> PactCompact.pay(payer, needs, definition.resonance())), now);
		return switch (sealed.outcome()) {
			case DONE -> new Answered(SEALED, true, line("answer.sealed"));
			case CANNOT_AFFORD -> new Answered(sealed.outcome().id, false, line("answer.cannot_afford", definition.resonance()));
			default -> refused(sealed.outcome());
		};
	}

	private static Answered refused(Pacts.Outcome outcome) {
		return new Answered(outcome.id, false, AgreementHolder.told(outcome));
	}

	private Answered decline(ServerPlayer player) {
		PactSessions.close(player.getUUID(), containerId);
		player.closeContainer();
		return new Answered(DECLINED, true, line("answer.declined"));
	}

	/** Its holder pays offerings into the pact's keeping: a server action, never a slot (D-S6, critique 21). */
	private Answered deposit(ServerLevel level, ServerPlayer player, int choice, long now) {
		if (choice >= offeringItems.size()) {
			return notOffered();
		}
		Item kind = offeringItems.get(choice);
		int taken = PactKeeping.deposit(level, player, perch.holder(), kind, now);
		return new Answered(taken > 0 ? DEPOSITED : NOTHING_DEPOSITED, taken > 0, PactKeeping.deposited(level, perch.holder(), kind, taken));
	}

	/** Star-reckoning (D-05, D-S1): the pattern this menu last offered at {@code choice}, reckoned through the one recall. */
	private Answered recall(ServerLevel level, ServerPlayer player, int choice, long now) {
		if (choice >= patternIds.size()) {
			return notOffered();
		}
		String id = patternIds.get(choice);
		Pacts.Reckoning reckoning = StarReckoning.reckon(level, player, perch.holder(), id, Sky.time(level), now);
		BoundWill record = perch.holder().record(level);
		BoundWill.Service service = record == null || record.pact() == null ? null : record.pact().service();
		return new Answered(reckoning.id, reckoning == Pacts.Reckoning.RECALLED, StarReckoning.told(reckoning, Sky.catalog().pattern(id), service));
	}

	private static Answered toggled(AgreementHolder.Toggle toggle) {
		return new Answered(toggle.name().toLowerCase(Locale.ROOT),
				toggle == AgreementHolder.Toggle.SUSPENDED || toggle == AgreementHolder.Toggle.RESUMED, AgreementHolder.told(toggle));
	}

	/** Its holder ends the pact kept here, or (at a perch keeping nothing) their pact without its perch (D-S6). */
	private Answered end(ServerLevel level, ServerPlayer player, long now) {
		PactKeeping.Ending ending = PactKeeping.end(level, player, perch.holder(), PactDefinition.STOLAS, now);
		boolean done = ending.ended() == AgreementHolder.Ended.ENDED;
		concluded |= done;
		return new Answered(ending.ended().name().toLowerCase(Locale.ROOT), done, ending.said());
	}

	private Answered seat(ServerLevel level, ServerPlayer player, long now) {
		AgreementHolder.Seat seat = perch.holder().seat(level, player, PactDefinition.STOLAS, now);
		return new Answered(seat.id, seat == AgreementHolder.Seat.SEATED, AgreementHolder.told(seat));
	}

	private static Answered notOffered() {
		return new Answered(NOT_OFFERED, false, line("answer.not_offered"));
	}

	/** A changed offer moves the revision. */
	private void revise() {
		PactDefinition definition = PactPerchBlockEntity.definition();
		List<PactStatePayload.Term> current = definition == null ? List.of() : offer(definition);
		if (revision == 0 || !current.equals(offer)) {
			offer = current;
			revision++;
		}
	}

	/** What the screen shows now. */
	private View view(ServerLevel level, ServerPlayer player, long now) {
		revise();
		if (!Workers.enabled()) {
			return View.closed(List.of(Component.translatable("message.jugcraft.concordance.disabled")));
		}
		AgreementHolder holder = perch.holder();
		AgreementHolder.Standing standing = holder.verify(level, now);
		if (standing == AgreementHolder.Standing.ELSEWHERE) {
			return View.closed(List.of(ComposeText.show(Diagnostic.of(Code.CONFLICTING_AGREEMENT).text())));
		}
		if (standing == AgreementHolder.Standing.UNREADABLE) {
			return View.closed(List.of(line("reading.unreadable")));
		}
		if (standing == AgreementHolder.Standing.VALID) {
			BoundWill record = holder.record(level);
			return record != null && record.pact() != null && record.holder().equals(player.getUUID()) ? kept(level, player, record, now)
					: View.closed(List.of(line("reading.taken")));
		}
		if (concluded) {
			return View.closed(List.of(line("ended_here")));
		}
		BoundWill away = PactKeeping.strandedPact(level, player, PactDefinition.STOLAS);
		if (away != null) {
			return seating(level, player, away);
		}
		PactDefinition definition = PactPerchBlockEntity.definition();
		if (definition == null) {
			return View.closed(List.of(line("unavailable")));
		}
		return negotiate(level, player, definition);
	}

	private View negotiate(ServerLevel level, ServerPlayer player, PactDefinition definition) {
		List<Component> notes = missing(level, player, definition);
		List<PactCompact.Need> needs = PactCompact.needs(definition);
		List<ItemStack> compact = needs == null ? List.of() : needs.stream().map(need -> new ItemStack(need.item(), need.count())).toList();
		int actions = PactStatePayload.mask(PactRequestPayload.CHOOSE, PactRequestPayload.DECLINE)
				| (notes.isEmpty() ? PactStatePayload.mask(PactRequestPayload.ACCEPT) : 0);
		return new View(PactStatePayload.NEGOTIATE, beats(definition), offer, notes, List.of(), List.of(), List.of(), List.of(), compact,
				definition.resonance(), actions);
	}

	private View kept(ServerLevel level, ServerPlayer player, BoundWill record, long now) {
		PactKeeping.Page page = PactKeeping.page(level, player, perch.holder(), now);
		BoundWill.Pact pact = record.pact();
		List<PactStatePayload.Choice> patterns = new ArrayList<>();
		long time = Sky.time(level);
		for (String id : page.patterns()) {
			Pattern pattern = Sky.catalog().pattern(id);
			if (pattern != null) {
				patterns.add(new PactStatePayload.Choice(id, Sky.patternName(id), pattern.recallCost(),
						AstrolabeItem.mayRecall(level, player, pattern, time, now)));
			}
		}
		List<String> ids = patterns.stream().map(PactStatePayload.Choice::id).toList();
		List<PactStatePayload.Choice> offerings = new ArrayList<>();
		boolean room = PactKeeping.room(level, record);
		for (Item item : page.offerings()) {
			offerings.add(new PactStatePayload.Choice(BuiltInRegistries.ITEM.getKey(item).toString(), new ItemStack(item).getHoverName(),
					player.getInventory().countItem(item), room));
		}
		boolean unheld = pact.hold() == BoundWill.Hold.NONE;
		int actions = PactStatePayload.mask(PactRequestPayload.CHOOSE, PactRequestPayload.END)
				| (unheld ? PactStatePayload.mask(PactRequestPayload.SUSPEND) : 0)
				| (pact.hold() == BoundWill.Hold.HOLDER ? PactStatePayload.mask(PactRequestPayload.RESUME) : 0)
				| (!offerings.isEmpty() ? PactStatePayload.mask(PactRequestPayload.DEPOSIT) : 0)
				| (unheld && !patterns.isEmpty() ? PactStatePayload.mask(PactRequestPayload.RECALL) : 0);
		return new View(PactStatePayload.KEEP, List.of(), terms(record), page.rows(), patterns, ids, offerings, page.offerings(), List.of(), 0,
				actions);
	}

	private View seating(ServerLevel level, ServerPlayer player, BoundWill away) {
		List<Component> notes = new ArrayList<>(List.of(line("seat.offer")));
		boolean may = perch.holder().maySeal(level, player, false);
		if (!may) {
			notes.add(line("missing.owner"));
		}
		int actions = PactStatePayload.mask(PactRequestPayload.CHOOSE, PactRequestPayload.END) | (may ? PactStatePayload.mask(PactRequestPayload.SEAT) : 0);
		return new View(PactStatePayload.SEAT, List.of(), terms(away), notes, List.of(), List.of(), List.of(), List.of(), List.of(), 0, actions);
	}

	/** What is missing before Stolas will agree, each named (D-04): the owner, prerequisites, each step of his Regard, the sky step, a pact already held. */
	private List<Component> missing(ServerLevel level, ServerPlayer player, PactDefinition definition) {
		List<Component> notes = new ArrayList<>();
		if (!perch.holder().maySeal(level, player, false)) {
			notes.add(line("missing.owner"));
		}
		Definitions.Research entry = ConcordanceData.rules().research(definition.research());
		if (entry != null) {
			for (Definitions.Requirement requirement : entry.requires()) {
				if (!ConcordanceProgress.knowledge(player).state(requirement.research()).atLeast(requirement.state())) {
					notes.add(line("missing.prerequisite", ConcordanceProgress.researchName(requirement.research())));
				}
			}
		}
		ResearchState has = ConcordanceProgress.knowledge(player).state(definition.research());
		for (ResearchState step : List.of(ResearchState.ENCOUNTERED, ResearchState.OBSERVED, ResearchState.UNDERSTOOD)) {
			if (!has.atLeast(step)) {
				Identifier advancement = ConcordanceProgress.advancementFor(definition.research(), step);
				notes.add(line("missing.step", ConcordanceProgress.researchName(definition.research()),
						Component.translatable("research_state.jugcraft." + step.id()),
						Component.translatable("advancements." + advancement.getNamespace() + "." + advancement.getPath() + ".description")));
			}
		}
		if (!observed(level, player)) {
			notes.add(line("missing.observation"));
		}
		if (Pacts.holds(BoundWills.of(level.getServer()).ledger(), player.getUUID(), definition.id())) {
			notes.add(line("missing.held"));
		}
		return notes;
	}

	/** What accepting {@code d} binds its reader to (D-S15): every term, the compact's cost in words. */
	static List<PactStatePayload.Term> offer(PactDefinition d) {
		List<PactStatePayload.Term> terms = new ArrayList<>(service(d.work(), d.radius(), d.from(), d.to(), d.quota()));
		terms.addAll(upkeep(d.offering(), d.count(), d.period(), d.grace(), d.cap()));
		terms.addAll(standing(d.beneficiaries()));
		terms.add(new PactStatePayload.Term("compact", line("term.compact", d.resonance(),
				ComponentUtils.formatList(d.compact(), cost -> line("term.compact_item", cost.count(), itemName(cost.item()))))));
		return terms;
	}

	/** A sealed pact's terms, as copied onto its record at sealing (D-S23): a reload never changes them. */
	static List<PactStatePayload.Term> terms(BoundWill record) {
		BoundWill.Pact pact = record.pact();
		List<PactStatePayload.Term> terms = new ArrayList<>();
		BoundWill.Service service = pact.service();
		if (service != null) {
			terms.addAll(service(service.work(), service.radius(), service.from(), service.to(), service.quota()));
		}
		BoundWill.Upkeep upkeep = pact.upkeep();
		if (upkeep != null) {
			terms.addAll(upkeep(upkeep.offering(), upkeep.count(), upkeep.period(), upkeep.grace(), upkeep.cap()));
		}
		terms.addAll(standing(pact.beneficiaries()));
		terms.add(new PactStatePayload.Term("compact", line("term.compact_paid")));
		return terms;
	}

	private static List<PactStatePayload.Term> service(String work, int radius, int from, int to, int quota) {
		return List.of(new PactStatePayload.Term("service", line("term.service." + work)),
				new PactStatePayload.Term("hours", from == 0 && to == 24000 ? line("term.hours.any") : line("term.hours", Workers.clock(from), Workers.clock(to))),
				new PactStatePayload.Term("area", line("term.area", radius)), new PactStatePayload.Term("quota", line("term.quota", quota)));
	}

	private static List<PactStatePayload.Term> upkeep(String offering, int count, long period, long grace, int cap) {
		return List.of(new PactStatePayload.Term("upkeep", line("term.upkeep", count, AgreementHolder.offeringName(offering), period / Pacts.DAY, cap)),
				new PactStatePayload.Term("grace", line("term.grace", grace / Pacts.DAY)));
	}

	private static List<PactStatePayload.Term> standing(String beneficiaries) {
		List<PactStatePayload.Term> terms = new ArrayList<>();
		for (String key : List.of("suspension", "duration", "breach", "ending", "returns", "perch")) {
			terms.add(new PactStatePayload.Term(key, line("term." + key)));
		}
		terms.add(new PactStatePayload.Term("beneficiaries", line("term.beneficiaries." + beneficiaries)));
		return terms;
	}

	private static List<Component> beats(PactDefinition d) {
		String name = d.id().substring(d.id().indexOf(':') + 1);
		return d.beats().stream().map(beat -> line(name + ".beat." + beat)).toList();
	}

	private static Component itemName(String id) {
		Identifier key = Identifier.tryParse(id);
		Item item = key == null ? null : BuiltInRegistries.ITEM.getOptional(key).orElse(null);
		return item == null ? Component.translatableWithFallback("item." + id.replace(':', '.'), id) : new ItemStack(item).getHoverName();
	}

	private PactStatePayload answer(ServerLevel level, ServerPlayer player, @Nullable Answered answered, int answering, long now) {
		PactStatePayload state = state(level, player, answered, answering, now);
		lastSent = state;
		if (ServerPlayNetworking.canSend(player, PactStatePayload.TYPE)) {
			ServerPlayNetworking.send(player, state);
		}
		return state;
	}

	/** The state this menu shows {@code player} now, answering request {@code answering}: built, not sent. */
	private PactStatePayload state(ServerLevel level, ServerPlayer player, @Nullable Answered answered, int answering, long now) {
		View view = view(level, player, now);
		patternIds = view.patternIds();
		offeringItems = view.offeringItems();
		return new PactStatePayload(containerId, revision, view.mode(), PactKeeping.name(PactDefinition.STOLAS), view.beats(),
				view.terms(), view.notes(), view.patterns(), view.offerings(), view.compact(), view.resonance(), view.actions(),
				Optional.ofNullable(answered).map(done -> new PactStatePayload.Result(answering, done.reason(), done.done(), done.said())));
	}

	private static void refreshAll(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.containerMenu instanceof PactMenu menu && menu.perch != null && menu.stillValid(player)) {
				menu.answer(player.level(), player, null, menu.sequence, player.level().getGameTime());
			}
		}
	}

	private static Component line(String key, Object... args) {
		return Component.translatable("message.jugcraft.concordance.pact." + key, args);
	}
}
