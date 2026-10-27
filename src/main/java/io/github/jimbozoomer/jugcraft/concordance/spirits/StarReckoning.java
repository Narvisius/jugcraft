package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ComposeText;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Pattern;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchEngine;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstralClaims;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstrolabeItem;
import io.github.jimbozoomer.jugcraft.concordance.sky.Sky;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, D-05 (D-S1, D-S12): Stolas's service, Star-reckoning, and the pact's first obligation. While a pact
 * with Stolas is active, once a game day its holder at the perch (within its area, inside its hours) recalls any sky
 * pattern they observed themselves through the one recall there is (AstrolabeItem.recall): their own astrolabe pays its
 * recall cost, under the same recall:<pattern> claim a master's recall makes, for a recall's two minutes (AstrolabeItem.recall
 * decides), whatever the sky or season. No mastery is needed, nothing need be up and Stolas need not be present (proposed
 * in unit 2). Refused with one reason (Pacts.Reckoning), before anything is spent. No resonance is made. Sealed terms are
 * the record's, so a definition that fails to load changes nothing here (D-S23). Not rate-gated: the perch's menu asks
 * RateGate first.
 */
public final class StarReckoning {
	/** The service's work (the pact definition's service.work; tools/concordance_pacts.py RECKONING_WORK). */
	public static final String WORK = "star_reckoning";
	/** The most patterns one reckoning offers: the screen's bound (PactStatePayload.MAX_CHOICES); the catalog has six. */
	public static final int MAX_CHOICES = 8;
	/** The evidence key the first obligation records under Stolas's Regard (EvidenceRule: practice:<activity>:<key>). */
	public static final String OBLIGATION_KEY = "practice:" + StolasRegard.ACTIVITY + ":" + StolasEntity.COUNTERPART;

	private StarReckoning() {
	}

	/** The patterns {@code player} observed that the catalog still has, in id order, at most MAX_CHOICES. */
	public static List<Pattern> choices(MinecraftServer server, UUID player) {
		List<Pattern> out = new ArrayList<>();
		for (String id : new TreeSet<>(AstralClaims.of(server).observed(player).keySet())) {
			Pattern pattern = Sky.catalog().pattern(id);
			if (pattern != null) {
				out.add(pattern);
				if (out.size() >= MAX_CHOICES) {
					break;
				}
			}
		}
		return out;
	}

	/** {@code player}, at {@code holder}'s place, reckons {@code patternId}: RECALLED, or the one reason not. Nothing is spent, claimed or counted unless RECALLED. */
	public static Pacts.Reckoning reckon(ServerLevel level, ServerPlayer player, AgreementHolder holder, String patternId, long time, long gameTime) {
		if (!Workers.enabled()) {
			return Pacts.Reckoning.DISABLED;
		}
		AgreementHolder.Standing standing = holder.verify(level, gameTime);
		if (standing == AgreementHolder.Standing.NONE) {
			return Pacts.Reckoning.NOTHING;
		}
		if (standing != AgreementHolder.Standing.VALID) {
			return Pacts.Reckoning.CONFLICTING;
		}
		BoundWill record = holder.record(level);
		BoundWill.Service service = record.pact() == null ? null : record.pact().service();
		if (service == null || !WORK.equals(service.work())) {
			return Pacts.Reckoning.NOTHING;
		}
		if (Authority.isStandIn(player) || !record.holder().equals(player.getUUID())) {
			return Pacts.Reckoning.NOT_HOLDER;
		}
		Pacts.Reckoning refused = Pacts.serves(record, Couriers.place(player.level(), player.blockPosition()), time, gameTime);
		if (refused != null) {
			return refused;
		}
		Pattern pattern = Sky.catalog().pattern(patternId);
		if (pattern == null || !AstralClaims.of(level.getServer()).observed(player.getUUID()).containsKey(patternId)) {
			return Pacts.Reckoning.NOT_OBSERVED;
		}
		ItemStack astrolabe = PactCompact.astrolabe(player, pattern.recallCost());
		if (astrolabe == null) {
			return PactCompact.astrolabe(player, 0) == null ? Pacts.Reckoning.NO_ASTROLABE : Pacts.Reckoning.TOO_LITTLE;
		}
		AstrolabeItem.Recall recall = AstrolabeItem.recall(player, level, astrolabe, pattern, time, gameTime);
		if (recall != AstrolabeItem.Recall.RECALLED) {
			return recall == AstrolabeItem.Recall.TOO_LITTLE ? Pacts.Reckoning.TOO_LITTLE : Pacts.Reckoning.RECALLED_ALREADY;
		}
		holder.taskDone(level, gameTime);
		return Pacts.Reckoning.RECALLED;
	}

