package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ComposedSpells;
import io.github.jimbozoomer.jugcraft.concordance.Composing;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceSpells;
import io.github.jimbozoomer.jugcraft.concordance.Inscription;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.compose.Catalog;
import io.github.jimbozoomer.jugcraft.concordance.compose.Compiler;
import io.github.jimbozoomer.jugcraft.concordance.compose.Component;
import io.github.jimbozoomer.jugcraft.concordance.compose.Draft;
import io.github.jimbozoomer.jugcraft.concordance.compose.Grammar;
import io.github.jimbozoomer.jugcraft.concordance.compose.Instrument;
import io.github.jimbozoomer.jugcraft.concordance.compose.Plan;
import io.github.jimbozoomer.jugcraft.concordance.compose.Slot;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.composer.Composer;
import io.github.jimbozoomer.jugcraft.concordance.composer.ComposerRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.composer.ComposerResultPayload;
import io.github.jimbozoomer.jugcraft.concordance.composer.ComposerStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.target.SpellTarget;

/**
 * Roadmap step 8, the spell compiler: the grammar loads from data; compositions compile to the numbers the codex
 * states, or name their exact problem; a spell is composed and inscribed by command, cast through Spell Engine's
 * carrier spell and settled once; the server compiles the inscription again (a forged cost is ignored); and a running
 * spell stays inside its compiled limits however many creatures it meets. Since Chunk A (A-01), the command and the
 * composer screen are answered by one service, {@link Composing}.
 */