	/**
	 * The pact's first obligation (D-S12, proposed in unit 2), kept when ObservatoryBlockEntity.observe has just recorded an
	 * observation by {@code player}: if their pact with Stolas is active and the Concordance on, it records the practice
	 * jugcraft:pact_service keyed by its counterpart, which masters Stolas's Regard. Once a player: never again after the
	 * key is recorded or the entry mastered, so the Conclave hears it once. Another's observation, or one while the pact is
	 * suspended or without its perch (unseated, or stranded: its perch broken while the Concordance was off), keeps nothing.
	 */
	public static void observed(ServerPlayer player) {
		if (!Workers.enabled() || Authority.isStandIn(player) || kept(player)) {
			return;
		}
		MinecraftServer server = player.level().getServer();
		BoundWill record = Pacts.pactWith(BoundWills.of(server).ledger(), player.getUUID(), StolasEntity.COUNTERPART);
		if (record == null || !Pacts.serving(record) || AgreementHolder.stranded(server, record)) {
			return;
		}
		ResearchEngine.Result result = ConcordanceProgress.record(player, new Evidence.Practiced(StolasRegard.ACTIVITY, Pacts.counterpart(record.counterpart())));
		if (result.recorded()) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.pact.obligation_kept", PactKeeping.name(record.counterpart())));
		}
	}

	/** Whether {@code player} has kept the first obligation (its key recorded) or mastered Stolas's Regard otherwise. */
	public static boolean kept(ServerPlayer player) {
		Knowledge.Progress progress = ConcordanceProgress.knowledge(player).progress(StolasRegard.RESEARCH);
		return progress.state().atLeast(ResearchState.MASTERED) || progress.evidence().containsKey(OBLIGATION_KEY);
	}

	/** What a reckoning tells its holder; {@code pattern} and {@code service} may be null when not known. */
	public static Component told(Pacts.Reckoning reckoning, @Nullable Pattern pattern, BoundWill.@Nullable Service service) {
		Component name = pattern == null ? Component.empty() : Sky.patternName(pattern.id());
		return switch (reckoning) {
			case RECALLED -> Component.translatable("message.jugcraft.concordance.pact.reckoning.recalled", name);
			case DISABLED -> Component.translatable("message.jugcraft.concordance.disabled");
			case NOTHING -> Component.translatable("message.jugcraft.concordance.pact.reckoning.nothing");
			case CONFLICTING -> ComposeText.show(Diagnostic.of(Code.CONFLICTING_AGREEMENT).text());
			case NOT_HOLDER -> Component.translatable("message.jugcraft.concordance.pact.reckoning.not_holder");
			case SUSPENDED -> Component.translatable("message.jugcraft.concordance.pact.reckoning.suspended");
			case UPKEEP_HELD -> Component.translatable("message.jugcraft.concordance.pact.reckoning.upkeep_held");
			case OUTSIDE_AREA -> Component.translatable("message.jugcraft.concordance.pact.reckoning.outside_area", service == null ? 0 : service.radius());
			case OUTSIDE_HOURS -> Component.translatable("message.jugcraft.concordance.pact.reckoning.outside_hours",
					Workers.clock(service == null ? 0 : service.from()), Workers.clock(service == null ? 0 : service.to()));
			case QUOTA_SPENT -> Component.translatable("message.jugcraft.concordance.pact.reckoning.quota_spent");
			case NOT_OBSERVED -> Component.translatable("message.jugcraft.concordance.pact.reckoning.not_observed");
			case NO_ASTROLABE -> Component.translatable("message.jugcraft.concordance.pact.reckoning.no_astrolabe");
			case TOO_LITTLE -> Component.translatable("message.jugcraft.concordance.pact.reckoning.too_little", pattern == null ? 0 : pattern.recallCost(), name);
			case RECALLED_ALREADY -> Component.translatable("message.jugcraft.concordance.pact.reckoning.recalled_already", name);
		};
	}
}