public class ConcordanceComposeGameTests {
	private static final String FIRST_LIGHT = "jugcraft:first_light";
	/**
	 * Ticks to let newly spawned creatures settle before a spell searches for them: tests run in freshly loaded chunks,
	 * where an area search may not see a creature in its first tick (a reference to it always does).
	 */
	private static final int SETTLE_TICKS = 10;

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** A survival player with the Initiate's Wand, standing on {@code standAt} and facing south (+Z). */
	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 0.0F, 0.0F);
		// A living entity looks where its head turns; a mock player's head is not turned by snapTo.
		player.setYHeadRot(0.0F);
		player.setYBodyRot(0.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
		RateGate.forget(player.getUUID());
		return player;
	}

	private static Compiler.Knows knows(ResearchState state) {
		return research -> FIRST_LIGHT.equals(research) ? state : ResearchState.NONE;
	}

	private static Instrument wand() {
		return ConcordanceData.rules().catalog().instrumentFor("jugcraft:initiate_wand");
	}

	private static boolean names(Compiler.Compilation compiled, String key) {
		return !compiled.ok() && compiled.problems().stream().map(Text::key).anyMatch(key::equals);
	}

	private static Holder<Spell> composed(ServerLevel level) {
		return SpellRegistry.from(level).get(Identifier.parse(ComposedSpells.SPELL)).orElseThrow();
	}

	private static void refreshSpells(ServerPlayer player) {
		SpellContainerSource.setDirty(player, ConcordanceSpells.SOURCE);
		SpellContainerSource.setDirty(player, "main_hand");
		SpellContainerSource.update(player);
	}

	private static void cast(ServerLevel level, ServerPlayer player) {
		SpellExecution.performSpell(level, player, composed(level), SpellTarget.SearchResult.empty(), SpellCast.Action.RELEASE, 1.0F);
	}

	private static void command(ServerPlayer player, String command) {
		player.level().getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withSuppressedOutput(), command);
	}

	/** The grammar loads whole, the codex examples cost what the codex says, and invalid spells name their problem. */
	@GameTest(maxTicks = 20)
	public void grammarLoadsAndNamesProblems(GameTestHelper helper) {
		Catalog catalog = ConcordanceData.rules().catalog();
		Instrument wand = wand();
		// First Light's grammar; later research adds its own words (Hearthbinding's five, all its invocations' own).
		long firstLight = catalog.components().values().stream()
				.filter(component -> component.requires().research().equals("jugcraft:first_light")).count();
		helper.assertTrue(firstLight == 23 && wand != null && wand.capacity() == 8 && wand.targets() == 6
				&& wand.work() == 48 && wand.branches() == 1 && wand.duration() == 1200,
				"First Light's 23 components (6 of them invocations' own) and the Initiate's Wand load: " + catalog.components().keySet() + " "
						+ wand);
		Object[][] examples = {
			{"touch struck light", ResearchState.UNDERSTOOD, 2, 2, 20},
			{"here struck ward", ResearchState.UNDERSTOOD, 3, 3, 25},
			{"touch struck ward", ResearchState.UNDERSTOOD, 3, 3, 25},
			{"ray struck sear then here creatures dazzle", ResearchState.MASTERED, 8, 8, 50},
		};
		for (Object[] example : examples) {
			Compiler.Compilation compiled = Compiler.compile((String) example[0], catalog, knows((ResearchState) example[1]), wand);
			Plan plan = compiled.plan();
			helper.assertTrue(plan != null && plan.focus() == (int) example[2] && plan.capacity() == (int) example[3]
					&& plan.cooldown() == (int) example[4], example[0] + ": Focus, capacity and cooldown as the codex says: "
					+ (plan == null ? compiled.problems() : plan.focus() + "/" + plan.capacity() + "/" + plan.cooldown()));
		}
		helper.assertTrue(names(Compiler.compile("ray struck sear", catalog, knows(ResearchState.UNDERSTOOD), wand), "problem.research"),
				"Ray and Sear need First Light mastered");
		helper.assertTrue(names(Compiler.compile("touch creatures light", catalog, knows(ResearchState.MASTERED), wand), "problem.selection"),
				"Light acts on blocks, which creatures does not choose");
		helper.assertTrue(names(Compiler.compile("here struck sear", catalog, knows(ResearchState.MASTERED), wand), "problem.harms_caster"),
				"Here struck sear would harm the caster");
		helper.assertTrue(names(Compiler.compile("ray+extend creatures sear+intensify pulse", catalog, knows(ResearchState.MASTERED), wand),
				"problem.capacity"), "Too much for the wand");
		helper.assertTrue(names(Compiler.compile("touch struck light+prolong", catalog, knows(ResearchState.MASTERED), wand), "problem.duration"),
				"Prolonged light outlasts what the wand allows");
		helper.assertTrue(names(Compiler.compile("struck touch light", catalog, knows(ResearchState.MASTERED), wand), "problem.expected"),
				"Words out of order");
		helper.succeed();
	}

	/**
	 * Compose by command, inscribe on the wand, cast through Spell Engine: the light is set, the plan's Focus taken once
	 * and its cooldown started. A spell that finds nothing to act on costs nothing; a forged cost on the item is ignored
	 * (the server compiles the text again); a spell the caster's research no longer allows is refused.
	 */
	@GameTest(maxTicks = 40)
	public void inscribeAndCastThroughSpellEngine(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.setBlock(new BlockPos(1, 2, 4), Blocks.STONE);
		helper.setBlock(new BlockPos(1, 3, 4), Blocks.STONE);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		command(player, "jugcraft concordance compose inscribe touch struck light");
		helper.assertTrue(ComposedSpells.inscription(player.getMainHandItem()) == null, "Without First Light nothing can be inscribed");

		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		RateGate.forget(player.getUUID());
		command(player, "jugcraft concordance compose inscribe touch struck light");
		Inscription inscription = ComposedSpells.inscription(player.getMainHandItem());
		helper.assertTrue(inscription != null && inscription.text().equals("touch struck light") && inscription.focus() == 2
				&& inscription.cooldown() == 20, "The wand carries the spell, its Focus and cooldown: " + inscription);

		refreshSpells(player);
		cast(level, player);
		BlockPos lit = helper.absolutePos(new BlockPos(1, 3, 3));
		helper.assertTrue(level.getBlockState(lit).is(JugcraftConcordance.LUMEN_MOTE), "Touch struck light lights the open side of the wall");
		helper.assertTrue(ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 2, "It takes its 2 Focus once: "
				+ ConcordanceProgress.currentFocus(player));
		SpellCaster.Player caster = (SpellCaster.Player) player;
		helper.assertTrue(caster.getCooldownManager().isCoolingDown(composed(level)), "and starts its cooldown");

		// Nothing to act on: no cost, no cooldown.
		caster.getCooldownManager().reset(null);
		RateGate.forget(player.getUUID());
		command(player, "jugcraft concordance compose inscribe touch struck reveal");
		ConcordanceProgress.setFocus(player, FocusPool.MAX);
		refreshSpells(player);
		cast(level, player);
		helper.assertTrue(ConcordanceProgress.currentFocus(player) == FocusPool.MAX && !caster.getCooldownManager().isCoolingDown(composed(level)),
				"Revealing nothing costs nothing and starts no cooldown");

		// A forged inscription: the server compiles the text and charges what it really costs. (A villager is tall
		// enough to meet the line from the caster's eyes.)
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.MASTERED);
		Mob villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 3));
		helper.runAfterDelay(SETTLE_TICKS, () -> {
			ConcordanceProgress.setFocus(player, FocusPool.MAX);
			player.getMainHandItem().set(JugcraftConcordance.INSCRIPTION, new Inscription("touch struck sear", 1, 0));
			refreshSpells(player);
			cast(level, player);
			helper.assertTrue(villager.getHealth() == villager.getMaxHealth() - 4.0F && ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 3,
					"A forged Focus cost is ignored: Sear costs its 3: " + ConcordanceProgress.currentFocus(player) + ", villager " + villager.getHealth());

			// Research taken away: the same inscription no longer compiles, and the cast is refused.
			caster.getCooldownManager().reset(null);
			ConcordanceProgress.reset(player);
			ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
			ConcordanceProgress.setFocus(player, FocusPool.MAX);
			float before = villager.getHealth();
			refreshSpells(player);
			cast(level, player);
			helper.assertTrue(villager.getHealth() == before && ConcordanceProgress.currentFocus(player) == FocusPool.MAX,
					"Without Sear's research the inscription does nothing and costs nothing");
			RateGate.forget(player.getUUID());
			command(player, "jugcraft concordance compose clear");
			helper.assertTrue(ComposedSpells.inscription(player.getMainHandItem()) == null, "compose clear wipes it");
			helper.succeed();
		});
	}

	/**
	 * Chunk A (A-D13): the route's support spell. {@code touch struck ward} is inscribed through the composing service
	 * and cast: the friend touched (a villager here) is warded (Absorption, 4 points), it costs its 3 Focus and starts its
	 * cooldown; turned away, with no one to touch, it costs nothing and starts no cooldown.
	 */
	@GameTest(maxTicks = 40)
	public void aWardForAFriendIsComposedAndCast(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		Composing.Result inscribed = Composing.compose(player, "touch struck ward", true, Composing.Held.of(player));
		Inscription ward = ComposedSpells.inscription(player.getMainHandItem());
		helper.assertTrue(inscribed.outcome() == Composing.Outcome.INSCRIBED && ward != null && ward.focus() == 3 && ward.cooldown() == 25,
				"The ward for a friend is inscribed with its cost: " + inscribed.outcome() + " " + ward);
		Mob villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 3));
		helper.runAfterDelay(SETTLE_TICKS, () -> {
			ConcordanceProgress.setFocus(player, FocusPool.MAX);
			refreshSpells(player);
			cast(level, player);
			SpellCaster.Player caster = (SpellCaster.Player) player;
			helper.assertTrue(villager.hasEffect(MobEffects.ABSORPTION) && villager.getEffect(MobEffects.ABSORPTION).getAmplifier() == 0,
					"The friend touched is warded: " + villager.getEffect(MobEffects.ABSORPTION));
			helper.assertTrue(ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 3 && caster.getCooldownManager().isCoolingDown(composed(level)),
					"It costs its 3 Focus and starts its cooldown: " + ConcordanceProgress.currentFocus(player));

			// Turned away: nothing to touch, so nothing is spent.
			caster.getCooldownManager().reset(null);
			ConcordanceProgress.setFocus(player, FocusPool.MAX);
			BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
			player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180.0F, 0.0F);
			player.setYHeadRot(180.0F);
			player.setYBodyRot(180.0F);
			refreshSpells(player);
			cast(level, player);
			helper.assertTrue(ConcordanceProgress.currentFocus(player) == FocusPool.MAX && !caster.getCooldownManager().isCoolingDown(composed(level)),
					"Warding no one costs nothing and starts no cooldown");
			helper.succeed();
		});
	}

	/**
	 * Chunk A, A-01: the command and the composer screen are one service. Each refusal comes in its order (the
	 * Concordance off, then no instrument, then the rate gate the command shares) and writes nothing; an unknown word,
	 * over-long text and an illegal branch are invalid with their problem named; a valid spell is checked without being
	 * written, then inscribed in its canonical form; a screen's inscription is refused, writing nothing, once the
	 * instrument in hand or the rules have changed since the screen showed them; a screen's clear wipes it; and what the
	 * command writes is what the service shows.
	 */
	@GameTest(maxTicks = 20)
	public void oneServiceAnswersCommandAndScreen(GameTestHelper helper) {
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		String light = "touch struck light";

		// The Concordance off: refused first, whatever is held (switched back at once; the server runs other tests).
		boolean on = JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
		JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, false);
		Composing.Result off;
		try {
			off = Composing.compose(player, light, true, Composing.Held.of(player));
		} finally {
			JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, on);
		}
		helper.assertTrue(off.outcome() == Composing.Outcome.DISABLED && ComposedSpells.inscription(player.getMainHandItem()) == null,
				"With the Concordance off nothing is composed: " + off.outcome());

		// No instrument in hand.
		ItemStack wand = player.getMainHandItem();
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		Composing.Result bare = Composing.compose(player, light, true, null);
		player.setItemInHand(InteractionHand.MAIN_HAND, wand);
		helper.assertTrue(bare.outcome() == Composing.Outcome.NO_INSTRUMENT && bare.instrument() == null,
				"Bare hands hold no instrument: " + bare.outcome());

		// Checked, not written; then the command, straight after, meets the same gate and writes nothing.
		Composing.Result checked = Composing.compose(player, light, false, null);
		helper.assertTrue(checked.outcome() == Composing.Outcome.VALID && checked.plan() != null && checked.plan().focus() == 2
				&& checked.text().equals(light) && !checked.lines().isEmpty() && ComposedSpells.inscription(wand) == null,
				"A valid spell is checked and explained, and nothing is written: " + checked.outcome() + " " + checked.problems());
		command(player, "jugcraft concordance compose inscribe " + light);
		Composing.Result fast = Composing.compose(player, light, false, null);
		helper.assertTrue(ComposedSpells.inscription(wand) == null && fast.outcome() == Composing.Outcome.TOO_FAST,
				"Command and screen share one rate: the command just after writes nothing, and the next check waits: " + fast.outcome());

		// Invalid texts name their problem and write nothing.
		String[][] invalid = {
			{"touch struck glitter", "problem.unknown"},
			{"touch ".repeat(43) + "struck light", "problem.too_long"},
			{"touch struck light then touch struck light", "problem.branch_delivery"},
		};
		for (String[] example : invalid) {
			RateGate.forget(player.getUUID());
			Composing.Result result = Composing.compose(player, example[0], true, Composing.Held.of(player));
			helper.assertTrue(result.outcome() == Composing.Outcome.INVALID && result.plan() == null
					&& result.problems().stream().map(Text::key).anyMatch(example[1]::equals) && result.lines().equals(result.problems())
					&& ComposedSpells.inscription(wand) == null, example[1] + " is named and nothing is written: " + result.problems());
		}

		// A screen inscribes the wand it showed, in canonical form.
		RateGate.forget(player.getUUID());
		Composing.Held shown = Composing.Held.of(player);
		Composing.Result inscribed = Composing.compose(player, "  Touch  struck   LIGHT ", true, shown);
		Inscription written = ComposedSpells.inscription(player.getMainHandItem());
		helper.assertTrue(inscribed.outcome() == Composing.Outcome.INSCRIBED && written != null && written.text().equals(light)
				&& written.focus() == 2 && written.cooldown() == 20, "Inscribed in canonical form with its Focus and cooldown: " + written);

		// What that screen showed no longer holds: the wand has changed.
		RateGate.forget(player.getUUID());
		Composing.Result stale = Composing.compose(player, "touch struck reveal", true, shown);
		helper.assertTrue(stale.outcome() == Composing.Outcome.CHANGED && light.equals(ComposedSpells.inscription(player.getMainHandItem()).text()),
				"A screen that showed the wand before it changed writes nothing: " + stale.outcome());

		// Another wand swapped in before the commit is left as it is.
		Composing.Held showing = Composing.Held.of(player);
		ItemStack inscribedWand = player.getMainHandItem();
		ItemStack other = new ItemStack(JugcraftConcordance.INITIATE_WAND);
		player.setItemInHand(InteractionHand.MAIN_HAND, other);
		Composing.Result swapped = Composing.compose(player, "touch struck reveal", true, showing);
		helper.assertTrue(swapped.outcome() == Composing.Outcome.CHANGED && ComposedSpells.inscription(other) == null,
				"A wand swapped in after the screen showed another is not written on: " + swapped.outcome());

		// The same wand back holds again, until the rules are reloaded.
		player.setItemInHand(InteractionHand.MAIN_HAND, inscribedWand);
		helper.assertTrue(showing.holds(player), "The wand the screen showed, back in hand, is what it showed");
		ConcordanceData.replace(ConcordanceData.rules());
		Composing.Result reloaded = Composing.compose(player, "touch struck reveal", true, showing);
		helper.assertTrue(reloaded.outcome() == Composing.Outcome.CHANGED && light.equals(ComposedSpells.inscription(player.getMainHandItem()).text()),
				"After a reload, a screen showing the old rules writes nothing: " + reloaded.outcome());

		// A screen's clear, then the command's finds nothing to wipe.
		RateGate.forget(player.getUUID());
		Composing.Result cleared = Composing.clear(player, Composing.Held.of(player));
		Composing.Result again = Composing.clear(player);
		helper.assertTrue(cleared.outcome() == Composing.Outcome.CLEARED && again.outcome() == Composing.Outcome.NOTHING_INSCRIBED
				&& ComposedSpells.inscription(player.getMainHandItem()) == null, "Cleared once; then nothing is inscribed: " + again.outcome());

		// What the command writes is what the service shows.
		RateGate.forget(player.getUUID());
		command(player, "jugcraft concordance compose inscribe touch struck reveal");
		Composing.Result shownNow = Composing.show(player);
		Inscription byCommand = ComposedSpells.inscription(player.getMainHandItem());
		helper.assertTrue(byCommand != null && shownNow.outcome() == Composing.Outcome.VALID && shownNow.text().equals(byCommand.text())
				&& shownNow.plan() != null && shownNow.plan().focus() == byCommand.focus(), "The command inscribes through the service: "
						+ byCommand + " / " + shownNow.outcome());
		helper.succeed();
	}

	private static ComposerRequestPayload request(int sequence, int action, int revision, String text) {
		return new ComposerRequestPayload(sequence, action, revision, text);
	}

	private static ComposerStatePayload.Word learned(ComposerStatePayload state, String id) {
		return state.words().stream().filter(word -> word.id().equals(id)).findFirst().orElse(null);
	}

	/**
	 * Chunk A, A-03: the composer's draft writes what the grammar wants. Words chosen from buttons go where their slot
	 * belongs (a new delivery or selection replaces the old one); a modifier joins the chosen word, once; words move and
	 * are removed; a branch is its own level and shows as "then" while empty; typed text that reads as a spell becomes
	 * its words, and any other text is kept as typed for the check to explain; every edit can be undone, at most
	 * Draft.UNDO_LIMIT deep.
	 */
	@GameTest(maxTicks = 20)
	public void theDraftWritesWhatTheGrammarWants(GameTestHelper helper) {
		Catalog catalog = ConcordanceData.rules().catalog();
		Function<String, Slot> slots = id -> {
			Component component = catalog.component(id);
			return component == null ? null : component.slot();
		};
		Draft draft = new Draft();
		draft.add("jugcraft:light", Slot.OPERATION, slots);
		draft.add("jugcraft:touch", Slot.DELIVERY, slots);
		draft.add("jugcraft:struck", Slot.SELECTION, slots);
		helper.assertValueEqual(draft.text(), "touch struck light", "words from buttons, in the grammar's order");
		draft.add("jugcraft:here", Slot.DELIVERY, slots);
		helper.assertValueEqual(draft.text(), "here struck light", "a new delivery replaces the old");
		helper.assertTrue(draft.undo() && draft.text().equals("touch struck light"), "an undo brings it back: " + draft.text());
		draft.choose(0, 2);
		helper.assertTrue(draft.join("jugcraft:prolong") && draft.text().equals("touch struck light+prolong") && !draft.join("jugcraft:prolong"),
				"a modifier joins the chosen word, once: " + draft.text());
		helper.assertTrue(draft.unjoin() && draft.move(-1) && draft.text().equals("touch light struck"), "unjoined, then moved earlier: " + draft.text());
		helper.assertTrue(draft.undo() && draft.text().equals("touch struck light") && draft.index() == 2, "the move undone: " + draft.text());
		helper.assertTrue(draft.branch() && draft.text().equals("touch struck light then") && !draft.canBranch(),
				"an empty branch shows as then, and no second begins until it has words: " + draft.text());
		draft.add("jugcraft:here", Slot.DELIVERY, slots);
		draft.add("jugcraft:dazzle", Slot.OPERATION, slots);
		draft.add("jugcraft:creatures", Slot.SELECTION, slots);
		helper.assertTrue(draft.levels().size() == 2 && draft.text().equals("touch struck light then here creatures dazzle"),
				"the branch's words go in its own level: " + draft.text());
		draft.read("Ray  STRUCK   sear", true);
		helper.assertTrue(draft.text().equals("ray struck sear") && !draft.typed() && draft.levels().getFirst().size() == 3,
				"typed text that reads as a spell becomes its words: " + draft.text());
		draft.read("ray struck # sear", true);
		helper.assertTrue(draft.typed() && draft.text().equals("ray struck # sear"), "other text is kept as typed: " + draft.text());
		helper.assertTrue(draft.undo() && draft.text().equals("ray struck sear") && draft.undo() && draft.text().contains(" then "),
				"and both are undone in turn: " + draft.text());
		draft.read("touch struck light", true);
		draft.choose(0, 1);
		helper.assertTrue(draft.remove() && draft.text().equals("touch light") && draft.index() == 1, "removed, the next word chosen: "
				+ draft.text());
		draft.read("touch struck light then here creatures dazzle", true);
		draft.choose(0, 0);
		helper.assertTrue(draft.remove() && draft.remove() && draft.remove() && draft.index() == -1 && draft.level() == 0
				&& !draft.canBranch(), "the spell's words all removed, its emptied level is where words go next: " + draft.text());
		draft.add("jugcraft:ray", Slot.DELIVERY, slots);
		helper.assertValueEqual(draft.text(), "ray then here creatures dazzle", "a word written into the emptied level");
		Draft full = new Draft();
		int written = 0;
		while (full.add("jugcraft:light", Slot.OPERATION, slots)) {
			written++;
		}
		helper.assertTrue(written > 0 && full.text().length() <= Grammar.MAX_TEXT && full.text().length() > Grammar.MAX_TEXT - "light ".length(),
				"words from buttons stop at the grammar's length: " + full.text().length());
		Draft deep = new Draft();
		for (int i = 0; i < Draft.UNDO_LIMIT + 6; i++) {
			deep.read(i % 2 == 0 ? "touch struck light" : "here struck ward", true);
		}
		int undone = 0;
		while (deep.undo()) {
			undone++;
		}
		helper.assertValueEqual(undone, Draft.UNDO_LIMIT, "edits that can be undone");
		helper.succeed();
	}

	/**
	 * Chunk A, A-02: the composer screen's server side answers as the command does. Opening sends the wand's limits and
	 * only the words the player has learned (the rest are counted, not named), each with what can change it; an answer
	 * names and blames an unknown word, shows each distinct problem once, and carries a valid spell's numbers; a check
	 * within the shared rate waits and the newest is answered at the allowed tick; an inscribe or clear naming an old
	 * revision writes nothing and the screen is sent its state again; and an unknown action is ignored.
	 */
	@GameTest(maxTicks = 60)
	public void theComposerAnswersAsTheCommandDoes(GameTestHelper helper) {
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		Composer.receive(player, request(1, ComposerRequestPayload.OPEN, 0, ""));
		int opened = Composer.revision(player);
		ComposerStatePayload state = Composer.state(player, opened);
		helper.assertTrue(opened >= 1 && !Composer.waiting(player) && state.status() == ComposerStatePayload.READY
				&& state.instrument().map(ComposerStatePayload.InstrumentView::capacity).orElse(0) == 8 && state.inscribed().isEmpty(),
				"It opens on the wand: " + state);
		List<String> words = state.words().stream().map(ComposerStatePayload.Word::id).toList();
		helper.assertTrue(words.size() == 7 && state.locked() == 10 + io.github.jimbozoomer.jugcraft.nouveau.NouveauCatalog.ITEM_ENTRIES.values().stream().filter(e -> e.kind().equals("glyph")).count() && words.contains("jugcraft:touch") && !words.contains("jugcraft:ray")
				&& !words.contains("jugcraft:mend"), "Only the seven words First Light understood teaches are sent; the ten other core words and all Nouveau glyphs are counted, "
						+ "not named, and no invocation's own word: " + words + " and " + state.locked());
		ComposerStatePayload.Word touch = learned(state, "jugcraft:touch");
		ComposerStatePayload.Word light = learned(state, "jugcraft:light");
		ComposerStatePayload.Word prolong = learned(state, "jugcraft:prolong");
		ComposerStatePayload.Word struck = learned(state, "jugcraft:struck");
		helper.assertTrue(touch.aspects() == 1 << Component.Aspect.RANGE.ordinal() && prolong.aspects() == 1 << Component.Aspect.DURATION.ordinal()
				&& (light.aspects() & prolong.aspects()) != 0 && struck.aspects() == 0 && touch.slot() == Slot.DELIVERY.ordinal(),
				"Each word says what can change it: touch its range, light its time (so prolong may join it), struck nothing");

		RateGate.forget(player.getUUID());
		ComposerResultPayload unknown = Composer.result(2, opened, Composing.compose(player, "touch struck glitter", false, null), player);
		helper.assertTrue(unknown.what() == Composing.Outcome.INVALID && unknown.report().blamed().contains("glitter")
				&& unknown.report().summary().isEmpty() && !unknown.report().lines().isEmpty() && unknown.text().equals("touch struck glitter")
				&& unknown.heading().getContents() instanceof TranslatableContents heading && heading.getKey().equals("compose.jugcraft.invalid"),
				"An unknown word is named and blamed: " + unknown.report());
		RateGate.forget(player.getUUID());
		ComposerResultPayload crowded = Composer.result(3, opened, Composing.compose(player, "here allies" + " dazzle".repeat(22), false, null), player);
		helper.assertTrue(crowded.what() == Composing.Outcome.INVALID && !crowded.report().lines().isEmpty()
				&& new HashSet<>(crowded.report().lines()).size() == crowded.report().lines().size()
				&& crowded.report().lines().size() <= ComposerResultPayload.MAX_LINES, "Each distinct problem is shown once: "
						+ crowded.report().lines().size());
		RateGate.forget(player.getUUID());
		ComposerResultPayload valid = Composer.result(4, opened, Composing.compose(player, "touch struck light", false, null), player);
		helper.assertTrue(valid.what() == Composing.Outcome.VALID && valid.text().equals("touch struck light") && valid.report().blamed().isEmpty()
				&& valid.report().summary().map(ComposerResultPayload.Summary::focus).orElse(0) == 2, "A valid spell carries its numbers: "
						+ valid.report().summary());

		// The rate the command shares: the first check is answered, the next waits, and a newer one replaces it.
		RateGate.forget(player.getUUID());
		Composer.receive(player, request(5, ComposerRequestPayload.CHECK, 0, "touch struck light"));
		boolean first = !Composer.waiting(player);
		Composer.receive(player, request(6, ComposerRequestPayload.CHECK, 0, "touch struck glitter"));
		Composer.receive(player, request(7, ComposerRequestPayload.CHECK, 0, "touch struck reveal"));
		helper.assertTrue(first && Composer.waiting(player), "The first check is answered at once; one within the second waits");
		helper.runAfterDelay(Composing.COMPOSE_RATE_TICKS + 2, () -> {
			helper.assertTrue(!Composer.waiting(player), "and is answered at the allowed tick");
			RateGate.forget(player.getUUID());
			int before = Composer.revision(player);
			Composer.receive(player, request(8, ComposerRequestPayload.INSCRIBE, before - 1, "touch struck light"));
			helper.assertTrue(ComposedSpells.inscription(player.getMainHandItem()) == null && Composer.revision(player) == before + 1,
					"An inscribe naming an old revision writes nothing, and the screen is sent the state again");
			RateGate.forget(player.getUUID());
			Composer.receive(player, request(9, ComposerRequestPayload.INSCRIBE, Composer.revision(player), "touch struck light"));
			Inscription written = ComposedSpells.inscription(player.getMainHandItem());
			helper.assertTrue(written != null && written.text().equals("touch struck light") && Composer.revision(player) == before + 2
					&& Composer.state(player, Composer.revision(player)).inscribed().equals("touch struck light"),
					"The latest revision inscribes, and the new state says so: " + written);
			RateGate.forget(player.getUUID());
			Composer.receive(player, request(10, ComposerRequestPayload.CLEAR, before + 1, ""));
			helper.assertTrue(ComposedSpells.inscription(player.getMainHandItem()) != null, "A clear naming an old revision wipes nothing");
			RateGate.forget(player.getUUID());
			Composer.receive(player, request(11, ComposerRequestPayload.CLEAR, Composer.revision(player), ""));
			helper.assertTrue(ComposedSpells.inscription(player.getMainHandItem()) == null, "The latest revision wipes it");
			Composer.receive(player, request(12, 7, Composer.revision(player), "touch struck light"));
			helper.assertTrue(!Composer.waiting(player) && ComposedSpells.inscription(player.getMainHandItem()) == null,
					"An action the server does not know is ignored");
			helper.succeed();
		});
	}

	/** Chunk A, A-02: the composer's three messages come through encoding whole, and an over-long text cannot be sent. */
	@GameTest(maxTicks = 20)
	public void composerMessagesSurviveTheTrip(GameTestHelper helper) {
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
		ComposerRequestPayload request = request(3, ComposerRequestPayload.INSCRIBE, 2, "touch struck light");
		ComposerRequestPayload.CODEC.encode(buffer, request);
		helper.assertTrue(ComposerRequestPayload.CODEC.decode(buffer).equals(request), "the request");
		ComposerStatePayload state = Composer.state(player, 4);
		ComposerStatePayload.CODEC.encode(buffer, state);
		ComposerStatePayload stateBack = ComposerStatePayload.CODEC.decode(buffer);
		helper.assertTrue(stateBack.equals(state) && stateBack.words().size() == 7, "the state: " + stateBack);
		RateGate.forget(player.getUUID());
		ComposerResultPayload result = Composer.result(5, 4, Composing.compose(player, "touch struck light", false, null), player);
		ComposerResultPayload.CODEC.encode(buffer, result);
		ComposerResultPayload back = ComposerResultPayload.CODEC.decode(buffer);
		helper.assertTrue(back.sequence() == 5 && back.what() == Composing.Outcome.VALID && back.text().equals(result.text())
				&& back.report().summary().equals(result.report().summary()) && back.report().lines().size() == result.report().lines().size()
				&& back.heading().getString().equals(result.heading().getString()), "the answer");
		boolean refused = false;
		try {
			ComposerRequestPayload.CODEC.encode(new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess()),
					request(6, ComposerRequestPayload.CHECK, 0, "a".repeat(ComposerRequestPayload.MAX_TEXT + 1)));
		} catch (RuntimeException expected) {
			refused = true;
		}
		helper.assertTrue(refused, "A text past the bound cannot be encoded, so it is never sent or received");
		helper.succeed();
	}

	/**
	 * A spell that pulses among ten pigs reaches only as many as it compiled to (four), whatever wanders in later, and
	 * never spends more work than compiled.
	 */
	@GameTest(maxTicks = 100)
	public void compiledLimitsHoldAtRuntime(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 0));
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.MASTERED);
		List<Mob> pigs = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			pigs.add(helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1 + i % 5, 2, 2 + i / 5)));
		}
		Compiler.Compilation compiled = ComposedSpells.compile(player, wand(), "here creatures dazzle pulse");
		Plan plan = compiled.plan();
		helper.assertTrue(plan != null && plan.limits().targets() == 4 && plan.limits().work() == 12, "Compiled: 4 targets, 12 work: "
				+ (plan == null ? compiled.problems() : plan.limits()));
		helper.runAfterDelay(SETTLE_TICKS, () -> pulseAmongPigs(helper, level, player, plan, pigs));
	}

	private static void pulseAmongPigs(GameTestHelper helper, ServerLevel level, ServerPlayer player, Plan plan, List<Mob> pigs) {
		Ledger ledger = new Ledger(plan.limits());
		boolean applied = ComposedSpells.cast(level, player, plan, Cause.of(player.getUUID(), Cause.Origin.SPELL, ComposedSpells.SPELL,
				ConcordanceEffects.nextSerial()), ledger);
		helper.assertTrue(applied && pigs.stream().filter(pig -> pig.hasEffect(MobEffects.SLOWNESS)).count() == 4
				&& !player.hasEffect(MobEffects.SLOWNESS) && ComposedSpells.lingering(player.getUUID()) == 1,
				"The first pulse dazzles the four nearest pigs, never the caster, and two more pulses wait: "
						+ pigs.stream().filter(pig -> pig.hasEffect(MobEffects.SLOWNESS)).count() + " dazzled");
		// Lift the dazzled pigs out of reach: later pulses meet new pigs, but the spell has spent its four targets.
		List<Mob> first = pigs.stream().filter(pig -> pig.hasEffect(MobEffects.SLOWNESS)).toList();
		for (Mob pig : first) {
			pig.setNoGravity(true);
			pig.teleportTo(pig.getX(), pig.getY() + 30.0, pig.getZ());
		}
		helper.runAfterDelay(50, () -> {
			helper.assertTrue(pigs.stream().filter(pig -> !first.contains(pig)).noneMatch(pig -> pig.hasEffect(MobEffects.SLOWNESS)),
					"No fifth pig is ever dazzled");
			helper.assertTrue(ledger.targets() == 4 && ledger.work() <= plan.limits().work() && ComposedSpells.lingering(player.getUUID()) == 0,
					"The ledger stayed within the plan: " + ledger.targets() + " targets, " + ledger.work() + " work; nothing lingers");
			helper.succeed();
		});
	}

	/** A branch starts where its spell landed, shares its ledger and is still the caster's. */
	@GameTest(maxTicks = 40)
	public void branchesKeepTheirCause(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 0));
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.MASTERED);
		// Close in front, as in the inscription test: a villager is tall enough to meet the line from the eyes.
		Mob target = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 2));
		Mob beside = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(2, 2, 3));
		Compiler.Compilation compiled = ComposedSpells.compile(player, wand(), "touch struck sear then here creatures dazzle");
		Plan plan = compiled.plan();
		helper.assertTrue(plan != null && plan.limits().branches() == 1 && plan.limits().targets() == 5, "Compiled: one branch, five targets: "
				+ (plan == null ? compiled.problems() : plan.limits()));
		helper.runAfterDelay(SETTLE_TICKS, () -> {
			ComposedSpells.Impact landing = ComposedSpells.deliver(level, player, plan.root(), null);
			Ledger ledger = new Ledger(plan.limits());
			helper.assertTrue(ComposedSpells.cast(level, player, plan, Cause.of(player.getUUID(), Cause.Origin.SPELL, ComposedSpells.SPELL,
					ConcordanceEffects.nextSerial()), ledger), "It takes effect: the touch struck " + landing.struck() + " (the villager is "
					+ target.getUUID() + ") at " + landing.point() + "; looking " + player.getViewVector(1.0F));
			helper.assertTrue(target.getLastHurtByMob() == player && target.getHealth() == target.getMaxHealth() - 4.0F,
					"Sear strikes the villager touched, credited to the caster");
			helper.assertTrue(target.hasEffect(MobEffects.SLOWNESS) && beside.hasEffect(MobEffects.SLOWNESS) && !player.hasEffect(MobEffects.SLOWNESS),
					"then the branch dazzles the creatures where it landed, never the caster");
			helper.assertTrue(ledger.branches() == 1 && ledger.targets() <= plan.limits().targets() && ledger.work() <= plan.limits().work(),
					"one branch taken, within the plan's limits");
			helper.succeed();
		});
	}
}
